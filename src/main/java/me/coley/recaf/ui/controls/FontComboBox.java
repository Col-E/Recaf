package me.coley.recaf.ui.controls;

import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.text.Font;
import javafx.util.StringConverter;
import me.coley.recaf.Recaf;
import me.coley.recaf.config.FieldWrapper;
import me.coley.recaf.control.gui.GuiController;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * ComboBox for font types.
 *
 * @author Matt
 */
public class FontComboBox extends ComboBox<Font> {
	private static final List<Font> FONT_LIST = new ArrayList<>();
	private static final File FONT_MONO_CSS = Recaf.getDirectory("style").resolve("font-mono.css").toFile();


	/**
	 * @param values
	 * 		Font types.
	 * @param initial
	 * 		Initial selection.
	 */
	public FontComboBox(Collection<Font> values, Font initial) {
		super(FXCollections.observableArrayList(values));
		setCellFactory(e -> new FontCell());
		setConverter(new StringConverter<Font>() {
			@Override
			public String toString(Font object) {
				return object.getName();
			}

			@Override
			public Font fromString(String string) {
				return Font.font(string, 12);
			}
		});
		setValue(initial);
	}

	/**
	 * @param controller
	 * 		Controller to update.
	 * @param wrapper
	 * 		Wrapper of an string field representing the font family name.
	 * @param initial
	 * 		Initial selection.
	 */
	public FontComboBox(GuiController controller, FieldWrapper wrapper, String initial) {
		this(FONT_LIST, new Font(initial, 12));
		getSelectionModel().selectedItemProperty().addListener((ob, o, n) -> {
			wrapper.set(n.getFamily());
			update(controller);
		});
	}

	/**
	 * Update's the font-size override sheet and reapplies styles to open windows.
	 *
	 * @param controller
	 * 		Controller to update.
	 */
	private static void update(GuiController controller) {
		try {
			String family = controller.config().display().monoFont;
			String css = ".monospaced { -fx-font-family: \"" + family + "\" !important; }";
			FileUtils.write(FONT_MONO_CSS, css, StandardCharsets.UTF_8);
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
	public static void addMonoFontStyleSheet(Scene scene) {
		if (FONT_MONO_CSS.exists())
			scene.getStylesheets().add("file:///" + FONT_MONO_CSS.getAbsolutePath().replace("\\", "/"));
	}

	static class FontCell extends ListCell<Font> {
		@Override
		protected void updateItem(Font item, boolean empty) {
			super.updateItem(item, empty);
			if (empty) {
				setText(null);
				setGraphic(null);
			} else {
				setStyle("-fx-font-family: \"" + item.getFamily() + "\" !important;");
				setText(item.getName());
			}
		}
	}

	static {
		SortedSet<Font> fonts = new TreeSet<>(Comparator.comparing(Font::getFamily));
		for (String family : Font.getFamilies()) {
			Font font = Font.font(family, 12);
			fonts.add(font);
		}
		FONT_LIST.addAll(fonts);
	}
}