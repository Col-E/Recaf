package software.coley.recaf.ui.window;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import software.coley.recaf.services.window.WindowManager;
import software.coley.recaf.ui.pane.MappingProgressPane;
import software.coley.recaf.util.Lang;

/**
 * Window wrapper for {@link MappingProgressPane}.
 *
 * @author Matt Coley
 * @see MappingProgressPane
 */
@Dependent
public class MappingProgressWindow extends AbstractIdentifiableStage {
	@Inject
	public MappingProgressWindow(@Nonnull MappingProgressPane previewPane) {
		super(WindowManager.WIN_MAP_PROGRESS);

		// Bind mapping preview updates to the window visibility
		previewPane.activeProperty().bind(showingProperty());

		// Layout
		titleProperty().bind(Lang.getBinding("mapprog"));
		setMinWidth(750);
		setMinHeight(450);
		setScene(new RecafScene(previewPane, 750, 450));
	}
}
