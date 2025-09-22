package software.coley.recaf.ui.docking;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import javafx.geometry.Orientation;
import javafx.geometry.Side;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import org.slf4j.Logger;
import software.coley.bentofx.building.DockBuilding;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.layout.DockContainer;
import software.coley.bentofx.layout.container.DockContainerBranch;
import software.coley.bentofx.layout.container.DockContainerLeaf;
import software.coley.bentofx.layout.container.DockContainerRootBranch;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.services.info.summary.ResourceSummaryServiceConfig;
import software.coley.recaf.services.navigation.Actions;
import software.coley.recaf.services.navigation.NavigationManager;
import software.coley.recaf.services.workspace.WorkspaceCloseListener;
import software.coley.recaf.services.workspace.WorkspaceManager;
import software.coley.recaf.services.workspace.WorkspaceOpenListener;
import software.coley.recaf.ui.control.ActionMenuItem;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.ui.pane.LoggingPane;
import software.coley.recaf.ui.pane.WelcomePane;
import software.coley.recaf.ui.pane.WorkspaceExplorerPane;
import software.coley.recaf.ui.pane.WorkspaceInformationPane;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.Lang;
import software.coley.recaf.workspace.model.Workspace;

/**
 * Manages updates to the UI layout.
 * <p/>
 * This currently covers:
 * <ul>
 *     <li>Displaying {@link WelcomePane} when no workspace is open</li>
 *     <li>Displaying {@link WorkspaceExplorerPane} when a workspace is open</li>
 * </ul>
 *
 * @see DockingManager
 * @see NavigationManager
 */
@ApplicationScoped
public class DockingLayoutManager {
	private static final Logger logger = Logging.get(DockingLayoutManager.class);

	/** Size in px for {@link #newBottomContainer()} */
	private static final double BOTTOM_SIZE = 100;
	/** Size in px for the {@link WorkspaceExplorerPane} in {@link #newWorkspaceContainer()} */
	private static final double TOP_WORKSPACE_EXPLORER_SIZE = 0.25;
	/** Split layout holding {@link #ID_CONTAINER_ROOT_TOP} and {@link #ID_CONTAINER_ROOT_BOTTOM} */
	public static final String ID_CONTAINER_ROOT_SPLIT = "layout-root-split";
	/** Top half of the main UI at initial layout. */
	public static final String ID_CONTAINER_ROOT_TOP = "layout-root-top";
	/** Bottom half of the main UI at initial layout. */
	public static final String ID_CONTAINER_ROOT_BOTTOM = "layout-root-bottom";
	/** {@link DockContainerLeaf} holding the {@link WorkspaceExplorerPane} */
	public static final String ID_CONTAINER_WORKSPACE_TOOLS = "layout-workspace-tools";
	/** {@link DockContainerLeaf} holding the primary editor tabs. */
	public static final String ID_CONTAINER_WORKSPACE_PRIMARY = "layout-workspace-primary";

	private final DockingManager dockingManager;
	private final Actions actions;
	private final Instance<LoggingPane> loggingPaneProvider;
	private final Instance<WelcomePane> welcomePaneProvider;
	private final Instance<WorkspaceExplorerPane> workspaceExplorerProvider;
	private final DockContainerRootBranch root;
	private final ResourceSummaryServiceConfig resourceSummaryConfig;

	@Inject
	public DockingLayoutManager(@Nonnull DockingManager dockingManager,
	                            @Nonnull WorkspaceManager workspaceManager,
	                            @Nonnull Actions actions,
	                            @Nonnull Instance<LoggingPane> loggingPaneProvider,
	                            @Nonnull Instance<WelcomePane> welcomePaneProvider,
	                            @Nonnull Instance<WorkspaceInformationPane> workspaceInfoProvider,
	                            @Nonnull Instance<WorkspaceExplorerPane> workspaceExplorerProvider,
	                            @Nonnull ResourceSummaryServiceConfig resourceSummaryConfig) {
		this.dockingManager = dockingManager;
		this.actions = actions;
		this.loggingPaneProvider = loggingPaneProvider;
		this.welcomePaneProvider = welcomePaneProvider;
		this.workspaceExplorerProvider = workspaceExplorerProvider;
		this.resourceSummaryConfig = resourceSummaryConfig;

		// Register listener
		ListenerHost host = new ListenerHost();
		workspaceManager.addWorkspaceOpenListener(host);
		workspaceManager.addWorkspaceCloseListener(host);

		// Create root
		DockBuilding builder = dockingManager.getBento().dockBuilding();
		DockContainer top = newWelcomeContainer();
		DockContainer bottom = newBottomContainer();
		builder.root(ID_CONTAINER_ROOT_SPLIT);
		root = builder.root(ID_CONTAINER_ROOT_SPLIT);
		root.setOrientation(Orientation.VERTICAL);
		root.addContainers(top, bottom);
		root.setContainerSizePx(bottom, BOTTOM_SIZE);
		dockingManager.getBento().registerRoot(root);
	}

