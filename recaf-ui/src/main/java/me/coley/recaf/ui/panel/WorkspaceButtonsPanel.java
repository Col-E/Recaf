package me.coley.recaf.ui.panel;

import javafx.scene.control.Button;
import javafx.scene.effect.Blend;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.Glow;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import me.coley.recaf.ui.control.IconView;
import me.coley.recaf.ui.control.tree.WorkspaceTree;

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
		Button hideLibraries = new Button();
		hideLibraries.setGraphic(new IconView("icons/eye.png"));
		hideLibraries.setOnAction(e -> tree.toggleHideLibraries());
		tree.hideLibrarySubElementsProperty().addListener((ob, old, current) -> {
			if (current) {
				hideLibraries.setEffect(null);
			} else {
				// TODO: Color map the "white" texture to blue
				hideLibraries.setEffect(new ColorAdjust(0.5, 1.0, 1.0, 1.0));
			}
		});
		setCenter(new HBox(hideLibraries));
	}
}
