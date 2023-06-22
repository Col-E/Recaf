package software.coley.recaf.ui.control.richtext;

import jakarta.annotation.Nonnull;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import software.coley.recaf.ui.pane.editing.ProblemOverlay;
import software.coley.recaf.ui.pane.editing.AbstractDecompilerPaneConfigurator;

/**
 * Used by {@link Node} values to be shown on an editor's {@link Editor#getPrimaryStack()}.
 * If a node is shown on the right, the padding will need to be adjusted when scrollbars are visible.
 *
 * @author Matt Coley
 * @see ProblemOverlay
 * @see AbstractDecompilerPaneConfigurator
 */
public class ScrollbarPaddingUtil {
	/**
	 * When the {@link Editor#getVerticalScrollbar()} is visible, our {@link StackPane#setMargin(Node, Insets)} will cause
	 * us to overlap with it. This doesn't look great, so when it is visible we will shift a bit over to the left so that we
	 * do not overlap.
	 *
	 * @param node
	 * 		Node to update.
	 * @param currentlyVisible
	 * 		Current visibility state of the editor's vertical scrollbar.
	 */
	public static void handleScrollbarVisibility(@Nonnull Node node, boolean currentlyVisible) {
		StackPane.setMargin(node, new Insets(7, currentlyVisible ? 14 : 7, 7, 7));
	}
}
