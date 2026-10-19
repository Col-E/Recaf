package software.coley.recaf.ui.window;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.scene.layout.BorderPane;
import software.coley.recaf.config.ConfigContainer;
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
	private final ConfigPane configPane;

	@Inject
	public ConfigWindow(@Nonnull ConfigPane configPane) {
		super(WindowManager.WIN_CONFIG);
		this.configPane = configPane;
		hideOnEscape();

		// Layout
		titleProperty().bind(Lang.getBinding("menu.config"));
		setMinWidth(750);
		setMinHeight(450);
		setScene(new RecafScene(new BorderPane(configPane), 750, 450));
	}

	/**
	 * Selects a config page.
	 *
	 * @param container
	 * 		Config container whose page should be selected.
	 *
	 * @return {@code true} when the container has a visible page and it was selected.
	 */
	public boolean selectPage(@Nonnull ConfigContainer container) {
		return configPane.selectPage(container);
	}
}
