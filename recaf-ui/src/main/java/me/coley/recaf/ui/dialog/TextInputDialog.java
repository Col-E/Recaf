package me.coley.recaf.ui.dialog;

import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

/**
 * Confirmation dialog that includes a text field for supplying a name.
 *
 * @author Matt Coley
 */
public class TextInputDialog extends ConfirmDialog {
	private final TextField txtName = new TextField();
	private String presetName;

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
		GridPane.setHgrow(txtName, Priority.ALWAYS);
		grid.add(txtName, 0, 0);
		// Ensure confirmation is only allowed when a new value is provided.
		Node confirmButton = getDialogPane().lookupButton(confirmType);
		confirmButton.setDisable(true);
		txtName.textProperty().addListener((observable, oldValue, newValue) -> {
			confirmButton.setDisable(newValue.trim().isEmpty() || newValue.equals(presetName));
		});
		// Window appears with text box focused.
		setOnShown(e -> txtName.requestFocus());
	}

	/**
	 * @param name
	 * 		Name field value.
	 */
	public void setName(String name) {
		presetName = name;
		txtName.setText(name);
		txtName.positionCaret(name.length());
	}

	/**
	 * @return Name field value.
	 */
	public String getName() {
		return txtName.getText();
	}
}
