package me.coley.recaf.ui.context;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import me.coley.recaf.RecafUI;
import me.coley.recaf.code.FileInfo;
import me.coley.recaf.config.Configs;
import me.coley.recaf.ui.CommonUX;
import me.coley.recaf.ui.dialog.ConfirmDialog;
import me.coley.recaf.ui.dialog.DirectorySelectDialog;
import me.coley.recaf.ui.dialog.TextInputDialog;
import me.coley.recaf.ui.pane.SearchPane;
import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.ui.window.GenericWindow;
import me.coley.recaf.util.StringUtil;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.resource.Resource;

import java.util.Optional;

import static me.coley.recaf.ui.util.Menus.*;

/**
 * Context menu builder for files.
 *
 * @author Matt Coley
 */
public class FileContextBuilder extends ContextBuilder {
	private FileInfo info;

	/**
	 * @param info
	 * 		File info.
	 *
	 * @return Builder.
	 */
	public FileContextBuilder setFileInfo(FileInfo info) {
		this.info = info;
		return this;
	}

	@Override
	public ContextMenu build() {
		String name = info.getName();
		ContextMenu menu = new ContextMenu();
		menu.getItems().add(createHeader(StringUtil.shortenPath(name), Icons.getFileIcon(info)));
		menu.getItems().add(action("menu.goto.file", Icons.OPEN, this::openFile));
		if (isPrimary()) {
			Menu refactor = menu("menu.refactor");
			refactor.getItems().add(action("menu.refactor.move", Icons.ACTION_MOVE, this::move));
			refactor.getItems().add(action("menu.refactor.rename", Icons.ACTION_EDIT, this::rename));
			menu.getItems().add(action("menu.edit.copy", Icons.ACTION_COPY, this::copy));
			menu.getItems().add(action("menu.edit.delete", Icons.ACTION_DELETE, this::delete));
			menu.getItems().add(refactor);
		}
		Menu search = menu("menu.search", Icons.ACTION_SEARCH);
		search.getItems().add(action("menu.search.references", Icons.QUOTE, this::search));
		menu.getItems().add(search);
		return menu;
	}

	@Override
	public Resource findContainerResource() {
		String name = info.getName();
		Workspace workspace = RecafUI.getController().getWorkspace();
		Resource resource = workspace.getResources().getPrimary();
		if (resource.getFiles().containsKey(name))
			return resource;
		for (Resource library : workspace.getResources().getLibraries()) {
			if (library.getFiles().containsKey(name))
				return library;
		}
		logger.warn("Could not find container resource for file {}", name);
		return null;
	}

	private void openFile() {
		CommonUX.openFile(info);
	}

	private void copy() {
		String name = info.getName();
		Resource resource = getContainingResource();
		if (resource != null) {
			String title = Lang.get("dialog.title.copy-file");
			String header = Lang.get("dialog.header.copy-file");
			TextInputDialog copyDialog = new TextInputDialog(title, header, Icons.getImageView(Icons.ACTION_COPY));
			copyDialog.setName(name);
			Optional<Boolean> copyResult = copyDialog.showAndWait();
			if (copyResult.isPresent() && copyResult.get()) {
				String newName = copyDialog.getName();
				resource.getFiles().put(new FileInfo(newName, info.getValue()));
			}
		} else {
			logger.error("Failed to resolve containing resource for file '{}'", name);
		}
	}

	private void delete() {
		String name = info.getName();
		Resource resource = getContainingResource();
		if (resource != null) {
			if (Configs.display().promptDeleteItem) {
				String title = Lang.get("dialog.title.delete-file");
				String header = String.format(Lang.get("dialog.header.delete-file"), "\n" + name);
				ConfirmDialog deleteDialog = new ConfirmDialog(title, header, Icons.getImageView(Icons.ACTION_DELETE));
				boolean canRemove = deleteDialog.showAndWait().orElse(false);
				if (!canRemove) {
					return;
				}
			}
			boolean removed = resource.getFiles().remove(name) != null;
			if (!removed) {
				logger.warn("Tried to delete file '{}' but failed", name);
			}
		} else {
			logger.error("Failed to resolve containing resource for file '{}'", name);
		}
	}

	private void move() {
		String name = info.getName();
		Resource resource = getContainingResource();
		if (resource != null) {
			String title = Lang.get("dialog.title.move-file");
			String header = Lang.get("dialog.header.move-file");
			int directorySeparator = name.lastIndexOf('/');
			String currentDirectory = directorySeparator > 0 ? name.substring(0, directorySeparator) : "";
			DirectorySelectDialog directoryDialog = new DirectorySelectDialog(title, header,
					Icons.getImageView(Icons.ACTION_EDIT));
			directoryDialog.populate(resource);
			directoryDialog.setCurrentDirectory(currentDirectory);
			Optional<Boolean> moveResult = directoryDialog.showAndWait();
			if (moveResult.isPresent() && moveResult.get()) {
				// Create mappings to use for renaming.
				String newDirectory = directoryDialog.getSelectedDirectory();
				String newName = newDirectory + "/" + name.substring(directorySeparator + 1);
				resource.getFiles().remove(name);
				resource.getFiles().put(new FileInfo(newName, info.getValue()));
			}
		} else {
			logger.error("Failed to resolve containing resource for file '{}'", name);
		}
	}

	private void rename() {
		String name = info.getName();
		Resource resource = getContainingResource();
		if (resource != null) {
			String title = Lang.get("dialog.title.rename-file");
			String header = Lang.get("dialog.header.rename-file");
			TextInputDialog renameDialog = new TextInputDialog(title, header, Icons.getImageView(Icons.ACTION_EDIT));
			renameDialog.setName(name);
			Optional<Boolean> renameResult = renameDialog.showAndWait();
			if (renameResult.isPresent() && renameResult.get()) {
				String newName = renameDialog.getName();
				resource.getFiles().remove(name);
				resource.getFiles().put(newName, new FileInfo(newName, info.getValue()));
			}
		} else {
			logger.error("Failed to resolve containing resource for file '{}'", name);
		}
	}

	private void search() {
		new GenericWindow(SearchPane.createTextSearch(info.getName())).show();
	}
}
