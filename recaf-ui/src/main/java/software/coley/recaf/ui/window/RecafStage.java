package software.coley.recaf.ui.window;

import jakarta.annotation.Nonnull;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import software.coley.recaf.util.Icons;

/**
 * Base stage, adding common Recaf adjustments.
 *
 * @author Matt Coley
 */
public class RecafStage extends Stage {
	/**
	 * Decorated stage.
	 */
	public RecafStage() {
		this(StageStyle.DECORATED);
	}

	/**
	 * Stage of the given style.
	 *
	 * @param style
	 * 		Specific stage style.
	 */
	public RecafStage(@Nonnull StageStyle style) {
		super(style);
		getIcons().add(Icons.getImage(Icons.LOGO));
	}

	/**
	 * Hides the stage when escape is pressed and the event is not consumed by the focused control.
	 *
	 * @return Self.
	 */
	@Nonnull
	public RecafStage hideOnEscape() {
		// We want to hide the stage when escape is pressed.
		//
		// An event handler is used instead of an event filter to allow
		// controls to consume the event and prevent the stage from hiding.
		// For instance, input text fields that can use 'ESCAPE' to clear their value.
		addEventHandler(KeyEvent.KEY_PRESSED, e -> {
			if (e.getCode() == KeyCode.ESCAPE) {
				hide();
				e.consume();
			}
		});
		return this;
	}
}
