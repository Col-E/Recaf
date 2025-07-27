package software.coley.recaf.ui.window;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import software.coley.recaf.ui.pane.MappingApplicationPane;
import software.coley.recaf.util.Lang;

/**
 * Window wrapper for {@link MappingApplicationPane}.
 *
 * @author Matt Coley
 * @see MappingApplicationPane
 */
@Dependent
public class MappingApplicationWindow extends RecafStage {
	private final MappingApplicationPane generatorPane;

	@Inject
	public MappingApplicationWindow(@Nonnull MappingApplicationPane generatorPane) {
		this.generatorPane = generatorPane;
		generatorPane.setApplyCallback(this::close);

		// Add event filter to handle closing the window when escape is pressed.
		addEventFilter(KeyEvent.KEY_PRESSED, e -> {
			if (e.getCode() == KeyCode.ESCAPE)
				hide();
		});

		// Layout
		titleProperty().bind(Lang.getBinding("mapapply"));
		setMinWidth(450);
		setMinHeight(300);
		setScene(new RecafScene(generatorPane, 800, 600));
	}

	@Nonnull
	public MappingApplicationPane getApplicationPane() {
		return generatorPane;
	}
}
