package me.coley.recaf.code;

/**
 * Literal item that contains an absolute value. This is opposed to an item info that contains data that cannot be
 * represented literally in a raw format, such as classes of an Android dex file.
 *
 * @author Matt Coley
 */
public abstract class LiteralInfo extends ItemInfo {
	private final byte[] value;

	/**
	 * @param name
	 * 		Item name.
	 * @param value
	 * 		Item value <i>(Literal raw content)</i>.
	 */
	protected LiteralInfo(String name, byte[] value) {
		super(name);
		this.value = value;
	}

	/**
	 * @return Item value.
	 */
	public byte[] getValue() {
		return value;
	}
}
