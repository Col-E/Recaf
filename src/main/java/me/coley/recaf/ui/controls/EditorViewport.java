package me.coley.recaf.ui.controls;

import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import jregex.Matcher;
import jregex.Pattern;
import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.ui.controls.HexEditor;
import me.coley.recaf.util.struct.ListeningMap;
import me.coley.recaf.workspace.JavaResource;

import java.util.Arrays;
import java.util.Map;

/**
 * Multi-view wrapper for files in resources.
 *
 * @author Matt
 */
public class EditorViewport extends BorderPane {
	private static final float TEXT_THRESHOLD = 0.9f;
	private static final Pattern TEXT_MATCHER = new Pattern("[\\w\\d\\s\\<\\>\\-\\\\\\/\\.:,!@#$%^&*\"=\\[\\]?;\\{\\}]+");
	//
	private final GuiController controller;
	private final JavaResource resource;
	private final String path;
	private final boolean isClass;
	private byte[] last;
	private byte[] current;

	/**
	 * @param controller
	 * 		Controller context.
	 * @param resource
	 * 		Resource the file resides in.
	 * @param path
	 * 		Path to file.
	 * @param isClass
	 *        {@code true} if the file is a class.
	 */
	public EditorViewport(GuiController controller, JavaResource resource, String path, boolean isClass) {
		this.controller = controller;
		this.resource = resource;
		this.path = path;
		this.isClass = isClass;
		fetchLast();
		updateView();
		// TODO: Bind-able action keys
		//  - ex:   String key = controller.config().keys.save;
		//          KeyCode.valueOf(key);
		setOnKeyReleased(e -> {
			if (e.isControlDown() && e.getCode() == KeyCode.S)
				save();
			if (e.isControlDown() && e.getCode() == KeyCode.U)
				undo();
		});
	}

	/**
	 * Save current modifications &amp; create a history entry for the changed item.<br>
	 * If {@link #current} is {@code null} there is no modification to save.
	 */
	private void save() {
		// TODO: Should saving changes also include making a save state?
		//  - Or should saving the state be a unique action/keybind?

		// Skip if no modifications to save.
		if (current == null || Arrays.equals(last, current))
			return;
		// Save current & create history entry.
		if(isClass) {
			resource.getClasses().put(path, current);
			resource.getClassHistory(path).push(current);
		} else {
			resource.getResources().put(path, current);
			resource.getResourceHistory(path).push(current);
		}
		// Set current to null so we can't save the same thing over and over.
		last = current;
		current = null;
	}


	/**
	 * Loads the most recent save from the file history.
	 */
	private void undo() {
		byte[] prior;
		if (isClass)
			prior = resource.getClassHistory(path).pop();
		else
			prior = resource.getResourceHistory(path).pop();
		// Reset caches
		last = prior;
		current = null;
		// Update view with popped content
		// TODO: Instead of replacing the control, update it.
		//  - Handle locally (here) or handle with control wrapper classes?
		updateView();
	}

	/**
	 * Set {@link #last} to the current file content.
	 */
	private void fetchLast() {
		Map<String, byte[]> map = isClass ? resource.getClasses() : resource.getResources();
		last = map.get(path);
	}

	/**
	 * Set the viewport for the current editor mode.
	 */
	private void updateView() {
		if(isClass)
			updateClassView(getClassMode());
		else
			updateResourceView(getResourceMode());
	}

	private void updateClassView(ClassMode mode) {
		switch(mode) {
			case DECOMPILE:
				// TODO: shows decompiled code, allow actions to run on selections
				break;
			case NODE_EDITOR:
				// TODO: more like how Recaf was in 1.X
				break;
			case HEX:
			default:
				HexEditor hex = new HexEditor(last);
				hex.setContentCallback(array -> current = array);
				setCenter(hex);
				break;
		}
	}

	private void updateResourceView(ResourceMode mode) {
		switch(mode) {
			case AUTO:
				// Determine which resource mode to use based on the % of the
				// content matches common text symbols. Binary data will likely
				// not contain a high % of legible text content.
				String text = new String(last);
				Matcher m = TEXT_MATCHER.matcher(text);
				float size = 0;
				while (m.find())
					size += m.length();
				if (size / text.length() > TEXT_THRESHOLD)
					updateResourceView(ResourceMode.TEXT);
				else
					updateResourceView(ResourceMode.HEX);
				return;
			case TEXT:
				// TODO: More varied support for certain kinds of files:
				//  - JSON
				//  - XML
				//  - Properties / INI
				//  - Source code
				TextArea area = new TextArea(new String(last));
				area.setWrapText(true);
				area.setOnKeyReleased(e -> current = area.getText().getBytes());
				setCenter(area);
				break;
			case HEX:
			default:
				HexEditor hex = new HexEditor(last);
				hex.setContentCallback(array -> current = array);
				setCenter(hex);
				break;
		}
		// Focus after setting
		if (getCenter() != null)
			getCenter().requestFocus();
	}

	// =================== Enums for the different editors =================== //

	/**
	 * @return Mode that indicated which view to use for modifying classes.
	 */
	public ClassMode getClassMode() {
		return controller.config().backend().classEditorMode;
	}

	/**
	 * @return Mode that indicated which view to use for modifying resources.
	 */
	public ResourceMode getResourceMode() {
		return controller.config().backend().resourceEditorMode;
	}

	public enum ClassMode {
		DECOMPILE, NODE_EDITOR, HEX
	}

	public enum ResourceMode {
		TEXT, HEX, AUTO
	}
}
