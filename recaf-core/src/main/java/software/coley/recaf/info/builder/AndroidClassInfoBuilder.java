package software.coley.recaf.info.builder;

import com.google.common.collect.Streams;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import me.darknet.dex.tree.definitions.ClassDefinition;
import me.darknet.dex.tree.definitions.InnerClass;
import me.darknet.dex.tree.definitions.MemberIdentifier;
import me.darknet.dex.tree.definitions.annotation.Annotation;
import me.darknet.dex.tree.definitions.annotation.AnnotationPart;
import me.darknet.dex.tree.definitions.constant.AnnotationConstant;
import me.darknet.dex.tree.definitions.constant.ArrayConstant;
import me.darknet.dex.tree.definitions.constant.BoolConstant;
import me.darknet.dex.tree.definitions.constant.ByteConstant;
import me.darknet.dex.tree.definitions.constant.CharConstant;
import me.darknet.dex.tree.definitions.constant.Constant;
import me.darknet.dex.tree.definitions.constant.DoubleConstant;
import me.darknet.dex.tree.definitions.constant.EnumConstant;
import me.darknet.dex.tree.definitions.constant.FloatConstant;
import me.darknet.dex.tree.definitions.constant.HandleConstant;
import me.darknet.dex.tree.definitions.constant.IntConstant;
import me.darknet.dex.tree.definitions.constant.LongConstant;
import me.darknet.dex.tree.definitions.constant.MemberConstant;
import me.darknet.dex.tree.definitions.constant.NullConstant;
import me.darknet.dex.tree.definitions.constant.ShortConstant;
import me.darknet.dex.tree.definitions.constant.StringConstant;
import me.darknet.dex.tree.definitions.constant.TypeConstant;
import org.objectweb.asm.Type;
import software.coley.recaf.info.AndroidClassInfo;
import software.coley.recaf.info.BasicAndroidClassInfo;
import software.coley.recaf.info.BasicInnerClassInfo;
import software.coley.recaf.info.InnerClassInfo;
import software.coley.recaf.info.annotation.AnnotationElement;
import software.coley.recaf.info.annotation.AnnotationInfo;
import software.coley.recaf.info.annotation.BasicAnnotationElement;
import software.coley.recaf.info.annotation.BasicAnnotationEnumReference;
import software.coley.recaf.info.annotation.BasicAnnotationInfo;
import software.coley.recaf.info.member.BasicFieldMember;
import software.coley.recaf.info.member.BasicMethodMember;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.MethodMember;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static software.coley.recaf.util.NumberUtil.isNonZero;

/**
 * Builder for {@link AndroidClassInfo}.
 *
 * @author Matt Coley
 */
public class AndroidClassInfoBuilder extends AbstractClassInfoBuilder<AndroidClassInfoBuilder> {
	private static final Object UNMAPPED_CONSTANT_VALUE = new Object();
	private ClassDefinition def;

	/**
	 * Create empty builder.
	 */
	public AndroidClassInfoBuilder() {
		super();
	}

	/**
	 * Create a builder with data pulled from the given dex class definition.
	 *
	 * @param def
	 * 		Dex class definition to pull data from.
	 */
	public AndroidClassInfoBuilder(@Nonnull ClassDefinition def) {
		super();
		adaptFrom(def);
	}

	/**
	 * Create a builder with data pulled from the given class.
	 *
	 * @param classInfo
	 * 		Class to pull data from.
	 */
	public AndroidClassInfoBuilder(@Nonnull AndroidClassInfo classInfo) {
		super(classInfo);
	}

	/**
	 * @return Wrapped dex class, if any. Used as a source for information when adapted from.
	 *
	 * @see #adaptFrom(ClassDefinition) Where this value is set.
	 */
	public ClassDefinition getDef() {
		return def;
	}

	@Override
	public AndroidClassInfo build() {
		verify();
		return new BasicAndroidClassInfo(this);
	}

	/**
	 * Copies over values by pulling values from the contents of the given class model.
	 *
	 * @param def
	 * 		Dex class structure to pull data from.
	 *
	 * @return Builder.
	 */
	@Nonnull
	public AndroidClassInfoBuilder adaptFrom(@Nonnull ClassDefinition def) {
		this.def = def;
		withName(def.getType().internalName().replace('.', '/'));
		withSuperName(def.getSuperClass() == null ? "java/lang/Object" : def.getSuperClass().internalName().replace('.', '/'));
		withInterfaces(def.getInterfaces().stream().map(i -> i.internalName().replace('.', '/')).toList());
		withAccess(def.getAccess());
		withSourceFileName(def.getSourceFile());
		withAnnotations(mapAnnos(def.getAnnotations()));
		withFields(mapFields(def.getFields().values()));
		withMethods(mapMethods(def.getMethods().values()));
		withSignature(def.getSignature());
		withOuterClassName(def.getEnclosingClass() == null ? null : def.getEnclosingClass().internalName());
		MemberIdentifier enclosingMethod = def.getEnclosingMethod();
		if (enclosingMethod != null) {
			withOuterMethodName(enclosingMethod.name());
			withOuterMethodDescriptor(enclosingMethod.descriptor());
		}
		withInnerClasses(mapInnerClasses(def.getInnerClasses()));
		return this;
	}

