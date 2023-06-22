package software.coley.recaf.ui.window;

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
	public AbstractIdentifiableStage(String id) {
		this.id = id;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public Stage asStage() {
		return this;
	}
}
