package me.coley.recaf.decompile.jadx.dexcompat;

import jadx.api.plugins.input.data.annotations.EncodedValue;
import jadx.api.plugins.input.data.annotations.*;
import jadx.api.plugins.input.data.attributes.IJadxAttribute;
import jadx.api.plugins.input.data.attributes.types.*;
import me.coley.recaf.util.logging.Logging;
import org.jf.dexlib2.ValueType;
import org.jf.dexlib2.iface.Annotation;
import org.jf.dexlib2.iface.AnnotationElement;
import org.jf.dexlib2.iface.BasicAnnotation;
import org.jf.dexlib2.iface.value.*;
import org.slf4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

/**
 * JadX utility methods, largely inspired from the java and dex input plugins.
 *
 * @author Matt Coley.
 */
public class DexCompatUtil {
	private static final Logger logger = Logging.get(DexCompatUtil.class);

	public static JadxAnnotation mapAnnotation(BasicAnnotation annotation) {
		String type = annotation.getType();
		Map<String, EncodedValue> elements = new HashMap<>();
		annotation.getElements().forEach(e -> elements.put(e.getName(), mapValue(e.getValue())));
		AnnotationVisibility vis = AnnotationVisibility.RUNTIME;
		if (annotation instanceof Annotation) {
			int index = ((Annotation) annotation).getVisibility();
			if (index > 2)
				index = 2;
			else if (index < 0)
				index = 0;
			vis = AnnotationVisibility.values()[index];
		}
		return new JadxAnnotation(vis, type, elements);
	}

	public static IJadxAttribute mapAnnotationAttribute(String declaring, Annotation annotation) {
		switch (annotation.getType()) {
			case "Ldalvik/annotation/Signature;":
				String sig = extractSignature(annotation);
				if (sig != null)
					return new SignatureAttr(sig);
				break;
			case "Ldalvik/annotation/InnerClass;":
				try {
					String name = getAnnoValue(annotation, "name", EncodedType.ENCODED_STRING, null);
					int accFlags = getAnnoValue(annotation, "accessFlags", EncodedType.ENCODED_INT, 0);
					if (name != null || accFlags != 0) {
						InnerClsInfo innerClsInfo = new InnerClsInfo(declaring, null, name, accFlags);
						return new InnerClassesAttr(Collections.singletonMap(declaring, innerClsInfo));
					}
				} catch (Exception e) {
					logger.warn("Failed to parse annotation: " + annotation, e);
				}
				break;

			case "Ldalvik/annotation/AnnotationDefault;":
				EncodedValue annValue = getAnnoValue(annotation, "value", EncodedType.ENCODED_ANNOTATION, null);
				if (annValue != null) {
					IAnnotation defAnnotation = (IAnnotation) annValue.getValue();
					return new AnnotationDefaultClassAttr(defAnnotation.getValues());
				}
				break;

			case "Ldalvik/annotation/Throws;":
				try {
					for (AnnotationElement element : annotation.getElements()) {
						if (element.getValue() instanceof ArrayEncodedValue) {
							ArrayEncodedValue array = (ArrayEncodedValue) element.getValue();
							List<String> excs = array.getValue().stream()
									.map(DexCompatUtil::mapValue)
									.filter(e -> e instanceof StringEncodedValue)
									.map(e -> (String) e.getValue())
									.collect(Collectors.toList());
							return new ExceptionsAttr(excs);
						}
					}
				} catch (Exception e) {
					logger.warn("Failed to convert dalvik throws annotation", e);
				}
				break;

			case "Ldalvik/annotation/MethodParameters;":
				try {
					List<EncodedValue> names = getAnnoArray(annotation, "names");
					List<EncodedValue> accFlags = getAnnoArray(annotation, "accessFlags");
					if (!names.isEmpty() && names.size() == accFlags.size()) {
						int size = names.size();
						List<MethodParametersAttr.Info> list = new ArrayList<>(size);
						for (int i = 0; i < size; i++) {
							String name = (String) names.get(i).getValue();
							int accFlag = (int) accFlags.get(i).getValue();
							list.add(new MethodParametersAttr.Info(accFlag, name));
						}
						return new MethodParametersAttr(list);
					}
				} catch (Exception e) {
					logger.warn("Failed to parse annotation: " + annotation, e);
				}
				break;
		}
		return null;
	}

	private static <T> List<T> getAnnoArray(Annotation annotation, String name) {
		return getAnnoValue(annotation, name, EncodedType.ENCODED_ARRAY, Collections.emptyList());
	}

