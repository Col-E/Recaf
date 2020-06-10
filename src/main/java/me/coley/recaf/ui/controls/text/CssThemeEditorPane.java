package me.coley.recaf.ui.controls.text;

import com.steadystate.css.parser.CSSOMParser;
import me.coley.recaf.Recaf;
import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.ui.controls.ExceptionAlert;
import me.coley.recaf.ui.controls.text.model.Languages;
import me.coley.recaf.util.Log;
import me.coley.recaf.util.UiUtil;
import org.apache.commons.io.FileUtils;
import org.w3c.css.sac.CSSException;
import org.w3c.css.sac.CSSParseException;
import org.w3c.css.sac.ErrorHandler;
import org.w3c.css.sac.InputSource;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
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
		setErrorHandler(new CssErrorHandling(this));
		setOnCodeChange(text -> getErrorHandler().onCodeChange(() -> {
			InputSource cssSource = new InputSource(new StringReader(text));
			CSSOMParser parser = new CSSOMParser();
			parser.setErrorHandler(new ErrorHandler() {
				@Override
				public void warning(CSSParseException exception) throws CSSException {
					throw exception;
				}

				@Override
				public void error(CSSParseException exception) throws CSSException {
					throw exception;
				}

				@Override
				public void fatalError(CSSParseException exception) throws CSSException {
					throw exception;
				}
			});
			parser.parseStyleSheet(cssSource, null, null);
			// TODO: Does this throw exceptions for illegal CSS?
			//  - Update CssErrorHandling
		}));
		setOnKeyReleased(e -> {
			if(controller.config().keys().save.match(e))
				save();
		});
		// Load existing theme content
		try {
			if(THEME_FILE.exists())
				setText(FileUtils.readFileToString(THEME_FILE, StandardCharsets.UTF_8));
			else
				setText("/* ======== Custom theme ======== \n" +
						" * Tips:\n" +
						" *  - Change theme to custom in config to see live changes\n" +
						" *  - Located at: " + THEME_FILE.getAbsolutePath() + "\n" +
						" */");
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
			UiUtil.animateSuccess(getCenter(), 500);
		} catch(Exception ex) {
			UiUtil.animateFailure(getCenter(), 500);
			ExceptionAlert.show(ex, "Failed to save custom theme");
		}
	}
}
