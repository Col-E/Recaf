package software.coley.recaf.ui.pane.editing.jvm.lowlevel;

import jakarta.annotation.Nonnull;
import software.coley.cafedude.classfile.ClassFile;
import software.coley.cafedude.classfile.Field;
import software.coley.cafedude.classfile.Method;
import software.coley.cafedude.classfile.constant.CpClass;
import software.coley.cafedude.classfile.constant.CpEntry;
import software.coley.recaf.util.AccessFlag;
import software.coley.recaf.util.Icons;

import java.util.List;

/**
 * Registers class-file and member values for low-level trees.
 *
 * @author Matt Coley
 * @see JvmLowLevelTreeBuilder
 */
public final class JvmClassFileItems {
	private JvmClassFileItems() {}

	/**
	 * Registers class-file model handlers.
	 *
	 * @param registry
	 * 		Tree item registry.
	 * @param context
	 * 		Per-build rendering context.
	 */
	static void register(@Nonnull ClassItemRegistry registry, @Nonnull JvmLowLevelContext context) {
		registry.register(ClassFile.class)
				.text(value -> "")
				.children((root, classFile) -> {
					registry.add(root, "Major version", classFile.getVersionMajor());
					registry.add(root, "Minor version", classFile.getVersionMinor());
					registry.add(root, "Access", context.flags(AccessFlag.Type.CLASS, classFile.getAccess()));
					registry.add(root, "This", classFile.getThisClass());
					registry.addNullable(root, "Super", classFile.getSuperClass());

					ClassItem interfaces = registry.add(root, "Interfaces", classFile.getInterfaceClasses(), definition -> definition
							.text(values -> "[" + values.size() + "]")
							.graphic(values -> Icons.getIconView(Icons.ARRAY)));
					for (CpClass interfaceClass : classFile.getInterfaceClasses())
						registry.add(interfaces, "Interface", interfaceClass);

					ClassItem pool = registry.add(root, "Constant pool", classFile.getPool(), definition -> definition
							.text(values -> "[" + values.size() + "]")
							.graphic(values -> Icons.getIconView(Icons.ARRAY)));
					for (CpEntry entry : classFile.getPool())
						registry.add(pool, poolPrefix(entry), entry);

					ClassItem fields = registry.add(root, "Fields", classFile.getFields(), definition -> definition
							.text(values -> "[" + values.size() + "]")
							.graphic(values -> context.memberGraphic(null, false)));
					for (Field field : classFile.getFields())
						addField(registry, context, fields, field);

					ClassItem methods = registry.add(root, "Methods", classFile.getMethods(), definition -> definition
							.text(values -> "[" + values.size() + "]")
							.graphic(values -> context.memberGraphic(null, true)));
					for (Method method : classFile.getMethods())
						addMethod(registry, context, methods, method);

					JvmAttributeItems.addAttributes(registry, context, root, classFile);
				})
			.graphic(value -> null);

		registry.register(Field.class)
				.text(field -> memberText(context, AccessFlag.Type.FIELD, field.getAccess(),
						field.getName().getText(), field.getType().getText(), false))
				.graphic(field -> context.memberGraphic(context.classPath().child(field.getName().getText(), field.getType().getText()), false));
		registry.register(Method.class)
				.text(method -> memberText(context, AccessFlag.Type.METHOD, method.getAccess(),
						method.getName().getText(), method.getType().getText(), true))
				.graphic(method -> context.memberGraphic(context.classPath().child(method.getName().getText(), method.getType().getText()), true));
	}

	private static void addField(@Nonnull ClassItemRegistry registry, @Nonnull JvmLowLevelContext context,
	                            @Nonnull ClassItem parent, @Nonnull Field field) {
		ClassItem item = registry.add(parent, "Field", field);
		registry.add(item, "Access", context.flags(AccessFlag.Type.FIELD, field.getAccess()));
		registry.add(item, "Name", field.getName());
		registry.add(item, "Descriptor", field.getType());
		JvmAttributeItems.addAttributes(registry, context, item, field);
	}

	private static void addMethod(@Nonnull ClassItemRegistry registry, @Nonnull JvmLowLevelContext context,
	                             @Nonnull ClassItem parent, @Nonnull Method method) {
		ClassItem item = registry.add(parent, "Method", method);
		registry.add(item, "Access", context.flags(AccessFlag.Type.METHOD, method.getAccess()));
		registry.add(item, "Name", method.getName());
		registry.add(item, "Descriptor", method.getType());
		JvmAttributeItems.addAttributes(registry, context, item, method);
	}

	@Nonnull
	private static String memberText(@Nonnull JvmLowLevelContext context, @Nonnull AccessFlag.Type type, int access,
	                                 @Nonnull String name, @Nonnull String descriptor, boolean method) {
		String display = method
				? context.memberDisplay().getMethodDisplay(name, descriptor)
				: context.memberDisplay().getFieldDisplay(name, descriptor);
		return context.flags(type, access) + " " + display;
	}

	@Nonnull
	private static String poolPrefix(@Nonnull CpEntry entry) {
		String index = entry.isInPool() ? String.valueOf(entry.getIndex()) : "?";
		return "[" + index + "] " + entry.getClass().getSimpleName();
	}
}
