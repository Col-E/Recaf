package software.coley.recaf.util;

import jakarta.annotation.Nonnull;

import java.util.Collections;
import java.util.List;

/**
 * Various collection utils.
 *
 * @author Matt Coley
 * @author <a href="https://stackoverflow.com/a/29356678/8071915">Paul Boddington</a> - Binary search.
 */
public class CollectionUtil {
	/**
	 * @param list
	 * 		List to insert into.
	 * @param item
	 * 		Item to insert.
	 * @param <T>
	 * 		Item type.
	 *
	 * @return Index to insert the item at to ensure sorted order.
	 * Results are only correct if the list itself is already in sorted order.
	 */
	public static <T extends Comparable<T>> int sortedInsertIndex(@Nonnull List<T> list, @Nonnull T item) {
		if (list.isEmpty()) return 0;
		int i = Collections.binarySearch(list, item);
		if (i < 0) i = -i - 1; // When not found, invert to get correct index.
		return i;
	}

	/**
	 * @param listA
	 * 		Some list of comparable items. Assumed to be in sorted order.
	 * @param listB
	 * 		Another list of comparable items. Assumed to be in sorted order.
	 * @param <T>
	 * 		Item type.
	 *
	 * @return Comparison of the lists.
	 */
	public static <T extends Comparable<T>> int compare(List<T> listA, List<T> listB) {
		int len = Math.min(listA.size(), listB.size());
		for (int i = 0; i < len; i++) {
			int cmp = listA.get(i).compareTo(listB.get(i));
			if (cmp != 0) return cmp;
		}
		return 0;
	}

	/**
	 * @param items
	 * 		Item array to search in.
	 * @param target
	 * 		Item to search for.
	 * @param <T>
	 * 		Item type.
	 *
	 * @return Index of item in array.
	 * If the item is not in the array, the negative value of the index where it would appear in sorted order.
	 */
	public static <T extends Comparable<T>> int binarySearch(T[] items, T target) {
		return binarySearch(items, target, 0, items.length - 1);
	}

	/**
	 * @param items
	 * 		Item list to search in.
	 * @param target
	 * 		Item to search for.
	 * @param <T>
	 * 		Item type.
	 *
	 * @return Index of item in list.
	 * If the item is not in the list, the negative value of the index where it would appear in sorted order.
	 */
	public static <T extends Comparable<T>> int binarySearch(List<T> items, T target) {
		return binarySearch(items, target, 0, items.size() - 1);
	}

	/**
	 * @param items
	 * 		Item array to search in.
	 * @param target
	 * 		Item to search for.
	 * @param first
	 * 		Start range.
	 * @param last
	 * 		End range.
	 * @param <T>
	 * 		Item type.
	 *
	 * @return Index of item in array, within the range.
	 * If the item is not in the array, the negative value of the index where it would appear in sorted order.
	 */
	public static <T extends Comparable<T>> int binarySearch(T[] items, T target, int first, int last) {
		if (first > last)
			// Typically yield '-1' but with this, we will have it such that if 'target' is not in the list
			// then the return value will be the negative value of the index where it would be inserted into
			// while maintaining sorted order.
			return (first == 0 && last == -1) ? 0 : -last;
		else {
			int middle = (first + last) / 2;
			int compResult = target.compareTo(items[middle]);
			if (compResult == 0)
				return middle;
			else if (compResult < 0)
				return binarySearch(items, target, first, middle - 1);
			else
				return binarySearch(items, target, middle + 1, last);
		}
	}

	/**
	 * @param items
	 * 		Item list to search in.
	 * @param target
	 * 		Item to search for.
	 * @param first
	 * 		Start range.
	 * @param last
	 * 		End range.
	 * @param <T>
	 * 		Item type.
	 *
	 * @return Index of item in list, within the range.
	 * If the item is not in the list, the negative value of the index where it would appear in sorted order.
	 */
	public static <T extends Comparable<T>> int binarySearch(List<T> items, T target, int first, int last) {
		if (first > last)
			// Typically yield '-1' but with this, we will have it such that if 'target' is not in the list
			// then the return value will be the negative value of the index where it would be inserted into
			// while maintaining sorted order.
			return (first == 0 && last == -1) ? 0 : -last;
		else {
			int middle = (first + last) / 2;
			int compResult = target.compareTo(items.get(middle));
			if (compResult == 0)
				return middle;
			else if (compResult < 0)
				return binarySearch(items, target, first, middle - 1);
			else
				return binarySearch(items, target, middle + 1, last);
		}
	}
}
