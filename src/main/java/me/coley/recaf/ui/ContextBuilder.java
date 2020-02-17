package me.coley.recaf.ui;

import javafx.application.Platform;
import javafx.scene.control.*;
import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.ui.controls.ActionMenuItem;
import me.coley.recaf.ui.controls.view.BytecodeViewport;
import me.coley.recaf.ui.controls.view.ClassViewport;
import me.coley.recaf.util.*;
import me.coley.recaf.workspace.JavaResource;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Context menu builder.
 *
 * @author Matt
 */
public class ContextBuilder {
	private static final int SKIP = ClassReader.SKIP_DEBUG | ClassReader.SKIP_CODE;
	private GuiController controller;
	private JavaResource resource;
	// class ctx options
	private ClassViewport classView;
	private boolean declaration;
	private ClassReader reader;

	/**
	 * @return Context menu builder.
	 */
	public static ContextBuilder menu() {
		return new ContextBuilder();
	}

	/**
	 * @param controller
	 * 		Controller context.
	 *
	 * @return Builder.
	 */
	public ContextBuilder controller(GuiController controller) {
		this.controller = controller;
		return this;
	}

	/**
	 * @param classView
	 * 		Viewport containing the class/declaring-class.
	 *
	 * @return Builder.
	 */
	public ContextBuilder view(ClassViewport classView) {
		this.classView = classView;
		return this;
	}

	/**
	 * @param declaration
	 * 		If the member is a declaration <i>(As opposed to a reference)</i>
	 *
	 * @return Builder.
	 */
	public ContextBuilder declaration(boolean declaration) {
		this.declaration = declaration;
		return this;
	}

	/**
	 * @param name
	 * 		Class name.
	 *
	 * @return {@code true} if the containing resource can be found and a class-reader
	 * generated.
	 */
	private boolean setupClass(String name) {
		resource = controller.getWorkspace().getContainingResource(name);
		if(resource == null)
			return false;
		// Try to fetch class
		reader = controller.getWorkspace().getClassReader(name);
		if(reader == null)
			try {
				reader = new ClassReader(name);
			} catch(Exception ex) {
				return false;
			}
		return true;
	}

	/**
	 * @param name
	 * 		Class name.
	 *
	 * @return Context menu for classes.
	 */
	public ContextMenu ofClass(String name) {
		if(!setupClass(name))
			return null;
		// Create header
		int access = reader.getAccess();
		MenuItem header = new MenuItem(shorten(name));
		header.getStyleClass().add("context-menu-header");
		header.setGraphic(UiUtil.createClassGraphic(access));
		header.setDisable(true);
		ContextMenu menu = new ContextMenu();
		menu.getItems().add(header);
		// Add options for classes we have knowledge of
		if(hasClass(controller, name)) {
			if(!declaration) {
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
	 * @param owner
	 * 		Declaring class name.
	 * @param name
	 * 		Field name.
	 * @param desc
	 * 		Field descriptor.
	 *
	 * @return Context menu for fields.
	 */
	public ContextMenu ofField(String owner, String name, String desc) {
		if(!setupClass(owner))
			return null;
		// Fetch field
		FieldNode node = ClassUtil.getField(reader, SKIP, name, desc);
		if(node == null)
			return null;
		int access = node.access;
		// Create header
		MenuItem header = new MenuItem(name);
		header.getStyleClass().add("context-menu-header");
		header.setGraphic(UiUtil.createFieldGraphic(access));
		header.setDisable(true);
		ContextMenu menu = new ContextMenu();
		menu.getItems().add(header);
		// Add options for fields we have knowledge of
		if(hasClass(controller, owner)) {
			if(!declaration) {
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
	 * @param owner
	 * 		Declaring class name.
	 * @param name
	 * 		Method name.
	 * @param desc
	 * 		Method descriptor.
	 *
	 * @return Context menu for methods.
	 */
	public ContextMenu ofMethod(String owner, String name, String desc) {
		if(!setupClass(owner))
			return null;
		// Fetch method
		MethodNode node = ClassUtil.getMethod(reader, SKIP, name, desc);
		if(node == null)
			return null;
		int access = node.access;
		// Create header
		MenuItem header = new MenuItem(name);
		header.getStyleClass().add("context-menu-header");
		header.setGraphic(UiUtil.createMethodGraphic(access));
		header.setDisable(true);
		ContextMenu menu = new ContextMenu();
		menu.getItems().add(header);
		// Add options for methods we have knowledge of
		if(hasClass(controller, owner)) {
			if(!declaration) {
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
				BytecodeViewport view = new BytecodeViewport(controller, classView, resource, owner, name, desc);
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
