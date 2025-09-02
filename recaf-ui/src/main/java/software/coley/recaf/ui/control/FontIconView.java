package software.coley.recaf.ui.control;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.IkonHandler;
import org.kordamp.ikonli.IkonResolver;
import org.kordamp.ikonli.IkonResolverProvider;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.javafx.JavaFXFontLoader;

import java.util.HashMap;
import java.util.Map;

/**
 * Minimal reimplementation of {@link FontIcon} because its manual CSS writing instead of working with properties
 * prevents it from being used with AtlantaFX.
 *
 * @author Matt Coley
 */
public class FontIconView extends Text {
	private static final IkonResolver resolver = IkonResolverProvider.getInstance(JavaFXFontLoader.getInstance());
	private static final Map<Integer, IkonHandler> codeToHandler = new HashMap<>();

	/**
	 * @param icon
	 * 		Icon to use.
	 */
	public FontIconView(@Nonnull Ikon icon) {
		this(icon, IconView.DEFAULT_ICON_SIZE, null);
	}

	/**
	 * @param icon
	 * 		Icon to use.
	 * @param color
	 * 		Color to use.
	 */
	public FontIconView(@Nonnull Ikon icon, @Nonnull Color color) {
		this(icon, IconView.DEFAULT_ICON_SIZE, color);
	}

	/**
	 * @param icon
	 * 		Icon to use.
	 * @param size
	 * 		Size of icon in pixels.
	 */
	public FontIconView(@Nonnull Ikon icon, int size) {
		this(icon, size, null);
	}

	/**
	 * @param icon
	 * 		Icon to use.
	 * @param size
	 * 		Size of icon in pixels.
	 * @param color
	 * 		Optional color to use.
	 */
	public FontIconView(@Nonnull Ikon icon, int size, @Nullable Color color) {
		int code = icon.getCode();
		IkonHandler ikonHandler = codeToHandler.computeIfAbsent(code, k -> resolver.resolve(icon.getDescription()));

		// Fetch expected font instance, configure size/color
		Font font = (Font) ikonHandler.getFont();
		Font sizedFont = new Font(font.getFamily(), size);
		setFont(sizedFont);
		if (color != null) setFill(color);
		else setStyle("-fx-fill: -color-fg-default;");

		// Set text to ikonli character
		if (code <= '\uFFFF') {
			setText(String.valueOf((char) code));
		} else {
			char[] charPair = Character.toChars(code);
			String symbol = new String(charPair);
			setText(symbol);
		}
	}
}
