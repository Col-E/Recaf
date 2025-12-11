package software.coley.recaf.behavior;

import jakarta.annotation.Nonnull;
import software.coley.collections.Lists;

import java.util.List;

/**
 * Priority sortable item.
 *
 * @author Matt Coley
 */
public interface PrioritySortable extends Comparable<PrioritySortable> {
	/**
	 * @return This item's priority value.
	 * Negative values have higher priority.
	 * Positive values have lower priority.
	 *
	 * @see PriorityKeys
	 */
	default int getPriority() {
		// Everything will default to '0' and the order of items is not guaranteed.
		//
		// The idea is that most things do not need a guaranteed run order, but for the few edge cases that do
		// those specific cases will use higher/lower values to be moved to the front/end of the sorted list.
		return PriorityKeys.DEFAULT;
	}

	@Override
	default int compareTo(@Nonnull PrioritySortable o) {
		return Integer.compare(getPriority(), o.getPriority());
	}

	/**
	 * Add a sortable item to a sortable list.
	 *
	 * @param items
	 * 		List to add to.
	 * @param item
	 * 		Item to add.
	 * @param <T>
	 * 		Child priority-sortable type.
	 *
	 * @return {@code true} on add. {@code false} on failure <i>(Insertion index cannot be computed)</i>,
	 */
	static <T extends PrioritySortable> boolean add(@Nonnull List<T> items, @Nonnull T item) {
		return Lists.sortedInsert(PrioritySortable::compareTo, items, item);
	}
}
