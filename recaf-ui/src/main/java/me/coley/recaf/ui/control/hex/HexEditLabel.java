package me.coley.recaf.ui.control.hex;

import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import me.coley.recaf.util.StringUtil;

/**
 * Hex label extension that allows modification of values.
 *
 * @author Matt Coley
 */
public class HexEditLabel extends HexLabel {
	private final TextField tf = new TextField();
	private String backup = "";

	/**
	 * @param owner
	 * 		Initial row owner.
	 * @param offset
	 * 		Local offset. Should be Should be {@code 0-NUM_COLUMNS} .
	 */
	public HexEditLabel(HexRow owner, int offset) {
		this(owner, offset, "");
	}

	/**
	 * @param owner
	 * 		Initial row owner.
	 * @param offset
	 * 		Local offset. Should be Should be {@code 0-NUM_COLUMNS} .
	 * @param initialText
	 * 		Initial text.
	 */
	public HexEditLabel(HexRow owner, int offset, String initialText) {
		super(owner, offset, initialText);
		setOnMouseClicked(e -> {
			if (e.getClickCount() == 2 && e.getButton() == MouseButton.PRIMARY) {
				showEditor();
			}
		});
		tf.focusedProperty().addListener((observable, oldValue, isFocused) -> {
			if (!isFocused) {
				apply();
			}
		});
		tf.setOnKeyReleased(e -> {
			KeyCode code = e.getCode();
			if (code.equals(KeyCode.ENTER)) {
				apply();
			} else if (code.equals(KeyCode.ESCAPE)) {
				tf.setText(backup);
				cancel();
			}
		});
		tf.textProperty().addListener((observable, oldValue, newValue) -> {
			String copy = newValue.replaceAll("[^0-9a-fA-F]+", "");
			if (copy.length() > 2) {
				copy = copy.substring(0, 2);
			}
			if (!copy.equals(newValue)) {
				tf.setText(copy);
			}
		});
		tf.getStyleClass().add("hex-value-editor");
		tf.setPromptText("00");
	}

	/**
	 * Hides the {@link #tf editor}.
	 *
	 * @param apply
	 *        {@code true} to replaces the current text with what was in the editor.
	 */
	private void hideEditor(boolean apply) {
		if (apply) {
			String editText = HexView.caseHex(tf.getText());
			if (editText.isEmpty()) {
				editText = "00";
			} else if (editText.length() < 2) {
				editText = StringUtil.fillLeft(2, "0", editText);
			}
			setText(editText);
		}
		setGraphic(null);
	}

	/**
	 * Show the editor.
	 */
	private void showEditor() {
		tf.setText(backup = HexView.caseHex(getText()));
		setGraphic(tf);
		setText("");
		tf.requestFocus();
	}

	/**
	 * Hide the editor and do not apply changed.
	 */
	private void cancel() {
		hideEditor(false);
	}

	/**
	 * Hide the editor and apply changes.
	 */
	private void apply() {
		hideEditor(true);
		owner.onEdit(offset, Integer.parseInt(getText(), HexAccessor.HEX_RADIX));
	}
}
