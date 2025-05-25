package software.coley.recaf.ui.docking;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import javafx.geometry.Orientation;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import org.slf4j.Logger;
import software.coley.bentofx.builder.LayoutBuilder;
import software.coley.bentofx.builder.LeafLayoutArgs;
import software.coley.bentofx.builder.SingleSpaceArgs;
import software.coley.bentofx.builder.SplitLayoutArgs;
import software.coley.bentofx.builder.TabbedSpaceArgs;
import software.coley.bentofx.layout.DockLayout;
import software.coley.bentofx.layout.LeafDockLayout;
import software.coley.bentofx.layout.RootDockLayout;
import software.coley.bentofx.layout.SplitDockLayout;
import software.coley.bentofx.space.DockSpace;
import software.coley.bentofx.space.TabbedDockSpace;
import software.coley.bentofx.space.TabbedSpaceMenuFactory;
import software.coley.recaf.analytics.logging.Logging;
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

@ApplicationScoped
public class DockingLayoutManager {
	private static final Logger logger = Logging.get(DockingLayoutManager.class);

	/** Top half of the main UI at initial layout. */
	public static final String ID_LAYOUT_ROOT_TOP = "layout-root-top";
	/** Bottom half of the main UI at initial layout. */
	public static final String ID_LAYOUT_ROOT_BOTTOM = "layout-root-bottom";
	/** {@link LeafDockLayout} holding the {@link WorkspaceExplorerPane} */
	public static final String ID_LAYOUT_WORKSPACE_TOOLS = "layout-workspace-tools";
	/** {@link LeafDockLayout} holding the primary editor tabs. */
	public static final String ID_LAYOUT_WORKSPACE_PRIMARY = "layout-workspace-primary";
	/** {@link TabbedDockSpace} holding workspace tools like {@link WorkspaceExplorerPane} */
	public static final String ID_SPACE_WORKSPACE_TOOLS = "space-workspace-tools";
	/** {@link TabbedDockSpace} holding the primary editor tabs. */
	public static final String ID_SPACE_WORKSPACE_PRIMARY = "space-workspace-primary";
	/** {@link TabbedDockSpace} holding other tools like {@link LoggingPane} */
	public static final String ID_SPACE_TOOLS_BOTTOM = "space-tools-bottom";

	private final DockingManager dockingManager;
	private final Instance<LoggingPane> loggingPaneProvider;
	private final Instance<WelcomePane> welcomePaneProvider;
	private final Instance<WorkspaceInformationPane> workspaceInfoProvider;
	private final Instance<WorkspaceExplorerPane> workspaceExplorerProvider;
	private final RootDockLayout root;

	@Inject
	public DockingLayoutManager(@Nonnull DockingManager dockingManager,
	                            @Nonnull WorkspaceManager workspaceManager,
	                            @Nonnull Instance<LoggingPane> loggingPaneProvider,
	                            @Nonnull Instance<WelcomePane> welcomePaneProvider,
	                            @Nonnull Instance<WorkspaceInformationPane> workspaceInfoProvider,
	                            @Nonnull Instance<WorkspaceExplorerPane> workspaceExplorerProvider) {
		this.dockingManager = dockingManager;
		this.loggingPaneProvider = loggingPaneProvider;
		this.welcomePaneProvider = welcomePaneProvider;
		this.workspaceInfoProvider = workspaceInfoProvider;
		this.workspaceExplorerProvider = workspaceExplorerProvider;

		// Register listener
		ListenerHost host = new ListenerHost();
		workspaceManager.addWorkspaceOpenListener(host);
		workspaceManager.addWorkspaceCloseListener(host);

		// Create root
		LayoutBuilder builder = dockingManager.getBento().newLayoutBuilder();
		DockLayout top = newEmptyTop();
		DockLayout bottom = newBottom();
		root = builder.root(builder.split(new SplitLayoutArgs()
				.setOrientation(Orientation.VERTICAL)
				.setChildrenSizes(-1, 150)
				.addChildren(top, bottom)));
	}

	@Nonnull
	public RootDockLayout getRoot() {
		return root;
	}

	@Nonnull
	private DockSpace newWelcomeSpace() {
		LayoutBuilder builder = dockingManager.getBento().newLayoutBuilder();
		return builder.single(new SingleSpaceArgs()
				.setDockable(builder.dockable()
						.withNode(welcomePaneProvider.get())
						.withDragGroup(-1)
						.build())
				.setSide(null)
		);
	}

