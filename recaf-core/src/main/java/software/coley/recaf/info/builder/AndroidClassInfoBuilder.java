package software.coley.recaf.info.builder;

import com.android.tools.r8.graph.*;
import com.google.common.collect.Streams;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.AndroidClassInfo;
import software.coley.recaf.info.BasicAndroidClassInfo;
import software.coley.recaf.info.annotation.AnnotationInfo;
import software.coley.recaf.info.annotation.BasicAnnotationElement;
import software.coley.recaf.info.annotation.BasicAnnotationInfo;
import software.coley.recaf.info.member.*;

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
	private DexProgramClass dexClass;

	/**
	 * Create empty builder.
	 */
	public AndroidClassInfoBuilder() {
		super();
	}

	/**
	 * Create a builder with data pulled from the given class.
	 *
	 * @param classInfo
	 * 		Class to pull data from.
	 */
	public AndroidClassInfoBuilder(AndroidClassInfo classInfo) {
		super(classInfo);
	}

	/**
	 * @return Wrapped dex class, if any. Used as a source for information when adapted from.
	 *
	 * @see #adaptFrom(DexProgramClass) Where this value is set.
	 */
	public DexProgramClass getDexClass() {
		return dexClass;
	}

	@Override
	public AndroidClassInfo build() {
		verify();
		return new BasicAndroidClassInfo(this);
	}

	/**
	 * Copies over values by pulling values from the contents of the given class model.
	 *
	 * @param dexClass
	 * 		D8 Class structure to pull data from.
	 *
	 * @return Builder.
	 */
	@Nonnull
	public AndroidClassInfoBuilder adaptFrom(@Nonnull DexProgramClass dexClass) {
		this.dexClass = dexClass;
		withName(dexClass.getTypeName().replace('.', '/'));
		withSuperName(dexClass.getSuperType().getTypeName().replace('.', '/'));
		withInterfaces(dexClass.getInterfaces().stream().map(i -> i.getTypeName().replace('.', '/')).toList());
		withAccess(dexClass.getAccessFlags().getAsCfAccessFlags());
		withSourceFileName(dexClass.getSourceFile() == null ? null : dexClass.getSourceFile().toString());
		withAnnotations(mapAnnos(dexClass.annotations()));
		withFields(mapFields(dexClass.fields()));
		withMethods(mapMethods(dexClass.methods()));
		withSignature(dexClass.getClassSignature().toString());
		InnerClassAttribute innerClasses = dexClass.getInnerClassAttributeForThisClass();
		if (innerClasses != null) {
			DexType outerType = innerClasses.getOuter();
			if (outerType != null) {
				withOuterClassName(outerType.getTypeName().replace('.', '/'));
			}
		}
		if (dexClass.hasEnclosingMethodAttribute()) {
			DexMethod enclosingMethod = dexClass.getEnclosingMethodAttribute().getEnclosingMethod();
			if (enclosingMethod != null) {
				withOuterMethodName(enclosingMethod.getName().toString());
				withOuterMethodDescriptor(enclosingMethod.getProto().toDescriptorString());
				withOuterClassName(enclosingMethod.getHolderType().getTypeName().replace('.', '/'));
			}
		}
		return this;
	}

	@Nonnull
	private List<FieldMember> mapFields(Iterable<DexEncodedField> fields) {
		if (fields == null) return Collections.emptyList();
		return Streams.stream(fields)
				.map(f -> {
					String name = f.getName().toString();
					String desc = f.getType().toDescriptorString();
					String sig = f.getGenericSignature().toString();
					int access = f.accessFlags.getAsCfAccessFlags();
					Object value = unbox(f.getStaticValue());
					BasicFieldMember field = new BasicFieldMember(name, desc, sig, access, value);
					for (AnnotationInfo anno : mapAnnos(f.annotations())) field.addAnnotation(anno);
					return field;
				})
				.collect(Collectors.toList());
	}

	@Nonnull
	private List<MethodMember> mapMethods(@Nullable Iterable<DexEncodedMethod> methods) {
		if (methods == null) return Collections.emptyList();
		return Streams.stream(methods)
				.map(m -> {
					String name = m.getName().toString();
					String desc = m.getProto().toDescriptorString();
					String sig = m.getSignature().toString();
					int access = m.getAccessFlags().getAsCfAccessFlags();
					List<String> thrownTypes = Collections.emptyList();
					BasicMethodMember method = new BasicMethodMember(name, desc, sig, access, thrownTypes);
					for (AnnotationInfo anno : mapAnnos(m.annotations())) method.addAnnotation(anno);
					return method;
				})
				.collect(Collectors.toList());
	}

	@Nonnull
	private static List<AnnotationInfo> mapAnnos(@Nullable DexAnnotationSet anns) {
		if (anns == null) return Collections.emptyList();
		return anns.stream()
				.map(AndroidClassInfoBuilder::mapAnno)
				.collect(Collectors.toList());
	}

	@Nonnull
	private static BasicAnnotationInfo mapAnno(@Nonnull DexAnnotation anno) {
		BasicAnnotationInfo info = new BasicAnnotationInfo(isNonZero(anno.getVisibility()),
				anno.getAnnotationType().getTypeName().replace('.', '/'));
		anno.annotation.forEachElement(element -> {
			String name = element.getName().toString();
			Object unbox = unbox(element.getValue());
			info.addElement(new BasicAnnotationElement(name, unbox));
		});
		return info;
	}

	@Nonnull
	private static BasicAnnotationInfo mapAnno(@Nonnull DexEncodedAnnotation anno) {
		BasicAnnotationInfo info = new BasicAnnotationInfo(true, anno.type.getTypeName().replace('.', '/'));
		for (DexAnnotationElement element : anno.elements) {
			String name = element.getName().toString();
			DexValue value = element.getValue();
			Object unbox = unbox(value);
			info.addElement(new BasicAnnotationElement(name, unbox));
		}
		return info;
	}

	private static Object unbox(DexValue value) {
		if (value instanceof DexValue.DexValueString dexString) {
			return dexString.toString();
		} else if (value instanceof DexValue.DexValueBoolean dexBoolean) {
			return dexBoolean.getValue();
		} else if (value instanceof DexValue.DexValueByte dexByte) {
			return dexByte.getValue();
		} else if (value instanceof DexValue.DexValueChar dexChar) {
			return dexChar.getValue();
		} else if (value instanceof DexValue.DexValueShort dexShort) {
			return dexShort.getValue();
		} else if (value instanceof DexValue.DexValueInt dexInt) {
			return dexInt.getValue();
		} else if (value instanceof DexValue.DexValueFloat dexFloat) {
			return dexFloat.getValue();
		} else if (value instanceof DexValue.DexValueLong dexLong) {
			return dexLong.getValue();
		} else if (value instanceof DexValue.DexValueDouble dexFloat) {
			return dexFloat.getValue();
		} else if (value instanceof DexValue.DexValueAnnotation dexAnnotation) {
			return mapAnno(dexAnnotation.getValue());
		} else if (value instanceof DexValue.DexValueArray dexArray) {
			DexValue[] values = dexArray.getValues();
			Object[] unboxed = new Object[values.length];
			for (int i = 0; i < values.length; i++) {
				unboxed[i] = unbox(values[i]);
			}
			return unboxed;
		} else if (value instanceof DexValue.DexValueEnum dexEnum) {
			DexField field = dexEnum.getValue();
			return field.getHolderType().getTypeName() + " " +
					field.getName() +
					field.getType().toDescriptorString();
		} else if (value instanceof DexValue.DexValueField dexField) {
			DexField field = dexField.getValue();
			return field.getHolderType().getTypeName() + " " +
					field.getName() +
					field.getType().toDescriptorString();
		} else if (value instanceof DexValue.DexValueMethod dexMethod) {
			DexMethod method = dexMethod.getValue();
			return method.getHolderType().getTypeName() + " " +
					method.getName() +
					method.getProto().toDescriptorString();
		} else if (value instanceof DexValue.DexValueMethodHandle dexMethodHandle) {
			return dexMethodHandle.getValue().toAsmHandle(null);
		} else if (value instanceof DexValue.DexValueMethodType dexMethodType) {
			return dexMethodType.getValue().toDescriptorString();
		} else if (value instanceof DexValue.DexValueType dexType) {
			return dexType.getValue().getTypeName();
		} else if (value instanceof DexValue.DexValueNull) {
			return null;
		}
		throw new UnsupportedOperationException("Unsupported dex value type: " + value);
	}
}
