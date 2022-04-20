package me.coley.recaf.ui.controls.view;

import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import jregex.Matcher;
import jregex.Pattern;
import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.plugin.PluginKeybinds;
import me.coley.recaf.ui.controls.HexEditor;
import me.coley.recaf.ui.controls.text.JavaEditorPane;
import me.coley.recaf.ui.controls.text.EditorPane;
import me.coley.recaf.ui.controls.text.model.Language;
import me.coley.recaf.ui.controls.text.model.Languages;
import me.coley.recaf.util.Log;
import me.coley.recaf.util.StringUtil;
import me.coley.recaf.util.UiUtil;
import me.coley.recaf.workspace.History;
import me.coley.recaf.workspace.JavaResource;

import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Multi-view wrapper for files in resources.
 *
 * @author Matt
 */
public class FileViewport extends EditorViewport {
	private static final float TEXT_THRESHOLD = 0.9f;
	private static final Pattern TEXT_MATCHER = new Pattern(
			"[\\w\\d\\s\\<\\>\\-\\\\\\/\\.:,!@#+$%^&*\"=\\[\\]?;\\{\\}\\(\\)|]+");
	private FileMode overrideMode;

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
	protected void handleKeyReleased(KeyEvent e) {
		super.handleKeyReleased(e);
		// Custom bind support
		PluginKeybinds.getInstance().getFileViewBinds().forEach((bind, action) -> {
			try {
				if (bind.match(e))
					action.accept(this);
			} catch(Throwable t) {
				Log.error(t, "Failed executing file keybind action");
			}
		});
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
		if (overrideMode != null)
			return overrideMode;
		return controller.config().display().fileEditorMode;
	}

	private void updateFileMode(FileMode mode) {
		switch(mode) {
			case AUTOMATIC:
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
				else {
					// Check for image types
					BufferedImage img = UiUtil.toImage(last);
					if(img != null) {
						ImageView view = new ImageView(UiUtil.toFXImage(img));
						setCenter(view);
						return;
					}
					// Fall back to default hex mode.
					updateFileMode(FileMode.HEX);
				}
				return;
			case TEXT:
				updateTextMode();
				break;
			case HEX:
			default:
				updateHexMode();
				break;
		}
		// Focus after setting
		if (getCenter() != null)
			getCenter().requestFocus();
	}

	/**
	 * Handle the current file as a binary type.
	 */
	private void updateHexMode() {
		// Fallback: Hex editor
		HexEditor hex = new HexEditor(last);
		hex.setContentCallback(array -> current = array);
		hex.setEditable(resource.isPrimary());
		setCenter(hex);
	}

	/**
	 * Handle the current file as a text type.
	 */
	private void updateTextMode() {
		// Get language by extension
		String ext = "none";
		if (path.contains("."))
			ext = path.substring(path.lastIndexOf(".") + 1);
		Language lang = Languages.find(ext);
		// Create editor
		EditorPane pane = lang.getName().equals("Java") ?
				new JavaEditorPane(controller, resource) :
				new EditorPane<>(controller, lang, (a, b) -> null);
		pane.setText(new String(last, StandardCharsets.UTF_8));
		pane.scrollToTop();
		pane.setWrapText(lang.doWrap() || controller.config().display().forceWordWrap);
		pane.setEditable(resource.isPrimary());
		pane.setOnKeyReleased(e -> current = pane.getText().getBytes());
		setCenter(pane);
	}

	/**
	 * Set a new mode to view files in then refresh the view.
	 *
	 * @param overrideMode
	 * 		New mode to view files in.
	 */
	public void setOverrideMode(FileMode overrideMode) {
		this.overrideMode = overrideMode;
		updateView();
	}

	/**
	 * @return Controller
	 */
	public GuiController getController() {
		return controller;
	}

	/**
	 * Viewport editor type.
	 */
	public enum FileMode {
		AUTOMATIC, TEXT, HEX;

		@Override
		public String toString() {
			return StringUtil.toString(this);
		}
	}
}
