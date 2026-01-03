package software.coley.recaf.ui.docking;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Orientation;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import javafx.stage.Stage;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import org.slf4j.Logger;
import software.coley.bentofx.Bento;
import software.coley.bentofx.building.DockBuilding;
import software.coley.bentofx.building.StageBuilding;
import software.coley.bentofx.control.DragDropStage;
import software.coley.bentofx.control.Headers;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.dockable.DockableDragDropBehavior;
import software.coley.bentofx.dockable.DockableIconFactory;
import software.coley.bentofx.layout.DockContainer;
import software.coley.bentofx.layout.container.DockContainerBranch;
import software.coley.bentofx.layout.container.DockContainerLeaf;
import software.coley.bentofx.layout.container.DockContainerRootBranch;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.behavior.PriorityKeys;
import software.coley.recaf.services.info.summary.ResourceSummaryServiceConfig;
import software.coley.recaf.services.navigation.Actions;
import software.coley.recaf.services.navigation.NavigationManager;
import software.coley.recaf.services.window.WindowManager;
import software.coley.recaf.services.workspace.WorkspaceCloseListener;
import software.coley.recaf.services.workspace.WorkspaceManager;
import software.coley.recaf.services.workspace.WorkspaceOpenListener;
import software.coley.recaf.ui.control.ActionMenuItem;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.ui.pane.LoggingPane;
import software.coley.recaf.ui.pane.WelcomePane;
import software.coley.recaf.ui.pane.WorkspaceExplorerPane;
import software.coley.recaf.ui.pane.WorkspaceInformationPane;
import software.coley.recaf.ui.window.RecafScene;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.Lang;
import software.coley.recaf.workspace.model.Workspace;

import java.util.UUID;

/**
 * Facilitates creation, inspection, and updates of dockable UI content.
 * <p/>
 * This currently covers:
 * <ul>
 *     <li>Displaying {@link WelcomePane} when no workspace is open</li>
 *     <li>Displaying {@link WorkspaceExplorerPane} when a workspace is open</li>
 * </ul>
 *
 * @author Matt Coley
 * @see NavigationManager
 */
@ApplicationScoped
public class DockingManager {
	private static final Logger logger = Logging.get(DockingManager.class);

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
	/** Bento group ID for tool tabs. */
	public static final int GROUP_TOOLS = 1;
	/** Bento group ID for things that can go anywhere, except for in {@link #GROUP_NEVER_RECEIVE}. */
	public static final int GROUP_ANYWHERE = -1;
	/** Bento group ID for things that can never have things dragged on them. */
	public static final int GROUP_NEVER_RECEIVE = -25565;

	private final Actions actions;
	private final Instance<LoggingPane> loggingPaneProvider;
	private final Instance<WelcomePane> welcomePaneProvider;
	private final Instance<WorkspaceExplorerPane> workspaceExplorerProvider;
	private final DockContainerRootBranch root;
	private final ResourceSummaryServiceConfig resourceSummaryConfig;

	// The primary bento instance
	private final Bento bento = new Bento() {
		@Nonnull
		@Override
		protected DockBuilding newDockBuilding() {
			return new DockBuilding(this) {
				@Nonnull
				@Override
				public DockContainerLeaf leaf(@Nonnull String identifier) {
					DockContainerLeaf leaf = super.leaf(identifier);
					leaf.setMenuFactory(DockingManager.this::buildMenu);
					return leaf;
				}
			};
		}

		@Nonnull
		@Override
		protected StageBuilding newStageBuilding() {
			return new StageBuilding(this) {
				@Override
				protected void initializeFromSource(@Nonnull Scene sourceScene, @Nonnull Scene newScene,
				                                    @Nullable Stage sourceStage, @Nonnull DragDropStage newStage,
				                                    boolean sourceIsOwner) {
					// Always pass 'false' for 'sourceIsOwner'. Window ownership is generally something you would
					// want to specify, but it makes the window behave like a dialog which cannot be minimized.
					super.initializeFromSource(sourceScene, newScene, sourceStage, newStage, false);
				}
			};
		}

		@Nonnull
		@Override
		protected DockableDragDropBehavior newDragDropBehavior() {
			return new DockableDragDropBehavior() {
				@Override
				public boolean canReceiveDockable(@Nonnull DockContainerLeaf targetContainer, @Nullable Side targetSide,
				                                  @Nonnull Dockable dockable) {
					// If we see the 'never receive' ID in a container then we bail.
					if (targetContainer.getDockables().stream().anyMatch(d -> d.getDragGroupMask() == GROUP_NEVER_RECEIVE))
						return false;

					// If the dockable being dragged can go 'anywhere' then we let it go anywhere.
					if (dockable.getDragGroupMask() == GROUP_ANYWHERE)
						return true;

					// Otherwise we fall back to the default behavior.
					return DockableDragDropBehavior.super.canReceiveDockable(targetContainer, targetSide, dockable);
				}
			};
		}
	};

