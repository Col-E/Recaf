package software.coley.recaf.ui.menubar;

import com.panemu.tiwulfx.control.dock.DetachableTab;
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
import software.coley.recaf.ui.config.RecentFilesConfig;
import software.coley.recaf.ui.control.ClosableActionMenuItem;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.ui.control.popup.OpenUrlPopup;
import software.coley.recaf.ui.docking.DockingManager;
import software.coley.recaf.ui.docking.DockingRegion;
import software.coley.recaf.ui.pane.WorkspaceInformationPane;
import software.coley.recaf.ui.wizard.MultiPathWizardPage;
import software.coley.recaf.ui.wizard.SinglePathWizardPage;
import software.coley.recaf.ui.wizard.WizardStage;
import software.coley.recaf.util.ErrorDialogs;
import software.coley.recaf.util.Icons;
import software.coley.recaf.util.Lang;
import software.coley.recaf.workspace.PathExportingManager;
import software.coley.recaf.workspace.PathLoadingManager;
import software.coley.recaf.services.workspace.WorkspaceManager;
import software.coley.recaf.workspace.model.Workspace;

import java.awt.*;
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
	private final Instance<WorkspaceInformationPane> infoPaneProvider;
	private final Instance<OpenUrlPopup> openUrlProvider;
	private final DockingManager dockingManager;
	private final WindowManager windowManager;
	// config
	private final RecentFilesConfig recentFilesConfig;

	@Inject
	public FileMenu(@Nonnull WorkspaceManager workspaceManager,
					@Nonnull PathLoadingManager pathLoadingManager,
					@Nonnull PathExportingManager pathExportingManager,
					@Nonnull Instance<WorkspaceInformationPane> infoPaneProvider,
					@Nonnull Instance<OpenUrlPopup> openUrlProvider,
					@Nonnull DockingManager dockingManager,
					@Nonnull WindowManager windowManager,
					@Nonnull RecentFilesConfig recentFilesConfig) {
		super(workspaceManager);
		this.workspaceManager = workspaceManager;
		this.pathLoadingManager = pathLoadingManager;
		this.pathExportingManager = pathExportingManager;
		this.infoPaneProvider = infoPaneProvider;
		this.openUrlProvider=openUrlProvider;
		this.dockingManager = dockingManager;
		this.windowManager = windowManager;
		this.recentFilesConfig = recentFilesConfig;

		textProperty().bind(getBinding("menu.file"));
		setGraphic(new FontIconView(CarbonIcons.WORKSPACE));

		SimpleListProperty<MenuItem> recentItemsProperty = new SimpleListProperty<>(menuRecent.getItems());
		menuRecent.disableProperty().bind(recentItemsProperty.emptyProperty());

		MenuItem itemAddToWorkspace = action("menu.file.addtoworkspace", CarbonIcons.WORKSPACE_IMPORT, this::addToWorkspace);
		MenuItem itemExportPrimary = action("menu.file.exportapp", CarbonIcons.EXPORT, this::exportCurrent);
		MenuItem itemViewChanges = action("menu.file.modifications", CarbonIcons.COMPARE, this::openChangeViewer);
		MenuItem itemViewSummary = action("menu.file.summary", CarbonIcons.INFORMATION, this::openSummary);
		MenuItem itemClose = action("menu.file.close", CarbonIcons.TRASH_CAN, this::closeWorkspace);
		itemAddToWorkspace.disableProperty().bind(hasWorkspace.not());
		itemExportPrimary.disableProperty().bind(hasWorkspace.not().and(hasAgentWorkspace.not()));
		itemViewChanges.disableProperty().set(true); // TODO: Not-implemented
		// itemViewChanges.disableProperty().bind(hasWorkspace.not());
		itemViewSummary.disableProperty().bind(hasWorkspace.not());
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
		items.add(itemViewSummary);
		items.add(separator());
		items.add(itemClose);
		items.add(itemQuit);

		refreshRecent();
	}

	@Override
	protected void workspaceStateChanged() {
		// Add
		Workspace current = workspaceManager.getCurrent();
		if (current != null)
			recentFilesConfig.addWorkspace(current);

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
				Node graphic = Icons.getIconView(Icons.FILE_JAR); // TODO: Derive proper icon
				menuRecent.getItems().add(new ClosableActionMenuItem(title, graphic, () -> {
					// Get paths from model
					Path primaryPath = Paths.get(model.primary().path());
					List<Path> supportingPaths = model.libraries().stream()
							.map(resource -> Paths.get(resource.path()))
							.toList();

					// Pass to loader
					pathLoadingManager.asyncNewWorkspace(primaryPath, supportingPaths, ex -> {
						Toolkit.getDefaultToolkit().beep();
						recentWorkspaces.remove(model);
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
		SinglePathWizardPage pagePrimary = new SinglePathWizardPage(getBinding("dialog.title.primary"), recentFilesConfig);
		MultiPathWizardPage pageSupporting = new MultiPathWizardPage(getBinding("dialog.title.supporting"), recentFilesConfig);

		// Create the window for the wizard and display it.
		Stage stage = new WizardStage(List.of(pagePrimary, pageSupporting), () -> {
			Path primaryPath = pagePrimary.getPath();
			List<Path> supportingPaths = pageSupporting.getPaths();

			// Pass paths to loader.
			pathLoadingManager.asyncNewWorkspace(primaryPath, supportingPaths, ex -> {
				Toolkit.getDefaultToolkit().beep();
				logger.error("Failed to load workspace from selected files. Primary file: {}",
						primaryPath.getFileName().toString(), ex);
				ErrorDialogs.show(
						getBinding("dialog.error.loadworkspace.title"),
						getBinding("dialog.error.loadworkspace.header"),
						getBinding("dialog.error.loadworkspace.content"),
						ex
				);
			});
		});
		stage.setMinWidth(630);
		stage.setMinHeight(390);
		stage.show();
		windowManager.registerAnonymous(stage);
	}

	/**
	 * Display a wizard for adding additional resources.
	 */
	private void addToWorkspace() {
		MultiPathWizardPage pageSupporting = new MultiPathWizardPage(getBinding("dialog.title.supporting"), recentFilesConfig);

		// Create the window for the wizard and display it.
		Stage stage = new WizardStage(List.of(pageSupporting), () -> {
			// Validate workspace is open.
			Workspace current = workspaceManager.getCurrent();
			if (current == null) throw new IllegalStateException("Cannot add resources, no workspace is open!");

			// Pass paths to loader.
			List<Path> supportingPaths = pageSupporting.getPaths();
			pathLoadingManager.asyncAddSupportingResourcesToWorkspace(current, supportingPaths, ex -> {
				Toolkit.getDefaultToolkit().beep();
				logger.error("Failed to load supporting resources from selected files.", ex);
				ErrorDialogs.show(
						getBinding("dialog.error.loadsupport.title"),
						getBinding("dialog.error.loadsupport.header"),
						getBinding("dialog.error.loadsupport.content"),
						ex
				);
			});
		});
		stage.setMinWidth(630);
		stage.setMinHeight(390);
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
	}

	/**
	 * Display the workspace summary / current information.
	 */
	private void openSummary() {
		WorkspaceInformationPane informationPane = infoPaneProvider.get();
		DockingRegion dockInfo = dockingManager.getPrimaryRegion();
		DetachableTab infoTab = dockInfo.createTab(Lang.getBinding("workspace.info"), informationPane);
		infoTab.getTabPane().getSelectionModel().select(infoTab);
		infoTab.setGraphic(new FontIconView(CarbonIcons.INFORMATION));
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
