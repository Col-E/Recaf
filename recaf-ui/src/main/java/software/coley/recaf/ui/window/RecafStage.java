package software.coley.recaf.ui.window;

import jakarta.annotation.Nonnull;
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
}
