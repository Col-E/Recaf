package me.coley.recaf.ui.context;

import javafx.beans.binding.StringBinding;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import me.coley.recaf.RecafUI;
import me.coley.recaf.config.Configs;
import me.coley.recaf.mapping.MappingsAdapter;
import me.coley.recaf.ui.dialog.ConfirmDialog;
import me.coley.recaf.ui.dialog.PackageSelectDialog;
import me.coley.recaf.ui.dialog.TextInputDialog;
import me.coley.recaf.ui.pane.SearchPane;
import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.ui.window.GenericWindow;
import me.coley.recaf.util.TextDisplayUtil;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.resource.Resource;

import java.util.Optional;
import java.util.TreeSet;

import static me.coley.recaf.ui.util.Menus.*;

/**
 * Context menu builder for packages <i>(For paths in {@link Resource#getClasses()} ()})</i>.
 *
 * @author Matt Coley
 */
public class PackageContextBuilder extends ContextBuilder {
	private String packageName;

	/**
	 * @param packageName
	 * 		Name of package.
	 *
	 * @return Builder.
	 */
	public PackageContextBuilder setPackageName(String packageName) {
		this.packageName = packageName;
		return this;
	}

	@Override
	public ContextMenu build() {
		String name = packageName;
		ContextMenu menu = new ContextMenu();
		menu.getItems().add(createHeader(TextDisplayUtil.shortenEscapeLimit(name), Icons.getIconView(Icons.FOLDER_PACKAGE)));
		if (isPrimary()) {
			Menu refactor = menu("menu.refactor");
			refactor.getItems().add(action("menu.refactor.move", Icons.ACTION_MOVE, this::move));
			refactor.getItems().add(action("menu.refactor.rename", Icons.ACTION_EDIT, this::rename));
			menu.getItems().add(action("menu.edit.delete", Icons.ACTION_DELETE, this::delete));
			menu.getItems().add(refactor);
		}
		Menu search = menu("menu.search", Icons.ACTION_SEARCH);
		search.getItems().add(action("menu.search.references", Icons.REFERENCE, this::search));
		menu.getItems().add(search);

		// TODO: Since PackageItems dont know if they belong to a java class or dex class
		//       this breaks on android since the implementations assume java usage

		return menu;
	}

	@Override
	public Resource findContainerResource() {
		Workspace workspace = RecafUI.getController().getWorkspace();
		Resource resource = workspace.getResources().getContainingForPackage(packageName);
		if (resource == null)
			logger.warn("Could not find container resource for package {}", packageName);
		return resource;
	}

	private void delete() {
		String name = TextDisplayUtil.shortenEscapeLimit(packageName);
		Resource resource = getContainingResource();
		if (resource != null) {
			if (Configs.display().promptDeleteItem) {
				StringBinding title = Lang.getBinding("dialog.title.delete-package");
				StringBinding header = Lang.format("dialog.header.delete-package", "\n" + name);
				ConfirmDialog deleteDialog = new ConfirmDialog(title, header, Icons.getImageView(Icons.ACTION_DELETE));
				boolean canRemove = deleteDialog.showAndWait().orElse(false);
				if (!canRemove) {
					return;
				}
			}
			for (String className : new TreeSet<>(resource.getClasses().keySet())) {
				if (className.startsWith(packageName + "/")) {
					boolean removed = resource.getClasses().remove(className) != null;
					if (!removed) {
						logger.warn("Tried to delete class '{}' but failed", name);
					}
				}
			}
			for (String className : new TreeSet<>(resource.getDexClasses().keySet())) {
				if (className.startsWith(packageName + "/")) {
					boolean removed = resource.getDexClasses().remove(className) != null;
					if (!removed) {
						logger.warn("Tried to delete dex class '{}' but failed", name);
					}
				}
			}

		} else {
			logger.error("Failed to resolve containing resource for package '{}'", name);
		}
	}

	private void move() {
		Resource resource = getContainingResource();
		if (resource != null) {
			StringBinding title = Lang.getBinding("dialog.title.move-package");
			StringBinding header = Lang.getBinding("dialog.header.move-package");
			String originalPackage = packageName;
			PackageSelectDialog packageDialog =
					new PackageSelectDialog(title, header, Icons.getImageView(Icons.ACTION_EDIT));
			packageDialog.populate(resource);
			packageDialog.setCurrentPackage(originalPackage);
			Optional<Boolean> moveResult = packageDialog.showAndWait();
			if (moveResult.isPresent() && moveResult.get()) {
				String localName = originalPackage;
				if (localName.contains("/"))
					localName = localName.substring(localName.lastIndexOf('/') + 1);
				// Create mappings to use for renaming.
				String newHostPackage = packageDialog.getSelectedPackage();
				MappingsAdapter mappings = new MappingsAdapter("RECAF-MOVE", false, false);
				// Add mappings for all classes in the package and sub-packages
				for (String className : resource.getClasses().keySet()) {
					if (className.startsWith(originalPackage + "/")) {
						String newPackage = newHostPackage + "/" + localName;
						String renamedClass = className.replace(originalPackage + "/", newPackage + "/");
						mappings.addClass(className, renamedClass);
					}
				}
				// Update all classes in the resource
				applyMappings(resource, mappings);
			}
		} else {
			logger.error("Failed to resolve containing resource for package '{}'", packageName);
		}
	}

	private void rename() {
		String currentPackage = packageName;
		Resource resource = getContainingResource();
		if (resource != null) {
			StringBinding title = Lang.getBinding("dialog.title.rename-package");
			StringBinding header = Lang.getBinding("dialog.header.rename-package");
			TextInputDialog renameDialog = new TextInputDialog(title, header, Icons.getImageView(Icons.ACTION_EDIT));
			renameDialog.setText(currentPackage);
			Optional<Boolean> renameResult = renameDialog.showAndWait();
			if (renameResult.isPresent() && renameResult.get()) {
				// Create mappings to use for renaming.
				String newPackage = renameDialog.getText();
				MappingsAdapter mappings = new MappingsAdapter("RECAF-RENAME", false, false);
				// Add mappings for all classes in the package and sub-packages
				for (String className : resource.getClasses().keySet()) {
					if (className.startsWith(currentPackage + "/")) {
						String renamedClass = className.replace(currentPackage + "/", newPackage + "/");
						mappings.addClass(className, renamedClass);
					}
				}
				// Update all classes in the resource
				applyMappings(resource, mappings);
			}
		} else {
			logger.error("Failed to resolve containing resource for package '{}'", packageName);
		}
	}

	private void search() {
		new GenericWindow(SearchPane.createReferenceSearch(packageName + "/", null, null)).show();
	}
}
