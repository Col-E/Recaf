package software.coley.recaf.ui.control.tree;

import jakarta.annotation.Nonnull;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;
import software.coley.recaf.ui.pane.WorkspaceExplorerPane;
import software.coley.recaf.ui.pane.editing.tabs.FieldsAndMethodsPane;
import software.coley.recaf.util.NodeEvents;

/**
 * Common tree filtering setup.
 *
 * @author Matt Coley
 * @see WorkspaceExplorerPane Used for workspace filter.
 * @see FieldsAndMethodsPane Used for member filter.
 */
public class TreeFiltering {
	/**
	 * @param filter
	 * 		Text input with filter text.
	 * @param tree
	 * 		Tree to filter based on input.
	 * 		Assumed that tree contents are {@link FilterableTreeItem}.
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static void install(@Nonnull TextField filter, @Nonnull TreeView<?> tree) {
		NodeEvents.addKeyPressHandler(filter, e -> {
			if (e.getCode() == KeyCode.ESCAPE) {
				filter.clear();
			} else if (e.getCode().isArrowKey()) {
				tree.requestFocus();

				// Select first expanded leaf when arrow keys used.
				// This should jump the navigation close to where the use intends
				// to be instead of restarting selection/focus at the top of the tree.
				TreeItem<?> item = tree.getRoot();
				while (item.isExpanded() && item.getChildren().size() > 0) {
					boolean matched = false;
					for (TreeItem<?> child : item.getChildren()) {
						if (child.isExpanded() || child.isLeaf()) {
							matched = true;
							item = child;
							break;
						}
					}
					if (!matched)
						break;
				}
				tree.getSelectionModel().select((TreeItem) item);
			}
		});
		NodeEvents.addKeyPressHandler(tree, e -> {
			String text = e.getText();
			if (e.getCode() == KeyCode.ESCAPE) {
				filter.clear();
			} else if (e.getCode() == KeyCode.ENTER) {
				// no-op, allow tree may have key-bind action for 'enter'
			} else if (!(e.isControlDown() || e.isAltDown()) && text != null && !text.isEmpty()) {
				// If no mask key is held down, request focus for the filter.
				filter.requestFocus();
			}
		});
	}
}
