package me.coley.recaf.ui;

import javafx.application.Platform;
import javafx.scene.control.*;
import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.ui.controls.ActionMenuItem;
import me.coley.recaf.ui.controls.text.JavaPane;
import me.coley.recaf.ui.controls.view.BytecodeViewport;
import me.coley.recaf.ui.controls.view.ClassViewport;
import me.coley.recaf.util.*;
import me.coley.recaf.workspace.JavaResource;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Context menu helper / factory.
 *
 * @author Matt
 */
public class ContextMenus {
	private static int SKIP = ClassReader.SKIP_DEBUG | ClassReader.SKIP_CODE;

	/**
	 * @param controller
	 * 		Controller context.
	 * @param pane
	 * 		Editor pane containing the selected code.
	 * @param name
	 * 		Class name.
	 * @param declaration
	 * 		If the class is a declaration <i>(As opposed to a reference)</i>
	 *
	 * @return Context menu for classes.
	 */
	public static ContextMenu ofClass(GuiController controller, JavaPane pane, String name,
									  boolean declaration) {
		ContextMenu menu = new ContextMenu();
		JavaResource resource = controller.getWorkspace().getContainingResource(name);
		if (resource == null)
			return null;
		// Try to fetch class
		ClassReader reader = controller.getWorkspace().getClassReader(name);
		if (reader == null)
			try {
				reader = new ClassReader(name);
			} catch(Exception ex) {
				return null;
			}
		// Create header
		int access = reader.getAccess();
		MenuItem header = new MenuItem(shorten(name));
		header.getStyleClass().add("context-menu-header");
		header.setGraphic(UiUtil.createClassGraphic(access));
		header.setDisable(true);
		menu.getItems().add(header);
		// Add options for classes we have knowledge of
		if(hasClass(controller, name)) {
			if (!declaration) {
				MenuItem jump = new ActionMenuItem(LangUtil.translate("ui.edit.method.goto"), () -> {
					controller.windows().getMainWindow().openClass(resource, name);
				});
				menu.getItems().add(jump);
			}
		}
		// Add edit options
		if(resource.isPrimary()) {
			// TODO: Add edit options
		}
		return menu;
	}

	/**
	 * @param controller
	 * 		Controller context.
	 * @param pane
	 * 		Editor pane containing the selected code.
	 * @param owner
	 * 		Declaring class name.
	 * @param name
	 * 		Field name.
	 * @param desc
	 * 		Field descriptor.
	 * @param declaration
	 * 		If the field is a declaration <i>(As opposed to a reference)</i>
	 *
	 * @return Context menu for fields.
	 */
	public static ContextMenu ofField(GuiController controller, JavaPane pane, String owner, String name,
									  String desc, boolean declaration) {
		ContextMenu menu = new ContextMenu();
		JavaResource resource = controller.getWorkspace().getContainingResource(owner);
		if (resource == null)
			return null;
		// Try to fetch class
		ClassReader reader = controller.getWorkspace().getClassReader(owner);
		if (reader == null)
			try {
				reader = new ClassReader(owner);
			} catch(Exception ex) {
				return null;
			}
		FieldNode node = ClassUtil.getField(reader, SKIP, name, desc);
		int access = node.access;
		// Create header
		MenuItem header = new MenuItem(name);
		header.getStyleClass().add("context-menu-header");
		header.setGraphic(UiUtil.createFieldGraphic(access));
		header.setDisable(true);
		menu.getItems().add(header);
		// Add options for fields we have knowledge of
		if(hasClass(controller, owner)) {
			if (!declaration) {
				MenuItem jump = new ActionMenuItem(LangUtil.translate("ui.edit.method.goto"), () -> {
					ClassViewport view = controller.windows().getMainWindow().openClass(resource, owner);
					Platform.runLater(() -> view.selectMember(name, desc));
				});
				menu.getItems().add(jump);
			}
		}
		// Add edit options
		if(resource.isPrimary()) {
			// TODO: Add edit options
		}
		return menu;
	}

	/**
	 * @param controller
	 * 		Controller context.
	 * @param pane
	 * 		Editor pane containing the selected code.
	 * @param owner
	 * 		Declaring class name.
	 * @param name
	 * 		Method name.
	 * @param desc
	 * 		Method descriptor.
	 * @param declaration
	 * 		If the method is a declaration <i>(As opposed to a reference)</i>
	 *
	 * @return Context menu for methods.
	 */
	public static ContextMenu ofMethod(GuiController controller, JavaPane pane, String owner, String name,
									   String desc,  boolean declaration) {
		ContextMenu menu = new ContextMenu();
		JavaResource resource = controller.getWorkspace().getContainingResource(owner);
		if (resource == null)
			return null;
		// Try to fetch class
		ClassReader reader = controller.getWorkspace().getClassReader(owner);
		if (reader == null)
			try {
				reader = new ClassReader(owner);
			} catch(Exception ex) {
				return null;
			}
		MethodNode node = ClassUtil.getMethod(reader, SKIP, name, desc);
		int access = node.access;
		// Create header
		MenuItem header = new MenuItem(name);
		header.getStyleClass().add("context-menu-header");
		header.setGraphic(UiUtil.createMethodGraphic(access));
		header.setDisable(true);
		menu.getItems().add(header);
		// Add options for methods we have knowledge of
		if(hasClass(controller, owner)) {
			if (!declaration) {
				MenuItem jump = new ActionMenuItem(LangUtil.translate("ui.edit.method.goto"), () -> {
					ClassViewport view = controller.windows().getMainWindow().openClass(resource, owner);
					new Thread(() -> view.selectMember(name, desc)).start();
				});
				menu.getItems().add(jump);
			}
		}
		// Add edit options
		if(resource.isPrimary()) {
			// TODO: Add edit options
			MenuItem edit = new ActionMenuItem(LangUtil.translate("ui.edit.method.editasm"), () -> {
				BytecodeViewport view = new BytecodeViewport(controller, pane, resource, owner, name, desc, access);
				view.updateView();
				controller.windows().window(name + desc, view, 600, 600).show();
			});
			menu.getItems().add(edit);
		}
		return menu;
	}

	private static String shorten(String name) {
		return name.contains("/") ? name.substring(name.lastIndexOf("/") + 1) : name;
	}

	private static boolean hasClass(GuiController controller, String name) {
		return controller.getWorkspace().hasClass(name);
	}
}