	private static <T> T getAnnoValue(Annotation annotation, String name, EncodedType type, Object fallback) {
		Optional<? extends AnnotationElement> optional = annotation.getElements().stream()
				.filter(e -> e.getName().equals(name))
				.findFirst();
		if (optional.isPresent()) {
			EncodedValue jadxValue = mapValue(optional.get().getValue());
			if (jadxValue.getType() == type)
				return (T) jadxValue.getValue();
		}
		return (T) fallback;
	}

	public static EncodedValue mapValue(org.jf.dexlib2.iface.value.EncodedValue value) {
		switch (value.getValueType()) {
			case ValueType.BYTE:
				return new EncodedValue(
						EncodedType.ENCODED_BYTE,
						((ByteEncodedValue) value).getValue());
			case ValueType.SHORT:
				return new EncodedValue(
						EncodedType.ENCODED_SHORT,
						((ShortEncodedValue) value).getValue());
			case ValueType.CHAR:
				return new EncodedValue(
						EncodedType.ENCODED_CHAR,
						((CharEncodedValue) value).getValue());
			case ValueType.INT:
				return new EncodedValue(
						EncodedType.ENCODED_INT,
						((IntEncodedValue) value).getValue());
			case ValueType.LONG:
				return new EncodedValue(
						EncodedType.ENCODED_LONG,
						((LongEncodedValue) value).getValue());
			case ValueType.FLOAT:
				return new EncodedValue(
						EncodedType.ENCODED_FLOAT,
						((FloatEncodedValue) value).getValue());
			case ValueType.DOUBLE:
				return new EncodedValue(
						EncodedType.ENCODED_DOUBLE,
						((DoubleEncodedValue) value).getValue());
			case ValueType.METHOD_TYPE:
				return new EncodedValue(
						EncodedType.ENCODED_METHOD_TYPE,
						((MethodTypeEncodedValue) value).getValue());
			case ValueType.METHOD_HANDLE:
				return new EncodedValue(
						EncodedType.ENCODED_METHOD_HANDLE,
						((MethodHandleEncodedValue) value).getValue());
			case ValueType.STRING:
				return new EncodedValue(
						EncodedType.ENCODED_STRING,
						((StringEncodedValue) value).getValue());
			case ValueType.TYPE:
				return new EncodedValue(
						EncodedType.ENCODED_TYPE,
						((TypeEncodedValue) value).getValue());
			case ValueType.FIELD:
				return new EncodedValue(
						EncodedType.ENCODED_FIELD,
						((FieldEncodedValue) value).getValue());
			case ValueType.METHOD:
				return new EncodedValue(
						EncodedType.ENCODED_METHOD,
						((MethodEncodedValue) value).getValue());
			case ValueType.ENUM:
				return new EncodedValue(
						EncodedType.ENCODED_ENUM,
						((EnumEncodedValue) value).getValue());
			case ValueType.ARRAY:
				List<EncodedValue> list = new ArrayList<>();
				((ArrayEncodedValue) value).getValue().forEach(e -> list.add(mapValue(e)));
				return new EncodedValue(
						EncodedType.ENCODED_ARRAY,
						list);
			case ValueType.ANNOTATION:
				AnnotationEncodedValue annotationEncodedValue = (AnnotationEncodedValue) value;
				return new EncodedValue(EncodedType.ENCODED_ANNOTATION, mapAnnotation(annotationEncodedValue));
			case ValueType.NULL:
				return new EncodedValue(EncodedType.ENCODED_NULL, EncodedValue.NULL);
			case ValueType.BOOLEAN:
				return new EncodedValue(EncodedType.ENCODED_BOOLEAN,
						((BooleanEncodedValue) value).getValue());
			default:
				throw new IllegalStateException("Unsupported value type: " + value.getValueType());
		}
	}

	@SuppressWarnings("unchecked")
	private static String extractSignature(Annotation annotation) {
		Optional<? extends AnnotationElement> element = annotation.getElements().stream()
				.filter(e -> e.getName().equals("value"))
				.findFirst();
		if (element.isPresent()) {
			EncodedValue value = mapValue(element.get().getValue());
			if (value.getValue() instanceof List) {
				List<EncodedValue> values = (List<EncodedValue>) value.getValue();
				if (values.size() == 1)
					return (String) values.get(0).getValue();
				StringBuilder sb = new StringBuilder();
				for (EncodedValue part : values)
					sb.append((String) part.getValue());
				return sb.toString();
			}
		}
		return null;
	}
}