	@Nonnull
	private List<FieldMember> mapFields(Iterable<me.darknet.dex.tree.definitions.FieldMember> fields) {
		if (fields == null) return Collections.emptyList();
		return Streams.stream(fields)
				.map(f -> {
					String name = f.getName();
					String desc = f.getType().descriptor();
					String sig = f.getSignature();
					int access = f.getAccess();
					Object value = f.getStaticValue() == null ? null : unbox(f.getStaticValue());
					BasicFieldMember field = new BasicFieldMember(name, desc, sig, access, value);
					for (AnnotationInfo anno : mapAnnos(f.getAnnotations())) field.addAnnotation(anno);
					return field;
				})
				.collect(Collectors.toList());
	}

	@Nonnull
	private List<MethodMember> mapMethods(@Nullable Iterable<me.darknet.dex.tree.definitions.MethodMember> methods) {
		if (methods == null) return Collections.emptyList();
		return Streams.stream(methods)
				.map(m -> {
					String name = m.getName();
					String desc = m.getType().descriptor();
					String sig = m.getSignature();
					int access = m.getAccess();
					List<String> thrownTypes = new ArrayList<>(m.getThrownTypes());
					BasicMethodMember method = new BasicMethodMember(name, desc, sig, access, thrownTypes);
					for (AnnotationInfo anno : mapAnnos(m.getAnnotations())) method.addAnnotation(anno);
					return method;
				})
				.collect(Collectors.toList());
	}

	@Nonnull
	private static List<AnnotationInfo> mapAnnos(@Nullable List<Annotation> anns) {
		if (anns == null) return Collections.emptyList();
		return anns.stream()
				.map(AndroidClassInfoBuilder::mapAnno)
				.collect(Collectors.toList());
	}

	@Nonnull
	private static AnnotationInfo mapAnno(@Nonnull Annotation anno) {
		AnnotationPart part = anno.annotation();
		return mapAnno(isNonZero(anno.visibility()), part);
	}

	@Nonnull
	private static AnnotationInfo mapAnno(boolean visible, @Nonnull AnnotationPart anno) {
		String annoName = 'L' + anno.type().internalName().replace('.', '/') + ';';
		BasicAnnotationInfo info = new BasicAnnotationInfo(visible, annoName);
		anno.elements().forEach((name, constant) -> {
			Object unbox = unbox(constant);
			if (unbox != UNMAPPED_CONSTANT_VALUE)
				info.addElement(new BasicAnnotationElement(name, unbox));
		});
		return info;
	}

	@Nonnull
	private static List<InnerClassInfo> mapInnerClasses(@Nonnull List<InnerClass> inners) {
		return inners.stream().map(AndroidClassInfoBuilder::mapInnerClass).toList();
	}

	@Nonnull
	private static InnerClassInfo mapInnerClass(@Nonnull InnerClass inner) {
		return new BasicInnerClassInfo(inner.outerClassName(),
				inner.innerClassName(),
				inner.outerClassName(),
				inner.innerName(),
				inner.access());
	}

	/**
	 * @param value
	 * 		Value to unbox.
	 *
	 * @return A value compatible with {@link AnnotationElement#getElementValue()}
	 */
	private static Object unbox(Constant value) {
		return switch (value) {
			case AnnotationConstant constant -> mapAnno(true, constant.annotation());
			case ArrayConstant constant -> constant.constants().stream().map(AndroidClassInfoBuilder::unbox).toList();
			case BoolConstant constant -> constant.value();
			case ByteConstant constant -> constant.value();
			case CharConstant constant -> constant.value();
			case DoubleConstant constant -> constant.value();
			case EnumConstant constant ->
					new BasicAnnotationEnumReference(constant.field().descriptor(), constant.field().name());
			case FloatConstant constant -> constant.value();
			case HandleConstant constant -> UNMAPPED_CONSTANT_VALUE;
			case IntConstant constant -> constant.value();
			case LongConstant constant -> constant.value();
			case MemberConstant constant -> UNMAPPED_CONSTANT_VALUE;
			case NullConstant constant -> UNMAPPED_CONSTANT_VALUE;
			case ShortConstant constant -> constant.value();
			case StringConstant constant -> constant.value();
			case TypeConstant constant -> Type.getType(constant.type().descriptor());
		};
	}
}
