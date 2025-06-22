package software.coley.recaf.ui.pane.editing;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Orientation;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import org.kordamp.ikonli.Ikon;
import software.coley.bentofx.Bento;
import software.coley.bentofx.building.ContentWrapperFactory;
import software.coley.bentofx.building.DockBuilding;
import software.coley.bentofx.building.HeaderFactory;
import software.coley.bentofx.building.HeadersFactory;
import software.coley.bentofx.control.ContentWrapper;
import software.coley.bentofx.control.Header;
import software.coley.bentofx.control.HeaderPane;
import software.coley.bentofx.control.Headers;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.dockable.DockableIconFactory;
import software.coley.bentofx.layout.container.DockContainerLeaf;
import software.coley.bentofx.layout.container.DockContainerRootBranch;
import software.coley.bentofx.path.DockContainerPath;
import software.coley.bentofx.util.BentoStates;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.info.Info;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.services.navigation.Navigable;
import software.coley.recaf.services.navigation.UpdatableNavigable;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.ui.docking.EmbeddedBento;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Base type for common content panes displaying {@link Info} values.
 *
 * @param <P>
 * 		Info path type.
 *
 * @see FilePane For {@link FileInfo}
 * @see ClassPane For {@link ClassInfo}
 */
public abstract class AbstractContentPane<P extends PathNode<?>> extends BorderPane implements UpdatableNavigable {
	private static final String TOOL_TABS_ID = "tool-tabs";
	/** We use a separate instance because the side-tabs of this pane are not intended to be tracked by our docking manger. */
	private final Bento bento = new EmbeddedBento();
	/** Wrapper to hold {@link #displayWrapper} and {@link Dockable} side tabs. Split according to {@link #toolTabSide}. */
	private final DockContainerRootBranch displaySplit;
	/** Side of the UI to place additional tools on. */
	private final Side toolTabSide;
	/** Wrapper to hold display for provided content. See {@link #generateDisplay()} */
	private final BorderPane displayWrapper = new BorderPane();
	protected final List<Consumer<P>> pathUpdateListeners = new CopyOnWriteArrayList<>();
	protected final List<Navigable> children = new ArrayList<>();
	protected P path;

	/**
	 * New content pane. Any additional tools registered via {@link #addSideTab(Dockable)} will be placed on the right.
	 */
	protected AbstractContentPane() {
		this(Side.RIGHT);
	}

	/**
	 * New content pane.
	 *
	 * @param toolTabSide
	 * 		Side to place additional tools registered via {@link #addSideTab(Dockable)}.
	 */
	protected AbstractContentPane(@Nonnull Side toolTabSide) {
		this.toolTabSide = toolTabSide;

		DockBuilding builder = bento.dockBuilding();
		Dockable dockable = builder.dockable();
		dockable.setNode(displayWrapper);
		dockable.setCanBeDragged(false);
		dockable.setClosable(false);
		dockable.setDragGroup(-1);

		// The display container contains the primary content display.
		//
		// We set the container's side to 'null' to prevent the rendering of headers
		// for the wrapper dockable we made above.
		DockContainerLeaf displayContainer = builder.leaf();
		displayContainer.setSide(null);
		displayContainer.addDockable(dockable);

		displaySplit = builder.root();
		displaySplit.addContainer(displayContainer);
		if (toolTabSide.isHorizontal())
			displaySplit.setOrientation(Orientation.VERTICAL);

		// Register so we can search the hierarchy (used later for tool-tabs) before scene graph population.
		bento.registerRoot(displaySplit);

		// Register the side pseudo-state so we can have side-specific UI styling.
		Region rootRegion = displaySplit.asRegion();
		switch (toolTabSide) {
			case TOP -> rootRegion.pseudoClassStateChanged(BentoStates.PSEUDO_SIDE_TOP, true);
			case BOTTOM -> rootRegion.pseudoClassStateChanged(BentoStates.PSEUDO_SIDE_BOTTOM, true);
			case LEFT -> rootRegion.pseudoClassStateChanged(BentoStates.PSEUDO_SIDE_LEFT, true);
			case RIGHT -> rootRegion.pseudoClassStateChanged(BentoStates.PSEUDO_SIDE_RIGHT, true);
		}
		setCenter(rootRegion);
	}

	/**
	 * @param targetType
	 * 		Type of children to filter with.
	 * @param action
	 * 		Action to run on children of the target type.
	 * @param <T>
	 * 		Target type.
	 */
	@SuppressWarnings("unchecked")
	protected <T> void eachChild(@Nonnull Class<T> targetType, @Nonnull Consumer<T> action) {
		for (Navigable child : children) {
			if (targetType.isAssignableFrom(child.getClass())) {
				action.accept((T) child);
			}
		}
	}

	/**
	 * @return Current display
	 */
	@Nullable
	public Node getDisplay() {
		return displayWrapper.getCenter();
	}

