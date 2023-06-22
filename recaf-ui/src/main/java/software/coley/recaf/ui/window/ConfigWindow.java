package software.coley.recaf.ui.window;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.scene.layout.BorderPane;
import software.coley.recaf.services.window.WindowManager;
import software.coley.recaf.ui.pane.ConfigPane;
import software.coley.recaf.util.Lang;

/**
 * Window wrapper for {@link ConfigPane}.
 *
 * @author Matt Coley
 * @see ConfigPane
 */
@Dependent
public class ConfigWindow extends AbstractIdentifiableStage {
	@Inject
	public ConfigWindow(ConfigPane configPane) {
		super(WindowManager.WIN_CONFIG);

		// Layout
		titleProperty().bind(Lang.getBinding("menu.config"));
		setMinWidth(750);
		setMinHeight(450);
		setScene(new RecafScene(new BorderPane(configPane), 750, 450));
	}
}
