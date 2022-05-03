package me.coley.recaf.ui.pane;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
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
public class WorkspaceButtonsPane extends BorderPane {
	private final Button btnHide;
	private final Button btnCase;

	/**
	 * @param tree
	 * 		Associated workspace tree.
	 */
	public WorkspaceButtonsPane(WorkspaceTreeWrapper tree) {
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
		Tooltip tooltip = new Tooltip();
		tooltip.textProperty().bind(Lang.getBinding("tree.hidelibs"));
		button.setTooltip(tooltip);
		button.setGraphic(getIconView(Icons.EYE));
		button.setOnAction(e -> tree.toggleHideLibraries());
		SimpleBooleanProperty hideProperty = tree.hideLibrarySubElementsProperty();
		button.graphicProperty().bind(Bindings.createObjectBinding(
				() -> getIconView(hideProperty.get() ? Icons.EYE_DISABLED : Icons.EYE),
				hideProperty
		));
		return button;
	}

	private Button createCaseSensitive(WorkspaceTreeWrapper tree) {
		Button button = new Button();
		Tooltip tooltip = new Tooltip();
		tooltip.textProperty().bind(Lang.getBinding("tree.casesensitive"));
		button.setTooltip(tooltip);
		button.setGraphic(getIconView(Icons.CASE_SENSITIVITY));
		button.setOnAction(e -> tree.toggleCaseSensitivity());
		SimpleBooleanProperty caseProperty = tree.caseSensitiveProperty();
		button.opacityProperty().bind(Bindings.createDoubleBinding(
				() -> caseProperty.get() ? 0.4D : 1.0D,
				caseProperty
		));
		return button;
	}
}
