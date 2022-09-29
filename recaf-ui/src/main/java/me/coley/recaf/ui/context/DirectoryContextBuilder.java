package me.coley.recaf.ui.context;

import javafx.beans.binding.StringBinding;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import me.coley.recaf.RecafUI;
import me.coley.recaf.code.FileInfo;
import me.coley.recaf.config.Configs;
import me.coley.recaf.ui.dialog.ConfirmDialog;
import me.coley.recaf.ui.dialog.DirectorySelectDialog;
import me.coley.recaf.ui.dialog.TextInputDialog;
import me.coley.recaf.ui.pane.SearchPane;
import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.ui.window.GenericWindow;
import me.coley.recaf.util.TextDisplayUtil;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.resource.Resource;

import java.util.ArrayList;
import java.util.Optional;
import java.util.TreeSet;

import static me.coley.recaf.ui.util.Menus.*;

/**
 * Context menu builder for directories <i>(For paths in {@link Resource#getFiles()})</i>.
 *
 * @author Matt Coley
 */
public class DirectoryContextBuilder extends ContextBuilder {
	private String directoryName;

	/**
	 * @param directoryName
	 * 		Name of directory.
	 *
	 * @return Builder.
	 */
	public DirectoryContextBuilder setDirectoryName(String directoryName) {
		this.directoryName = directoryName;
		return this;
	}

	@Override
	public ContextMenu build() {
		String name = directoryName;
		ContextMenu menu = new ContextMenu();
		menu.getItems().add(createHeader(TextDisplayUtil.shortenEscapeLimit(name), Icons.getIconView(Icons.FOLDER)));
		if (isPrimary()) {
			Menu refactor = menu("menu.refactor");
			menu.getItems().add(action("menu.edit.copy", Icons.ACTION_COPY, this::copy));
			menu.getItems().add(action("menu.edit.delete", Icons.ACTION_DELETE, this::delete));
			refactor.getItems().add(action("menu.refactor.move", Icons.ACTION_MOVE, this::move));
			refactor.getItems().add(action("menu.refactor.rename", Icons.ACTION_EDIT, this::rename));
			menu.getItems().add(refactor);
		}
		Menu search = menu("menu.search", Icons.ACTION_SEARCH);
		search.getItems().add(action("menu.search.references", Icons.QUOTE, this::search));
		menu.getItems().add(search);
		return menu;
	}

	@Override
	public Resource findContainerResource() {
		Workspace workspace = RecafUI.getController().getWorkspace();
		Resource resource = workspace.getResources().getContainingForDirectory(directoryName);
		if (resource == null)
			logger.warn("Could not find container resource for directory {}", directoryName);
		return resource;
	}

	private void copy() {
		Resource resource = getContainingResource();
		if (resource != null) {
			String originalDirectory = directoryName;
			StringBinding title = Lang.getBinding("dialog.title.copy-directory");
			StringBinding header = Lang.getBinding("dialog.header.copy-directory");
			TextInputDialog copyDialog = new TextInputDialog(title, header, Icons.getImageView(Icons.ACTION_COPY));
			copyDialog.setText(originalDirectory);
			Optional<Boolean> copyResult = copyDialog.showAndWait();
			if (copyResult.isPresent() && copyResult.get()) {
				String renamedDirectory = copyDialog.getText();
				for (FileInfo fileInfo : new ArrayList<>(resource.getFiles().values())) {
					String fileName = fileInfo.getName();
					if (fileName.startsWith(originalDirectory + "/")) {
						String renamedFile = fileName.replace(originalDirectory + "/", renamedDirectory + "/");
						resource.getFiles().put(new FileInfo(renamedFile, fileInfo.getValue()));
					}
				}
			}
		} else {
			logger.error("Failed to resolve containing resource for file '{}'", directoryName);
		}
	}


	private void delete() {
		Resource resource = getContainingResource();
		if (resource != null) {
			if (Configs.display().promptDeleteItem) {
				StringBinding title = Lang.getBinding("dialog.title.delete-directory");
				StringBinding header = Lang.format("dialog.header.delete-directory", "\n" + TextDisplayUtil.shortenEscapeLimit(directoryName));
				ConfirmDialog deleteDialog = new ConfirmDialog(title, header, Icons.getImageView(Icons.ACTION_DELETE));
				boolean canRemove = deleteDialog.showAndWait().orElse(false);
				if (!canRemove) {
					return;
				}
			}
			for (String fileName : new TreeSet<>(resource.getFiles().keySet())) {
				if (fileName.startsWith(directoryName + "/")) {
					boolean removed = resource.getFiles().remove(fileName) != null;
					if (!removed) {
						logger.warn("Tried to delete file '{}' but failed", directoryName);
					}
				}
			}

		} else {
			logger.error("Failed to resolve containing resource for directory '{}'", directoryName);
		}
	}

	private void move() {
		Resource resource = getContainingResource();
		if (resource != null) {
			StringBinding title = Lang.getBinding("dialog.title.move-directory");
			StringBinding header = Lang.getBinding("dialog.header.move-directory");
			String originalDirectory = directoryName;
			DirectorySelectDialog directoryDialog =
					new DirectorySelectDialog(title, header, Icons.getImageView(Icons.ACTION_EDIT));
			directoryDialog.populate(resource);
			directoryDialog.setCurrentDirectory(originalDirectory);
			Optional<Boolean> moveResult = directoryDialog.showAndWait();
			if (moveResult.isPresent() && moveResult.get()) {
				String localName = originalDirectory;
				if (localName.contains("/"))
					localName = localName.substring(localName.lastIndexOf('/') + 1);
				// Move files within the directory
				String newHostDirectory = directoryDialog.getSelectedDirectory();
				for (FileInfo fileInfo : new ArrayList<>(resource.getFiles().values())) {
					String fileName = fileInfo.getName();
					if (fileName.startsWith(originalDirectory + "/")) {
						String newDirectory = newHostDirectory + "/" + localName;
						String renamedFile = fileName.replace(originalDirectory + "/", newDirectory + "/");
						resource.getFiles().remove(fileName);
						resource.getFiles().put(new FileInfo(renamedFile, fileInfo.getValue()));
					}
				}
			}
		} else {
			logger.error("Failed to resolve containing resource for directory '{}'", directoryName);
		}
	}

	private void rename() {
		Resource resource = getContainingResource();
		if (resource != null) {
			String originalDirectory = directoryName;
			StringBinding title = Lang.getBinding("dialog.title.rename-directory");
			StringBinding header = Lang.getBinding("dialog.header.rename-directory");
			TextInputDialog renameDialog = new TextInputDialog(title, header, Icons.getImageView(Icons.ACTION_EDIT));
			renameDialog.setText(originalDirectory);
			Optional<Boolean> renameResult = renameDialog.showAndWait();
			if (renameResult.isPresent() && renameResult.get()) {
				// Move files into the new directory
				String newDirectory = renameDialog.getText();
				for (FileInfo fileInfo : new ArrayList<>(resource.getFiles().values())) {
					String fileName = fileInfo.getName();
					if (fileName.startsWith(originalDirectory + "/")) {
						String renamedFile = fileName.replace(originalDirectory + "/", newDirectory + "/");
						resource.getFiles().remove(fileName);
						resource.getFiles().put(new FileInfo(renamedFile, fileInfo.getValue()));
					}
				}
			}
		} else {
			logger.error("Failed to resolve containing resource for directory '{}'", directoryName);
		}
	}

	private void search() {
		new GenericWindow(SearchPane.createTextSearch(directoryName)).show();
	}
}