	@Nonnull
	private DockLayout newWorkspaceExplorerLayout() {
		LayoutBuilder builder = dockingManager.getBento().newLayoutBuilder();
		return builder.leaf(new LeafLayoutArgs()
				.setResizeWithParent(false)
				.setIdentifier(ID_LAYOUT_WORKSPACE_TOOLS)
				.setSpace(builder.tabbed(
						new TabbedSpaceArgs()
								.setIdentifier(ID_SPACE_WORKSPACE_TOOLS)
								.setCanSplit(false)
								.addDockables(dockingManager.newToolDockable("workspace.title", CarbonIcons.TREE_VIEW, workspaceExplorerProvider.get()))
								.setMenuFactory(space -> {
									ContextMenu menu = new ContextMenu();
									addSideItems(menu, space);
									return menu;
								})
				))
		);
	}

	private static void addSideItems(@Nonnull ContextMenu menu, @Nonnull TabbedDockSpace tabbed) {
		// TODO: Move this to common location, and make other things use it
		for (Side side : Side.values()) {
			FontIconView sideIcon = switch (side) {
				case TOP -> new FontIconView(CarbonIcons.OPEN_PANEL_FILLED_TOP);
				case BOTTOM -> new FontIconView(CarbonIcons.OPEN_PANEL_FILLED_BOTTOM);
				case LEFT -> new FontIconView(CarbonIcons.OPEN_PANEL_FILLED_LEFT);
				case RIGHT -> new FontIconView(CarbonIcons.OPEN_PANEL_FILLED_RIGHT);
			};
			Label graphic = new Label(side == tabbed.sideProperty().get() ? "✓" : " ", sideIcon);
			MenuItem item = new ActionMenuItem(Lang.getBinding("misc.direction." + side.name().toLowerCase()), graphic,
					() -> tabbed.sideProperty().set(side));
			menu.getItems().add(item);
		}
	}

	@Nonnull
	private DockLayout newWorkspacePrimaryLayout() {
		LayoutBuilder builder = dockingManager.getBento().newLayoutBuilder();
		return builder.leaf(new LeafLayoutArgs()
				.setIdentifier(ID_LAYOUT_WORKSPACE_PRIMARY)
				.setSpace(builder.tabbed(
						new TabbedSpaceArgs()
								.setIdentifier(ID_SPACE_WORKSPACE_PRIMARY)
								.setAutoPruneWhenEmpty(false)
								.addDockables(builder.dockable()
										.withNode(workspaceInfoProvider.get())
										.withTitle(Lang.getBinding("workspace.info"))
										.withIconFactory(d -> new FontIconView(CarbonIcons.INFORMATION))
										.build())
				))
		);
	}

	@Nonnull
	private DockLayout newEmptyTop() {
		LayoutBuilder builder = dockingManager.getBento().newLayoutBuilder();
		return builder.leaf(new LeafLayoutArgs()
				.setIdentifier(ID_LAYOUT_ROOT_TOP)
				.setSpace(newWelcomeSpace())
		);
	}

	@Nonnull
	private SplitDockLayout newWorkspaceTop() {
		LayoutBuilder builder = dockingManager.getBento().newLayoutBuilder();
		return builder.split(new SplitLayoutArgs()
				.setOrientation(Orientation.HORIZONTAL)
				.setIdentifier(ID_LAYOUT_ROOT_TOP)
				.setChildrenSizes(200)
				.addChildren(newWorkspaceExplorerLayout(), newWorkspacePrimaryLayout())
		);
	}

	@Nonnull
	private DockLayout newBottom() {
		LayoutBuilder builder = dockingManager.getBento().newLayoutBuilder();
		return builder.leaf(new LeafLayoutArgs()
				.setIdentifier(ID_LAYOUT_ROOT_BOTTOM)
				.setSpace(builder.tabbed(new TabbedSpaceArgs()
						.setCanSplit(false)
						.setIdentifier(ID_SPACE_TOOLS_BOTTOM)
						.setSide(Side.BOTTOM)
						.setCanSplit(false)
						.addDockables(dockingManager.newToolDockable("logging.title", CarbonIcons.TERMINAL, loggingPaneProvider.get()))
				)).setResizeWithParent(false)
		);
	}

	private class ListenerHost implements WorkspaceOpenListener, WorkspaceCloseListener {
		@Override
		public void onWorkspaceOpened(@Nonnull Workspace workspace) {
			FxThreadUtil.run(() -> {
				if (!dockingManager.replace(ID_LAYOUT_ROOT_TOP, DockingLayoutManager.this::newWorkspaceTop)) {
					logger.error("Failed replacing root on workspace open");
				}
			});
		}

		@Override
		public void onWorkspaceClosed(@Nonnull Workspace workspace) {
			FxThreadUtil.run(() -> {
				if (!dockingManager.replace(ID_LAYOUT_ROOT_TOP, DockingLayoutManager.this::newEmptyTop)) {
					logger.error("Failed replacing root on workspace close");
				}
			});
		}
	}
}
