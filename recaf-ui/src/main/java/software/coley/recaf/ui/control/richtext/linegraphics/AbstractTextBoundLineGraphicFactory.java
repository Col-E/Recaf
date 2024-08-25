package software.coley.recaf.ui.control.richtext.linegraphics;

import jakarta.annotation.Nonnull;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.layout.StackPane;
import org.openrewrite.jgit.annotations.NonNull;
import software.coley.recaf.ui.control.VirtualizedScrollPaneWrapper;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.SceneUtils;

/**
 * Base implementation of a {@link LineGraphicFactory} which has
 * displayed graphics offset to appear on top of the editor's contents.
 *
 * @author Justus Garbe
 */
public abstract class AbstractTextBoundLineGraphicFactory extends AbstractLineGraphicFactory {
	protected final int containerHeight = 16; // Each line graphic region is only 16px tall
	protected final int containerWidth = 16;
	protected Editor editor;

	/**
	 * @param priority
	 * 		Priority dictating the order of graphics displayed in {@link RootLineGraphicFactory}.
	 * 		See {@link LineGraphicFactory} for constants.
	 */
	protected AbstractTextBoundLineGraphicFactory(int priority) {
		super(priority);
	}

	@Override
	public void install(@Nonnull Editor editor) {
		this.editor = editor;
	}

	@Override
	public void uninstall(@Nonnull Editor editor) {
		this.editor = null;
	}

	@Override
	public void apply(@Nonnull LineContainer container, int paragraph) {
		// To keep the ordering of the line graphic factory priority we need to add the stack pane now
		// since the rest of the work is done async below. We want this to have zero width so that it doesn't
		// shit the editor around when the content becomes active/inactive.
		StackPane stack = new StackPane();
		stack.setManaged(false);
		stack.setPrefWidth(0);
		stack.setMouseTransparent(true);
		container.addHorizontal(stack);

		// This delayed execution looks stupid because it is, however it is necessary.
		//
		// Any operation that requires knowledge of positioning and sizing cannot be
		// done on the same frame as the creation of the cell. Thus, we'll delay it by
		// one frame.
		FxThreadUtil.delayedRun(0, () -> {
			stack.setPrefSize(containerWidth, containerHeight);
			stack.setAlignment(Pos.CENTER_LEFT);
			SceneUtils.getParentOfTypeLater(container, VirtualizedScrollPaneWrapper.class).whenComplete((parentScroll, error) -> {
				ObservableValue<? extends Number> translateX;
				if (parentScroll != null) {
					translateX = Bindings.add(container.widthProperty().subtract(containerHeight), parentScroll.horizontalScrollProperty().negate());
				} else {
					// Should never happen since the 'VirtualizedScrollPaneWrapper' is mandated internally by 'Editor'.
					translateX = container.widthProperty().subtract(containerHeight);
				}
				stack.translateXProperty().bind(translateX);
			});

			apply(stack, paragraph);
		});
	}

	/**
	 * Apply the line graphic to the given pane,
	 * the pane is always bound to the beginning of the line.
	 *
	 * @param pane
	 * 		The pane to apply the graphic to.
	 * @param paragraph
	 * 		The paragraph index.
	 */
	protected abstract void apply(@NonNull StackPane pane, int paragraph);
}
