package software.coley.recaf.ui.window;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import software.coley.recaf.ui.pane.mapping.SimilarityMappingPane;
import software.coley.recaf.util.Lang;

/**
 * Window wrapper for {@link SimilarityMappingPane}.
 *
 * @author Matt Coley
 */
@Dependent
public class SimilarityMappingWindow extends RecafStage {
	private final SimilarityMappingPane pane;

	@Inject
	public SimilarityMappingWindow(@Nonnull SimilarityMappingPane pane) {
		this.pane = pane;
		pane.setApplyCallback(this::close);

		hideOnEscape();
		titleProperty().bind(Lang.getBinding("mapsim"));
		setMinWidth(900);
		setMinHeight(550);
		setScene(new RecafScene(pane, 1200, 700));
	}

	@Nonnull
	public SimilarityMappingPane getPane() {
		return pane;
	}
}
