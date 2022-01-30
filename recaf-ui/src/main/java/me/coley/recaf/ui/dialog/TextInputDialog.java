package me.coley.recaf.ui.dialog;

import javafx.beans.binding.StringBinding;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

/**
 * Confirmation dialog that includes a text field for supplying a value.
 *
 * @author Matt Coley
 */
public class TextInputDialog extends ConfirmDialog {
	private final TextField text = new TextField();
	private String presetName;

	/**
	 * @param title
	 * 		Dialog window title.
	 * @param header
	 * 		Header text.
	 * @param graphic
	 * 		Header graphic.
	 */
	public TextInputDialog(StringBinding title, StringBinding header, Node graphic) {
		super(title, header, graphic);
	}

	/**
	 * @param title
	 * 		Dialog window title.
	 * @param header
	 * 		Header text.
	 * @param graphic
	 * 		Header graphic.
	 */
	public TextInputDialog(String title, String header, Node graphic) {
		super(title, header, graphic);
	}

	/**
	 * @param name
	 * 		Name field value.
	 */
	public void setText(String name) {
		presetName = name;
		text.setText(name);
		text.positionCaret(name.length());
	}

	/**
	 * @return Name field value.
	 */
	public String getText() {
		return text.getText();
	}

	@Override
	protected void init() {
		super.init();
		GridPane.setHgrow(text, Priority.ALWAYS);
		grid.add(text, 0, 0);
		// Ensure confirmation is only allowed when a new value is provided.
		Node confirmButton = getDialogPane().lookupButton(confirmType);
		confirmButton.setDisable(true);
		text.textProperty().addListener((observable, oldValue, newValue) -> {
			confirmButton.setDisable(newValue.trim().isEmpty() || newValue.equals(presetName));
		});
		// Window appears with text box focused.
		setOnShown(e -> text.requestFocus());
	}
}
