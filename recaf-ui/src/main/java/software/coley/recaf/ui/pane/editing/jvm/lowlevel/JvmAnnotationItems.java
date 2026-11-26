package software.coley.recaf.ui.pane.editing.jvm.lowlevel;

import jakarta.annotation.Nonnull;
import javafx.scene.Node;
import software.coley.cafedude.classfile.annotation.Annotation;
import software.coley.cafedude.classfile.annotation.AnnotationElementValue;
import software.coley.cafedude.classfile.annotation.ArrayElementValue;
import software.coley.cafedude.classfile.annotation.ClassElementValue;
import software.coley.cafedude.classfile.annotation.ElementValue;
import software.coley.cafedude.classfile.annotation.EnumElementValue;
import software.coley.cafedude.classfile.annotation.PrimitiveElementValue;
import software.coley.cafedude.classfile.annotation.Utf8ElementValue;
import software.coley.cafedude.classfile.constant.CpUtf8;
import software.coley.recaf.util.Icons;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Registers annotation and element-value values for low-level class-file trees.
 *
 * @author Matt Coley
 * @see JvmLowLevelTreeBuilder
 */
public final class JvmAnnotationItems {
	private JvmAnnotationItems() {}

	/**
	 * Registers annotation model handlers.
	 *
	 * @param registry
	 * 		Tree item registry.
	 * @param context
	 * 		Per-build rendering context.
	 */
	static void register(@Nonnull ClassItemRegistry registry, @Nonnull JvmLowLevelContext context) {
		registry.register(Annotation.class)
				.text(annotation -> annotation.getType().getText())
				.graphic(annotation -> Icons.getIconView(Icons.ANNOTATION))
				.children((item, annotation) -> {
					registry.add(item, "Type", annotation.getType());
					Map<CpUtf8, ElementValue> values = annotation.getValues();
					ClassItem valuesItem = registry.add(item, "Values", values, definition -> definition
							.text(map -> "[" + map.size() + "]")
							.graphic(map -> Icons.getIconView(Icons.ARRAY)));
					for (Map.Entry<CpUtf8, ElementValue> entry : values.entrySet()) {
						ClassItem valueItem = registry.add(valuesItem, entry.getKey().getText(), entry.getValue());
						registry.add(valueItem, "Name", entry.getKey());
					}
				});

		registerValue(registry, ElementValue.class, context, context::elementValueToString,
				value -> null, (item, value) -> {
				});
		registerValue(registry, AnnotationElementValue.class, context, context::elementValueToString,
				value -> Icons.getIconView(Icons.ANNOTATION),
				(item, value) -> registry.add(item, "Annotation", value.getAnnotation()));
		registerValue(registry, ArrayElementValue.class, context, context::elementValueToString,
				value -> Icons.getIconView(Icons.ARRAY),
				(item, value) -> {
					List<ElementValue> values = value.getArray();
					ClassItem array = registry.add(item, "Array", values, definition -> definition
							.text(list -> "[" + list.size() + "]")
							.graphic(list -> Icons.getIconView(Icons.ARRAY)));
					for (int i = 0; i < values.size(); i++)
						registry.add(array, "[" + i + "]", values.get(i));
				});
		registerValue(registry, ClassElementValue.class, context, context::elementValueToString,
				value -> context.classGraphic(value.getClassEntry().getText()),
				(item, value) -> registry.add(item, "Class", value.getClassEntry()));
		registerValue(registry, EnumElementValue.class, context, context::elementValueToString,
				value -> Icons.getIconView(Icons.ENUM),
				(item, value) -> {
					registry.add(item, "Type", value.getType());
					registry.add(item, "Name", value.getName());
				});
		registerValue(registry, PrimitiveElementValue.class, context, context::elementValueToString,
				value -> context.cpToGraphic(value.getValue()),
				(item, value) -> registry.add(item, "Value", value.getValue()));
		registerValue(registry, Utf8ElementValue.class, context, context::elementValueToString,
				value -> context.cpToGraphic(value.getValue()),
				(item, value) -> registry.add(item, "Value", value.getValue()));
	}

	@Nonnull
	private static <E extends ElementValue> ClassItemRegistry.Definition<E> registerValue(
			@Nonnull ClassItemRegistry registry,
			@Nonnull Class<E> type,
			@Nonnull JvmLowLevelContext context,
			@Nonnull Function<E, String> text,
			@Nonnull Function<E, Node> graphic,
			@Nonnull BiConsumer<ClassItem, E> children) {
		return registry.register(type)
				.text(text)
				.graphic(graphic)
				.children((item, value) -> registry.add(item, "Tag", value.getTag()))
				.children(children);
	}
}
