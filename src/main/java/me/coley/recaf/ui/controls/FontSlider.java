package me.coley.recaf.ui.controls;

import javafx.scene.Scene;
import javafx.scene.control.Slider;
import me.coley.recaf.Recaf;
import me.coley.recaf.config.FieldWrapper;
import me.coley.recaf.control.gui.GuiController;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Font scaling control &amp; utility.
 *
 * @author Matt
 */
public class FontSlider extends Slider {
	private static final File FONT_SIZE_CSS = Recaf.getDirectory("style").resolve("font-size.css").toFile();

	/**
	 * @param controller
	 * 		Controller to act on.
	 * @param wrapper
	 * 		Font size field wrapper.
	 */
	public FontSlider(GuiController controller, FieldWrapper wrapper) {
		setMin(10);
		setMax(16);
		setMajorTickUnit(1);
		setMinorTickCount(0);
		setShowTickMarks(true);
		setShowTickLabels(true);
		setSnapToTicks(true);
		setValue(Objects.requireNonNull(wrapper.get()));
		// On release, set the font field value and update the UI
		valueProperty().addListener(((observable, oldValue, newValue) -> {
			double oldValue2 = Math.round(oldValue.doubleValue());
			double newValue2 = Math.round(newValue.doubleValue());
			setValue(newValue2);
			if (newValue2 != oldValue2) {
				wrapper.set(newValue2);
				update(controller);
			}
		}));
	}

	/**
	 * Update's the font-size override sheet and reapplies styles to open windows.
	 *
	 * @param controller
	 * 		Controller to update.
	 */
	private static void update(GuiController controller) {
		try {
			double uiFontSize = controller.config().display().uiFontSize;
			double monoFontSize = controller.config().display().monoFontSize;
			String css = 	".root { -fx-font-size: " + uiFontSize + "px; }\n" +
							".lineno { -fx-font-size: " + uiFontSize + "px; }\n" +
							".h1 { -fx-font-size: " + (uiFontSize + 5) + "px; }\n" +
							".h2 { -fx-font-size: " + (uiFontSize + 3) + "px; }\n" +
							".monospaced { -fx-font-size: " + monoFontSize + "px; }\n";
			FileUtils.write(FONT_SIZE_CSS, css, StandardCharsets.UTF_8);
			controller.windows().reapplyStyles();
		} catch (IOException ex) {
			ExceptionAlert.show(ex, "Failed to set font size");
		}
	}

	/**
	 * Adds the font-size override to the given scene.
	 *
	 * @param scene
	 * 		Scene to add stylesheet to.
	 */
	public static void addFontSizeStyleSheet(Scene scene) {
		if (FONT_SIZE_CSS.exists())
			scene.getStylesheets().add("file:///" + FONT_SIZE_CSS.getAbsolutePath().replace("\\", "/"));
	}
}
