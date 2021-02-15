package me.coley.recaf.ui.panel;

import javafx.scene.control.Button;
import javafx.scene.effect.Blend;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.Glow;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import me.coley.recaf.ui.control.IconView;
import me.coley.recaf.ui.control.tree.WorkspaceTree;
import me.coley.recaf.util.Threads;

/**
 * Wrapper panel for buttons to change how the workspace is displayed.
 *
 * @author Matt Coley
 */
public class WorkspaceButtonsPanel extends BorderPane {
	/**
	 * @param tree
	 * 		Associated workspace tree.
	 */
	public WorkspaceButtonsPanel(WorkspaceTree tree) {
		setCenter(new HBox(
				createHideLibraries(tree),
				createCaseSensitive(tree)
		));
	}

	private Button createHideLibraries(WorkspaceTree tree) {
		Button button = new Button();
		button.setGraphic(new IconView("icons/eye.png"));
		button.setOnAction(e -> tree.toggleHideLibraries());
		tree.hideLibrarySubElementsProperty().addListener((ob, old, current) -> {
			if (old) {
				button.setGraphic(new IconView("icons/eye.png"));
			} else {
				button.setGraphic(new IconView("icons/eye-disabled.png"));
			}
		});
		return button;
	}

	private Button createCaseSensitive(WorkspaceTree tree) {
		Button button = new Button();
		button.setGraphic(new IconView("icons/case-sensitive.png"));
		button.setOnAction(e -> tree.toggleCaseSensitivity());
		tree.caseSensitiveProperty().addListener((ob, old, current) -> {
			if (old) {
				button.setOpacity(1.0);
			} else {
				button.setOpacity(0.4);
			}
		});
		return button;
	}
}
