package me.coley.recaf.ui.context;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import me.coley.recaf.RecafUI;
import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.config.Configs;
import me.coley.recaf.mapping.MappingsAdapter;
import me.coley.recaf.mapping.RemappingVisitor;
import me.coley.recaf.ui.CommonUX;
import me.coley.recaf.ui.dialog.ConfirmDialog;
import me.coley.recaf.ui.dialog.PackageSelectDialog;
import me.coley.recaf.ui.dialog.TextInputDialog;
import me.coley.recaf.ui.pane.ClassHierarchyPane;
import me.coley.recaf.ui.pane.SearchPane;
import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.ui.window.GenericWindow;
import me.coley.recaf.util.StringUtil;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.resource.Resource;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.util.Optional;

import static me.coley.recaf.ui.util.Menus.*;

/**
 * Context menu builder for classes.
 *
 * @author Matt Coley
 */
public class ClassContextBuilder extends DeclarableContextBuilder {
	private ClassInfo info;
	private boolean declaration;

	/**
	 * @param info
	 * 		Class information about selected item.
	 *
	 * @return Builder.
	 */
	public ClassContextBuilder setClassInfo(ClassInfo info) {
		this.info = info;
		return this;
	}

	@Override
	public ContextMenu build() {
		String name = info.getName();
		ContextMenu menu = new ContextMenu();
		menu.getItems().add(createHeader(StringUtil.shortenPath(name), Icons.getClassIcon(info)));
		if (!declaration)
			menu.getItems().add(action("menu.goto.class", Icons.OPEN, this::openClass));
		if (isPrimary()) {
			Menu refactor = menu("menu.refactor");
			menu.getItems().add(action("menu.edit.copy", Icons.ACTION_COPY, this::copy));
			menu.getItems().add(action("menu.edit.delete", Icons.ACTION_DELETE, this::delete));
			refactor.getItems().add(action("menu.refactor.move", Icons.ACTION_MOVE, this::move));
			refactor.getItems().add(action("menu.refactor.rename", Icons.ACTION_EDIT, this::rename));
			menu.getItems().add(refactor);
		}
		Menu search = menu("menu.search", Icons.ACTION_SEARCH);
		search.getItems().add(action("menu.search.references", Icons.REFERENCE, this::search));
		menu.getItems().add(search);
		Menu view = menu("menu.view", Icons.EYE);
		view.getItems().add(action("menu.view.hierarchy", Icons.T_TREE, this::openHierarchy));
		menu.getItems().add(view);
		return menu;
	}

	@Override
	public Resource findContainerResource() {
		String name = info.getName();
		Workspace workspace = RecafUI.getController().getWorkspace();
		Resource resource = workspace.getResources().getContainingForClass(name);
		if (resource == null)
			logger.warn("Could not find container resource for class {}", name);
		return resource;
	}

	@Override
	public ClassContextBuilder setDeclaration(boolean declaration) {
		this.declaration = declaration;
		return this;
	}

	private void openClass() {
		CommonUX.openClass(info);
	}

	private void copy() {
		String name = info.getName();
		Resource resource = getContainingResource();
		if (resource != null) {
			String title = Lang.get("dialog.title.copy-class");
			String header = Lang.get("dialog.header.copy-class");
			TextInputDialog copyDialog = new TextInputDialog(title, header, Icons.getImageView(Icons.ACTION_COPY));
			copyDialog.setText(name);
			Optional<Boolean> copyResult = copyDialog.showAndWait();
			if (copyResult.isPresent() && copyResult.get()) {
				// Create mappings and pass the class through it. This will be our copied class.
				String newName = copyDialog.getText();
				MappingsAdapter mappings = new MappingsAdapter("RECAF-COPY", false, false);
				mappings.addClass(name, newName);
				// Create the new class bytecode filtered through the renamer
				ClassWriter cw = new ClassWriter(WRITE_FLAGS);
				ClassReader cr = new ClassReader(info.getValue());
				cr.accept(new RemappingVisitor(cw, mappings), READ_FLAGS);
				resource.getClasses().put(ClassInfo.read(cw.toByteArray()));
			}
		} else {
			logger.error("Failed to resolve containing resource for class '{}'", name);
		}
	}

	private void delete() {
		String name = info.getName();
		Resource resource = getContainingResource();
		if (resource != null) {
			if (Configs.display().promptDeleteItem) {
				String title = Lang.get("dialog.title.delete-class");
				String header = String.format(Lang.get("dialog.header.delete-class"), "\n" + name);
				ConfirmDialog deleteDialog = new ConfirmDialog(title, header, Icons.getImageView(Icons.ACTION_DELETE));
				boolean canRemove = deleteDialog.showAndWait().orElse(false);
				if (!canRemove) {
					return;
				}
			}
			boolean removed = resource.getClasses().remove(name) != null;
			if (!removed) {
				logger.warn("Tried to delete class '{}' but failed", name);
			}
		} else {
			logger.error("Failed to resolve containing resource for class '{}'", name);
		}
	}

	private void move() {
		String name = info.getName();
		Resource resource = getContainingResource();
		if (resource != null) {
			String title = Lang.get("dialog.title.move-class");
			String header = Lang.get("dialog.header.move-class");
			int packageSeparator = name.lastIndexOf('/');
			String currentPackage = packageSeparator > 0 ? name.substring(0, packageSeparator) : "";
			PackageSelectDialog packageDialog
					= new PackageSelectDialog(title, header, Icons.getImageView(Icons.ACTION_EDIT));
			packageDialog.populate(resource);
			packageDialog.setCurrentPackage(currentPackage);
			Optional<Boolean> moveResult = packageDialog.showAndWait();
			if (moveResult.isPresent() && moveResult.get()) {
				// Create mappings to use for renaming.
				String newPackage = packageDialog.getSelectedPackage();
				MappingsAdapter mappings = new MappingsAdapter("RECAF-MOVE", false, false);
				mappings.addClass(name, newPackage + "/" + name.substring(packageSeparator + 1));
				// Update all classes in the resource
				applyMappings(resource, mappings);
			}
		} else {
			logger.error("Failed to resolve containing resource for class '{}'", name);
		}
	}

	private void rename() {
		String name = info.getName();
		Resource resource = getContainingResource();
		if (resource != null) {
			String title = Lang.get("dialog.title.rename-class");
			String header = Lang.get("dialog.header.rename-class");
			TextInputDialog renameDialog = new TextInputDialog(title, header, Icons.getImageView(Icons.ACTION_EDIT));
			renameDialog.setText(name);
			Optional<Boolean> renameResult = renameDialog.showAndWait();
			if (renameResult.isPresent() && renameResult.get()) {
				// Create mappings to use for renaming.
				String newName = renameDialog.getText();
				MappingsAdapter mappings = new MappingsAdapter("RECAF-RENAME", false, false);
				mappings.addClass(name, newName);
				// Update all classes in the resource
				applyMappings(resource, mappings);
			}
		} else {
			logger.error("Failed to resolve containing resource for class '{}'", name);
		}
	}

	private void search() {
		new GenericWindow(SearchPane.createReferenceSearch(info.getName(), null, null)).show();
	}

	private void openHierarchy() {
		String title = "Hierarchy: " + StringUtil.shortenPath(info.getName());
		RecafUI.getWindows().getMainWindow().getDockingRootPane()
				.openTab(title, () -> new ClassHierarchyPane(info));
	}
}
