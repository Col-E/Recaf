package me.coley.recaf.plugin.api;

import javafx.scene.control.ContextMenu;
import me.coley.recaf.ui.ContextBuilder;
import me.coley.recaf.workspace.JavaResource;
import org.objectweb.asm.tree.AbstractInsnNode;

/**
 * Allow plugins to update context menus.
 *
 * @author Matt
 */
public interface ContextMenuInjectorPlugin extends BasePlugin {
	/**
	 * Intercept context-menus for classes.
	 *
	 * @param builder
	 * 		Context menu builder.
	 * @param menu
	 * 		The menu to modify.
	 * @param name
	 * 		Package name.
	 */
	default void forPackage(ContextBuilder builder, ContextMenu menu, String name) {}

	/**
	 * Intercept context-menus for classes.
	 *
	 * @param builder
	 * 		Context menu builder.
	 * @param menu
	 * 		The menu to modify.
	 * @param name
	 * 		Class name.
	 */
	default void forClass(ContextBuilder builder, ContextMenu menu, String name) {}

	/**
	 * Intercept context-menus for fields.
	 *
	 * @param builder
	 * 		Context menu builder.
	 * @param menu
	 * 		The menu to modify.
	 * @param owner
	 * 		Declaring class name.
	 * @param name
	 * 		Field name.
	 * @param desc
	 * 		Field descriptor.
	 */
	default void forField(ContextBuilder builder, ContextMenu menu, String owner, String name, String desc) {}

	/**
	 * Intercept context-menus for methods.
	 *
	 * @param builder
	 * 		Context menu builder.
	 * @param menu
	 * 		The menu to modify.
	 * @param owner
	 * 		Declaring class name.
	 * @param name
	 * 		Method name.
	 * @param desc
	 * 		Method descriptor.
	 */
	default void forMethod(ContextBuilder builder, ContextMenu menu, String owner, String name, String desc) {}

	/**
	 * Intercept context-menus for methods.
	 *
	 * @param builder
	 * 		Context menu builder.
	 * @param menu
	 * 		The menu to modify.
	 * @param owner
	 * 		Class that declares the method.
	 * @param name
	 * 		Declaring method name.
	 * @param desc
	 * 		Declaring method descriptor.
	 * @param insn
	 * 		Instruction value.
	 */
	default void forInsn(ContextBuilder builder, ContextMenu menu, String owner, String name, String desc,
						 AbstractInsnNode insn) {}

	/**
	 * Intercept context-menus for files.
	 *
	 * @param builder
	 * 		Context menu builder.
	 * @param menu
	 * 		The menu to modify.
	 * @param name
	 * 		File name.
	 */
	default void forFile(ContextBuilder builder, ContextMenu menu, String name) {}

	/**
	 * Intercept context-menus for resource roots.
	 *
	 * @param builder
	 * 		Context menu builder.
	 * @param menu
	 * 		The menu to modify.
	 * @param resource
	 * 		Root resource.
	 */
	default void forResourceRoot(ContextBuilder builder, ContextMenu menu, JavaResource resource) {}

	/**
	 * Intercept context-menus for class tabs.
	 *
	 * @param builder
	 * 		Context menu builder.
	 * @param menu
	 * 		The menu to modify.
	 */
	default void forClassTab(ContextBuilder builder, ContextMenu menu) {}

	/**
	 * Intercept context-menus for file tabs.
	 *
	 * @param builder
	 * 		Context menu builder.
	 * @param menu
	 * 		The menu to modify.
	 */
	default void forFileTab(ContextBuilder builder, ContextMenu menu) {}
}
