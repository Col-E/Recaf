package software.coley.recaf.ui.pane.editing;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Orientation;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import org.kordamp.ikonli.Ikon;
import software.coley.bentofx.Bento;
import software.coley.bentofx.builder.LayoutBuilder;
import software.coley.bentofx.builder.SingleSpaceArgs;
import software.coley.bentofx.builder.TabbedSpaceArgs;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.dockable.DockableIconFactory;
import software.coley.bentofx.header.Header;
import software.coley.bentofx.impl.ImplBento;
import software.coley.bentofx.layout.LeafDockLayout;
import software.coley.bentofx.layout.RootDockLayout;
import software.coley.bentofx.layout.SplitDockLayout;
import software.coley.bentofx.path.SpacePath;
import software.coley.bentofx.space.TabbedDockSpace;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.info.Info;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.services.navigation.Navigable;
import software.coley.recaf.services.navigation.UpdatableNavigable;
import software.coley.recaf.ui.control.FontIconView;

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
	private final ImplBento bento = (ImplBento) Bento.newBento();
	private final BorderPane displayWrapper = new BorderPane();
	private final SplitDockLayout displaySplit;
	private final Side toolTabSide;
	protected final List<Consumer<P>> pathUpdateListeners = new CopyOnWriteArrayList<>();
	protected final List<Navigable> children = new ArrayList<>();
	protected P path;

	protected AbstractContentPane() {
		this(Side.RIGHT);
	}

	protected AbstractContentPane(@Nonnull Side toolTabSide) {
		this.toolTabSide = toolTabSide;

		LayoutBuilder builder = bento.newLayoutBuilder();
		Dockable dockable = bento.newDockableBuilder().withNode(displayWrapper).withCanBeDragged(false).withDragGroup(-1).build();
		LeafDockLayout displayLayout = builder.leaf(builder.single(new SingleSpaceArgs().setSide(null).setDockable(dockable)));

		displaySplit = builder.split(toolTabSide.isHorizontal() ? Orientation.VERTICAL : Orientation.HORIZONTAL, displayLayout);

		RootDockLayout root = builder.root(displaySplit);
		Region rootRegion = root.getBackingRegion();
		rootRegion.getStyleClass().add("embedded-bento");
		switch (toolTabSide) {
			case TOP -> rootRegion.pseudoClassStateChanged(Header.PSEUDO_SIDE_TOP, true);
			case BOTTOM -> rootRegion.pseudoClassStateChanged(Header.PSEUDO_SIDE_BOTTOM, true);
			case LEFT -> rootRegion.pseudoClassStateChanged(Header.PSEUDO_SIDE_LEFT, true);
			case RIGHT -> rootRegion.pseudoClassStateChanged(Header.PSEUDO_SIDE_RIGHT, true);
		}
		setCenter(rootRegion);
		bento.registerRoot(root);
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

	public void addSideTab(@Nonnull ObservableValue<String> binding, @Nonnull Ikon icon, @Nullable Node content) {
		addSideTab(binding, d -> new FontIconView(icon), content);
	}

	public void addSideTab(@Nonnull ObservableValue<String> binding, @Nonnull DockableIconFactory iconFactory, @Nullable Node content) {
		Dockable dockable = dockable(binding, iconFactory, content);
		addSideTab(dockable);
	}

	@Nonnull
	private Dockable dockable(@Nonnull ObservableValue<String> binding, @Nonnull DockableIconFactory iconFactory, @Nullable Node content) {
		return bento.newDockableBuilder()
				.withCanBeDragged(false)
				.withClosable(false)
				.withDragGroup(-1)
				.withTitle(binding)
				.withIconFactory(iconFactory)
				.withNode(content)
				.build();
	}

	public void addSideTab(@Nonnull Dockable dockable) {
		if (displaySplit.getChildLayouts().size() < 2) {
			LayoutBuilder builder = bento.newLayoutBuilder();
			TabbedDockSpace space = builder.tabbed(new TabbedSpaceArgs()
					.setCanSplit(false)
					.setSide(toolTabSide)
					.setIdentifier(TOOL_TABS_ID)
					.addDockables(dockable));
			LeafDockLayout leaf = builder.fitLeaf(space);
			switch (toolTabSide) {
				case TOP, LEFT -> displaySplit.addChildLayout(0, leaf);
				case BOTTOM, RIGHT -> displaySplit.addChildLayout(leaf);
			}
			displaySplit.setChildCollapsed(leaf, true);
			switch (toolTabSide) {
				case TOP, LEFT -> displaySplit.setChildSize(leaf, 232);
				case BOTTOM, RIGHT -> displaySplit.setChildSize(leaf, 180);
			}
		} else {
			SpacePath path = bento.findSpace(TOOL_TABS_ID);
			if (path != null && path.space() instanceof TabbedDockSpace space) {
				space.addDockable(dockable);
				space.selectDockable(space.getDockables().getFirst());
			}
		}
		if (dockable.getNode() instanceof Navigable dockableNode)
			children.add(dockableNode);
	}

	public void clearSideTabs() {
		SpacePath path = bento.findSpace(TOOL_TABS_ID);
		if (path != null && path.space() instanceof TabbedDockSpace space) {
			for (Dockable dockable : new ArrayList<>(space.getDockables())) {
				space.closeDockable(dockable);
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
