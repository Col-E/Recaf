package software.coley.recaf.ui.window;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
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

		// Add event filter to handle closing the window when escape is pressed.
		addEventFilter(KeyEvent.KEY_PRESSED, e -> {
			if (e.getCode() == KeyCode.ESCAPE)
				hide();
		});

		// Layout
		titleProperty().bind(Lang.getBinding("mapprog"));
		setMinWidth(750);
		setMinHeight(450);
		setScene(new RecafScene(previewPane, 750, 450));
	}
}
