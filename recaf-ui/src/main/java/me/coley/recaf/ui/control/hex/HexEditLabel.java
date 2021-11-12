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
	 * @param location
	 * 		Location in the row the label is located in.
	 * @param offset
	 * 		Local offset. Should be Should be {@code 0-NUM_COLUMNS} .
	 */
	public HexEditLabel(HexRow owner, EditableHexLocation location, int offset) {
		this(owner, location, offset, "");
	}

	/**
	 * @param owner
	 * 		Initial row owner.
	 * @param location
	 * 		Location in the row the label is located in.
	 * @param offset
	 * 		Local offset. Should be Should be {@code 0-NUM_COLUMNS} .
	 * @param initialText
	 * 		Initial text.
	 */
	public HexEditLabel(HexRow owner, EditableHexLocation location, int offset, String initialText) {
		super(owner, location, offset, initialText);
		boolean isRaw = isRaw();
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
			String copy = isRaw ? newValue.replaceAll("[^0-9a-fA-F]+", "") : newValue;
			int max = isRaw ? 2 : 1;
			if (copy.length() > max) {
				copy = copy.substring(0, max);
			}
			if (!copy.equals(newValue)) {
				tf.setText(copy);
			}
		});
		tf.getStyleClass().add(isRaw ? "hex-value-editor" : "hex-text-editor");
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
			if (isRaw()) {
				String editText = HexView.caseHex(tf.getText());
				if (editText.isEmpty()) {
					editText = "00";
				} else if (editText.length() < 2) {
					editText = StringUtil.fillLeft(2, "0", editText);
				}
				setText(editText);
			} else {
				String editText = tf.getText();
				if (editText.isEmpty()) {
					editText = ".";
				}
				setText(editText);
			}
		}
		setGraphic(null);
	}

	/**
	 * Show the editor.
	 */
	private void showEditor() {
		if (isRaw()) {
			tf.setText(backup = HexView.caseHex(getText()));
		} else {
			tf.setText(backup = getText());
		}
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
		int value;
		if (isRaw()) {
			value = Integer.parseInt(getText(), HexAccessor.HEX_RADIX);
		} else {
			value = getText().charAt(0);
		}
		owner.onEdit(offset, value);
	}

	private boolean isRaw() {
		return location == EditableHexLocation.RAW;
	}
}