	@Inject
	public DockingManager(@Nonnull WorkspaceManager workspaceManager,
	                      @Nonnull WindowManager windowManager,
	                      @Nonnull Actions actions,
	                      @Nonnull Instance<LoggingPane> loggingPaneProvider,
	                      @Nonnull Instance<WelcomePane> welcomePaneProvider,
	                      @Nonnull Instance<WorkspaceInformationPane> workspaceInfoProvider,
	                      @Nonnull Instance<WorkspaceExplorerPane> workspaceExplorerProvider,
	                      @Nonnull ResourceSummaryServiceConfig resourceSummaryConfig) {
		this.actions = actions;
		this.loggingPaneProvider = loggingPaneProvider;
		this.welcomePaneProvider = welcomePaneProvider;
		this.workspaceExplorerProvider = workspaceExplorerProvider;
		this.resourceSummaryConfig = resourceSummaryConfig;

		// Stages created via our docking framework need to be tracked in the window manager.
		bento.stageBuilding().setStageFactory(originScene -> {
			DragDropStage stage = new DragDropStage(true);
			windowManager.register("dnd-" + UUID.randomUUID(), stage);
			return stage;
		});
		bento.stageBuilding().setSceneFactory((sourceScene, content, width, height) -> {
			content.getStyleClass().add("bg-inset");
			return new RecafScene(content, width, height);
		});

		// Due to how we style the headers, we want the drawing to be clipped.
		bento.controlsBuilding().setHeadersFactory(ClippedHeaders::new);

		// Register listeners
		workspaceManager.addWorkspaceOpenListener(new SummaryDisplayListener());
		workspaceManager.addWorkspaceCloseListener(new WelcomeDisplayListener());

		// Create root
		DockBuilding builder = bento.dockBuilding();
		DockContainer top = newWelcomeContainer();
		DockContainer bottom = newBottomContainer();
		builder.root(ID_CONTAINER_ROOT_SPLIT);
		root = builder.root(ID_CONTAINER_ROOT_SPLIT);
		root.setOrientation(Orientation.VERTICAL);
		root.addContainers(top, bottom);
		root.setContainerSizePx(bottom, BOTTOM_SIZE);
		bento.registerRoot(root);
	}

	/**
	 * @return Backing bento docking instance.
	 */
	@Nonnull
	public Bento getBento() {
		return bento;
	}

	/**
	 * @return The root docking container.
	 */
	@Nonnull
	public DockContainerRootBranch getRoot() {
		return root;
	}

	/**
	 * The primary container is where content in the workspace is displayed when opened.
	 * Opening classes and files should place content in here.
	 *
	 * @return The primary docking container leaf where most content is placed in the UI.
	 */
	@Nonnull
	public DockContainerLeaf getPrimaryDockingContainer() {
		var path = bento.search().container(DockingManager.ID_CONTAINER_WORKSPACE_PRIMARY);
		if (path != null && path.tailContainer() instanceof DockContainerLeaf leaf)
			return leaf;
		// TODO: Only fails when opening tutorial
		throw new IllegalStateException("Primary docking leaf could not be found");
	}

	/**
	 * Creates a {@link Dockable} that is assigned to the {@link #GROUP_TOOLS} group.
	 *
	 * @param translationKey
	 * 		Dockable title translation key.
	 * @param icon
	 * 		Dockable icon.
	 * @param content
	 * 		Dockable content to display.
	 *
	 * @return Created dockable.
	 */
	@Nonnull
	public Dockable newToolDockable(@Nonnull String translationKey, @Nonnull Ikon icon, @Nonnull Node content) {
		return newToolDockable(translationKey, d -> new FontIconView(icon), content);
	}

