package software.coley.recaf.ui.control;

import jakarta.annotation.Nonnull;
import javafx.scene.Node;
import org.kordamp.ikonli.Ikon;

/**
 * Action button but with only a graphic.
 *
 * @author Matt Coley
 */
public class GraphicActionButton extends ActionButton {
	/**
	 * @param graphic
	 * 		Button graphic.
	 * @param action
	 * 		Action to run on-click.
	 */
	public GraphicActionButton(@Nonnull Node graphic, @Nonnull Runnable action) {
		super((String) null, action);
		setGraphic(graphic);
		getStyleClass().add("graphic-button");
	}

	/**
	 * @param icon
	 * 		Button display icon.
	 * @param action
	 * 		Action to run on-click.
	 */
	public GraphicActionButton(@Nonnull Ikon icon, @Nonnull Runnable action) {
		super(icon, action);
		getStyleClass().add("graphic-button");
	}
}
