package software.coley.recaf.ui.window;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.scene.layout.BorderPane;
import software.coley.recaf.services.window.WindowManager;
import software.coley.recaf.ui.pane.PluginManagerPane;
import software.coley.recaf.util.Lang;

/**
 * Window wrapper for {@link PluginManagerPane}.
 *
 * @author Canrad
 * @see PluginManagerPane
 */
@Dependent
public class PluginManagerWindow extends AbstractIdentifiableStage {
	@Inject
	public PluginManagerWindow(PluginManagerPane pluginManagerPane) {
		super(WindowManager.WIN_PLUGINS);

		// Layout
		titleProperty().bind(Lang.getBinding("menu.plugin.manage"));
		setMinWidth(750);
		setMinHeight(450);
		setScene(new RecafScene(new BorderPane(pluginManagerPane), 750, 450));
	}
}
