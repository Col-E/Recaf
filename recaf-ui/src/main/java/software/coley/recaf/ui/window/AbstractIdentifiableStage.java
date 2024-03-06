package software.coley.recaf.ui.window;

import jakarta.annotation.Nonnull;
import javafx.stage.Stage;

/**
 * Base implementation of {@link IdentifiableStage}.
 *
 * @author Matt Coley
 */
public class AbstractIdentifiableStage extends RecafStage implements IdentifiableStage {
	private final String id;

	/**
	 * @param id
	 * 		Unique stage identifier.
	 */
	public AbstractIdentifiableStage(@Nonnull String id) {
		this.id = id;
	}

	@Nonnull
	@Override
	public String getId() {
		return id;
	}

	@Nonnull
	@Override
	public Stage asStage() {
		return this;
	}
}
