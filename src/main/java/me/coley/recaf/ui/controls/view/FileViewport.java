package me.coley.recaf.ui.controls.view;

import jregex.Matcher;
import jregex.Pattern;
import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.ui.controls.HexEditor;
import me.coley.recaf.ui.controls.text.TextPane;
import me.coley.recaf.ui.controls.text.model.Language;
import me.coley.recaf.ui.controls.text.model.Languages;
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
	private static final Pattern TEXT_MATCHER = new Pattern("[\\w\\d\\s\\<\\>\\-\\\\\\/\\.:," +
			"!@#$%^&*\"=\\[\\]?;\\{\\}]+");

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
	public void updateView() {
		updateFileMode(getFileMode());
	}

	/**
	 * @return Mode that indicated which view to use for modifying files.
	 */
	public FileMode getFileMode() {
		return controller.config().display().fileEditorMode;
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
				// Get language by extension
				String ext = "none";
				if (path.contains("."))
					ext = path.substring(path.lastIndexOf(".") + 1);
				Language lang = Languages.find(ext);
				// Create editor
				TextPane pane = new TextPane<>(controller, lang, (a, b) -> null);
				pane.setText(new String(last));
				pane.setWrapText(lang.doWrap());
				pane.setEditable(resource.isPrimary());
				pane.setOnKeyReleased(e -> current = pane.getText().getBytes());
				setCenter(pane);
				break;
			case HEX:
			default:
				HexEditor hex = new HexEditor(last);
				hex.setContentCallback(array -> current = array);
				hex.setEditable(resource.isPrimary());
				setCenter(hex);
				break;
		}
		// Focus after setting
		if (getCenter() != null)
			getCenter().requestFocus();
	}

	/**
	 * Viewport editor type.
	 */
	public enum FileMode {
		AUTO, TEXT, HEX
	}
}
