package software.coley.recaf.services.deobfuscation.transform.generic;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.Dependent;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import regexodus.Pattern;
import software.coley.recaf.RecafConstants;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.member.ClassMember;
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
import software.coley.recaf.util.kotlin.model.KtVariable;
import software.coley.recaf.util.visitors.SkippingClassVisitor;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 * A transformer that collects kotlin metadata and offers utilities to process it.
 * <p/>
 * For deobfuscating descriptors it is recommended to use the following methods in order:
 * <ol>
 *     <li>{@link #mapKtDescriptor(KtType)} / {@link #mapKtDescriptor(KtFunction)}</li>
 *     <li>{@link #mapToJvm(String)}</li>
 *     <li>{@link #reverseMapDescriptor(String)}</li>
 * </ol>
 *
 * @author Matt Coley
 */
@Dependent
public class KotlinMetadataCollectionTransformer implements JvmClassTransformer {
	private final Map<String, KtClass> kotlinClassModels = new HashMap<>();
	private AggregatedMappings kotlinClassMappings;

	@Override
	public void setup(@Nonnull JvmTransformerContext context, @Nonnull Workspace workspace) {
		kotlinClassMappings = new AggregatedMappings(workspace);
	}

	@Override
	public void transform(@Nonnull JvmTransformerContext context, @Nonnull Workspace workspace,
	                      @Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle,
	                      @Nonnull JvmClassInfo initialClassState) throws TransformationException {
		String ownerName = initialClassState.getName();

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
								kotlinClassMappings.addClass(ownerName, initialClassState.getPackageName() + '/' + sourceName);
							}
						}
					};
				}
			}, ClassReader.SKIP_DEBUG | ClassReader.SKIP_CODE);
			return;
		}

		// The @MetaData class name is the full class descriptor. Add it as-is to the mappings.
		if (ktClass.getName() != null)
			kotlinClassMappings.addClass(ownerName, ktClass.getName());
		kotlinClassModels.put(ownerName, ktClass);
	}

	/**
	 * @param info
	 * 		Class to find matching field within.
	 * @param property
	 * 		Kotlin property model.
	 *
	 * @return Matching field for the property model.
	 */
	@Nullable
	public FieldMember getField(@Nonnull ClassInfo info, @Nonnull KtProperty property) {
		String descriptor = mapKtDescriptor(property.getType());
		List<FieldMember> candidates = getCandidates(descriptor, info, ClassInfo::getFields);

		// No candidates found
		if (candidates.isEmpty())
			return null;

		// Sole candidate found
		if (candidates.size() == 1)
			return candidates.getFirst();

		return null;
	}

	/**
	 * @param info
	 * 		Class to find matching field within.
	 * @param property
	 * 		Kotlin property model.
	 *
	 * @return Matching getter method for the property model.
	 */
	@Nullable
	public MethodMember getFieldGetter(@Nonnull ClassInfo info, @Nonnull KtProperty property) {
		String descriptor = "()" + mapKtDescriptor(property.getType());
		List<MethodMember> candidates = getCandidates(descriptor, info, ClassInfo::getMethods);

		// No candidates found
		if (candidates.isEmpty())
			return null;

		// Sole candidate found
		if (candidates.size() == 1)
			return candidates.getFirst();

		return null;
	}

	/**
	 * @param info
	 * 		Class to find matching field within.
	 * @param function
	 * 		Kotlin function model.
	 *
	 * @return Matching method for the function model.
	 */
	@Nullable
	public MethodMember getMethod(@Nonnull ClassInfo info, @Nonnull KtFunction function) {
		String descriptor = mapKtDescriptor(function);
		List<MethodMember> candidates = getCandidates(descriptor, info, ClassInfo::getMethods);

		// No candidates found
		if (candidates.isEmpty())
			return null;

		// Sole candidate found
		if (candidates.size() == 1)
			return candidates.getFirst();

		return null;
	}

	@Nonnull
	private <T extends ClassMember> List<T> getCandidates(@Nonnull String descriptor, @Nonnull ClassInfo info,
	                                                      @Nonnull Function<ClassInfo, Iterable<T>> memberLookup) {
		String descriptorMapped1 = reverseMapDescriptor(descriptor);
		String descriptorMapped2 = mapToJvm(descriptorMapped1);

		// Count candidates (members matching mapped descriptor)
		Iterable<T> members = memberLookup.apply(info);
		List<T> candidates = null;
		for (T member : members) {
			if (member.getName().charAt(0) == '<')
				continue;
			String memberDescriptor = member.getDescriptor();
			if (memberDescriptor.equals(descriptorMapped1) || memberDescriptor.equals(descriptorMapped2)) {
				if (candidates == null)
					candidates = new ArrayList<>(4);
				candidates.add(member);
			}
		}

		return candidates == null ? Collections.emptyList() : candidates;
	}

	/**
	 * Say you have some obfuscated {@code class c {...}} that has a {@code @Metadata} that
	 * tells you {@code "c" == "FooService"}. This maps {@code c} in descriptors to {@code FooService}.
	 *
	 * @param descriptor
	 * 		Some descriptor.
	 *
	 * @return Descriptor with reverse mappings applied from the collected kotlin metadata.
	 */
	@Nonnull
	public String reverseMapDescriptor(@Nonnull String descriptor) {
		return Objects.requireNonNull(kotlinClassMappings.applyReverseMappings(descriptor));
	}

	/**
	 * @param name
	 * 		Class name.
	 *
	 * @return Kotlin class metadata for the class, if found.
	 */
	@Nullable
	public KtClass getKtClass(@Nonnull String name) {
		return kotlinClassModels.get(name);
	}

	/**
	 * @param name
	 * 		Class name.
	 *
	 * @return Kotlin class name within the metadata, if found.
	 */
	@Nullable
	public String getKtFallbackMapping(@Nonnull String name) {
		return kotlinClassMappings.getMappedClassName(name);
	}

	@Nonnull
	@Override
	public String name() {
		return "Kotlin metadata collection";
	}

	/**
	 * @param function
	 * 		Kotlin function model.
	 *
	 * @return Descriptor of the function model.
	 *
	 * @see #reverseMapDescriptor(String)
	 */
	@Nonnull
	public static String mapKtDescriptor(@Nonnull KtFunction function) {
		StringBuilder sb = new StringBuilder("(");
		for (KtVariable parameter : function.getParameters())
			sb.append(mapKtDescriptor(parameter.getType()));
		sb.append(')').append(mapKtDescriptor(function.getReturnType()));
		return sb.toString();
	}

	/**
	 * @param type
	 * 		Kotlin type model.
	 *
	 * @return Descriptor of the type model.
	 *
	 * @see #reverseMapDescriptor(String)
	 */
	@Nonnull
	public static String mapKtDescriptor(@Nullable KtType type) {
		String descriptor = KtType.toDescriptor(type);

		// Special case handling for types that require knowledge of type arguments to map.
		// Any other cases will be handled later via 'mapToJvm'.
		if (descriptor.equals("Lkotlin/Array;")) {
			List<KtType> arguments = Objects.requireNonNull(type).getArguments();
			if (arguments != null) {
				descriptor = "[" + mapKtDescriptor(arguments.getFirst());
			}
		}

		return descriptor;
	}

	/**
	 * @param kotlinDescriptor
	 * 		Descriptor containing Kotlin std-lib types.
	 *
	 * @return Descriptor with known std-lib type substitutions.
	 *
	 * @see #reverseMapDescriptor(String)
	 */
	@Nonnull
	public static String mapToJvm(@Nonnull String kotlinDescriptor) {
		if (kotlinDescriptor.charAt(0) == '(') {
			Type methodType = Type.getMethodType(kotlinDescriptor);
			StringBuilder sb = new StringBuilder("(");
			for (Type arg : methodType.getArgumentTypes())
				sb.append(mapToJvm(arg.getDescriptor()));
			sb.append(')').append(mapToJvm(methodType.getReturnType().getDescriptor()));
			return sb.toString();
		}
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
			case "Lkotlin/Unit;" -> "V";
			case "Lkotlin/Any;" -> "Ljava/lang/Object;"; // This one isn't a 1-to-1...
			case "Lkotlin/String;" -> "Ljava/lang/String;";
			case "Lkotlin/Enum;" -> "Ljava/lang/Enum;";
			case "Lkotlin/Comparable;" -> "Ljava/lang/Comparable;";
			case "Lkotlin/Comparator;" -> "Ljava/lang/Comparator;";

			// Reflection
			// Some things are inconsistently mapped (like KFunction1/KMutableProperty1), so we can't operate on those.
			case "Lkotlin/reflect/KClass;" -> "Ljava/lang/Class;";

			// Some collections are directly mapped to Java's
			case "Lkotlin/collections/Collection;" -> "Ljava/util/Collection;";
			case "Lkotlin/collections/Iterable;" -> "Ljava/util/Iterable;";
			case "Lkotlin/collections/Iterator;", "Lkotlin/collections/MutableIterator;" -> "Ljava/util/Iterator;";
			case "Lkotlin/collections/ListIterator;",
					"Lkotlin/collections/MutableListIterator;" -> "Ljava/util/ListIterator;";
			case "Lkotlin/collections/List;", "Lkotlin/collections/MutableList;" -> "Ljava/util/List;";
			case "Lkotlin/collections/Set;", "Lkotlin/collections/MutableSet;" -> "Ljava/util/Set;";
			case "Lkotlin/collections/Map;", "Lkotlin/collections/MutableMap;" -> "Ljava/util/Map;";

			// Some function types get migrated
			case "Lkotlin/Function0;" -> "Lkotlin/jvm/functions/Function0;";
			case "Lkotlin/Function1;" -> "Lkotlin/jvm/functions/Function1;";
			case "Lkotlin/Function2;" -> "Lkotlin/jvm/functions/Function2;";
			case "Lkotlin/Function3;" -> "Lkotlin/jvm/functions/Function3;";
			case "Lkotlin/Function4;" -> "Lkotlin/jvm/functions/Function4;";
			case "Lkotlin/Function5;" -> "Lkotlin/jvm/functions/Function5;";
			case "Lkotlin/Function6;" -> "Lkotlin/jvm/functions/Function6;";
			case "Lkotlin/Function7;" -> "Lkotlin/jvm/functions/Function7;";
			case "Lkotlin/Function8;" -> "Lkotlin/jvm/functions/Function8;";
			case "Lkotlin/Function9;" -> "Lkotlin/jvm/functions/Function9;";
			case "Lkotlin/Function10;" -> "Lkotlin/jvm/functions/Function10;";
			case "Lkotlin/FunctionN;" -> "Lkotlin/jvm/functions/FunctionN;";
			default -> kotlinDescriptor;
		};
	}
}
