package software.coley.recaf.ui.control;

import atlantafx.base.theme.Styles;
import atlantafx.base.theme.Tweaks;
import jakarta.annotation.Nonnull;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;
import software.coley.recaf.path.IncompletePathException;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.services.cell.CellConfigurationService;
import software.coley.recaf.services.cell.context.ContextSource;
import software.coley.recaf.services.navigation.Actions;
import software.coley.recaf.ui.control.tree.TreeItems;
import software.coley.recaf.ui.control.tree.WorkspaceTreeCell;

/**
 * A {@link TreeView} handling {@link PathNode} content. Pre-configures the following:
 * <ul>
 *     <li>{@link TreeView#cellFactoryProperty()} - Text / graphic / context menu</li>
 *     <li>{@link TreeView#onKeyPressedProperty()} - Recursive expansion/closure, open selected paths with enter</li>
 *     <li>{@link TreeView#getStyleClass()} - Some additional css styles</li>
 * </ul>
 *
 * @author Matt Coley
 */
public class PathNodeTree extends TreeView<PathNode<?>> {
	/**
	 * @param configurationService
	 * 		Cell service to configure tree cell rendering and population.
	 * @param actions
	 * 		Actions service to handle opening {@link PathNode} items.
	 */
	public PathNodeTree(@Nonnull CellConfigurationService configurationService, @Nonnull Actions actions) {
		setShowRoot(false);
		setCellFactory(param -> new WorkspaceTreeCell(ContextSource.DECLARATION, configurationService));
		getStyleClass().addAll(Tweaks.EDGE_TO_EDGE, Styles.DENSE);
		setOnKeyPressed(e -> {
			KeyCode code = e.getCode();
			if (code == KeyCode.RIGHT || code == KeyCode.KP_RIGHT) {
				TreeItem<PathNode<?>> selected = getSelectionModel().getSelectedItem();
				if (selected != null)
					TreeItems.recurseOpen(selected);
			} else if (code == KeyCode.LEFT || code == KeyCode.KP_LEFT) {
				TreeItem<PathNode<?>> selected = getSelectionModel().getSelectedItem();
				if (selected != null)
					TreeItems.recurseClose(this, selected);
			} else if (code == KeyCode.ENTER) {
				TreeItem<PathNode<?>> selected = getSelectionModel().getSelectedItem();
				if (selected != null) {
					try {
						actions.gotoDeclaration(selected.getValue());
					} catch (IncompletePathException ignored) {
						// ignored
					}
				}
			}
		});
	}
}
