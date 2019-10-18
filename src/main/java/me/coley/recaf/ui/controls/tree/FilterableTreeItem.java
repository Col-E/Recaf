package me.coley.recaf.ui.controls.tree;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.*;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.TreeItem;

import java.util.Arrays;
import java.util.function.Predicate;

/**
 * Filterable tree item.
 *
 * @param <T>
 * 		Type of value held by the item.
 *
 * @author kaznovac - https://stackoverflow.com/a/34426897
 */
public class FilterableTreeItem<T> extends TreeItem<T> {
	private final ObservableList<TreeItem<T>> sourceChildren = FXCollections.observableArrayList();
	private final ObjectProperty<Predicate<TreeItem<T>>> predicate = new SimpleObjectProperty<>();
	// Do not inline this. It breaks for some reason. Not sure why but would love to know.
	private final FilteredList<TreeItem<T>> filteredChildren = new FilteredList<>(sourceChildren);

	/**
	 * @param value
	 * 		Item value
	 */
	public FilterableTreeItem(T value) {
		super(value);
		// Support filtering by using a filtered list backing.
		// - Apply predicate to items
		filteredChildren.predicateProperty().bind(Bindings.createObjectBinding(() -> child -> {
			if(child instanceof FilterableTreeItem)
				((FilterableTreeItem<T>) child).predicateProperty().set(predicate.get());
			if(predicate.get() == null || !child.getChildren().isEmpty())
				return true;
			return predicate.get().test(child);
		}, predicate));
		// Reset children based on changed values (from updating the predicate)
		filteredChildren.addListener((ListChangeListener<TreeItem<T>>) c -> {
			while(c.next()) {
				getChildren().removeAll(c.getRemoved());
				// Add changed items
				// - Directory items need to maintain sorted order
				// - Otherwise, default ordering is fine
				if (this instanceof DirectoryItem) {
					for(TreeItem<T> item : c.getAddedSubList()) {
						int index = Arrays.binarySearch(getChildren().toArray(), item);
						if(index < 0)
							index = -(index + 1);
						getChildren().add(index, item);
					}
				} else {
					getChildren().addAll(c.getAddedSubList());
				}
			}
		});
	}

	/**
	 * Used to support filtering. Calls to the base {@link #getChildren()} will break the
	 * filtering effect.
	 *
	 * @return Wrapper of {@link #getChildren()}.
	 */
	public ObservableList<TreeItem<T>> getSourceChildren() {
		return sourceChildren;
	}

	/**
	 * @return Predicate property.
	 */
	public ObjectProperty<Predicate<TreeItem<T>>> predicateProperty() {
		return predicate;
	}
}