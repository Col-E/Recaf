package software.coley.recaf.ui.window;

import jakarta.annotation.Nonnull;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Base implementation of {@link IdentifiableStage}.
 *
 * @author Matt Coley
 */
public class AbstractIdentifiableStage extends RecafStage implements IdentifiableStage {
	private final String id;

	/**
	 * Decorated stage.
	 *
	 * @param id
	 * 		Unique stage identifier.
	 */
	public AbstractIdentifiableStage(@Nonnull String id) {
		this(StageStyle.DECORATED, id);
	}

	/**
	 * Stage of the given style.
	 *
	 * @param id
	 * 		Unique stage identifier.
	 * @param style
	 * 		Specific stage style.
	 */
	public AbstractIdentifiableStage(@Nonnull StageStyle style, @Nonnull String id) {
		super(style);
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
