package software.coley.recaf.ui.pane;

import jakarta.annotation.Nonnull;
import javafx.beans.property.DoubleProperty;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.transform.Scale;

/**
 * Wraps a child node and scales it by {@code scale} while keeping it sized to fill this pane.
 */
public class ScalePane extends Pane {
	private final DoubleProperty scale;
	private final Node content;

	/**
	 * @param content
	 * 		Original scene root to wrap (must have no parent!).
	 * @param scale
	 * 		Factor to scale by.
	 */
	public ScalePane(@Nonnull Node content, @Nonnull DoubleProperty scale) {
		this.scale = scale;
		this.content = content;

		//Pivot so the child grows down/right to fill the window
		var transform = new Scale();
		transform.xProperty().bind(scale);
		transform.yProperty().bind(scale);
		content.getTransforms().add(transform);

		//Reparent the orphaned root into this pane
		getChildren().add(content);

		scale.addListener(o -> requestLayout());
	}

	/**
	 * @return The wrapped original root.
	 */
	@Nonnull
	public Node getContent() {
		return content;
	}

	@Override
	protected void layoutChildren() {
		var scale = this.scale.get();
		if (scale <= 0)
			scale = 1;

		content.resize(getWidth() / scale, getHeight() / scale);
		content.relocate(0, 0);
	}
}
