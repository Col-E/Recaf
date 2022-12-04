package me.coley.recaf.ui.menu.component;

import jakarta.inject.Inject;
import javafx.beans.property.SimpleListProperty;
import javafx.scene.Node;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import me.coley.recaf.config.Configs;
import me.coley.recaf.config.container.RecentWorkspacesConfig;
import me.coley.recaf.ui.control.menu.ClosableActionMenuItem;
import me.coley.recaf.ui.menu.MainMenu;
import me.coley.recaf.ui.prompt.WorkspaceActionType;
import me.coley.recaf.ui.prompt.WorkspaceIOPrompts;
import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.ui.window.GenericWindow;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.util.threading.FxThreadUtil;
import me.coley.recaf.util.threading.ThreadUtil;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.WorkspaceCloseListener;
import me.coley.recaf.workspace.WorkspaceManager;
import org.slf4j.Logger;

import java.awt.*;
import java.nio.file.Path;
import java.util.List;

public class FileMenuComponent extends MenuComponent implements WorkspaceCloseListener {
	private static final Logger logger = Logging.get(FileMenuComponent.class);
	private final Menu menuRecent = menu("menu.file.recent", Icons.RECENT);
	private final WorkspaceManager workspaceManager;

	@Inject
	public FileMenuComponent(WorkspaceManager workspaceManager) {
		this.workspaceManager = workspaceManager;
	}

	@Override
	protected Menu create(MainMenu mainMenu) {
		Menu menuFile = menu("menu.file", Icons.WORKSPACE);

		SimpleListProperty<MenuItem> recentItemsProperty = new SimpleListProperty<>(menuRecent.getItems());
		menuRecent.disableProperty().bind(recentItemsProperty.emptyProperty());

		MenuItem itemAddToWorkspace = action("menu.file.addtoworkspace", Icons.PLUS, this::addToWorkspace);
		MenuItem itemExportPrimary = action("menu.file.exportapp", Icons.EXPORT, this::exportPrimary);
		MenuItem itemViewChanges = action("menu.file.modifications", Icons.SWAP, this::openChangeViewer);
		MenuItem itemClose = action("menu.file.close", Icons.ACTION_DELETE, this::closeWorkspace);
		itemAddToWorkspace.disableProperty().bind(mainMenu.noWorkspaceProperty());
		itemExportPrimary.disableProperty().bind(mainMenu.noWorkspaceProperty());
		itemViewChanges.disableProperty().bind(mainMenu.noWorkspaceProperty());
		itemClose.disableProperty().bind(mainMenu.noWorkspaceProperty());

		MenuItem itemQuit = action("menu.file.quit", Icons.CLOSE, this::quit);
		menuFile.getItems().add(itemAddToWorkspace);
		menuFile.getItems().add(action("menu.file.openworkspace", Icons.OPEN_FILE, this::openWorkspace));
		menuFile.getItems().add(action("menu.file.attach", Icons.DEBUG, this::openAttach));
		menuFile.getItems().add(menuRecent);
		menuFile.getItems().add(separator());
		menuFile.getItems().add(itemExportPrimary);
		menuFile.getItems().add(itemViewChanges);
		menuFile.getItems().add(separator());
		menuFile.getItems().add(itemClose);
		menuFile.getItems().add(itemQuit);

		refreshRecent();

		return menuFile;
	}

	@Override
	public void onWorkspaceClosed(Workspace workspace) {
		// Update recent workspaces list in main menu.
		// We do this in the "close" section because its makes it easy to assume
		// that this is the final form of the workspace.
		if (Configs.recentWorkspaces().canSerialize(workspace)) {
			Configs.recentWorkspaces().addWorkspace(workspace);
			refreshRecent();
		}
	}

	/**
	 * Update the recent workspaces menu.
	 */
	public void refreshRecent() {
		menuRecent.getItems().clear();
		for (RecentWorkspacesConfig.WorkspaceModel model : Configs.recentWorkspaces().recentWorkspaces) {
			int libraryCount = model.getLibraries().size();
			String title;
			if (libraryCount > 0) {
				title = model.getPrimary().getSimpleName() + " + " + libraryCount;
			} else {
				title = model.getPrimary().getSimpleName();
			}

			Node graphic = Icons.getPathIcon(model.getPrimary().getPath());
			menuRecent.getItems().add(new ClosableActionMenuItem(title, graphic, () -> {
				try {
					Workspace workspace = model.loadWorkspace();
					FxThreadUtil.run(() -> workspaceManager.setCurrent(workspace));
				} catch (Exception ex) {
					Toolkit.getDefaultToolkit().beep();
					Configs.recentWorkspaces().recentWorkspaces.remove(model);
					logger.error("Failed to open recent workspace for '{}'", title, ex);
				}
			}, () -> Configs.recentWorkspaces().recentWorkspaces.remove(model)));
		}
	}

	private void addToWorkspace() {
		java.util.List<Path> files = WorkspaceIOPrompts.promptWorkspaceFiles();
		if (!files.isEmpty())
			ThreadUtil.run(() -> WorkspaceIOPrompts.handleFiles(files, WorkspaceActionType.ADD_TO_WORKSPACE));
	}

	private void openWorkspace() {
		List<Path> files = WorkspaceIOPrompts.promptWorkspaceFiles();
		if (!files.isEmpty())
			ThreadUtil.run(() -> WorkspaceIOPrompts.handleFiles(files, WorkspaceActionType.CREATE_NEW_WORKSPACE));
	}

	private void openAttach() {
		GenericWindow window = windows.getAttachWindow();
		window.titleProperty().bind(Lang.getBinding("menu.file.attach"));
		window.getStage().setWidth(750);
		window.getStage().setHeight(450);
		window.show();
		window.requestFocus();
	}
	private void exportPrimary() {
		WorkspaceIOPrompts.promptExportApplication();
	}

	private void closeWorkspace() {
		workspaceManager.setCurrent(null);
	}

	private void quit() {
		windows.getMainWindow().close();
	}

	private void openChangeViewer() {
		GenericWindow window = windows.getModificationsWindow();
		window.titleProperty().bind(Lang.getBinding("modifications.title"));
		window.getStage().setWidth(750);
		window.getStage().setHeight(450);
		window.show();
		window.requestFocus();
	}
}
