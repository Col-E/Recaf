package software.coley.recaf.ui.pane;

import com.panemu.tiwulfx.control.dock.DetachableTab;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import software.coley.recaf.RecafApplication;
import software.coley.recaf.services.workspace.WorkspaceManager;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.ui.docking.DockingManager;
import software.coley.recaf.ui.docking.DockingRegion;
import software.coley.recaf.ui.docking.DockingTab;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.Lang;
import software.coley.recaf.workspace.model.Workspace;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Root panel for displaying a {@link Workspace}.
 * <br>
 * Provides:
 * <ul>
 *     <li>Searchable tree layout - {@link WorkspaceExplorerPane}</li>
 * </ul>
 * There should only ever be a single instance of this, managed by {@link RecafApplication}.
 *
 * @author Matt Coley
 */
@Dependent
public class WorkspaceRootPane extends BorderPane {
	@Inject
	public WorkspaceRootPane(@Nonnull DockingManager dockingManager,
	                         @Nonnull WorkspaceManager workspaceManager,
	                         @Nonnull Instance<WorkspaceExplorerPane> explorerPaneProvider,
	                         @Nonnull Instance<WorkspaceInformationPane> informationPaneProvider) {
		getStyleClass().add("bg-inset");

		AtomicReference<DockingRegion> lastTreeRegion = new AtomicReference<>();
		AtomicReference<DockingRegion> lastPrimaryRegion = new AtomicReference<>();

		// Register an open listener to fill the pane's regions when a workspace is loaded.
		workspaceManager.addWorkspaceOpenListener(workspace -> {
			// Most of these actions need to be on the UI thread.
			FxThreadUtil.run(() -> {
				DockingRegion dockTree = dockingManager.newRegion();
				DockingRegion dockPrimary = dockingManager.getPrimaryRegion();
				createWorkspaceExplorerTab(dockTree, explorerPaneProvider);
				createPrimaryTab(dockPrimary, informationPaneProvider);

				// Layout
				SplitPane split = new SplitPane(dockTree, dockPrimary);
				SplitPane.setResizableWithParent(dockTree, false);
				split.setDividerPositions(0.333);
				setCenter(split);

				// Record last regions
				lastTreeRegion.set(dockTree);
				lastPrimaryRegion.set(dockPrimary);
			});
		});

		// Register a close listener to remove the old workspace's content from the pane's regions.
		workspaceManager.addWorkspaceCloseListener(workspace -> {
			FxThreadUtil.run(() -> {
				DockingRegion dockTree = lastTreeRegion.get();
				if (dockTree != null) {
					// Close all tabs.
					for (DockingTab tab : new ArrayList<>(dockTree.getDockTabs())) {
						// Mark as closable so they can be closed.
						tab.setClosable(true);

						// When the last tab in the region is closed,
						// the close handler should kick in and clean things up for us.
						// We will validate this below.
						tab.close();
					}
				}
				DockingRegion dockPrimary = lastPrimaryRegion.get();
				if (dockPrimary != null) {
					// Only close closable tabs.
					for (DockingTab tab : new ArrayList<>(dockPrimary.getDockTabs())) {
						tab.close();
					}
				}
			});
		});
	}

	private void createPrimaryTab(@Nonnull DockingRegion region,
	                              @Nonnull Instance<WorkspaceInformationPane> informationPaneProvider) {
		// Add summary of workspace, targeting the primary region.
		// In the UI, this region will persist and be the default location for
		// opening most 'new' content/tabs.
		DetachableTab infoTab = region.createTab(Lang.getBinding("workspace.info"), informationPaneProvider.get());
		infoTab.setGraphic(new FontIconView(CarbonIcons.INFORMATION));
	}

	private void createWorkspaceExplorerTab(@Nonnull DockingRegion region,
	                                        @Nonnull Instance<WorkspaceExplorerPane> explorerPaneProvider) {
		// Add workspace explorer tree.
		DockingTab workspaceTab = region.createTab(Lang.getBinding("workspace.title"), explorerPaneProvider.get());
		workspaceTab.setGraphic(new FontIconView(CarbonIcons.TREE_VIEW));
		workspaceTab.setClosable(false);
		workspaceTab.setDetachable(false);
	}
}
