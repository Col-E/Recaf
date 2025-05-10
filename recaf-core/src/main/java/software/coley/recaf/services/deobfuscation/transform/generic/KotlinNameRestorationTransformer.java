package software.coley.recaf.services.deobfuscation.transform.generic;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import regexodus.Pattern;
import software.coley.recaf.RecafConstants;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.services.mapping.aggregate.AggregatedMappings;
import software.coley.recaf.services.transform.JvmClassTransformer;
import software.coley.recaf.services.transform.JvmTransformerContext;
import software.coley.recaf.services.transform.TransformationException;
import software.coley.recaf.util.RegexUtil;
import software.coley.recaf.util.kotlin.KotlinMetadata;
import software.coley.recaf.util.kotlin.model.KtClass;
import software.coley.recaf.util.kotlin.model.KtFunction;
import software.coley.recaf.util.kotlin.model.KtProperty;
import software.coley.recaf.util.kotlin.model.KtType;
import software.coley.recaf.util.visitors.SkippingClassVisitor;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.List;
import java.util.Objects;

/**
 * A transformer that renames classes and members based on Kotlin metadata.
 *
 * @author Matt Coley
 */
@Dependent
public class KotlinNameRestorationTransformer implements JvmClassTransformer {

	@Override
	public void transform(@Nonnull JvmTransformerContext context, @Nonnull Workspace workspace,
	                      @Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle,
	                      @Nonnull JvmClassInfo initialClassState) throws TransformationException {
		String ownerName = initialClassState.getName();
		AggregatedMappings mappings = context.getMappings();

		// Extract metadata
		KtClass ktClass = KotlinMetadata.extractKtModel(initialClassState);
		if (ktClass == null) {
			// Check if there is @JvmName as a fallback. The annotation is similar to SourceFileAttribute.
			initialClassState.getClassReader().accept(new SkippingClassVisitor() {
				@Override
				public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
					if (!descriptor.equals("Lkotlin/jvm/JvmName;"))
						return null;
					return new AnnotationVisitor(RecafConstants.getAsmVersion()) {
						private static final Pattern NAME_PATTERN = RegexUtil.pattern("\\w{1, 50}");

						@Override
						public void visit(String name, Object value) {
							if ("name".equals(name) && value instanceof String sourceName && NAME_PATTERN.matches(sourceName)) {
								mappings.addClass(ownerName, initialClassState.getPackageName() + '/' + sourceName);
							}
						}
					};
				}
			}, ClassReader.SKIP_DEBUG | ClassReader.SKIP_CODE);
			return;
		}

		// The @MetaData class name is the full class descriptor. Add it as-is to the mappings.
		if (ktClass.getName() != null)
			mappings.addClass(ownerName, ktClass.getName());

		// Sadly, the kotlin meta-data is unordered, so we can only be sure about name mappings
		// when there are only EXACT descriptor matches.
		for (KtProperty property : ktClass.getProperties()) {
			if (property.getName() == null)
				continue;
			String descriptor = Objects.requireNonNull(mappings.applyReverseMappings(KtType.toDescriptor(property)));
			List<FieldMember> descriptorMatchedProperties = initialClassState.fieldStream()
					.filter(f -> matchDescriptor(f, descriptor))
					.toList();
			if (descriptorMatchedProperties.size() == 1) {
				FieldMember field = descriptorMatchedProperties.getFirst();
				mappings.addField(ownerName, field.getDescriptor(), field.getName(), property.getName());
			}
		}
		for (KtFunction function : ktClass.getFunctions()) {
			if (function.getName() == null)
				continue;
			String descriptor = Objects.requireNonNull(mappings.applyReverseMappings(KtType.toDescriptor(function)));
			List<MethodMember> descriptorMatchedFunctions = initialClassState.methodStream()
					.filter(m -> matchDescriptor(m, descriptor))
					.toList();
			if (descriptorMatchedFunctions.size() == 1) {
				MethodMember method = descriptorMatchedFunctions.getFirst();
				mappings.addMethod(ownerName, method.getDescriptor(), method.getName(), function.getName());
			}
		}
	}

	@Nonnull
	private static String mapToJvm(@Nonnull String kotlinDescriptor) {
		return switch (kotlinDescriptor) {
			case "Lkotlin/Boolean;" -> "Z";
			case "Lkotlin/BooleanArray;" -> "[Z";
			case "Lkotlin/Byte;" -> "B";
			case "Lkotlin/ByteArray;" -> "[B";
			case "Lkotlin/UByte;",
					"Lkotlin/Int;",
					"Lkotlin/UInt;",
					"Lkotlin/UShort;" -> "I";
			case "Lkotlin/UByteArray;",
					"Lkotlin/IntArray;",
					"Lkotlin/UIntArray;",
					"Lkotlin/UShortArray;" -> "[I";
			case "Lkotlin/Char;" -> "C";
			case "Lkotlin/CharArray;" -> "[C";
			case "Lkotlin/Double;" -> "D";
			case "Lkotlin/DoubleArray;" -> "[D";
			case "Lkotlin/Float;" -> "F";
			case "Lkotlin/FloatArray;" -> "[F";
			case "Lkotlin/Long;", "Lkotlin/ULong;" -> "J";
			case "Lkotlin/LongArray;", "Lkotlin/ULongArray;" -> "[J";
			case "Lkotlin/Short;" -> "S";
			case "Lkotlin/ShortArray;" -> "[S";
			case "Lkotlin/String;" -> "Ljava/lang/String;";

			// Some collections are directly mapped to Java's
			case "Lkotlin/collections/List;" -> "Ljava/util/List;";
			case "Lkotlin/collections/Set;" -> "Ljava/util/Set;";
			case "Lkotlin/collections/Map;" -> "Ljava/util/Map;";
			default -> kotlinDescriptor;
		};
	}

	private static boolean matchDescriptor(@Nonnull FieldMember field, @Nonnull String kotlinDescriptor) {
		String fieldDescriptor = field.getDescriptor();
		if (fieldDescriptor.equals(kotlinDescriptor))
			return true;

		// Try checking against the mapped descriptor
		String mappedDescriptor = mapToJvm(kotlinDescriptor);
		return !mappedDescriptor.equals(kotlinDescriptor) && fieldDescriptor.equals(mappedDescriptor);
	}

	private static boolean matchDescriptor(@Nonnull MethodMember method, @Nonnull String kotlinDescriptor) {
		String methodDescriptor = method.getDescriptor();
		if (methodDescriptor.equals(kotlinDescriptor))
			return true;

		// Try checking against the mapped descriptor
		try {
			if (kotlinDescriptor.charAt(0) != '(')
				return false;
			Type methodType = Type.getMethodType(kotlinDescriptor);
			StringBuilder sb = new StringBuilder("(");
			for (Type arg : methodType.getArgumentTypes())
				sb.append(mapToJvm(arg.getDescriptor()));
			sb.append(')').append(mapToJvm(methodType.getReturnType().getDescriptor()));
			String mappedDescriptor = sb.toString();
			return !mappedDescriptor.equals(kotlinDescriptor) && methodDescriptor.equals(mappedDescriptor);
		} catch (Throwable ignored) {
			return false;
		}
	}

	@Nonnull
	@Override
	public String name() {
		return "Kotlin name restoration";
	}
}
