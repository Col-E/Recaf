package me.coley.recaf.ui;

import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.decompile.DecompileImpl;
import me.coley.recaf.parse.bytecode.Disassembler;
import me.coley.recaf.plugin.PluginsManager;
import me.coley.recaf.plugin.api.ContextMenuInjectorPlugin;
import me.coley.recaf.search.StringMatchMode;
import me.coley.recaf.ui.controls.ActionMenuItem;
import me.coley.recaf.ui.controls.IconView;
import me.coley.recaf.ui.controls.RenamingTextField;
import me.coley.recaf.ui.controls.pane.SearchPane;
import me.coley.recaf.ui.controls.ViewportTabs;
import me.coley.recaf.ui.controls.popup.YesNoWindow;
import me.coley.recaf.ui.controls.text.BytecodeMemberInserterPane;
import me.coley.recaf.ui.controls.tree.JavaResourceTree;
import me.coley.recaf.ui.controls.view.BytecodeViewport;
import me.coley.recaf.ui.controls.view.ClassViewport;
import me.coley.recaf.ui.controls.view.FileViewport;
import me.coley.recaf.util.*;
import me.coley.recaf.workspace.JavaResource;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
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
	private static PluginsManager plugins = PluginsManager.getInstance();
	private GuiController controller;
	private JavaResource resource;
	// class ctx options
	private ClassViewport classView;
	private TreeView<?> treeView;
	private boolean declaration;
	private ClassReader reader;
	// file ctx options
	private FileViewport fileView;

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
	 * @param resource
	 * 		JavaResource context.
	 *
	 * @return Builder.
	 */
	public ContextBuilder resource(JavaResource resource) {
		this.resource = resource;
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
	 * @param fileView
	 * 		File viewport containing the file.
	 *
	 * @return Builder.
	 */
	public ContextBuilder view(FileViewport fileView) {
		this.fileView = fileView;
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
		resource = controller.getWorkspace().getContainingResourceForClass(name);
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
	 * @return {@code true} when the item is a declaration instead of a reference.
	 * Otherwise, {@code false}.
	 */
	public boolean isDeclaration() {
		return declaration;
	}

	/**
	 * @return The viewport the item resides in.
	 * Will be {@code null} if the item does not belong to a class viewport.
	 */
	public ClassViewport getClassView() {
		return classView;
	}

	/**
	 * @return The viewport the item resides in.
	 * Will be {@code null} if the item does not belong to a file viewport.
	 */
	public FileViewport getFileView() {
		return fileView;
	}

	/**
	 * @return The UI controller.
	 */
	public GuiController getController() {
		return controller;
	}

	/**
	 * @return The class reader of the item.
	 * Will be {@code null} if the item does not relate to a class file.
	 */
	public ClassReader getReader() {
		return reader;
	}

	/**
	 * @return The resource the item belongs to.
	 * Will be {@code null} if the item does not relate to a class or file in one of the loaded resources.
	 */
	public JavaResource getResource() {
		return resource;
	}

	/**
	 * @return The host tree view that holds the item we're adding the context menu to.
	 * Will be {@code null} if the item does not belong to a tree-view.
	 */
	public TreeView<?> getTreeView() {
		return treeView;
	}

	/**
	 * @return {@code true} when the {@link #treeView} belongs to a resource tree
	 * <i>(Inside the workspace navigator)</i>.
	 */
	public boolean isWorkspaceTree() {
		return treeView != null && treeView.getParent() instanceof JavaResourceTree;
	}

	/**
	 * @param name
	 * 		Full path name of file.
	 *
	 * @return {@code true} if the primary resource contains the file.
	 */
	public boolean isPrimaryFile(String name) {
		return controller.getWorkspace().getPrimary().getFiles().containsKey(name);
	}

	/**
	 * @param name
	 * 		Full path name of class.
	 *
	 * @return {@code true} if the primary resource contains the class.
	 */
	public boolean isPrimaryClass(String name) {
		return controller.getWorkspace().getPrimary().getClasses().containsKey(name);
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
			if (!declaration) {
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
			// Renaming
			if (isPrimaryClass(name)) {
				MenuItem rename = new ActionMenuItem(LangUtil.translate("misc.rename"), () -> {
					Window main = controller.windows().getMainWindow().getStage();
					RenamingTextField popup = RenamingTextField.forClass(controller, name);
					popup.show(main);
				});
				menu.getItems().add(rename);
				if (isWorkspaceTree()) {
					// Add workspace-navigator specific items, but only for primary classes
					MenuItem remove = new ActionMenuItem(LangUtil.translate("misc.remove"), () -> {
						YesNoWindow.prompt(LangUtil.translate("misc.confirm.message"), () -> {
							controller.getWorkspace().getPrimary().getClasses().remove(name);
							controller.windows().getMainWindow().getTabs().closeTab(name);
						}, null).show(treeView);
					});
					menu.getItems().add(remove);
				} else {
					// Add class items, not shown in workspace navigator
					String sAddField = LangUtil.translate("misc.add") + " Field";
					String sAddMethod = LangUtil.translate("misc.add") + " Method";
					MenuItem addField = new ActionMenuItem(sAddField, () -> {
						BytecodeMemberInserterPane pane = new BytecodeMemberInserterPane(controller, name, false);
						BytecodeViewport view = new BytecodeViewport(controller,
								getClassView(),controller.getWorkspace().getPrimary(), name, pane);
						view.updateView();
						controller.windows().window(sAddField, view, 500, 200).show();
					});
					MenuItem addMethod = new ActionMenuItem(sAddMethod, () -> {
						BytecodeMemberInserterPane pane = new BytecodeMemberInserterPane(controller, name, true);
						BytecodeViewport view = new BytecodeViewport(controller,
								getClassView(),controller.getWorkspace().getPrimary(), name, pane);
						view.updateView();
						controller.windows().window(sAddMethod, view, 500, 200).show();
					});
					menu.getItems().addAll(new SeparatorMenuItem(), addField, addMethod);
				}
			}
			menu.getItems().addAll(
					new SeparatorMenuItem(),
					new ActionMenuItem(translate("ui.edit.copypath"), () -> {
						ClipboardContent content = new ClipboardContent();
						content.putString(name);
						Clipboard.getSystemClipboard().setContent(content);
					})
			);
		}
		// Inject plugin menus
		plugins.ofType(ContextMenuInjectorPlugin.class).forEach(injector -> injector.forClass(this, menu, name));
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
				if (isPrimaryClass(owner)) {
					MenuItem remove = new ActionMenuItem(LangUtil.translate("misc.remove"), () -> {
						YesNoWindow.prompt(LangUtil.translate("misc.confirm.message"), () -> {
							byte[] updated = ClassUtil.removeField(reader, node.name, node.desc);
							getResource().getClasses().put(reader.getClassName(), updated);
							getClassView().updateView();
						}, null).show(classView);
					});
					menu.getItems().add(remove);
				}
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
			// Renaming
			if (isPrimaryClass(owner)) {
				MenuItem rename = new ActionMenuItem(LangUtil.translate("misc.rename"), () -> {
					Window main = controller.windows().getMainWindow().getStage();
					RenamingTextField popup = RenamingTextField.forMember(controller, owner, name, desc);
					popup.show(main);
				});
				menu.getItems().add(rename);
			}
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
		// Inject plugin menus
		plugins.ofType(ContextMenuInjectorPlugin.class)
				.forEach(injector -> injector.forField(this, menu, owner, name, desc));
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
				if (isPrimaryClass(owner)) {
					MenuItem remove = new ActionMenuItem(LangUtil.translate("misc.remove"), () -> {
						YesNoWindow.prompt(LangUtil.translate("misc.confirm.message"), () -> {
							byte[] updated = ClassUtil.removeMethod(reader, node.name, node.desc);
							getResource().getClasses().put(reader.getClassName(), updated);
							getClassView().updateView();
						}, null).show(classView);
					});
					menu.getItems().add(remove);
				}
			} else {
				MenuItem jump = new ActionMenuItem(LangUtil.translate("ui.edit.method.goto"), () -> {
					ClassViewport view = controller.windows().getMainWindow().openClass(resource, owner);
					ThreadUtil.run(() -> view.selectMember(name, desc));
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
			// Renaming
			if (isPrimaryClass(owner)) {
				MenuItem rename = new ActionMenuItem(LangUtil.translate("ui.edit.method.rename"), () -> {
					Window main = controller.windows().getMainWindow().getStage();
					RenamingTextField popup = RenamingTextField.forMember(controller, owner, name, desc);
					popup.show(main);
				});
				menu.getItems().add(rename);
			}
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
		// Inject plugin menus
		plugins.ofType(ContextMenuInjectorPlugin.class)
				.forEach(injector -> injector.forMethod(this, menu, owner, name, desc));
		return menu;
	}

	/**
	 * @param owner
	 * 		Class method is declared in.
	 * @param name
	 * 		Declaring method name.
	 * @param desc
	 * 		Declaring method descriptor.
	 * @param insn
	 * 		Instruction value.
	 *
	 * @return Context menu for instructions in search results.
	 */
	public ContextMenu ofInsn(String owner, String name, String desc, AbstractInsnNode insn) {
		if(!setupClass(owner))
			return null;
		// Fetch declaring method
		MethodNode node = ClassUtil.getMethod(reader, SKIP, name, desc);
		if(node == null)
			return null;
		// Create header
		MenuItem header = new MenuItem(StringUtil.limit(Disassembler.insn(insn), 25));
		header.getStyleClass().add("context-menu-header");
		// TODO: Change graphic depending on insn type.
		//  - Icon for field return types
		//  - Icon for method return types
		//  - Icon for type declarations
		//  - Primitive icon for primitive values
		//  - No icon for anything else
		// header.setGraphic(...);
		header.setDisable(true);
		ContextMenu menu = new ContextMenu();
		menu.getItems().add(header);
		// Add options for insns in classes we have knowledge of.
		// This should cover all classes, but we still want to make the assertion.
		if (hasClass(controller, owner)) {
			// TODO: Add more instruction menu options:
			//  - Depends on insn type?
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
		// Add option to go to editor
		if(resource.isPrimary()) {
			MenuItem edit = new ActionMenuItem(LangUtil.translate("ui.edit.method.editasm"), () -> {
				BytecodeViewport view = new BytecodeViewport(controller, classView, resource, owner, name, desc);
				view.updateView();
				controller.windows().window(name + desc, view, 600, 600).show();
			});
			menu.getItems().add(edit);
		}
		// Inject plugin menus
		plugins.ofType(ContextMenuInjectorPlugin.class)
				.forEach(injector -> injector.forInsn(this, menu, owner, name, desc, insn));
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
		header.setGraphic(new IconView("icons/folder-package.png"));
		header.setDisable(true);
		ContextMenu menu = new ContextMenu();
		menu.getItems().add(header);
		String packagePrefix = name + '/';
		// Add workspace-navigator specific items, but only for primary files
		if(isWorkspaceTree() && controller.getWorkspace().getPrimaryClassNames().stream()
				.anyMatch(cls -> cls.startsWith(packagePrefix))) {
			MenuItem rename = new ActionMenuItem(LangUtil.translate("misc.rename"), () -> {
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
			menu.getItems().addAll(
					rename,
					remove,
					new SeparatorMenuItem(),
					new ActionMenuItem(translate("ui.edit.copypath"), () -> {
						ClipboardContent content = new ClipboardContent();
						content.putString(name);
						Clipboard.getSystemClipboard().setContent(content);
					})
			);
		}
		// Inject plugin menus
		plugins.ofType(ContextMenuInjectorPlugin.class).forEach(injector -> injector.forPackage(this, menu, name));
		return menu;
	}

	/**
	 * @param name
	 * 		File name.
	 *
	 * @return Context menu for files.
	 */
	public ContextMenu ofFile(String name) {
		// Create the menu
		MenuItem header = new MenuItem(shorten(name));
		header.getStyleClass().add("context-menu-header");
		header.setGraphic(UiUtil.createFileGraphic(name));
		header.setDisable(true);
		ContextMenu menu = new ContextMenu();
		menu.getItems().add(header);
		// Add workspace-navigator specific items, but only for primary files
		if (isWorkspaceTree() && isPrimaryFile(name)) {
			MenuItem rename = new ActionMenuItem(LangUtil.translate("misc.rename"), () -> {
				Window main = controller.windows().getMainWindow().getStage();
				RenamingTextField popup = RenamingTextField.forFile(controller, name);
				popup.show(main);
			});
			MenuItem remove = new ActionMenuItem(LangUtil.translate("misc.remove"), () -> {
				YesNoWindow.prompt(LangUtil.translate("misc.confirm.message"), () -> {
					controller.getWorkspace().getPrimary().getFiles().remove(name);
					controller.windows().getMainWindow().getTabs().closeTab(name);
				}, null).show(treeView);
			});
			menu.getItems().addAll(
					rename,
					remove,
					new SeparatorMenuItem(),
					new ActionMenuItem(translate("ui.edit.copypath"), () -> {
						ClipboardContent content = new ClipboardContent();
						content.putString(name);
						Clipboard.getSystemClipboard().setContent(content);
					})
			);

		}
		// Inject plugin menus
		plugins.ofType(ContextMenuInjectorPlugin.class).forEach(injector -> injector.forFile(this, menu, name));
		return menu;
	}

	/**
	 * @param resource
	 * 		Root resource.
	 *
	 * @return Context menu for resource roots.
	 */
	public ContextMenu ofRoot(JavaResource resource) {
		this.resource = resource;
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
			loader.setTitle(translate("ui.fileprompt.open"));
			loader.getExtensionFilters().add(filter);
			loader.setSelectedExtensionFilter(filter);
			loader.setInitialDirectory(controller.config().backend().getRecentLoadDir());
			MenuItem addDoc = new ActionMenuItem(LangUtil.translate("ui.load.adddocs"), () -> {
				File file = loader.showOpenDialog(null);
				if (file != null) {
					try {
						resource.setClassDocs(file.toPath());
					} catch(IOException ex) {
						Log.error(ex, "Failed to set resource documentation");
					}
				}
			});
			MenuItem addSrc = new ActionMenuItem(LangUtil.translate("ui.load.addsrc"), () -> {
				File file = loader.showOpenDialog(null);
				if (file != null) {
					try {
						resource.setClassSources(file.toPath());
					} catch(IOException ex) {
						Log.error(ex, "Failed to set resource sources");
					}
				}
			});
			menu.getItems().add(addDoc);
			menu.getItems().add(addSrc);
		}
		// Inject plugin menus
		plugins.ofType(ContextMenuInjectorPlugin.class)
				.forEach(injector -> injector.forResourceRoot(this, menu, resource));
		return menu;
	}

	/**
	 * @param className The name of the class
	 * @return Context menu for tabs of {@link ClassViewport ClassViewports}.
	 */
	public ContextMenu ofClassTab(String className) {
		// No header necessary
		Menu menuDecompile = new Menu(LangUtil.translate("decompile.decompiler.name"));
		for (DecompileImpl impl : DecompileImpl.values())
			menuDecompile.getItems()
					.add(new ActionMenuItem(impl.toString(), () -> classView.setOverrideDecompiler(impl)));
		Menu menuMode = new Menu(LangUtil.translate("display.classmode.name"));
		for (ClassViewport.ClassMode mode : ClassViewport.ClassMode.values())
			menuMode.getItems().add(new ActionMenuItem(mode.toString(), () -> classView.setOverrideMode(mode)));
		// Create menu
		ContextMenu menu = new ContextMenu();
		menu.getItems().addAll(
				menuDecompile,
				menuMode
		);
		addTabOptions(menu, className);
		// Inject plugin menus
		plugins.ofType(ContextMenuInjectorPlugin.class).forEach(injector -> injector.forClassTab(this, menu));
		return menu;
	}

	/**
	 * @param fileName The name of the file
	 * @return Context menu for tabs of {@link FileViewport FileViewports}.
	 */
	public ContextMenu ofFileTab(String fileName) {
		// No header necessary
		Menu menuMode = new Menu(LangUtil.translate("display.classmode.name"));
		for (FileViewport.FileMode mode : FileViewport.FileMode.values())
			menuMode.getItems().add(new ActionMenuItem(mode.toString(), () -> fileView.setOverrideMode(mode)));
		// Create menu
		ContextMenu menu = new ContextMenu();
		menu.getItems().add(menuMode);
		addTabOptions(menu, fileName);
		// Inject plugin menus
		plugins.ofType(ContextMenuInjectorPlugin.class).forEach(injector -> injector.forFileTab(this, menu));
		return menu;
	}

	/**
	 * Add common tab options
	 *
	 * @param menu
	 * 		Context Menu
	 */
	private void addTabOptions(ContextMenu menu, String name) {
		menu.getItems().addAll(
				new SeparatorMenuItem(),
				new ActionMenuItem(translate("ui.edit.tab.close"), () -> {
					getTabs().closeTab(name);
				}),
				new ActionMenuItem(translate("ui.edit.tab.closeothers"), () -> {
					getTabs().closeAllExcept(name);
				}),
				new ActionMenuItem(translate("ui.edit.tab.closeall"), () -> {
					if (isClass())
						classView.getController().windows().getMainWindow().clearTabViewports();
					else
						fileView.getController().windows().getMainWindow().clearTabViewports();
				}),
				new SeparatorMenuItem(),
				new ActionMenuItem(translate("ui.edit.copypath"), () -> {
					ClipboardContent content = new ClipboardContent();
					content.putString(name);
					Clipboard.getSystemClipboard().setContent(content);
				})
		);
	}

	private ViewportTabs getTabs() {
		return isClass()
				? classView.getController().windows().getMainWindow().getTabs()
				: fileView.getController().windows().getMainWindow().getTabs();
	}

	/**
	 * Is the current viewport (opened tab) a class?
	 *
	 * @return boolean
	 */
	private boolean isClass() {
		return classView != null;
	}

	private static String shorten(String name) {
		return name.contains("/") ? name.substring(name.lastIndexOf("/") + 1) : name;
	}

	private static boolean hasClass(GuiController controller, String name) {
		return controller.getWorkspace().hasClass(name);
	}
}