	/**
	 * Clear the display.
	 */
	protected void clearDisplay() {
		// Remove navigable child.
		if (displayWrapper.getCenter() instanceof Navigable navigable)
			children.remove(navigable);

		// Remove display node.
		displayWrapper.setCenter(null);
	}

	/**
	 * @return {@code true} when there is a current displayed node.
	 */
	protected boolean hasDisplay() {
		return getDisplay() != null;
	}

	/**
	 * @param node
	 * 		Node to display.
	 */
	protected void setDisplay(Node node) {
		// Remove old navigable child.
		Node old = displayWrapper.getCenter();
		if (old instanceof Navigable navigableOld)
			children.remove(navigableOld);

		// Add navigable child.
		if (node instanceof Navigable navigableNode)
			children.add(navigableNode);

		// Set display node.
		displayWrapper.setCenter(node);
	}

	/**
	 * Refresh the display.
	 */
	protected void refreshDisplay() {
		// Refresh display
		clearDisplay();
		generateDisplay();

		// Refresh UI with path
		if (displayWrapper.getCenter() instanceof UpdatableNavigable updatable) {
			PathNode<?> currentPath = getPath();
			if (currentPath != null)
				updatable.onUpdatePath(currentPath);
		}
	}

	/**
	 * Generate display for the content denoted by {@link #getPath() the path node}.
	 * Children implementing this should call {@link #setDisplay(Node)}.
	 */
	protected abstract void generateDisplay();

	/**
	 * Adds a new side tab to this pane.
	 *
	 * @param binding
	 * 		Side tab title binding.
	 * @param icon
	 * 		Side tab icon.
	 * @param content
	 * 		Side tab content to display.
	 */
	public void addSideTab(@Nonnull ObservableValue<String> binding, @Nonnull Ikon icon, @Nullable Node content) {
		addSideTab(binding, d -> new FontIconView(icon), content);
	}

	/**
	 * Adds a new side tab to this pane.
	 *
	 * @param binding
	 * 		Side tab title binding.
	 * @param iconFactory
	 * 		Side tab icon factory.
	 * @param content
	 * 		Side tab content to display.
	 */
	public void addSideTab(@Nonnull ObservableValue<String> binding, @Nonnull DockableIconFactory iconFactory, @Nullable Node content) {
		Dockable dockable = bento.dockBuilding().dockable();
		dockable.setDragGroup(-1);// Prevent being used as a drag-drop target
		dockable.setCanBeDragged(false); // Prevent being used as a drag-drop
		dockable.setClosable(false);
		dockable.setIconFactory(iconFactory);
		dockable.setNode(content);
		dockable.titleProperty().bind(binding);
		addSideTab(dockable);
	}

	/**
	 * Adds a new side tab to this pane.
	 *
	 * @param dockable
	 * 		Side tab model.
	 */
	public void addSideTab(@Nonnull Dockable dockable) {
		if (displaySplit.getChildContainers().size() < 2) {
			DockBuilding builder = bento.dockBuilding();

			DockContainerLeaf leaf = builder.leaf(TOOL_TABS_ID);
			leaf.setCanSplit(false);
			leaf.setSide(toolTabSide);
			leaf.addDockable(dockable);
			SplitPane.setResizableWithParent(leaf.asRegion(), false);

			switch (toolTabSide) {
				case TOP, LEFT -> {
					displaySplit.addContainer(0, leaf);
					displaySplit.setContainerSizePx(leaf, 232);
				}
				case BOTTOM, RIGHT -> {
					displaySplit.addContainer(leaf);
					displaySplit.setContainerSizePx(leaf, 180);
				}
			}

			displaySplit.setContainerCollapsed(leaf, true);
		} else {
			DockContainerPath path = bento.search().container(TOOL_TABS_ID);
			if (path != null && path.tailContainer() instanceof DockContainerLeaf leaf)
				leaf.addDockable(dockable);
		}

		if (dockable.getNode() instanceof Navigable dockableNode)
			children.add(dockableNode);
	}

	/**
	 * Clears all side tabs from this pane.
	 */
	public void clearSideTabs() {
		DockContainerPath path = bento.search().container(TOOL_TABS_ID);
		if (path != null && path.tailContainer() instanceof DockContainerLeaf leaf) {
			for (Dockable dockable : new ArrayList<>(leaf.getDockables())) {
				leaf.closeDockable(dockable);
			}
		}
	}

	/**
	 * @param listener
	 * 		Listener to add.
	 */
	public void addPathUpdateListener(@Nonnull Consumer<P> listener) {
		pathUpdateListeners.add(listener);
	}

	/**
	 * @param listener
	 * 		Listener to remove.
	 */
	public void removePathUpdateListener(@Nonnull Consumer<P> listener) {
		pathUpdateListeners.remove(listener);
	}

	@Nonnull
	@Override
	public Collection<Navigable> getNavigableChildren() {
		return children;
	}

	@Override
	public void disable() {
		children.forEach(Navigable::disable);
		pathUpdateListeners.clear();
		setDisable(true);
	}
}
