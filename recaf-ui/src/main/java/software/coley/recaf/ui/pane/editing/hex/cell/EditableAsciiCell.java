package software.coley.recaf.ui.pane.editing.hex.cell;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.TextFormatter;
import software.coley.recaf.ui.pane.editing.hex.HexUtil;
import software.coley.recaf.ui.pane.editing.hex.ops.HexOperations;

import java.nio.charset.StandardCharsets;

/**
 * Hex cell for displaying a single ascii char.
 *
 * @author Matt Coley
 */
public class EditableAsciiCell extends HexCellBase implements HexCell {
	private final boolean canMarkDirty;
	private final BooleanProperty dirty = new SimpleBooleanProperty();

	public EditableAsciiCell(@Nonnull HexOperations ops, int offset, byte b) {
		super(ops, offset, b);

		// Despite being 'final' some code in the upstream class will reference it before we initialize it to 'true' here.
		// Thus, anything before that point should not be allowed to update the 'dirty' property as its under init.
		canMarkDirty = true;
	}

	@Override
	protected int maxLength() {
		return 1;
	}

	@Nullable
	@Override
	protected TextFormatter.Change processTextChange(@Nonnull TextFormatter.Change change, @Nonnull String changeText) {
		// Mark this cell as being dirty. We'll use this in byte mapping later.
		if (canMarkDirty) dirty.set(true);

		// No manipulation or additional work needed for this implementation
		return change;
	}

	@Nonnull
	@Override
	protected String processEditorText(@Nonnull String text) {
		// No processing
		return text;
	}

	@Nonnull
	@Override
	protected Byte2StringRepresentation mapper() {
		return HexUtil::strAscii;
	}

	@Override
	protected byte toByteValue() {
		// If we're dirty (user input has been made) then we compute the byte value bases on the ascii char value.
		if (dirty != null && dirty.get()) {
			String text = textProperty.get();
			if (text.isEmpty()) return 0;
			return text.getBytes(StandardCharsets.US_ASCII)[0];
		}

		// Delegate to existing value since there is no change.
		return ops.currentAccess().getByte(offset());
	}
}
