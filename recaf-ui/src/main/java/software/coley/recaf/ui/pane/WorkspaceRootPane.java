package software.coley.recaf.ui.pane;

import com.panemu.tiwulfx.control.dock.DetachableTab;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.ui.docking.DockingTab;
import software.coley.recaf.ui.docking.DockingManager;
import software.coley.recaf.ui.docking.DockingRegion;
import software.coley.recaf.util.Lang;
import software.coley.recaf.workspace.model.Workspace;

/**
 * Root panel for displaying a {@link Workspace}.
 * <br>
 * Provides:
 * <ul>
 *     <li>Searchable tree layout - {@link WorkspaceExplorerPane}</li>
 * </ul>
 *
 * @author Matt Coley
 */
@Dependent
public class WorkspaceRootPane extends BorderPane {
	@Inject
	public WorkspaceRootPane(@Nonnull DockingManager dockingManager,
							 @Nonnull WorkspaceExplorerPane explorerPane,
							 @Nonnull WorkspaceInformationPane informationPane) {
		getStyleClass().add("bg-inset");

		// Add workspace explorer tree.
		DockingRegion dockTree = dockingManager.newRegion();
		DockingTab treeTab = dockTree.createTab(Lang.getBinding("workspace.title"), explorerPane);
		treeTab.setGraphic(new FontIconView(CarbonIcons.TREE_VIEW));
		treeTab.setClosable(false);
		treeTab.setDetachable(false);

		// Add summary of workspace, targeting the primary region.
		// In the UI, this region will persist and be the default location for
		// opening most 'new' content/tabs.
		DockingRegion dockInfo = dockingManager.getPrimaryRegion();
		DetachableTab infoTab = dockInfo.createTab(Lang.getBinding("workspace.info"), informationPane);
		infoTab.setGraphic(new FontIconView(CarbonIcons.INFORMATION));

		// Layout
		SplitPane split = new SplitPane(dockTree, dockInfo);
		SplitPane.setResizableWithParent(dockTree, false);
		split.setDividerPositions(0.333);
		setCenter(split);
	}
}
