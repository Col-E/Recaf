package me.coley.recaf.ui.panel;

import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import me.coley.recaf.ui.control.tree.WorkspaceTreeWrapper;
import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.ui.util.Lang;

import static me.coley.recaf.ui.util.Icons.getIconView;

/**
 * Wrapper panel for buttons to change how the workspace is displayed.
 *
 * @author Matt Coley
 */
public class WorkspaceButtonsPanel extends BorderPane {
	private final Button btnHide;
	private final Button btnCase;

	/**
	 * @param tree
	 * 		Associated workspace tree.
	 */
	public WorkspaceButtonsPanel(WorkspaceTreeWrapper tree) {
		setCenter(new HBox(
				btnHide = createHideLibraries(tree),
				btnCase = createCaseSensitive(tree)
		));
	}

	/**
	 * @return Button that toggles {@link WorkspaceTreeWrapper#hideLibrarySubElementsProperty()}.
	 */
	public Button getHideLibrariesButton() {
		return btnHide;
	}

	/**
	 * @return Button that toggles {@link WorkspaceTreeWrapper#caseSensitiveProperty()}}.
	 */
	public Button getFilterCaseSensitivityButton() {
		return btnCase;
	}

	private Button createHideLibraries(WorkspaceTreeWrapper tree) {
		Button button = new Button();
		button.setTooltip(new Tooltip(Lang.get("tree.hidelibs")));
		button.setGraphic(getIconView(Icons.EYE));
		button.setOnAction(e -> tree.toggleHideLibraries());
		tree.hideLibrarySubElementsProperty().addListener((ob, old, current) -> {
			if (old) {
				button.setGraphic(getIconView(Icons.EYE));
			} else {
				button.setGraphic(getIconView(Icons.EYE_DISABLED));
			}
		});
		return button;
	}

	private Button createCaseSensitive(WorkspaceTreeWrapper tree) {
		Button button = new Button();
		button.setTooltip(new Tooltip(Lang.get("tree.casesensitive")));
		button.setGraphic(getIconView(Icons.CASE_SENSITIVITY));
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
