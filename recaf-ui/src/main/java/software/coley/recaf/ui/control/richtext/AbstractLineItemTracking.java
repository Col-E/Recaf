package software.coley.recaf.ui.control.richtext;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.PlainTextChange;
import org.fxmisc.richtext.model.ReadOnlyStyledDocument;
import org.fxmisc.richtext.model.TwoDimensional;
import software.coley.collections.Lists;
import software.coley.recaf.analytics.logging.DebuggingLogger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.behavior.PrioritySortable;
import software.coley.recaf.ui.control.richtext.inheritance.InheritanceTracking;
import software.coley.recaf.ui.control.richtext.problem.ProblemTracking;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Common base for tracking line-based items within an {@link Editor}.
 *
 * @param <T>
 * 		Item type.
 * @param <L>
 * 		Listener type.
 *
 * @author Matt Coley
 * @see ProblemTracking
 * @see InheritanceTracking
 */
public abstract class AbstractLineItemTracking<T extends Comparable<T>, L extends PrioritySortable> implements EditorComponent, Consumer<PlainTextChange> {
	protected static final DebuggingLogger logger = Logging.get(AbstractLineItemTracking.class);
	protected final List<L> listeners = new CopyOnWriteArrayList<>();
	protected final NavigableMap<Integer, List<T>> items = new TreeMap<>();
	protected Editor editor;

	/**
	 * @param item
	 * 		Item to get the line of.
	 *
	 * @return Line number of the item.
	 */
	protected abstract int getLine(@Nonnull T item);

	/**
	 * @param item
	 * 		Item to create a copy of with a new line number.
	 * @param newLine
	 * 		Line number of the new item.
	 *
	 * @return Copy of the item with the new line number.
	 */
	protected abstract T withLine(@Nonnull T item, int newLine);

	/**
	 * Invoke all listeners to notify them of an invalidation of tracked items.
	 *
	 * @param failureMessage
	 * 		Message to log when handling of a listener throws an exception.
	 */
	protected abstract void notifyListeners(@Nonnull String failureMessage);

	/**
	 * @param item
	 * 		Item to add.
	 *
	 * @return {@code true} when the item was added successfully.
	 */
	public boolean addItem(@Nonnull T item) {
		List<T> list;
		synchronized (items) {list = items.computeIfAbsent(getLine(item), k -> new ArrayList<>());}
		boolean insert = Lists.sortedInsert(list, item);
		if (insert)
			notifyListeners("Exception thrown when adding item to tracking");
		return insert;
	}

	/**
	 * @param additions
	 * 		Items to add.
	 *
	 * @return {@code true} when the items were added successfully.
	 */
	public boolean addItems(@Nonnull Collection<T> additions) {
		List<T> list;
		boolean update = false;
		synchronized (items) {
			for (T add : additions) {
				list = items.computeIfAbsent(getLine(add), k -> new ArrayList<>());
				update |= Lists.sortedInsert(list, add);
			}
		}
		if (update)
			notifyListeners("Exception thrown when adding items to tracking");
		return update;
	}

	/**
	 * @param line
	 * 		Line containing items to remove.
	 *
	 * @return {@code true} when an item at the line was removed.
	 * {@code false} when there were no items on the line.
	 */
	public boolean removeByLine(int line) {
		boolean updated;
		synchronized (items) {updated = items.remove(line) != null;}
		if (updated)
			notifyListeners("Exception thrown when removing items from tracking");
		return updated;
	}

	/**
	 * Clear all tracked items.
	 */
	public void clear() {
		boolean updated = !items.isEmpty();
		synchronized (items) {items.clear();}
		if (updated)
			notifyListeners("Exception thrown when clearing items from tracking");
	}

	/**
	 * @param listener
	 * 		Listener to add.
	 *
	 * @return {@code true} when listener was added.
	 */
	public boolean addListener(@Nonnull L listener) {
		// PrioritySortable is used by callers; keep the same behavior.
		return PrioritySortable.add(listeners, listener);
	}

	/**
	 * @param listener
	 * 		Listener to remove.
	 *
	 * @return {@code true} when listener was removed.
	 * {@code false} when listener was not present to begin with.
	 */
	public boolean removeListener(@Nonnull L listener) {
		return listeners.remove(listener);
	}

	/**
	 * @param line
	 * 		Line number to get items for.
	 *
	 * @return Matched items on the given line.
	 */
	@Nonnull
	public List<T> getItemsOnLine(int line) {
		synchronized (items) {return items.getOrDefault(line, Collections.emptyList());}
	}

	/**
	 * @param line
	 * 		Line number of item.
	 *
	 * @return First item on the line.
	 */
	@Nullable
	public T getFirstItemOnLine(int line) {
		List<T> items = getItemsOnLine(line);
		if (items.isEmpty())
			return null;
		return items.getFirst();
	}

	/**
	 * @return Map of all items by line number.
	 */
	@Nonnull
	public NavigableMap<Integer, List<T>> getItems() {
		return Collections.synchronizedNavigableMap(items);
	}

	/**
	 * @return All items as a flattened list.
	 */
	@Nonnull
	public List<T> getAllItems() {
		List<T> list = new ArrayList<>();
		synchronized (items) {items.values().forEach(list::addAll);}
		return list;
	}