	@Nonnull
	public DockContainerRootBranch getRoot() {
		return root;
	}

	@Nonnull
	private DockContainerLeaf newWelcomeContainer() {
		Dockable welcome = dockingManager.newTranslatableDockable("welcome.title", CarbonIcons.EARTH_FILLED, welcomePaneProvider.get());
		welcome.setClosable(false);

		DockContainerLeaf leaf = dockingManager.getBento().dockBuilding().leaf(ID_CONTAINER_ROOT_TOP);
		leaf.setCanSplit(false);
		leaf.setPruneWhenEmpty(false);
		leaf.setMenuFactory(this::buildMenu);
		leaf.addDockable(welcome);
		return leaf;
	}

	@Nonnull
	private DockContainerBranch newWorkspaceContainer() {
		// Container to hold:
		//  - Workspace explorer
		DockContainerLeaf explorer = dockingManager.getBento().dockBuilding().leaf(ID_CONTAINER_WORKSPACE_TOOLS);
		explorer.setMenuFactory(this::buildMenu);
		explorer.setCanSplit(false);
		explorer.addDockables(
				dockingManager.newToolDockable("workspace.title", CarbonIcons.TREE_VIEW, workspaceExplorerProvider.get())
		);
		SplitPane.setResizableWithParent(explorer.asRegion(), false);

		// Container to hold:
		//  - Tabs for displaying open classes/files in the workspace
		DockContainerLeaf primary = dockingManager.getBento().dockBuilding().leaf(ID_CONTAINER_WORKSPACE_PRIMARY);
		primary.setPruneWhenEmpty(false);
		primary.setMenuFactory(this::buildMenu);

		// Combining the two into a branch
		DockContainerBranch branch = dockingManager.getBento().dockBuilding().branch(ID_CONTAINER_ROOT_TOP);
		branch.addContainers(explorer, primary);
		branch.setContainerSizePercent(explorer, TOP_WORKSPACE_EXPLORER_SIZE);

		// We don't prune when empty because it breaks the 'replace top' container logic we have when
		// new workspaces get opened or existing ones get closed. As long as the top exists we can always
		// show new content (welcome display, or new workspace display) when needed.
		branch.setPruneWhenEmpty(false);

		return branch;
	}

	@Nonnull
	private DockContainerLeaf newBottomContainer() {
		DockContainerLeaf leaf = dockingManager.getBento().dockBuilding().leaf(ID_CONTAINER_ROOT_BOTTOM);
		leaf.setCanSplit(false);
		leaf.setSide(Side.BOTTOM);
		leaf.setMenuFactory(this::buildMenu);
		leaf.addDockables(
				dockingManager.newToolDockable("logging.title", CarbonIcons.TERMINAL, loggingPaneProvider.get())
		);
		SplitPane.setResizableWithParent(leaf, false);
		return leaf;
	}

	@Nonnull
	private ContextMenu buildMenu(@Nonnull DockContainerLeaf container) {
		ContextMenu menu = new ContextMenu();
		if (!container.isCollapsed())
			// TODO: Swapping sides while collapsed is buggy
			addSideOptions(menu, container);
		return menu;
	}

	private static void addSideOptions(@Nonnull ContextMenu menu, @Nonnull DockContainerLeaf container) {
		for (Side side : Side.values()) {
			FontIconView sideIcon = switch (side) {
				case TOP -> new FontIconView(CarbonIcons.OPEN_PANEL_FILLED_TOP);
				case BOTTOM -> new FontIconView(CarbonIcons.OPEN_PANEL_FILLED_BOTTOM);
				case LEFT -> new FontIconView(CarbonIcons.OPEN_PANEL_FILLED_LEFT);
				case RIGHT -> new FontIconView(CarbonIcons.OPEN_PANEL_FILLED_RIGHT);
			};
			Label graphic = new Label(side == container.sideProperty().get() ? "✓" : " ", sideIcon);
			MenuItem item = new ActionMenuItem(Lang.getBinding("misc.direction." + side.name().toLowerCase()), graphic,
					() -> container.sideProperty().set(side));
			menu.getItems().add(item);
		}
	}

	private class ListenerHost implements WorkspaceOpenListener, WorkspaceCloseListener {
		@Override
		public void onWorkspaceOpened(@Nonnull Workspace workspace) {
			FxThreadUtil.run(() -> {
				if (dockingManager.replace(ID_CONTAINER_ROOT_TOP, DockingLayoutManager.this::newWorkspaceContainer)) {
					if (resourceSummaryConfig.getSummarizeOnOpen().getValue()) actions.openSummary();
				} else {
					logger.error("Failed replacing root on workspace open");
				}
			});
		}

		@Override
		public void onWorkspaceClosed(@Nonnull Workspace workspace) {
			FxThreadUtil.run(() -> {
				if (!dockingManager.replace(ID_CONTAINER_ROOT_TOP, DockingLayoutManager.this::newWelcomeContainer))
					logger.error("Failed replacing root on workspace close");
			});
		}
	}
}
