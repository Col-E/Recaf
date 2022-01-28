package me.coley.recaf.ui.dialog;

import javafx.beans.binding.StringBinding;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.GridPane;
import me.coley.recaf.ui.util.Lang;

/**
 * Basic confirmation dialog.
 *
 * @author Matt Coley
 */
public class ConfirmDialog extends DialogBase<Boolean> {
	protected final ButtonType confirmType = new ButtonType(Lang.get("dialog.confirm"), ButtonBar.ButtonData.OK_DONE);
	protected final GridPane grid = new GridPane();

	/**
	 * @param graphic
	 * 		Header graphic.
	 */
	protected ConfirmDialog(Node graphic) {
		init();
		setGraphic(graphic);
		// Set the button types.
		getDialogPane().getButtonTypes().addAll(confirmType, ButtonType.CANCEL);
		// Content
		grid.setHgap(10);
		grid.setVgap(10);
		grid.setPadding(new Insets(15, 15, 15, 15));
		getDialogPane().setContent(grid);
		// Result
		setResultConverter(dialogButton -> dialogButton == confirmType);
	}

	/**
	 * @param title
	 * 		Dialog window title.
	 * @param header
	 * 		Header text.
	 * @param graphic
	 * 		Header graphic.
	 */
	public ConfirmDialog(String title, String header, Node graphic) {
		this(graphic);
		setTitle(title);
		setHeaderText(header);
	}

	/**
	 * @param title
	 * 		Dialog window title.
	 * @param header
	 * 		Header text.
	 * @param graphic
	 * 		Header graphic.
	 */
	public ConfirmDialog(StringBinding title, StringBinding header, Node graphic) {
		this(graphic);
		titleProperty().bind(title);
		headerTextProperty().bind(header);
	}
}
