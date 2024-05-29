package software.coley.recaf.ui.pane.editing.hex.cell;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import software.coley.recaf.ui.control.BoundLabel;
import software.coley.recaf.ui.pane.editing.hex.ops.HexOperations;
import software.coley.recaf.util.NodeEvents;

/**
 * Base hex cell implementation.
 *
 * @author Matt Coley
 * @see EditableHexCell For displaying hex codes for bytes.
 * @see EditableAsciiCell For displaying ascii chars for bytes.
 */
public abstract class HexCellBase extends BorderPane implements HexCell {
	protected static final PseudoClass PSEUDO_ZERO = PseudoClass.getPseudoClass("zero");
	protected static final PseudoClass PSEUDO_SELECTED = PseudoClass.getPseudoClass("selected");
	protected static final PseudoClass PSEUDO_EDITED = PseudoClass.getPseudoClass("edited");
	protected final StringProperty textProperty = new SimpleStringProperty();
	protected final StringProperty cachedTextProperty = new SimpleStringProperty();
	protected final IntegerProperty offsetProperty = new SimpleIntegerProperty();
	protected final Label label = new BoundLabel(textProperty);
	protected final TextField editor = new TextField();
	protected final HexOperations ops;

	public HexCellBase(@Nonnull HexOperations ops, int offset, byte b) {
		this.ops = ops;

		// Only allow the editor to contain hex text.
		int maxLength = maxLength();
		editor.setPrefColumnCount(maxLength);
		editor.setTextFormatter(new TextFormatter<>(c -> {
			String newText = c.getControlNewText();

			// Allow blank content
			if (newText.isEmpty())
				return c;

			// Don't allow more than two chars worth of content (one byte filtering)
			if (newText.length() > maxLength) {
				endEdit(true);

				// Move to the next editor, and if that succeeds pass the attempted typed character to the next editor.
				ops.navigation().selectNext();
				if (ops.navigation().selectionOffset() != offset) {
					ops.engageCurrent();

					// Pass the key to the next editor.
					KeyCode code = NodeEvents.getKeycode(newText.charAt(maxLength));
					if (code != null) ops.sendKeyToCurrentEngaged(code);
				}
				return null;
			}

			return processTextChange(c, newText);
		}));

		// When the editor updates, set the main property value.
		editor.textProperty().addListener((ob, old, cur) -> textProperty.setValue(processEditorText(cur)));

		// Setup forwarding to the primary hex editor navigation key listener.
		editor.setOnKeyPressed(e -> {
			// Some keys should not delegate to the primary key listener, such as navigation and enter.
			KeyCode code = e.getCode();
			if (code.isArrowKey() || code.isNavigationKey() || code == KeyCode.ENTER)
				return;

			// The rest can be handled properly by the main editor's key listener.
			ops.keyListener().handle(e);
		});


		// When the main property updates, set the editor's text since it's not bound.
		textProperty.addListener((ob, old, cur) -> {
			if (!editor.getText().equals(cur))
				editor.setText(cur);
			label.pseudoClassStateChanged(PSEUDO_ZERO, toByteValue() == 0);
		});

		// Initially show the read-only label.
		setCenter(label);

		// Assign properties last so property listeners configured above can act on them.
		offsetProperty.setValue(offset);

		// Update the initial text to model the hexadecimal representation of the byte.
		textProperty.setValue(mapper().map(b));
		cachedTextProperty.set(textProperty.get());

		// Initialize edited pseudo-state.
		updateEditedPseudoState();
	}

	/**
	 * @return Max length of this cell. Should be implemented as a constant value.
	 */
	protected abstract int maxLength();

	/**
	 * @param change
	 * 		Change to accept or deny.
	 * @param changeText
	 * 		Text of the cell if the change were accepted.
	 *
	 * @return {@code change} to accept the change, or {@code null} to deny it.
	 */
	@Nullable
	protected abstract TextFormatter.Change processTextChange(@Nonnull TextFormatter.Change change, @Nonnull String changeText);

	/**
	 * @param text
	 * 		Current text of the {@link #editor}.
	 *
	 * @return Text to apply to the {@link #label} based on the editor's text.
	 */
	@Nonnull
	protected abstract String processEditorText(@Nonnull String text);

	/**
	 * @return Mapper of {@code byte} values to {@code String} representations.
	 */
	@Nonnull
	protected abstract Byte2StringRepresentation mapper();

	/**
	 * @return Byte value of the current cell based on the current text value.
	 */
	protected abstract byte toByteValue();

	/**
	 * Called by {@link #endEdit(boolean)} before a commit is made, allowing processing of the {@link #textProperty}
	 * before that value is used to update the backing hex content.
	 */
	protected void preProcessCommit() {
		// no-op by default
	}

	/**
	 * Updates the cell to have the {@code :edited} pseudo-state if the current value does not match the orignal value
	 * of the hex content.
	 * <p/>
	 * Currently due to the way the hex rows are refreshed after edit commits are made, this only is called during
	 * the initial construction of the cell.
	 */
	protected void updateEditedPseudoState() {
		// Change the label text color if it differs from the original content
		label.pseudoClassStateChanged(PSEUDO_EDITED, ops.originalAccess().getByte(offset()) != toByteValue());
	}

	@Nonnull
	@Override
	public Node node() {
		return this;
	}

	@Override
	public int offset() {
		return offsetProperty.get();
	}

	@Override
	public void onSelectionGained() {
		label.pseudoClassStateChanged(PSEUDO_SELECTED, true);
	}

	@Override
	public void onSelectionLost() {
		label.pseudoClassStateChanged(PSEUDO_SELECTED, false);
		if (isEditing()) endEdit(true);
	}

	@Override
	public boolean isEditing() {
		return getCenter() == editor;
	}

	@Override
	public void beginEdit() {
		cachedTextProperty.set(textProperty.get());
		setCenter(editor);
		editor.requestFocus();
		editor.selectAll();
	}

	@Override
	public void endEdit(boolean commit) {
		if (!commit) {
			// Set text to what it was before the edit began.
			textProperty.set(cachedTextProperty.get());
		} else {
			preProcessCommit();

			// Commit the change.
			ops.currentAccess().setByte(offset(), toByteValue());

			// Refresh the display (so the hex column gets updated)
			ops.refreshDisplay(offset(), false);
		}
		setCenter(label);
	}

	@Override
	public void handleKeyCode(@Nonnull KeyCode code) {
		if (isEditing()) {
			textProperty.set(code.getChar());
			editor.positionCaret(textProperty.length().get());
		}
	}

	@Override
	public String toString() {
		return "data[" + offset() + "] = " + textProperty.get() + " / '" + (char) toByteValue() + "'";
	}

	/**
	 * Map a {@code byte} to {@link String}.
	 */
	protected interface Byte2StringRepresentation {
		/**
		 * @param b
		 * 		Byte input.
		 *
		 * @return String representation of byte.
		 */
		@Nonnull
		String map(byte b);
	}
}
