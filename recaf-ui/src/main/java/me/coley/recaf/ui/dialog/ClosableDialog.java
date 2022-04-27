package me.coley.recaf.ui.dialog;

import javafx.beans.binding.StringBinding;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.GridPane;
import me.coley.recaf.ui.util.Lang;

/**
 * Basic closable dialog.
 *
 * @author Matt Coley
 */
public class ClosableDialog extends DialogBase<Void> {
	protected final ButtonType closeType = new ButtonType(Lang.get("dialog.close"), ButtonBar.ButtonData.OK_DONE);
	protected final GridPane grid = new GridPane();

	/**
	 * @param graphic
	 * 		Header graphic.
	 */
	protected ClosableDialog(Node graphic) {
		init();
		setGraphic(graphic);
		// Set the button types.
		getDialogPane().getButtonTypes().addAll(closeType);
		// Content
		grid.setHgap(10);
		grid.setVgap(10);
		grid.setPadding(new Insets(15, 15, 15, 15));
		getDialogPane().setContent(grid);
	}

	/**
	 * @param title
	 * 		Dialog window title.
	 * @param header
	 * 		Header text.
	 * @param graphic
	 * 		Header graphic.
	 */
	public ClosableDialog(String title, String header, Node graphic) {
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
	public ClosableDialog(StringBinding title, StringBinding header, Node graphic) {
		this(graphic);
		titleProperty().bind(title);
		headerTextProperty().bind(header);
	}
}
