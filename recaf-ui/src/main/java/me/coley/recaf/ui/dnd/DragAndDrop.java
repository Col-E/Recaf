package me.coley.recaf.ui.dnd;

import javafx.scene.control.Control;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import me.coley.recaf.util.logging.Logging;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Drag and drop utilities.
 *
 * @author Matt Coley
 */
public class DragAndDrop {
	private static final Logger logger = Logging.get(DragAndDrop.class);

	/**
	 * Install drag-drop support on the given control.
	 *
	 * @param control
	 * 		Control to support.
	 * @param listener
	 * 		Behavior when file content is dropped into the control.
	 */
	public static void installFileSupport(Control control, FileDropListener listener) {
		control.setOnDragOver(e -> onDragOver(control, e));
		control.setOnDragDropped(e -> onDragDropped(control, e, listener));
		control.setOnDragEntered(e -> onDragEntered(control, e, listener));
		control.setOnDragExited(e -> onDragExited(control, e, listener));
	}

	/**
	 * Handle drag-over.
	 *
	 * @param control
	 * 		Control dragged over.
	 * @param event
	 * 		Drag event.
	 */
	private static void onDragOver(Control control, DragEvent event) {
		if (event.getGestureSource() != control && event.getDragboard().hasFiles()) {
			event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
		}
		event.consume();
	}

	/**
	 * Pass drag-drop to listener.
	 *
	 * @param control
	 * 		Control dropped completed on.
	 * @param event
	 * 		Drag event.
	 * @param listener
	 * 		Listener to call when drop completed.
	 */
	private static void onDragDropped(Control control, DragEvent event, FileDropListener listener) {
		Dragboard db = event.getDragboard();
		boolean success = true;
		if (db.hasFiles()) {
			try {
				List<Path> paths = db.getFiles().stream()
						.map(file -> Paths.get(file.getAbsolutePath()))
						.collect(Collectors.toList());
				listener.onDragDrop(control, event, paths);
			} catch (IOException ex) {
				logger.error("Failed drag-and-drop due to IO", ex);
				success = false;
			} catch (Throwable ex) {
				logger.error("Failed drag-and-drop due to unhanded error", ex);
				success = false;
			}
		}
		event.setDropCompleted(success);
		event.consume();
	}

	/**
	 * Pass drag-entering to listener.
	 *
	 * @param control
	 * 		Control dragged over.
	 * @param event
	 * 		Drag event.
	 * @param listener
	 * 		Listener to call when drag enters.
	 */
	private static void onDragEntered(Control control, DragEvent event, FileDropListener listener) {
		listener.onDragEnter(control, event);
		event.consume();
	}

	/**
	 * Pass drag-leaving to listener.
	 *
	 * @param control
	 * 		Control dragged over.
	 * @param event
	 * 		Drag event.
	 * @param listener
	 * 		Listener to call when drag leaves.
	 */
	private static void onDragExited(Control control, DragEvent event, FileDropListener listener) {
		listener.onDragExit(control, event);
		event.consume();
	}
}
