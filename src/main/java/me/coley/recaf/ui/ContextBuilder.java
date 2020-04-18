package me.coley.recaf.ui;

import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.search.StringMatchMode;
import me.coley.recaf.ui.controls.ActionMenuItem;
import me.coley.recaf.ui.controls.IconView;
import me.coley.recaf.ui.controls.RenamingTextField;
import me.coley.recaf.ui.controls.SearchPane;
import me.coley.recaf.ui.controls.ViewportTabs;
import me.coley.recaf.ui.controls.popup.YesNoWindow;
import me.coley.recaf.ui.controls.tree.JavaResourceTree;
import me.coley.recaf.ui.controls.view.BytecodeViewport;
import me.coley.recaf.ui.controls.view.ClassViewport;
import me.coley.recaf.util.*;
import me.coley.recaf.workspace.JavaResource;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;

import static me.coley.recaf.util.LangUtil.translate;

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
	private TreeView<?> treeView;
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
	 * 		Class viewport containing the class/declaring-class.
	 *
	 * @return Builder.
	 */
	public ContextBuilder view(ClassViewport classView) {
		this.classView = classView;
		return this;
	}

	/**
	 * @param treeView
	 * 		Tree viewport containing the class.
	 *
	 * @return Builder.
	 */
	public ContextBuilder tree(TreeView<?> treeView) {
		this.treeView = treeView;
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
	 * @return {@code true} when the {@link #treeView} belongs to a resource tree
	 * <i>(Inside the workspace navigator)</i>.
	 */
	private boolean isWorkspaceTree() {
		return treeView != null && treeView.getParent() instanceof JavaResourceTree;
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
			if (declaration) {
				MenuItem rename = new ActionMenuItem(LangUtil.translate("ui.edit.method.rename"), () -> {
					Window main = controller.windows().getMainWindow().getStage();
					RenamingTextField popup = RenamingTextField.forClass(controller, name);
					popup.show(main);
				});
				menu.getItems().add(rename);
			} else {
				MenuItem jump = new ActionMenuItem(LangUtil.translate("ui.edit.method.goto"), () -> {
					controller.windows().getMainWindow().openClass(resource, name);
				});
				menu.getItems().add(jump);
			}
			// Allow searching for class references
			MenuItem search = new ActionMenuItem(LangUtil.translate("ui.edit.search"), () -> {
				SearchPane sp = controller.windows().getMainWindow().getMenubar().searchClassReference();
				sp.setInput("ui.search.cls_reference.name", name);
				sp.setInput("ui.search.matchmode", StringMatchMode.EQUALS);
				sp.search();
			});
			menu.getItems().add(search);
			// Add workspace-navigator specific items, but only for primary classes
			if (isWorkspaceTree() && controller.getWorkspace().getPrimary().getClasses().containsKey(name)) {
				MenuItem remove = new ActionMenuItem(LangUtil.translate("misc.remove"), () -> {
					YesNoWindow.prompt(LangUtil.translate("misc.confirm.message"), () -> {
						controller.getWorkspace().getPrimary().getClasses().remove(name);
						controller.windows().getMainWindow().getTabs().closeTab(name);
					}, null).show(treeView);
				});
				menu.getItems().add(remove);
			}
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
			if (declaration) {
				MenuItem rename = new ActionMenuItem(LangUtil.translate("ui.edit.method.rename"), () -> {
					Window main = controller.windows().getMainWindow().getStage();
					RenamingTextField popup = RenamingTextField.forMember(controller, owner, name, desc);
					popup.show(main);
				});
				menu.getItems().add(rename);
			} else {
				MenuItem jump = new ActionMenuItem(LangUtil.translate("ui.edit.method.goto"), () -> {
					ClassViewport view = controller.windows().getMainWindow().openClass(resource, owner);
					Platform.runLater(() -> view.selectMember(name, desc));
				});
				menu.getItems().add(jump);
			}
			// Allow searching for references to this member
			MenuItem search = new ActionMenuItem(LangUtil.translate("ui.edit.search"), () -> {
				SearchPane sp = controller.windows().getMainWindow().getMenubar().searchMemberReference();
				sp.setInput("ui.search.mem_reference.owner", owner);
				sp.setInput("ui.search.mem_reference.name", name);
				sp.setInput("ui.search.mem_reference.desc", desc);
				sp.setInput("ui.search.matchmode", StringMatchMode.EQUALS);
				sp.search();
			});
			menu.getItems().add(search);
		}
		// Add other edit options
		if(declaration && resource.isPrimary()) {
			MenuItem edit = new ActionMenuItem(LangUtil.translate("ui.edit.method.editasm"), () -> {
				BytecodeViewport view = new BytecodeViewport(controller, classView, resource, owner, name, desc);
				view.updateView();
				controller.windows().window(name, view, 500, 100).show();
			});
			menu.getItems().add(edit);
			// TODO:
			//  - Remove
			//  - Duplicate
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
			if (declaration) {
				MenuItem rename = new ActionMenuItem(LangUtil.translate("ui.edit.method.rename"), () -> {
					Window main = controller.windows().getMainWindow().getStage();
					RenamingTextField popup = RenamingTextField.forMember(controller, owner, name, desc);
					popup.show(main);
				});
				menu.getItems().add(rename);
			} else {
				MenuItem jump = new ActionMenuItem(LangUtil.translate("ui.edit.method.goto"), () -> {
					ClassViewport view = controller.windows().getMainWindow().openClass(resource, owner);
					new Thread(() -> view.selectMember(name, desc)).start();
				});
				menu.getItems().add(jump);
			}
			// Allow searching for references to this member
			MenuItem search = new ActionMenuItem(LangUtil.translate("ui.edit.search"), () -> {
				SearchPane sp = controller.windows().getMainWindow().getMenubar().searchMemberReference();
				sp.setInput("ui.search.mem_reference.owner", owner);
				sp.setInput("ui.search.mem_reference.name", name);
				sp.setInput("ui.search.mem_reference.desc", desc);
				sp.setInput("ui.search.matchmode", StringMatchMode.EQUALS);
				sp.search();
			});
			menu.getItems().add(search);
		}
		// Add edit options
		if(declaration && resource.isPrimary()) {
			MenuItem edit = new ActionMenuItem(LangUtil.translate("ui.edit.method.editasm"), () -> {
				BytecodeViewport view = new BytecodeViewport(controller, classView, resource, owner, name, desc);
				view.updateView();
				controller.windows().window(name + desc, view, 600, 600).show();
			});
			menu.getItems().add(edit);
			// TODO:
			//  - Remove
			//  - Duplicate
		}
		return menu;
	}

	/**
	 * @param name
	 * 		Package name.
	 *
	 * @return Context menu for packages.
	 */
	public ContextMenu ofPackage(String name) {
		MenuItem header = new MenuItem(shorten(name));
		header.getStyleClass().add("context-menu-header");
		header.setGraphic(UiUtil.createFileGraphic(name.replace('/', '.')));
		header.setDisable(true);
		ContextMenu menu = new ContextMenu();
		menu.getItems().add(header);
		String packagePrefix = name + '/';
		// Add workspace-navigator specific items, but only for primary files
		if(isWorkspaceTree() && controller.getWorkspace().getPrimaryClassNames().stream()
				.anyMatch(cls -> cls.startsWith(packagePrefix))) {
			MenuItem rename = new ActionMenuItem(LangUtil.translate("ui.edit.method.rename"), () -> {
				Window main = controller.windows().getMainWindow().getStage();
				RenamingTextField popup = RenamingTextField.forPackage(controller, name);
				popup.show(main);
			});
			MenuItem remove = new ActionMenuItem(LangUtil.translate("misc.remove"), () -> {
				Map<String, byte[]> classes = controller.getWorkspace().getPrimary().getClasses();
				ViewportTabs tabs = controller.windows().getMainWindow().getTabs();
				YesNoWindow.prompt(LangUtil.translate("misc.confirm.message"), () -> {
					new HashSet<>(classes.keySet()).forEach(cls -> {
						if (cls.startsWith(packagePrefix)) {
							classes.remove(cls);
							tabs.closeTab(cls);
						}
					});
				}, null).show(treeView);
			});
			menu.getItems().add(rename);
			menu.getItems().add(remove);
		}
		return menu;
	}

	/**
	 * @param name
	 * 		File name.
	 *
	 * @return Context menu for files.
	 */
	public ContextMenu ofFile(String name) {
		MenuItem header = new MenuItem(shorten(name));
		header.getStyleClass().add("context-menu-header");
		header.setGraphic(UiUtil.createFileGraphic(name));
		header.setDisable(true);
		ContextMenu menu = new ContextMenu();
		menu.getItems().add(header);
		// Add workspace-navigator specific items, but only for primary files
		if (isWorkspaceTree() && controller.getWorkspace().getPrimary().getFiles().containsKey(name)) {
			MenuItem remove = new ActionMenuItem(LangUtil.translate("misc.remove"), () -> {
				YesNoWindow.prompt(LangUtil.translate("misc.confirm.message"), () -> {
					controller.getWorkspace().getPrimary().getFiles().remove(name);
					controller.windows().getMainWindow().getTabs().closeTab(name);
				}, null).show(treeView);
			});
			menu.getItems().add(remove);
		}
		return menu;
	}

	/**
	 * @param resource
	 * 		Root resource.
	 *
	 * @return Context menu for resource roots.
	 */
	public ContextMenu ofRoot(JavaResource resource) {
		String name = resource.toString();
		MenuItem header = new MenuItem(shorten(name));
		header.getStyleClass().add("context-menu-header");
		header.setGraphic(new IconView(UiUtil.getResourceIcon(resource)));
		header.setDisable(true);
		ContextMenu menu = new ContextMenu();
		menu.getItems().add(header);
		// Add workspace-navigator specific items, but only for primary files
		if(isWorkspaceTree()) {
			FileChooser.ExtensionFilter filter =
					new FileChooser.ExtensionFilter(translate("ui.fileprompt.open.extensions"),
							"*.jar", "*.zip", "*.tar", "*.tar.gz");
			FileChooser loader = new FileChooser();
			loader.setTitle(translate("ui.filepropt.open"));
			loader.getExtensionFilters().add(filter);
			loader.setSelectedExtensionFilter(filter);
			loader.setInitialDirectory(controller.config().backend().getRecentLoadDir());
			MenuItem addDoc = new ActionMenuItem(LangUtil.translate("ui.load.adddocs"), () -> {
				File file = loader.showOpenDialog(null);
				if (file != null) {
					try {
						resource.setClassDocs(file);
					} catch(IOException ex) {
						Log.error(ex, "Failed to set resource documentation");
					}
				}
			});
			MenuItem addSrc = new ActionMenuItem(LangUtil.translate("ui.load.addsrc"), () -> {
				File file = loader.showOpenDialog(null);
				if (file != null) {
					try {
						resource.setClassSources(file);
					} catch(IOException ex) {
						Log.error(ex, "Failed to set resource sources");
					}
				}
			});
			menu.getItems().add(addDoc);
			menu.getItems().add(addSrc);
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
