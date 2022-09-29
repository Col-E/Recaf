package me.coley.recaf.ui.context;

import javafx.beans.binding.StringBinding;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import me.coley.recaf.RecafUI;
import me.coley.recaf.code.FileInfo;
import me.coley.recaf.config.Configs;
import me.coley.recaf.ui.CommonUX;
import me.coley.recaf.ui.control.code.Language;
import me.coley.recaf.ui.control.code.Languages;
import me.coley.recaf.ui.control.menu.ActionMenuItem;
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

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static me.coley.recaf.ui.util.Menus.*;

/**
 * Context menu builder for files.
 *
 * @author Matt Coley
 */
public class FileContextBuilder extends ContextBuilder {
	private FileInfo info;
	private Node icon;

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

	/**
	 * @param icon
	 * 		File icon.
	 *
	 * @return Builder.
	 */
	public FileContextBuilder setIcon(Node icon) {
		this.icon = icon;
		return this;
	}

	@Override
	public ContextMenu build() {
		String name = info.getName();
		String extension = info.getExtension();
		ContextMenu menu = new ContextMenu();
		Node icon = this.icon;
		if (icon == null) {
			icon = Icons.getFileIcon(info);
		}
		menu.getItems().add(createHeader(TextDisplayUtil.shortenEscapeLimit(name), icon));
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
		// Add all the available languages
		Menu associationOverride = menu("menu.association.override", Icons.ACTION_EDIT);
		List<ActionMenuItem> items = Languages.allLanguages().stream()
				.sorted(Comparator.comparing(Language::getName))
				.map(language -> new ActionMenuItem(language.getName(), () -> Languages.setExtensionAssociation(extension, language)))
				.collect(Collectors.toList());
		associationOverride.getItems().addAll(items);
		menu.getItems().add(associationOverride);
		return menu;
	}

	@Override
	public Resource findContainerResource() {
		String name = info.getName();
		Workspace workspace = RecafUI.getController().getWorkspace();
		Resource resource = workspace.getResources().getContainingForFile(name);
		if (resource == null)
			logger.warn("Could not find container resource for file {}", name);
		return resource;
	}

	private void openFile() {
		CommonUX.openFile(info);
	}

	private void copy() {
		String name = info.getName();
		Resource resource = getContainingResource();
		if (resource != null) {
			StringBinding title = Lang.getBinding("dialog.title.copy-file");
			StringBinding header = Lang.getBinding("dialog.header.copy-file");
			TextInputDialog copyDialog = new TextInputDialog(title, header, Icons.getImageView(Icons.ACTION_COPY));
			copyDialog.setText(name);
			Optional<Boolean> copyResult = copyDialog.showAndWait();
			if (copyResult.isPresent() && copyResult.get()) {
				String newName = copyDialog.getText();
				resource.getFiles().put(new FileInfo(newName, info.getValue()));
			}
		} else {
			logger.error("Failed to resolve containing resource for file '{}'", name);
		}
	}

	private void delete() {
		String name = TextDisplayUtil.shortenEscapeLimit(info.getName());
		Resource resource = getContainingResource();
		if (resource != null) {
			if (Configs.display().promptDeleteItem) {
				StringBinding title = Lang.getBinding("dialog.title.delete-file");
				StringBinding header = Lang.format("dialog.header.delete-file", "\n" + name);
				ConfirmDialog deleteDialog = new ConfirmDialog(title, header, Icons.getImageView(Icons.ACTION_DELETE));
				boolean canRemove = deleteDialog.showAndWait().orElse(false);
				if (!canRemove) {
					return;
				}
			}
			boolean removed = resource.getFiles().remove(info.getName()) != null;
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
			StringBinding title = Lang.getBinding("dialog.title.move-file");
			StringBinding header = Lang.getBinding("dialog.header.move-file");
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
			StringBinding title = Lang.getBinding("dialog.title.rename-file");
			StringBinding header = Lang.getBinding("dialog.header.rename-file");
			TextInputDialog renameDialog = new TextInputDialog(title, header, Icons.getImageView(Icons.ACTION_EDIT));
			renameDialog.setText(name);
			Optional<Boolean> renameResult = renameDialog.showAndWait();
			if (renameResult.isPresent() && renameResult.get()) {
				String newName = renameDialog.getText();
				// Display warning if that name is already used
				if (resource.getFiles().containsKey(newName)) {
					StringBinding warningTitle = Lang.getBinding("dialog.title.rename-file-warning");
					StringBinding warningHeader = Lang.getBinding("dialog.header.rename-file-warning");
					ConfirmDialog warningDialog = new ConfirmDialog(warningTitle, warningHeader,
							Icons.getImageView(Icons.WARNING));
					Optional<Boolean> warningResult = warningDialog.showAndWait();
					if (warningResult.isEmpty() || !warningResult.get()) {
						return;
					}
				}
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
