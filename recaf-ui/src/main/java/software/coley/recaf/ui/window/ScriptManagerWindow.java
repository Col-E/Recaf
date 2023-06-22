package software.coley.recaf.ui.window;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.scene.layout.BorderPane;
import software.coley.recaf.services.window.WindowManager;
import software.coley.recaf.ui.pane.ScriptManagerPane;
import software.coley.recaf.util.Lang;

/**
 * Window wrapper for {@link ScriptManagerPane}.
 *
 * @author Matt Coley
 * @see ScriptManagerPane
 */
@Dependent
public class ScriptManagerWindow extends AbstractIdentifiableStage {
	@Inject
	public ScriptManagerWindow(ScriptManagerPane scriptManagerPane) {
		super(WindowManager.WIN_SCRIPTS);

		// Layout
		titleProperty().bind(Lang.getBinding("menu.scripting.manage"));
		setMinWidth(750);
		setMinHeight(450);
		setScene(new RecafScene(new BorderPane(scriptManagerPane), 750, 450));
	}
}
