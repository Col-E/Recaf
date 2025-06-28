package software.coley.recaf.ui.window;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import software.coley.recaf.services.window.WindowManager;
import software.coley.recaf.ui.pane.MappingGeneratorPane;
import software.coley.recaf.ui.pane.SystemInformationPane;
import software.coley.recaf.util.Lang;

/**
 * Window wrapper for {@link MappingGeneratorPane}.
 *
 * @author Matt Coley
 * @see MappingGeneratorPane
 */
@Dependent
public class MappingGeneratorWindow extends RecafStage {
	private final MappingGeneratorPane generatorPane;

	@Inject
	public MappingGeneratorWindow(@Nonnull MappingGeneratorPane generatorPane) {
		// TODO: When the generator pane "apply" is pressed we should close this window
		this.generatorPane = generatorPane;

		// Layout
		titleProperty().bind(Lang.getBinding("mapgen"));
		setMinWidth(450);
		setMinHeight(300);
		setScene(new RecafScene(generatorPane, 800, 600));
	}

	@Nonnull
	public MappingGeneratorPane getGeneratorPane() {
		return generatorPane;
	}
}
