package software.coley.recaf.ui.menubar;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.stage.Stage;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.services.window.WindowManager;
import software.coley.recaf.services.workspace.WorkspaceManager;
import software.coley.recaf.ui.config.RecentFilesConfig;
import software.coley.recaf.ui.control.ClosableActionMenuItem;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.ui.control.popup.OpenUrlPopup;
import software.coley.recaf.ui.docking.DockingManager;
import software.coley.recaf.ui.pane.WorkspaceBuilderPane;
import software.coley.recaf.ui.window.RecafScene;
import software.coley.recaf.ui.window.RecafStage;
import software.coley.recaf.util.ErrorDialogs;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.IOUtil;
import software.coley.recaf.util.Icons;
import software.coley.recaf.util.Lang;
import software.coley.recaf.workspace.PathExportingManager;
import software.coley.recaf.workspace.PathLoadingManager;
import software.coley.recaf.workspace.model.Workspace;

import java.awt.Toolkit;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static software.coley.recaf.util.Lang.getBinding;
import static software.coley.recaf.util.Menus.*;

/**
 * File menu component for {@link MainMenu}.
 *
 * @author Matt Coley
 */
@Dependent
public class FileMenu extends WorkspaceAwareMenu {
	private static final Logger logger = Logging.get(FileMenu.class);
	private final Menu menuRecent = menu("menu.file.recent", CarbonIcons.TIME);
	private final WorkspaceManager workspaceManager;
	private final PathLoadingManager pathLoadingManager;
	private final PathExportingManager pathExportingManager;
	private final Instance<OpenUrlPopup> openUrlProvider;
	private final WindowManager windowManager;
	// config
	private final RecentFilesConfig recentFilesConfig;

	@Inject
	public FileMenu(@Nonnull WorkspaceManager workspaceManager,
	                @Nonnull PathLoadingManager pathLoadingManager,
	                @Nonnull PathExportingManager pathExportingManager,
	                @Nonnull Instance<OpenUrlPopup> openUrlProvider,
	                @Nonnull DockingManager dockingManager,
	                @Nonnull WindowManager windowManager,
	                @Nonnull RecentFilesConfig recentFilesConfig) {
		super(workspaceManager);
		this.workspaceManager = workspaceManager;
		this.pathLoadingManager = pathLoadingManager;
		this.pathExportingManager = pathExportingManager;
		this.openUrlProvider = openUrlProvider;
		this.windowManager = windowManager;
		this.recentFilesConfig = recentFilesConfig;

		textProperty().bind(getBinding("menu.file"));
		setGraphic(new FontIconView(CarbonIcons.WORKSPACE));

		SimpleListProperty<MenuItem> recentItemsProperty = new SimpleListProperty<>(menuRecent.getItems());
		menuRecent.disableProperty().bind(recentItemsProperty.emptyProperty());

		MenuItem itemAddToWorkspace = action("menu.file.addtoworkspace", CarbonIcons.WORKSPACE_IMPORT, this::addToWorkspace);
		MenuItem itemExportPrimary = action("menu.file.exportapp", CarbonIcons.EXPORT, this::exportCurrent);
		MenuItem itemViewChanges = action("menu.file.modifications", CarbonIcons.COMPARE, this::openChangeViewer);
		MenuItem itemClose = action("menu.file.close", CarbonIcons.TRASH_CAN, this::closeWorkspace);
		itemAddToWorkspace.disableProperty().bind(hasWorkspace.not());
		itemExportPrimary.disableProperty().bind(hasWorkspace.not().and(hasAgentWorkspace.not()));
		itemViewChanges.disableProperty().set(true); // TODO: Not-implemented
		// itemViewChanges.disableProperty().bind(hasWorkspace.not());
		itemClose.disableProperty().bind(hasWorkspace.not());

		MenuItem itemQuit = action("menu.file.quit", CarbonIcons.CLOSE, this::quit);
		ObservableList<MenuItem> items = getItems();
		items.add(action("menu.file.openworkspace", CarbonIcons.FOLDER_ADD, this::openWorkspace));
		items.add(itemAddToWorkspace);
		items.add(menuRecent);
		items.add(action("menu.file.attach", CarbonIcons.DEBUG, this::openAttach));
		items.add(action("menu.file.openurl", CarbonIcons.ATTACHMENT, this::openUrl));
		items.add(separator());
		items.add(itemExportPrimary);
		items.add(itemViewChanges);
		items.add(separator());
		items.add(itemClose);
		items.add(itemQuit);

		refreshRecent();
	}

	@Override
	protected void workspaceStateChanged() {
		// Add
		if (workspaceManager.hasCurrentWorkspace()) {
			Workspace current = workspaceManager.getCurrent();
			recentFilesConfig.addWorkspace(current);
		}

		// Refresh
		refreshRecent();
	}

