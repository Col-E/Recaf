package software.coley.recaf.ui.control;

import com.google.common.collect.Iterables;
import jakarta.annotation.Nonnull;
import javafx.collections.ObservableList;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;

import java.util.Objects;
import java.util.function.Function;

/**
 * A list cell that allows re-ordering within the parent {@link ListView} via drag-drop.
 *
 * @param <T>
 * 		Cell content type.
 *
 * @author Matt Coley
 */
public class ReorderableListCell<T> extends ListCell<T> {
	/**
	 * New list cell with {@code T} mapped via {@link Object#toString()} to {@link String} for drag-drop identification.
	 */
	public ReorderableListCell() {
		this(Objects::toString);
	}

	/**
	 * New list cell with {@code T} mapped via a given function to {@link String} for drag-drop identification.
	 *
	 * @param converter
	 * 		T instance to string converter.
	 */
	public ReorderableListCell(@Nonnull Function<T, String> converter) {
		var thisCell = this;

		// Begin by placing the item's string representation into the drag-board.
		setOnDragDetected(event -> {
			if (getItem() == null)
				return;
			Dragboard dragboard = startDragAndDrop(TransferMode.MOVE);
			ClipboardContent content = new ClipboardContent();
			content.putString(converter.apply(getItem()));
			dragboard.setDragView(thisCell.snapshot(null, null));
			dragboard.setContent(content);
			event.consume();
		});

		// All reorderable list cells can accept others.
		setOnDragOver(e -> {
			if (e.getGestureSource() != thisCell && e.getDragboard().hasString())
				e.acceptTransferModes(TransferMode.MOVE);
			e.consume();
		});

		// While dragging over this cell, set it to be transparent to indicate this
		// is the cell that will be swapped upon completion.
		setOnDragEntered(e -> {
			if (e.getGestureSource() != thisCell && e.getDragboard().hasString())
				setOpacity(0.3);
		});
		setOnDragExited(e -> {
			if (e.getGestureSource() != thisCell && e.getDragboard().hasString())
				setOpacity(1);
		});

		// Complete by swapping the cell indices.
		setOnDragDropped(event -> {
			if (getItem() == null)
				return;

			Dragboard dragboard = event.getDragboard();
			if (dragboard.hasString()) {
				ListView<T> parent = getListView();
				if (parent == null)
					return;

				// Get the drag-drop initiator cell index, and the target cell index.
				ObservableList<T> items = parent.getItems();
				String string = dragboard.getString();
				int draggedIdx = Iterables.indexOf(items, item -> string.equals(converter.apply(item)));
				int thisIdx = items.indexOf(getItem());

				// Swap and complete.
				T draggedItem = items.get(draggedIdx);
				items.set(draggedIdx, getItem());
				items.set(thisIdx, draggedItem);

				event.setDropCompleted(true);
			} else {
				event.setDropCompleted(false);
			}

			event.consume();
		});

		setOnDragDone(DragEvent::consume);
	}
}
