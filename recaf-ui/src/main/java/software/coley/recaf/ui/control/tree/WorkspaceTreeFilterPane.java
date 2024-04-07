package software.coley.recaf.ui.control.tree;

import atlantafx.base.controls.CustomTextField;
import atlantafx.base.theme.Styles;
import jakarta.annotation.Nonnull;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.DirectoryPathNode;
import software.coley.recaf.path.FilePathNode;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.ui.control.BoundToggleIcon;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.util.Lang;

/**
 * Pane component to filter what is visible in a given {@link WorkspaceTree}.
 *
 * @author Matt Coley
 */
public class WorkspaceTreeFilterPane extends BorderPane {
	private final SimpleBooleanProperty caseSensitivity = new SimpleBooleanProperty(false);
	private final CustomTextField textField = new CustomTextField();

	/**
	 * @param tree
	 * 		Tree to filter.
	 */
	public WorkspaceTreeFilterPane(@Nonnull WorkspaceTree tree) {
		BoundToggleIcon toggleSensitivity = new BoundToggleIcon(new FontIconView(CarbonIcons.LETTER_CC), caseSensitivity).withTooltip("misc.casesensitive");
		toggleSensitivity.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.ACCENT, Styles.FLAT, Styles.SMALL);
		textField.rightProperty().set(toggleSensitivity);

		textField.promptTextProperty().bind(Lang.getBinding("workspace.filter-prompt"));
		setCenter(textField);
		getStyleClass().add("workspace-filter-pane");
		textField.getStyleClass().add("workspace-filter-text");


		textField.textProperty().addListener((ob, old, cur) -> update(tree));
		caseSensitivity.addListener((ob, old, cur) -> update(tree));
	}

	private void update(@Nonnull WorkspaceTree tree) {
		WorkspaceTreeNode root = (WorkspaceTreeNode) tree.getRoot();
		if (root == null) return;

		if (textField.getText().isEmpty())
			root.predicateProperty().set(null);
		else
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

				if (path == null) return true;

				return caseSensitivity.get() ?
						path.contains(textField.getText()) :
						path.toLowerCase().contains(textField.getText().toLowerCase());
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
