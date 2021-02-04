package me.coley.recaf.ui.dnd;

import javafx.scene.control.Control;
import javafx.scene.input.DragEvent;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Simple file drop listener.
 *
 * @author Matt Coley
 */
public interface FileDropListener {
	/**
	 * Called when the drop is complete.
	 *
	 * @param control
	 * 		Control dropped on.
	 * @param event
	 * 		Drop event.
	 * @param files
	 * 		Files dropped.
	 *
	 * @throws IOException
	 * 		When handling of the files fails.
	 */
	void onDragDrop(Control control, DragEvent event, List<Path> files) throws IOException;

	/**
	 * Called when drag moves over the control.
	 *
	 * @param control
	 * 		Control moused over.
	 * @param event
	 * 		Drag event.
	 */
	default void onDragEnter(Control control, DragEvent event) {
		// no-op
	}

	/**
	 * Called when drag moves outside the control.
	 *
	 * @param control
	 * 		Control left.
	 * @param event
	 * 		Drag event.
	 */
	default void onDragExit(Control control, DragEvent event) {
		// no-op
	}
}