	/**
	 * Get all items as a flattened list that match the given filter.
	 *
	 * @param filter
	 * 		Item filter.
	 *
	 * @return Matched items.
	 */
	@Nonnull
	public List<T> getItems(@Nonnull Predicate<T> filter) {
		List<T> list = new ArrayList<>();
		synchronized (items) {
			items.values().stream()
					.flatMap(Collection::stream)
					.filter(filter)
					.forEach(list::add);
		}
		return list;
	}

	@Override
	public void accept(PlainTextChange change) {
		// Skip if there is no associated editor, or there are no items to update
		if (editor == null || items.isEmpty()) return;

		// TODO: There are some edge cases where the tracking of item indicators will fail, and we just
		//  delete them. Deleting an empty line before a line with an error will void it.
		//  I'm not really sure how to make a clean fix for that, but because the rest of
		//  it works relatively well I'm not gonna touch it for now.
		try {
			String insertedText = change.getInserted();
			String removedText = change.getRemoved();
			boolean lineInserted = insertedText.contains("\n");
			boolean lineRemoved = removedText.contains("\n");

			// Handle line removal/insertion.
			//
			// Some thoughts, you may ask why we do an "if" block for each, but not with else if.
			// Well, copy-pasting text does both. So we remove then insert for replacement.
			if (lineRemoved) {
				ReadOnlyStyledDocument<?, ?, ?> lastDocumentSnapshot = editor.getLastDocumentSnapshot();

				// Get line number and add +1 to make it 1-indexed
				int start = lastDocumentSnapshot.offsetToPosition(change.getPosition(), TwoDimensional.Bias.Backward).getMajor() + 1;

				// End line number needs +1 since it will include the next line due to inclusion of "\n"
				int end = lastDocumentSnapshot.offsetToPosition(change.getRemovalEnd(), TwoDimensional.Bias.Backward).getMajor() + 1;

				onLinesRemoved(start, end);
			}
			if (lineInserted) {
				CodeArea area = editor.getCodeArea();

				// Get line number and add +1 to make it 1-indexed
				int start = area.offsetToPosition(change.getPosition(), TwoDimensional.Bias.Backward).getMajor() + 1;

				// End line number doesn't need +1 since it will include the next line due to inclusion of "\n"
				int end = area.offsetToPosition(change.getInsertionEnd(), TwoDimensional.Bias.Backward).getMajor();

				onLinesInserted(start, end);
			}
		} catch (Throwable t) {
			logger.error("Error updating offsets in text document", t);
		}
	}

	/**
	 * Shifts items beyond the given range by {@code 1 + (endLine - startLine)}.
	 * Items in the removed range are deleted.
	 *
	 * @param startLine
	 * 		Starting range of lines inserted <i>(inclusive)</i>.
	 * @param endLine
	 * 		Ending range of lines inserted <i>(inclusive)</i>.
	 */
	protected void onLinesInserted(int startLine, int endLine) {
		logger.debugging(l -> l.trace("Lines inserted: {}-{}", startLine, endLine));
		TreeSet<Map.Entry<Integer, List<T>>> set = new TreeSet<>((o1, o2) -> Integer.compare(o2.getKey(), o1.getKey()));
		set.addAll(items.entrySet());
		set.stream()
				.filter(e -> e.getKey() >= startLine)
				.forEach(e -> {
					int line = e.getKey();
					List<T> list = e.getValue();

					// Shift all items down by the shift amount.
					int shift = 1 + endLine - startLine;
					removeByLine(line);
					list.forEach(i -> {
						logger.debugging(l -> l.trace("Move item '{}' down {} lines", i, shift));
						addItem(withLine(i, line + shift));
					});
				});
	}

	/**
	 * Shifts items beyond the given range by {@code endLine - startLine}.
	 * Items in the removed range are deleted.
	 *
	 * @param startLine
	 * 		Starting range of lines removed <i>(inclusive)</i>.
	 * @param endLine
	 * 		Ending range of lines removed <i>(exclusive)</i>.
	 */
	protected void onLinesRemoved(int startLine, int endLine) {
		logger.debugging(l -> l.trace("Lines removed: {}-{}", startLine, endLine));

		// We will want to sort the order of removed lines so we
		TreeSet<Map.Entry<Integer, List<T>>> set = new TreeSet<>(Map.Entry.comparingByKey());
		synchronized (items) {set.addAll(items.entrySet());}
		set.stream()
				.filter(e -> e.getKey() >= startLine)
				.forEach(e -> {
					int line = e.getKey();
					List<T> list = e.getValue();

					// Shift all items up by the shift amount
					int shift = endLine - startLine;
					removeByLine(line);

					// Don't add item back if it's in the removed range
					list.stream()
							.filter(i -> getLine(i) < startLine || getLine(i) > endLine)
							.forEach(i -> {
								logger.debugging(l -> l.trace("Move item '{}' up {} lines", i, shift));
								addItem(withLine(i, line - shift));
							});
				});
	}

	@Override
	public void install(@Nonnull Editor editor) {
		this.editor = editor;
		clear();
		editor.getTextChangeEventStream().addObserver(this);
	}

	@Override
	public void uninstall(@Nonnull Editor editor) {
		this.editor = null;
		clear();
		editor.getTextChangeEventStream().removeObserver(this);
	}
}

