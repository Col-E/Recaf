package software.coley.recaf.ui.control.tree;

import jakarta.annotation.Nonnull;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.DirectoryPathNode;
import software.coley.recaf.path.FilePathNode;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.Lang;

/**
 * Pane component to filter what is visible in a given {@link WorkspaceTree}.
 *
 * @author Matt Coley
 */
public class WorkspaceTreeFilterPane extends BorderPane {
	private final TextField textField = new TextField();

	/**
	 * @param tree
	 * 		Tree to filter.
	 */
	public WorkspaceTreeFilterPane(@Nonnull WorkspaceTree tree) {
		textField.promptTextProperty().bind(Lang.getBinding("workspace.filter-prompt"));
		setCenter(textField);
		getStyleClass().add("workspace-filter-pane");
		textField.getStyleClass().add("workspace-filter-text");

		// TODO:
		//  - option to hide supporting resources
		//  - case sensitivity toggle

		// Setup tree item predicate property on FX thread.
		// The root is assigned on the FX thread, it won't be available if we call it immediately.
		FxThreadUtil.run(() -> {
			// We're not binding from the root's property since that will trigger immediately.
			// That will force-expand the entire workspace, which we do not want to do.
			textField.textProperty().addListener((ob, old, cur) -> {
				WorkspaceTreeNode root = (WorkspaceTreeNode) tree.getRoot();
				root.predicateProperty().set(item -> {
					String path;
					PathNode<?> node = item.getValue();
					if (node instanceof DirectoryPathNode directoryNode) {
						path = directoryNode.getValue();
					} else if (node instanceof ClassPathNode classPathNode) {
						path = classPathNode.getValue().getName();
					} else if (node instanceof FilePathNode classPathNode) {
						path = classPathNode.getValue().getName();
					} else {
						path = null;
					}
					return path == null || path.contains(cur);
				});
			});
		});
	}

	/**
	 * @return Text field for filtering by path names.
	 */
	@Nonnull
	public TextField getTextField() {
		return textField;
	}
}
