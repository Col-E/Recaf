package software.coley.recaf.ui.control;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.beans.binding.StringBinding;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import org.kordamp.ikonli.Ikon;

/**
 * Utility tab to quickly apply a {@link StringBinding}, commonly for UI translations.
 *
 * @author Matt Coley
 */
public class BoundTab extends Tab {
	/**
	 * @param binding
	 * 		Text binding for tab title.
	 * @param icon
	 * 		Tab graphic icon.
	 * @param content
	 * 		Optional tab content.
	 */
	public BoundTab(@Nonnull ObservableValue<String> binding, @Nonnull Ikon icon, @Nullable Node content) {
		this(binding, new FontIconView(icon), content);
	}

	/**
	 * @param binding
	 * 		Text binding for tab title.
	 * @param graphic
	 * 		Optional tab graphic.
	 * @param content
	 * 		Optional tab content.
	 */
	public BoundTab(@Nonnull ObservableValue<String> binding, @Nullable Node graphic, @Nullable Node content) {
		textProperty().bind(binding);
		setGraphic(graphic);
		setContent(content);
	}
}
