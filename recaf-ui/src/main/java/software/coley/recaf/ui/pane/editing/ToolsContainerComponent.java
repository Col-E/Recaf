package software.coley.recaf.ui.pane.editing;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.ui.control.richtext.EditorComponent;
import software.coley.recaf.ui.control.richtext.ScrollbarPaddingUtil;

/**
 * Editor component that sits in the bottom right of an {@link Editor} and houses other tool-like buttons.
 *
 * @author Matt Coley
 * @see AbstractDecompilerPaneConfigurator An example of a component that adds itself to this container.
 */
@Dependent
public class ToolsContainerComponent implements EditorComponent {
	private final HBox container = new HBox();
	private final Group containerWrapper = new Group(container);
	private final ChangeListener<Boolean> handleScrollbarVisibility =
			(ob, old, cur) -> ScrollbarPaddingUtil.handleScrollbarVisibility(containerWrapper, cur);

	@Inject
	public ToolsContainerComponent() {
		StackPane.setAlignment(containerWrapper, Pos.BOTTOM_RIGHT);
		StackPane.setMargin(containerWrapper, new Insets(7));
	}

	/**
	 * @param node
	 * 		Node to add to this container.
	 */
	public void add(@Nonnull Node node) {
		// Add at index 0 so new items are added to the left side.
		container.getChildren().add(0, new Group(node));
	}

	/**
	 * @param node
	 * 		Node to add to this container.
	 */
	public void addLast(@Nonnull Node node) {
		// Add at end so new items are added to the right side.
		container.getChildren().add(new Group(node));
	}

	@Override
	public void install(@Nonnull Editor editor) {
		editor.getPrimaryStack().getChildren().add(containerWrapper);
		editor.getVerticalScrollbar().visibleProperty().addListener(handleScrollbarVisibility);
	}

	@Override
	public void uninstall(@Nonnull Editor editor) {
		editor.getPrimaryStack().getChildren().remove(containerWrapper);
		editor.getVerticalScrollbar().visibleProperty().removeListener(handleScrollbarVisibility);
	}
}
