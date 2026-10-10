package software.coley.recaf.ui.pane.editing.jvm.lowlevel;

import jakarta.annotation.Nonnull;
import software.coley.cafedude.classfile.constant.ConstDynamic;
import software.coley.cafedude.classfile.constant.ConstRef;
import software.coley.cafedude.classfile.constant.CpClass;
import software.coley.cafedude.classfile.constant.CpDouble;
import software.coley.cafedude.classfile.constant.CpEntry;
import software.coley.cafedude.classfile.constant.CpFloat;
import software.coley.cafedude.classfile.constant.CpInt;
import software.coley.cafedude.classfile.constant.CpInternal;
import software.coley.cafedude.classfile.constant.CpLong;
import software.coley.cafedude.classfile.constant.CpMethodHandle;
import software.coley.cafedude.classfile.constant.CpMethodType;
import software.coley.cafedude.classfile.constant.CpModule;
import software.coley.cafedude.classfile.constant.CpNameType;
import software.coley.cafedude.classfile.constant.CpPackage;
import software.coley.cafedude.classfile.constant.CpString;
import software.coley.cafedude.classfile.constant.CpUtf8;

/**
 * Registers constant-pool values for low-level class-file trees.
 *
 * @author Matt Coley
 * @see JvmLowLevelTreeBuilder
 */
public final class JvmConstantPoolItems {
	private JvmConstantPoolItems() {}

	/**
	 * Registers constant-pool model handlers.
	 *
	 * @param registry
	 * 		Tree item registry.
	 * @param context
	 * 		Per-build rendering context.
	 */
	static void register(@Nonnull ClassItemRegistry registry, @Nonnull JvmLowLevelContext context) {
		registerBase(registry, context);
		registerConstDynamic(registry, context);
		registerConstRef(registry, context);
		register(registry, context, CpClass.class,
				context::cpToString, context::cpToGraphic,
				(item, value) -> registry.add(item, "Name", value.getName()));
		register(registry, context, CpDouble.class,
				context::cpToString, context::cpToGraphic,
				(item, value) -> registry.add(item, "Value", value.getValue()));
		register(registry, context, CpFloat.class,
				context::cpToString, context::cpToGraphic,
				(item, value) -> registry.add(item, "Value", value.getValue()));
		register(registry, context, CpInt.class,
				context::cpToString, context::cpToGraphic,
				(item, value) -> registry.add(item, "Value", value.getValue()));
		register(registry, context, CpLong.class,
				context::cpToString, context::cpToGraphic,
				(item, value) -> registry.add(item, "Value", value.getValue()));
		register(registry, context, CpMethodHandle.class,
				context::cpToString, context::cpToGraphic,
				(item, value) -> {
					registry.add(item, "Reference kind", value.getKind());
					registry.add(item, "Reference", value.getReference());
				});
		register(registry, context, CpMethodType.class,
				context::cpToString, context::cpToGraphic,
				(item, value) -> registry.add(item, "Descriptor", value.getDescriptor()));
		register(registry, context, CpModule.class,
				context::cpToString, context::cpToGraphic,
				(item, value) -> registry.add(item, "Name", value.getName()));
		register(registry, context, CpNameType.class,
				context::cpToString, context::cpToGraphic,
				(item, value) -> {
					registry.add(item, "Name", value.getName());
					registry.add(item, "Type", value.getType());
				});
		register(registry, context, CpPackage.class,
				context::cpToString, context::cpToGraphic,
				(item, value) -> registry.add(item, "Name", value.getPackageName()));
		register(registry, context, CpString.class,
				context::cpToString, context::cpToGraphic,
				(item, value) -> registry.add(item, "String", value.getString()));
		register(registry, context, CpUtf8.class,
				context::cpToString, context::cpToGraphic,
				(item, value) -> registry.add(item, "Text", value.getText()));
		register(registry, context, CpInternal.class,
				context::cpToString, context::cpToGraphic, (item, value) -> {
				});
	}

	private static void registerBase(@Nonnull ClassItemRegistry registry, @Nonnull JvmLowLevelContext context) {
		register(registry, context, CpEntry.class, context::cpToString, context::cpToGraphic, (item, value) -> {
		});
	}

	private static void registerConstDynamic(@Nonnull ClassItemRegistry registry, @Nonnull JvmLowLevelContext context) {
		register(registry, context, ConstDynamic.class, context::cpToString, context::cpToGraphic, (item, value) -> {
			registry.add(item, "Bootstrap method index", value.getBsmIndex());
			registry.add(item, "Name and type", value.getNameType());
		});
	}

	private static void registerConstRef(@Nonnull ClassItemRegistry registry, @Nonnull JvmLowLevelContext context) {
		register(registry, context, ConstRef.class, context::cpToString, context::cpToGraphic, (item, value) -> {
			registry.add(item, "Class", value.getClassRef());
			registry.add(item, "Name and type", value.getNameType());
		});
	}

	@Nonnull
	private static <E extends CpEntry> ClassItemRegistry.Definition<E> register(@Nonnull ClassItemRegistry registry,
	                                                            @Nonnull JvmLowLevelContext context,
	                                                            @Nonnull Class<E> type,
	                                                            @Nonnull java.util.function.Function<E, String> text,
	                                                            @Nonnull java.util.function.Function<E, javafx.scene.Node> graphic,
	                                                            @Nonnull java.util.function.BiConsumer<ClassItem, E> children) {
		return registry.register(type)
				.text(text)
				.graphic(graphic)
				.children((item, value) -> {
					registry.add(item, "Tag", value.getTag());
					registry.add(item, "Index", value.isInPool() ? String.valueOf(value.getIndex()) : "<unassigned>");
					registry.add(item, "Wide", value.isWide());
				})
				.children(children);
	}
}