	/**
	 * Update the items in the recent workspace menu.
	 */
	public void refreshRecent() {
		menuRecent.getItems().clear();
		List<RecentFilesConfig.WorkspaceModel> recentWorkspaces = recentFilesConfig.getRecentWorkspaces().getValue();
		for (RecentFilesConfig.WorkspaceModel model : recentWorkspaces) {
			int libraryCount = model.libraries().size();
			String title;
			if (libraryCount > 0) {
				title = model.primary().getSimpleName() + " + " + libraryCount;
			} else {
				title = model.primary().getSimpleName();
			}

			Runnable remove = () -> recentWorkspaces.remove(model);
			if (model.canLoadWorkspace()) {
				// Workspace can be loaded
				String primaryPathString = model.primary().path();
				String extension = IOUtil.getExtension(primaryPathString);
				Node graphic = new File(primaryPathString).isDirectory() ?
						Icons.getIconView(Icons.FOLDER) :
						Icons.getIconView(Icons.getIconPathForFileExtension(extension));
				menuRecent.getItems().add(new ClosableActionMenuItem(title, graphic, () -> {
					// If the model can no longer be loaded remove it.
					if (!model.canLoadWorkspace()) {
						Toolkit.getDefaultToolkit().beep();
						remove.run();
						logger.warn("Recent workspace for '{}' cannot be found", title);
						refreshRecent();
						return;
					}

					// Get paths from model
					Path primaryPath = Paths.get(primaryPathString);
					List<Path> supportingPaths = model.libraries().stream()
							.map(resource -> Paths.get(resource.path()))
							.toList();

					// Pass to loader
					pathLoadingManager.asyncNewWorkspace(primaryPath, supportingPaths, ex -> {
						Toolkit.getDefaultToolkit().beep();
						recentWorkspaces.remove(model);
						refreshRecent();
						logger.error("Failed to open recent workspace for '{}'", title, ex);
						ErrorDialogs.show(
								getBinding("dialog.error.loadworkspace.title"),
								getBinding("dialog.error.loadworkspace.header"),
								getBinding("dialog.error.loadworkspace.content"),
								ex
						);
					});
				}, remove));
			} else {
				// Workspace cannot be loaded (missing data), keep in list in-case user can restore file,
				// but allow user to remove it on their own too.
				Node graphic = new FontIconView(CarbonIcons.UNKNOWN);
				menuRecent.getItems().add(new ClosableActionMenuItem(title, graphic, remove, remove));
			}
		}
	}

	/**
	 * Display the workspace wizard.
	 */
	private void openWorkspace() {
		Stage stage = new RecafStage();
		WorkspaceBuilderPane root = new WorkspaceBuilderPane(pathLoadingManager, recentFilesConfig, () -> FxThreadUtil.run(stage::close));
		stage.titleProperty().bind(Lang.getBinding("dialog.title.create-workspace"));
		stage.setScene(new RecafScene(root));
		stage.setMinWidth(650);
		stage.setMinHeight(400);
		stage.show();
		windowManager.registerAnonymous(stage);
	}

	/**
	 * Display a wizard for adding additional resources.
	 */
	private void addToWorkspace() {
		if (!workspaceManager.hasCurrentWorkspace())
			return;
		Stage stage = new RecafStage();
		WorkspaceBuilderPane root = new WorkspaceBuilderPane(pathLoadingManager, recentFilesConfig, workspaceManager.getCurrent(), () -> FxThreadUtil.run(stage::close));
		stage.titleProperty().bind(Lang.getBinding("menu.file.addtoworkspace"));
		stage.setScene(new RecafScene(root));
		stage.setMinWidth(650);
		stage.setMinHeight(400);
		stage.show();
		windowManager.registerAnonymous(stage);
	}

	/**
	 * Display the attach window.
	 */
	private void openAttach() {
		Stage remoteVmWindow = windowManager.getRemoteVmWindow();
		remoteVmWindow.show();
		remoteVmWindow.requestFocus();
	}

	/**
	 * Display the open-url window.
	 */
	private void openUrl() {
		OpenUrlPopup popup = openUrlProvider.get();
		popup.show();
		popup.requestInputFocus();
		popup.setOnHiding(e -> openUrlProvider.destroy(popup));
	}

	/**
	 * Display the change viewer window.
	 */
	private void openChangeViewer() {
		// TODO: Reimplement change viewer, give it its own @Dependent window like 'RemoteVirtualMachinesWindow'
		//       and the behavior above in 'openAttach'
	}

	/**
	 * Delegate to {@link PathExportingManager#exportCurrent()}.
	 */
	private void exportCurrent() {
		pathExportingManager.exportCurrent();
	}

	/**
	 * Delegate to {@link WorkspaceManager#closeCurrent()}.
	 */
	private void closeWorkspace() {
		workspaceManager.closeCurrent();
	}

	/**
	 * Close all windows, which should trigger application shutdown.
	 */
	private void quit() {
		// Close all windows. The main window's exit handler should handle the application shutdown.
		for (Stage window : new ArrayList<>(windowManager.getActiveWindows()))
			window.close();
	}
}
