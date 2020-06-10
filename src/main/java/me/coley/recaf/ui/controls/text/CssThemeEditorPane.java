package me.coley.recaf.ui.controls.text;

import me.coley.recaf.Recaf;
import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.ui.controls.ExceptionAlert;
import me.coley.recaf.ui.controls.text.model.Languages;
import me.coley.recaf.util.Log;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Css-focused text editor. Used to update a custom theme.
 *
 * @author Matt
 */
public class CssThemeEditorPane extends EditorPane<CssErrorHandling, CssContextHandling> {
	private static final String THEME_NAME = "custom";
	private static final File THEME_FILE =
			Recaf.getDirectory("style").resolve("ui-" + THEME_NAME + ".css").toFile();


	/**
	 * @param controller
	 * 		Controller to act on.
	 */
	public CssThemeEditorPane(GuiController controller) {
		super(controller, Languages.find("css"), (c, t) -> null);
		setEditable(true);
		setOnKeyReleased(e -> {
			if(controller.config().keys().save.match(e))
				save();
		});
		// Load existing theme content
		try {
			if(THEME_FILE.exists())
				setText(FileUtils.readFileToString(THEME_FILE, StandardCharsets.UTF_8));
		} catch(IOException ex) {
			Log.error(ex, "Failed to load custom theme");
		}

	}

	/**
	 * Apply the current CSS.
	 */
	private void save() {
		String css = getText();
		if (!THEME_FILE.getParentFile().exists())
			THEME_FILE.getParentFile().mkdir();
		// Save and reload
		try {
			FileUtils.write(THEME_FILE, css, StandardCharsets.UTF_8);
			if (controller.config().display().appStyle.getFileName().equals("ui-" + THEME_NAME + ".css"))
				controller.windows().reapplyStyles();
		} catch(Exception ex) {
			ExceptionAlert.show(ex, "Failed to save custom theme");
		}
	}
}