	/**
	 * Creates a {@link Dockable} that is assigned to the {@link #GROUP_TOOLS} group.
	 *
	 * @param translationKey
	 * 		Dockable title translation key.
	 * @param iconFactory
	 * 		Dockable icon factory.
	 * @param content
	 * 		Dockable content to display.
	 *
	 * @return Created dockable.
	 */
	@Nonnull
	public Dockable newToolDockable(@Nonnull String translationKey, @Nonnull DockableIconFactory iconFactory, @Nonnull Node content) {
		Dockable dockable = bento.dockBuilding().dockable(translationKey);
		dockable.titleProperty().bind(Lang.getBinding(translationKey));
		dockable.setNode(content);
		dockable.setIconFactory(iconFactory);
		dockable.setClosable(false);
		dockable.setDragGroupMask(GROUP_TOOLS);
		return dockable;
	}

	/**
	 * Creates a {@link Dockable}.
	 *
	 * @param title
	 * 		Dockable title.
	 * @param icon
	 * 		Dockable icon.
	 * @param content
	 * 		Dockable content to display.
	 *
	 * @return Created dockable.
	 */
	@Nonnull
	public Dockable newDockable(@Nonnull String title, @Nonnull Ikon icon, @Nonnull Node content) {
		return newDockable(title, d -> new FontIconView(icon), content);
	}

	/**
	 * Creates a {@link Dockable}.
	 *
	 * @param title
	 * 		Dockable title.
	 * @param iconFactory
	 * 		Dockable icon factory.
	 * @param content
	 * 		Dockable content to display.
	 *
	 * @return Created dockable.
	 */
	@Nonnull
	public Dockable newDockable(@Nonnull String title, @Nonnull DockableIconFactory iconFactory, @Nonnull Node content) {
		Dockable dockable = bento.dockBuilding().dockable();
		dockable.setTitle(title);
		dockable.setNode(content);
		dockable.setIconFactory(iconFactory);
		return dockable;
	}

	/**
	 * Creates a {@link Dockable}.
	 *
	 * @param translationKey
	 * 		Dockable title translation key.
	 * @param icon
	 * 		Dockable icon.
	 * @param content
	 * 		Dockable content to display.
	 *
	 * @return Created dockable.
	 */
	@Nonnull
	public Dockable newTranslatableDockable(@Nonnull String translationKey, @Nonnull Ikon icon, @Nonnull Node content) {
		return newTranslatableDockable(translationKey, d -> new FontIconView(icon), content);
	}

	/**
	 * Creates a {@link Dockable}.
	 *
	 * @param translationKey
	 * 		Dockable title translation key.
	 * @param iconFactory
	 * 		Dockable icon factory.
	 * @param content
	 * 		Dockable content to display.
	 *
	 * @return Created dockable.
	 */
	@Nonnull
	public Dockable newTranslatableDockable(@Nonnull String translationKey, @Nonnull DockableIconFactory iconFactory, @Nonnull Node content) {
		return newTranslatableDockable(Lang.getBinding(translationKey), iconFactory, content);
	}

	/**
	 * Creates a {@link Dockable}.
	 *
	 * @param titleBinding
	 * 		Dockable title translation binding.
	 * @param iconFactory
	 * 		Dockable icon factory.
	 * @param content
	 * 		Dockable content to display.
	 *
	 * @return Created dockable.
	 */
	@Nonnull
	public Dockable newTranslatableDockable(@Nonnull ObservableValue<String> titleBinding, @Nonnull DockableIconFactory iconFactory, @Nonnull Node content) {
		Dockable dockable = bento.dockBuilding().dockable();
		dockable.titleProperty().bind(titleBinding);
		dockable.setNode(content);
		dockable.setIconFactory(iconFactory);
		return dockable;
	}

	/**
	 * @return Newly created leaf container.
	 */
	@Nonnull
	public DockContainerLeaf newLeafContainer() {
		return newLeafContainer(UUID.randomUUID().toString());
	}

