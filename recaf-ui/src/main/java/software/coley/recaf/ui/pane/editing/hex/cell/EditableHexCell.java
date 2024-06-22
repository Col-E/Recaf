package software.coley.recaf.ui.pane.editing.hex.cell;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.scene.control.TextFormatter;
import software.coley.recaf.ui.pane.editing.hex.HexUtil;
import software.coley.recaf.ui.pane.editing.hex.ops.HexOperations;

/**
 * Hex cell for displaying a single byte as a hex string.
 *
 * @author Matt Coley
 */
public class EditableHexCell extends HexCellBase implements HexCell {
	public EditableHexCell(@Nonnull HexOperations ops, int offset, byte b) {
		super(ops, offset, b);
	}

	@Override
	protected int maxLength() {
		// Each hex label uses two characters to represent the byte value display.
		return 2;
	}

	@Nullable
	@Override
	protected TextFormatter.Change processTextChange(@Nonnull TextFormatter.Change change, @Nonnull String changeText) {
		try {
			// Must be hex text
			Integer.parseInt(changeText, 16);
			return change;
		} catch (NumberFormatException ex) {
			// Not hex, discard the change.
			return null;
		}
	}

	@Nonnull
	@Override
	protected String processEditorText(@Nonnull String text) {
		// We'll upper-case the editor's text so everything looks consistent when it gets applied to the display label.
		return text.toUpperCase();
	}

	@Nonnull
	@Override
	protected Byte2StringRepresentation mapper() {
		// byte --> 2 char hex string
		return HexUtil::strFormat00;
	}

	@Override
	public byte toByteValue() {
		// Parse hex text, which should always be valid due to the editor's applied text-formatter
		// and its delegation to our implementation of 'processTextChange'.
		String text = textProperty.get();
		if (text.isEmpty()) return 0;
		return (byte) Integer.parseInt(text, 16);
	}

	@Override
	protected void preProcessCommit() {
		// Fill in remaining spaces if ending before text input is full.
		while (textProperty.length().get() < 2)
			textProperty.set(textProperty.get() + "0");
	}
}
