package software.coley.recaf.ui.pane.editing;

import jakarta.annotation.Nonnull;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.layout.BorderPane;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.info.Info;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.services.navigation.Navigable;
import software.coley.recaf.services.navigation.UpdatableNavigable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
	protected final List<Consumer<P>> pathUpdateListeners = new ArrayList<>();
	protected final List<Navigable> children = new ArrayList<>();
	protected SideTabs sideTabs;
	protected P path;

	/**
	 * Clear the display.
	 */
	protected void clearDisplay() {
		// Remove navigable child.
		if (getCenter() instanceof Navigable navigable)
			children.remove(navigable);

		// Remove display node.
		setCenter(null);
	}

	/**
	 * @param node
	 * 		Node to display.
	 */
	protected void setDisplay(Node node) {
		// Remove old navigable child.
		Node old = getCenter();
		if (old instanceof Navigable navigableOld)
			children.remove(navigableOld);

		// Add navigable child.
		if (node instanceof Navigable navigableNode)
			children.add(navigableNode);

		// Set display node.
		setCenter(node);
	}

	/**
	 * Refresh the display.
	 */
	protected void refreshDisplay() {
		// Refresh display
		clearDisplay();
		generateDisplay();

		// Refresh UI with path
		if (getCenter() instanceof UpdatableNavigable updatable)
			updatable.onUpdatePath(getPath());
	}

	/**
	 * Generate display for the content denoted by {@link #getPath() the path node}.
	 * Children implementing this should call {@link #setDisplay(Node)}.
	 */
	protected abstract void generateDisplay();

	/**
	 * @param tab
	 * 		Tab to add to the side panel.
	 */
	protected void addSideTab(Tab tab) {
		// Lazily create/add side-tabs to UI.
		if (sideTabs == null) {
			sideTabs = new SideTabs();
			children.add(sideTabs);
			setRight(sideTabs);
		}

		// Add the given tab.
		sideTabs.getTabs().add(tab);
	}

	/**
	 * @param listener
	 * 		Listener to add.
	 */
	public void addPathUpdateListener(Consumer<P> listener) {
		pathUpdateListeners.add(listener);
	}

	@Nonnull
	@Override
	public Collection<Navigable> getNavigableChildren() {
		return children;
	}

	@Override
	public void disable() {
		pathUpdateListeners.clear();
		setDisable(true);
	}
}
