package me.coley.recaf.ui.control.hex;

import javafx.scene.control.Label;

/**
 * Label extension that stores a relative offset <i>(Per each {@link HexRow})</i>.
 *
 * @author Matt Coley
 */
public class HexLabel extends Label {
	protected final int offset;
	protected HexRow owner;

	/**
	 * @param owner
	 * 		Initial row owner.
	 * @param offset
	 * 		Local offset. Should be Should be {@code 0-NUM_COLUMNS} .
	 */
	public HexLabel(HexRow owner, int offset) {
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
	public HexLabel(HexRow owner, int offset, String initialText) {
		super(initialText);
		this.owner = owner;
		this.offset = offset;
	}

	/**
	 * @return Local offset. Should be {@code 0-NUM_COLUMNS} .
	 */
	public int getOffset() {
		return offset;
	}

	/**
	 * @return Current row this label belongs to.
	 */
	public HexRow getOwner() {
		return owner;
	}

	/**
	 * @return Returns if the cell is empty.
	 */
	public boolean isEmpty() { return getText().replace(" ", "").isEmpty(); }
}
