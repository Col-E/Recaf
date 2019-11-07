package me.coley.recaf.ui.controls.view;

import javafx.scene.control.TextArea;
import jregex.Matcher;
import jregex.Pattern;
import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.ui.controls.HexEditor;
import me.coley.recaf.workspace.History;
import me.coley.recaf.workspace.JavaResource;

import java.util.Map;

/**
 * Multi-view wrapper for files in resources.
 *
 * @author Matt
 */
public class FileViewport extends EditorViewport {
	private static final float TEXT_THRESHOLD = 0.9f;
	private static final Pattern TEXT_MATCHER = new Pattern("[\\w\\d\\s\\<\\>\\-\\\\\\/\\.:,!@#$%^&*\"=\\[\\]?;\\{\\}]+");

	/**
	 * @param controller
	 * 		Controller context.
	 * @param resource
	 * 		Resource the file resides in.
	 * @param path
	 * 		Path to file.
	 */
	public FileViewport(GuiController controller, JavaResource resource, String path) {
		super(controller, resource, path);
	}

	@Override
	protected History getHistory(String path) {
		return resource.getFileHistory(path);
	}


	@Override
	protected Map<String, byte[]> getMap() {
		return resource.getFiles();
	}

	@Override
	protected void updateView() {
		updateFileMode(getFileMode());
	}

	/**
	 * @return Mode that indicated which view to use for modifying files.
	 */
	public FileMode getFileMode() {
		return controller.config().backend().fileEditorMode;
	}

	private void updateFileMode(FileMode mode) {
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
					updateFileMode(FileMode.TEXT);
				else
					updateFileMode(FileMode.HEX);
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

	public enum FileMode {
		TEXT, HEX, AUTO
	}
}
