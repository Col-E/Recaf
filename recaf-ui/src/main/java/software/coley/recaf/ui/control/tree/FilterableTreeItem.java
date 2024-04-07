package software.coley.recaf.ui.control.tree;

import jakarta.annotation.Nonnull;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.TreeItem;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.util.ReflectUtil;
import software.coley.recaf.util.Unchecked;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/**
 * Filterable tree item.
 *
 * @param <T>
 * 		Type of value held by the item.
 *
 * @author <a href="https://stackoverflow.com/a/34426897">kaznovac</a>
 * @author Matt Coley - Optimization of child management.
 */
public class FilterableTreeItem<T> extends TreeItem<T> {
	private static final Field CHILDREN_LISTENER_FIELD;
	private static final Field CHILDREN_FIELD;
	private static final Logger logger = Logging.get(FilterableTreeItem.class);
	private final ObservableList<TreeItem<T>> sourceChildren = FXCollections.observableArrayList();
	private final ObjectProperty<TreeItem<T>> sourceParent = new SimpleObjectProperty<>();
	private final ObjectProperty<Predicate<TreeItem<T>>> predicate = new SimpleObjectProperty<>();

	protected FilterableTreeItem() {
		// Support filtering by using a filtered list backing.
		// - Apply predicate to items
		FilteredList<TreeItem<T>> filteredChildren = new FilteredList<>(sourceChildren);
		filteredChildren.predicateProperty().bind(Bindings.createObjectBinding(() -> child -> {
			if (child instanceof FilterableTreeItem<T> filterable) {
				filterable.predicateProperty().set(predicate.get());
				if (filterable.forceVisible())
					return true;
			}
			if (predicate.get() == null || !child.getChildren().isEmpty())
				return true;
			boolean matched = predicate.get().test(child);
			onMatchResult(child, matched);
			return matched;
		}, predicate));

		// Reflection hackery
		setUnderlyingChildren(filteredChildren);
	}

	@Override
	@Deprecated(since = "Use FilterableTreeItem dedicated methods for interacting with children")
	public ObservableList<TreeItem<T>> getChildren() {
		return super.getChildren();
	}

	/**
	 * @return Source parent, ignoring filtering.
	 */
	@Nonnull
	public ObjectProperty<TreeItem<T>> sourceParentProperty() {
		return sourceParent;
	}

	/**
	 * @return {@code true} when the item MUST be shown.
	 */
	public boolean forceVisible() {
		return false;
	}

	/**
	 * Allow additional action on items when the {@link #predicate} is updated.
	 *
	 * @param child
	 * 		Child item.
	 * @param matched
	 * 		Match status.
	 */
	protected void onMatchResult(@Nonnull TreeItem<T> child, boolean matched) {
		// Expand items that match, hide those that do not.
		if (matched) {
			TreeItems.expandParents(child);
		} else {
			child.setExpanded(false);
		}
	}

	/**
	 * Use reflection to directly set the underlying {@link TreeItem#children} field.
	 * The javadoc for that field literally says:
	 * <pre>
	 *  It is important that interactions with this list go directly into the
	 *  children property, rather than via getChildren(), as this may be
	 *  a very expensive call.
	 * </pre>
	 * This will additionally add the current tree-item's {@link TreeItem#childrenListener} to
	 * the given list's listeners.
	 *
	 * @param list
	 * 		Children list.
	 */
	@SuppressWarnings("unchecked")
	private void setUnderlyingChildren(@Nonnull ObservableList<TreeItem<T>> list) {
		Field childrenListenerField = CHILDREN_LISTENER_FIELD;
		if (childrenListenerField == null) {
			// See static block below.
			return;
		}
		try {
			// Add our change listener to the passed list.
			list.addListener((ListChangeListener<? super TreeItem<T>>) childrenListenerField.get(this));

			// Directly set "TreeItem.children"
			CHILDREN_FIELD.set(this, list);
		} catch (ReflectiveOperationException ex) {
			logger.error("Failed to update filterable children", ex);
		}
	}

	/**
	 * Add an unfiltered unsorted child to this item.
	 *
	 * @param item
	 * 		Child item to add.
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public void addAndSortChild(@Nonnull TreeItem<T> item) {
		synchronized (sourceChildren) {
			if (sourceChildren.isEmpty()) {
				sourceChildren.add(item);
			} else {
				int index = Collections.binarySearch((List) getChildren(), item);
				if (index < 0)
					index = -(index + 1);
				sourceChildren.add(index, item);
			}
			if (item instanceof FilterableTreeItem<?> filterableItem)
				filterableItem.sourceParent.set(Unchecked.cast(this));
		}
	}

	/**
	 * Add an unfiltered unsorted child to this item.
	 *
	 * @param item
	 * 		Child item to add.
	 */
	protected void addPreSortedChild(@Nonnull TreeItem<T> item) {
		sourceChildren.add(item);
		if (item instanceof FilterableTreeItem<?> filterableItem)
			filterableItem.sourceParent.set(Unchecked.cast(this));
	}

	/**
	 * Remove an unfiltered child from this item.
	 *
	 * @param child
	 * 		Child item to remove.
	 *
	 * @return {@code true} when child removed.
	 * {@code false} when there was no item removed.
	 */
	public boolean removeSourceChild(@Nonnull TreeItem<T> child) {
		synchronized (sourceChildren) {
			if (child instanceof FilterableTreeItem<?> filterableItem)
				filterableItem.sourceParent.set(null);
			return sourceChildren.remove(child);
		}
	}

	/**
	 * @param comparator
	 * 		Comparator to run to handle child sorting.
	 */
	@SuppressWarnings("unchecked")
	public <I extends TreeItem<T>> void sortChildren(@Nonnull Comparator<I> comparator) {
		synchronized (sourceChildren) {
			sourceChildren.sort((Comparator<TreeItem<T>>) comparator);
		}
	}

	/**
	 * @return Predicate property.
	 */
	@Nonnull
	public ObjectProperty<Predicate<TreeItem<T>>> predicateProperty() {
		return predicate;
	}

	static {
		Field childrenListenerField;
		Field childrenField;
		try {
			childrenListenerField = ReflectUtil.getDeclaredField(TreeItem.class, "childrenListener");
			childrenField = ReflectUtil.getDeclaredField(TreeItem.class, "children");
		} catch (NoSuchFieldException ex) {
			logger.error("Failed to get necessary fields to set children list for TreeItem", ex);
			logger.error("Did the implementation of JavaFX change?");
			childrenListenerField = null;
			childrenField = null;
		}
		CHILDREN_LISTENER_FIELD = childrenListenerField;
		CHILDREN_FIELD = childrenField;
	}
}