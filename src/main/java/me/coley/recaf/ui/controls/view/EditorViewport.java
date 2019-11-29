package me.coley.recaf.ui.controls.view;

import javafx.scene.layout.BorderPane;
import me.coley.recaf.config.ConfKeybinding;
import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.workspace.History;
import me.coley.recaf.workspace.JavaResource;

import java.util.Arrays;
import java.util.Map;

/**
 * Multi-view wrapper for files in resources.
 *
 * @author Matt
 */
public abstract class EditorViewport extends BorderPane {
	protected final GuiController controller;
	protected final JavaResource resource;
	protected final String path;
	protected final boolean isPrimary;
	protected byte[] last;
	protected byte[] current;

	/**
	 * @param controller
	 * 		Controller context.
	 * @param resource
	 * 		Resource the file resides in.
	 * @param path
	 * 		Path to file.
	 */
	public EditorViewport(GuiController controller, JavaResource resource, String path) {
		this.controller = controller;
		this.resource = resource;
		this.path = path;
		this.isPrimary = controller.getWorkspace().getPrimary().equals(resource);
		fetchLast();
		updateView();
		ConfKeybinding keys = controller.config().keys();
		setOnKeyReleased(e -> {
			if (keys.save.match(e))
				save();
			if (keys.undo.match(e))
				undo();
		});
	}

	/**
	 * Set {@link #last} to the current file content.
	 */
	private void fetchLast() {
		last = getMap().get(path);
	}

	/**
	 * Save current modifications &amp; create a history entry for the changed item.<br>
	 * If {@link #current} is {@code null} there is no modification to save.
	 */
	protected void save() {
		// Skip if no modifications to save.
		if (current == null || Arrays.equals(last, current))
			return;
		// Save current & create history entry.
		getMap().put(path, current);
		getHistory(path).push(current);
		// Set current to null so we can't save the same thing over and over.
		last = current;
		current = null;
	}

	/**
	 * Loads the most recent save from the file history.
	 */
	private void undo() {
		byte[] prior;
		prior = getHistory(path).pop();
		// Reset caches
		last = prior;
		current = null;
		// Update view with popped content
		// TODO: Instead of replacing the control, update it.
		//  - Handle locally (in the control) or handle here?
		updateView();
	}

	/**
	 * @param path
	 * 		File path.
	 *
	 * @return Resource of file.
	 */
	protected abstract History getHistory(String path);

	/**
	 * @return Map in resource to use for pulling/putting files.
	 */
	protected abstract Map<String, byte[]> getMap();

	/**
	 * Set the viewport for the current editor mode.
	 */
	protected abstract void updateView();
}
