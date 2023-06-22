package software.coley.recaf.ui.window;

import javafx.stage.Stage;
import software.coley.recaf.util.Icons;

/**
 * Base stage, adding common Recaf adjustments.
 *
 * @author Matt Coley
 */
public class RecafStage extends Stage {
	{
		getIcons().add(Icons.getImage(Icons.LOGO));
	}
}
