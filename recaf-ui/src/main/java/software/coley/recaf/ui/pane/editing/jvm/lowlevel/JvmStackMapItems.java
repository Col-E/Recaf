package software.coley.recaf.ui.pane.editing.jvm.lowlevel;

import jakarta.annotation.Nonnull;
import javafx.scene.Node;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import software.coley.cafedude.classfile.attribute.StackMapTableAttribute;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.util.Icons;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Registers stack-map frames and verification types for low-level trees.
 *
 * @author Matt Coley
 */
public final class JvmStackMapItems {
	private JvmStackMapItems() {}

	/**
	 * Registers stack-map model handlers.
	 *
	 * @param registry
	 * 		Tree item registry.
	 * @param context
	 * 		Per-build rendering context.
	 */
	static void register(@Nonnull ClassItemRegistry registry, @Nonnull JvmLowLevelContext context) {
		registerType(registry, context, StackMapTableAttribute.TypeInfo.class,
				value -> value.getClass().getSimpleName() + " tag=" + value.getTag(),
				value -> null,
				(item, value) -> {
				});
		registerType(registry, context, StackMapTableAttribute.TopVariableInfo.class,
				JvmStackMapItems::typeText, value -> new FontIconView(CarbonIcons.COPY), (item, value) -> {
				});
		registerType(registry, context, StackMapTableAttribute.IntegerVariableInfo.class,
				JvmStackMapItems::typeText, value -> new FontIconView(CarbonIcons.STRING_INTEGER), (item, value) -> {
				});
		registerType(registry, context, StackMapTableAttribute.FloatVariableInfo.class,
				JvmStackMapItems::typeText, value -> new FontIconView(CarbonIcons.STRING_INTEGER), (item, value) -> {
				});
		registerType(registry, context, StackMapTableAttribute.LongVariableInfo.class,
				JvmStackMapItems::typeText, value -> new FontIconView(CarbonIcons.STRING_INTEGER), (item, value) -> {
				});
		registerType(registry, context, StackMapTableAttribute.DoubleVariableInfo.class,
				JvmStackMapItems::typeText, value -> new FontIconView(CarbonIcons.STRING_INTEGER), (item, value) -> {
				});
		registerType(registry, context, StackMapTableAttribute.NullVariableInfo.class,
				JvmStackMapItems::typeText, value -> new FontIconView(CarbonIcons.CLOSE), (item, value) -> {
				});
		registerType(registry, context, StackMapTableAttribute.UninitializedThisVariableInfo.class,
				JvmStackMapItems::typeText, value -> new FontIconView(CarbonIcons.COPY), (item, value) -> {
				});
		registerType(registry, context, StackMapTableAttribute.ObjectVariableInfo.class,
				JvmStackMapItems::typeText,
				value -> context.classGraphic(value.getClassEntry()),
				(item, value) -> registry.add(item, "Class", value.getClassEntry()));
		registerType(registry, context, StackMapTableAttribute.UninitializedVariableInfo.class,
				JvmStackMapItems::typeText, value -> new FontIconView(CarbonIcons.NUMBER_0),
				(item, value) -> registry.add(item, "Offset", value.getOffset()));

		registerFrame(registry, StackMapTableAttribute.StackMapFrame.class, value -> frameText(value), value -> null,
				(item, value) -> {
				});
		registerFrame(registry, StackMapTableAttribute.SameFrame.class, value -> frameText(value),
				value -> new FontIconView(CarbonIcons.COPY), (item, value) -> {
				});
		registerFrame(registry, StackMapTableAttribute.SameFrameExtended.class, value -> frameText(value),
				value -> new FontIconView(CarbonIcons.COPY), (item, value) -> {
				});
		registerFrame(registry, StackMapTableAttribute.SameLocalsOneStackItem.class, value -> frameText(value),
				value -> new FontIconView(CarbonIcons.COPY),
				(item, value) -> registry.add(item, "Stack", value.getStack()));
		registerFrame(registry, StackMapTableAttribute.SameLocalsOneStackItemExtended.class, value -> frameText(value),
				value -> new FontIconView(CarbonIcons.COPY),
				(item, value) -> registry.add(item, "Stack", value.getStack()));
		registerFrame(registry, StackMapTableAttribute.ChopFrame.class, value -> frameText(value),
				value -> new FontIconView(CarbonIcons.CUT_IN_HALF),
				(item, value) -> registry.add(item, "Absent variables", value.getAbsentVariables()));
		registerFrame(registry, StackMapTableAttribute.AppendFrame.class, value -> frameText(value),
				value -> new FontIconView(CarbonIcons.SUB_VOLUME),
				(item, value) -> addTypeList(registry, item, "Additional locals", value.getAdditionalLocals()));
		registerFrame(registry, StackMapTableAttribute.FullFrame.class, value -> frameText(value),
				value -> new FontIconView(CarbonIcons.STOP),
				(item, value) -> {
					addTypeList(registry, item, "Locals", value.getLocals());
					addTypeList(registry, item, "Stack", value.getStack());
				});
	}

	@Nonnull
	private static <E extends StackMapTableAttribute.TypeInfo> ClassItemRegistry.Definition<E> registerType(
			@Nonnull ClassItemRegistry registry,
			@Nonnull JvmLowLevelContext context,
			@Nonnull Class<E> type,
			@Nonnull Function<E, String> text,
			@Nonnull Function<E, Node> graphic,
			@Nonnull BiConsumer<ClassItem, E> children) {
		return registry.register(type)
				.text(text)
				.graphic(graphic)
				.children((item, value) -> registry.add(item, "Tag", value.getTag()))
				.children(children);
	}

	private static <E extends StackMapTableAttribute.StackMapFrame> void registerFrame(
			@Nonnull ClassItemRegistry registry,
			@Nonnull Class<E> type,
			@Nonnull Function<E, String> text,
			@Nonnull Function<E, Node> graphic,
			@Nonnull BiConsumer<ClassItem, E> children) {
		registry.register(type)
				.text(text)
				.graphic(graphic)
				.children((item, value) -> {
					registry.add(item, "Frame type", value.getFrameType());
					registry.add(item, "Offset delta", value.getOffsetDelta());
				})
				.children(children);
	}

	@Nonnull
	private static String typeText(@Nonnull StackMapTableAttribute.TypeInfo value) {
		return value.getClass().getSimpleName() + " tag=" + value.getTag();
	}

	@Nonnull
	private static String frameText(@Nonnull StackMapTableAttribute.StackMapFrame value) {
		return value.getClass().getSimpleName() + " frame=" + value.getFrameType();
	}

	@Nonnull
	private static ClassItem addTypeList(@Nonnull ClassItemRegistry registry, @Nonnull ClassItem parent,
	                                    @Nonnull String prefix, @Nonnull List<StackMapTableAttribute.TypeInfo> values) {
		ClassItem list = registry.add(parent, prefix, values, definition -> definition
				.text(items -> "[" + items.size() + "]")
				.graphic(items -> Icons.getIconView(Icons.ARRAY)));
		for (int i = 0; i < values.size(); i++)
			registry.add(list, "[" + i + "]", values.get(i));
		return list;
	}
}