	/**
	 * @param id
	 * 		ID of the container to create.
	 *
	 * @return Newly created leaf container.
	 */
	@Nonnull
	public DockContainerLeaf newLeafContainer(@Nonnull String id) {
		return bento.dockBuilding().leaf(id);
	}

	@Nonnull
	private DockContainerLeaf newWelcomeContainer() {
		Dockable welcome = newTranslatableDockable("welcome.title", CarbonIcons.EARTH_FILLED, welcomePaneProvider.get());
		welcome.setClosable(false);

		DockContainerLeaf leaf = newLeafContainer(ID_CONTAINER_ROOT_TOP);
		leaf.setCanSplit(false);
		leaf.setPruneWhenEmpty(false);
		leaf.addDockable(welcome);
		return leaf;
	}

	@Nonnull
	private DockContainerBranch newWorkspaceContainer() {
		// Container to hold:
		//  - Workspace explorer
		DockContainerLeaf explorer = newLeafContainer(ID_CONTAINER_WORKSPACE_TOOLS);
		explorer.setCanSplit(false);
		explorer.addDockables(newToolDockable("workspace.title", CarbonIcons.TREE_VIEW, workspaceExplorerProvider.get()));
		SplitPane.setResizableWithParent(explorer.asRegion(), false);

		// Container to hold:
		//  - Tabs for displaying open classes/files in the workspace
		DockContainerLeaf primary = newLeafContainer(ID_CONTAINER_WORKSPACE_PRIMARY);
		primary.setPruneWhenEmpty(false);

		// Combining the two into a branch
		DockContainerBranch branch = bento.dockBuilding().branch(ID_CONTAINER_ROOT_TOP);
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
		DockContainerLeaf leaf = newLeafContainer(ID_CONTAINER_ROOT_BOTTOM);
		leaf.setCanSplit(false);
		leaf.setSide(Side.BOTTOM);
		leaf.addDockables(newToolDockable("logging.title", CarbonIcons.TERMINAL, loggingPaneProvider.get()));
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

	/**
	 * Listener to show the workspace summary page for opened workspaces.
	 */
	private class SummaryDisplayListener implements WorkspaceOpenListener {
		@Override
		public void onWorkspaceOpened(@Nonnull Workspace workspace) {
			// Replace root with a summary of the workspace when it is opened.
			FxThreadUtil.run(() -> {
				if (bento.search().replaceContainer(ID_CONTAINER_ROOT_TOP, DockingManager.this::newWorkspaceContainer)) {
					if (resourceSummaryConfig.getSummarizeOnOpen().getValue()) actions.openSummary();
				} else {
					logger.error("Failed replacing root on workspace open");
				}
			});
		}

		@Override
		public int getPriority() {
			// This is set to run 'EARLIEST' so that other open listeners that use FX callbacks
			// will register their callbacks after this one. This ensures the 'ID_CONTAINER_WORKSPACE_PRIMARY' is
			// available when those other callbacks are executed.
			return PriorityKeys.EARLIEST;
		}
	}

	/**
	 * Listener to show the welcome page when no workspace is open.
	 */
	private class WelcomeDisplayListener implements WorkspaceCloseListener {
		@Override
		public void onWorkspaceClosed(@Nonnull Workspace workspace) {
			// When a workspace is closed, show the welcome screen.
			FxThreadUtil.run(() -> {
				if (!bento.search().replaceContainer(ID_CONTAINER_ROOT_TOP, DockingManager.this::newWelcomeContainer))
					logger.error("Failed replacing root on workspace close");
			});
		}

		@Override
		public int getPriority() {
			// This is set to run 'LATEST' so that the replace operation happens AFTER the navigation manager
			// handles closing UI content from the workspace being closed. If this happens too early then that logic
			// in the navigation manager will close our 'welcome' page/container.
			return PriorityKeys.LATEST;
		}
	}

	/**
	 * Headers impl that configures render clipping.
	 */
	private static class ClippedHeaders extends Headers {
		/**
		 * @param container
		 * 		Parent container.
		 * @param orientation
		 * 		Which axis to layout children on.
		 * @param side
		 * 		Side in the parent container where tabs are displayed.
		 */
		private ClippedHeaders(@Nonnull DockContainerLeaf container, @Nonnull Orientation orientation, @Nonnull Side side) {
			super(container, orientation, side);
			setupClip();
		}
	}
}
