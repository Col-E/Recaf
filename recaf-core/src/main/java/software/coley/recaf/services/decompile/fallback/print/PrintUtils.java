package software.coley.recaf.services.decompile.fallback.print;

import jakarta.annotation.Nonnull;
import org.objectweb.asm.Type;
import software.coley.recaf.info.annotation.Annotated;
import software.coley.recaf.info.annotation.AnnotationElement;
import software.coley.recaf.info.annotation.AnnotationEnumReference;
import software.coley.recaf.info.annotation.AnnotationInfo;
import software.coley.recaf.services.text.TextFormatConfig;
import software.coley.recaf.util.StringUtil;
import software.coley.recaf.util.Types;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Various printing utilities.
 *
 * @author Matt Coley
 */
public class PrintUtils {
	/**
	 * @param format
	 * 		Format config.
	 * @param container
	 * 		Annotation container. Can be a class, field, or method.
	 *
	 * @return String display of the annotations on the given container. Empty string if there are no annotations.
	 */
	@Nonnull
	public static String annotationsToString(@Nonnull TextFormatConfig format, @Nonnull Annotated container) {
		// Skip if there are no annotations.
		List<AnnotationInfo> annotations = container.getAnnotations();
		if (annotations.isEmpty())
			return "";

		// Print all annotations.
		StringBuilder sb = new StringBuilder();
		for (AnnotationInfo annotation : annotations)
			sb.append(annotationToString(format, annotation)).append('\n');
		sb.setLength(sb.length() - 1); // Cut off ending '\n'
		return sb.toString();
	}

	/**
	 * @param format
	 * 		Format config.
	 * @param annotation
	 * 		Annotation to represent.
	 *
	 * @return String display of the annotation.
	 */
	@Nonnull
	private static String annotationToString(@Nonnull TextFormatConfig format, @Nonnull AnnotationInfo annotation) {
		String annotationDesc = annotation.getDescriptor();
		if (Types.isValidDesc(annotationDesc)) {
			Map<String, AnnotationElement> elements = annotation.getElements();
			String annotationName = StringUtil.shortenPath(Type.getType(annotationDesc).getInternalName());
			StringBuilder sb = new StringBuilder("@");
			sb.append(format.filterEscape(annotationName));
			if (!elements.isEmpty()) {
				if (elements.size() == 1 && elements.get("value") != null) {
					// If we only have 'value' we can ommit the 'k=' portion of the standard 'k=v'
					AnnotationElement element = elements.values().iterator().next();
					sb.append("(").append(elementToString(format, element)).append(")");
				} else {
					// Print all args in k=v format
					String args = elements.entrySet().stream()
							.map(e -> e.getKey() + " = " + elementToString(format, e.getValue()))
							.collect(Collectors.joining(", "));
					sb.append("(").append(args).append(")");
				}
			}
			return sb.toString();
		} else {
			return "// Invalid annotation removed";
		}
	}

	/**
	 * @param format
	 * 		Format config.
	 * @param element
	 * 		Annotation element to represent.
	 *
	 * @return String display of the annotation element.
	 */
	@Nonnull
	public static String elementToString(@Nonnull TextFormatConfig format, @Nonnull AnnotationElement element) {
		Object value = element.getElementValue();
		return elementValueToString(format, value);
	}

	/**
	 * @param format
	 * 		Format config.
	 * @param value
	 * 		Annotation element value to represent.
	 *
	 * @return String display of the element value.
	 */
	@Nonnull
	private static String elementValueToString(@Nonnull TextFormatConfig format, @Nonnull Object value) {
		switch (value) {
			case String str -> {
				// String value
				return '"' + str + '"';
			}
			case Type type -> {
				// Class value
				return format.filter(type.getInternalName()) + ".class";
			}
			case AnnotationInfo subAnnotation -> {
				// Annotation value
				return annotationToString(format, subAnnotation);
			}
			case AnnotationEnumReference enumReference -> {
				// Enum value
				String enumType = Type.getType(enumReference.getDescriptor()).getInternalName();
				return format.filter(enumType) + '.' + enumReference.getValue();
			}
			case List<?> list -> {
				// List of values
				String elements = list.stream()
						.map(e -> elementValueToString(format, e))
						.collect(Collectors.joining(", "));
				return "{ " + elements + " }";
			}
			default -> {
				// Primitive
				return value.toString();
			}
		}
	}
}