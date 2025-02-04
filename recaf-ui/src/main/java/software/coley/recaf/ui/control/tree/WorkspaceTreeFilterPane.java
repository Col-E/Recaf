package software.coley.recaf.ui.control.tree;

import atlantafx.base.controls.CustomTextField;
import atlantafx.base.theme.Styles;
import jakarta.annotation.Nonnull;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.BorderPane;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.DirectoryPathNode;
import software.coley.recaf.path.FilePathNode;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.ui.control.BoundToggleIcon;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.util.Lang;

import java.util.function.Predicate;

/**
 * Pane component to filter what is visible in a given {@link WorkspaceTree}.
 *
 * @author Matt Coley
 */
public class WorkspaceTreeFilterPane extends BorderPane {
	private final ObjectProperty<Predicate<TreeItem<PathNode<?>>>> currentPredicate = new SimpleObjectProperty<>();
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

	/**
	 * @return Current predicate assigned to the workspace tree as a filter.
	 */
	@Nonnull
	public ObjectProperty<Predicate<TreeItem<PathNode<?>>>> currentPredicateProperty() {
		return currentPredicate;
	}

	/**
	 * @return Text field for filtering by path names.
	 */
	@Nonnull
	public TextField getTextField() {
		return textField;
	}

	private void update(@Nonnull WorkspaceTree tree) {
		WorkspaceTreeNode root = (WorkspaceTreeNode) tree.getRoot();
		if (root == null) return;

		ObjectProperty<Predicate<TreeItem<PathNode<?>>>> rootPredicate = root.predicateProperty();
		if (textField.getText().isEmpty()) {
			rootPredicate.set(null);
			currentPredicate.set(null);
		} else {
			Predicate<TreeItem<PathNode<?>>> matcher = this::match;
			rootPredicate.set(matcher);
			currentPredicate.set(matcher);
		}
	}

	private boolean match(@Nonnull TreeItem<PathNode<?>> item) {
		PathNode<?> node = item.getValue();
		String path = switch (node) {
			case DirectoryPathNode directoryNode -> directoryNode.getValue();
			case ClassPathNode classPathNode -> classPathNode.getValue().getName();
			case FilePathNode classPathNode -> classPathNode.getValue().getName();
			case null, default -> null;
		};

		// Some PathNode types do not correlate to things that can be represented as file paths.
		// For instance, the actual file bundle containing files, can't represent that because it is
		// effectively the root.
		//
		// When we find a PathNode that does not have a "file path" like string, we will only show it
		// if it has children. This will hide trees that don't have any actual results in them.
		if (path == null) return !item.getChildren().isEmpty();

		// Otherwise, for things like classes and files, we'll match their path in their respective bundles
		// to the text-field input.
		return caseSensitivity.get() ?
				path.contains(textField.getText()) :
				path.toLowerCase().contains(textField.getText().toLowerCase());
	}
}
