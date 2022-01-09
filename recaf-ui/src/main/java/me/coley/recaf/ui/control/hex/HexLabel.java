package me.coley.recaf.ui.control.hex;

import javafx.scene.control.Label;

/**
 * Label extension that stores a relative offset <i>(Per each {@link HexRow})</i>.
 *
 * @author Matt Coley
 */
public class HexLabel extends Label {
	protected final EditableHexLocation location;
	protected final int offset;
	protected HexRow owner;

	/**
	 * @param owner
	 * 		Initial row owner.
	 * @param location
	 * 		Location in the row the label is located in.
	 * @param offset
	 * 		Local offset. Should be Should be {@code 0-NUM_COLUMNS} .
	 */
	public HexLabel(HexRow owner, EditableHexLocation location, int offset) {
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
	public HexLabel(HexRow owner, EditableHexLocation location, int offset, String initialText) {
		super(initialText);
		this.owner = owner;
		this.location = location;
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
