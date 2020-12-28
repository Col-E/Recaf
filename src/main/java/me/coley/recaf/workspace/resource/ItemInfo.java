package me.coley.recaf.workspace.resource;

/**
 * Base outline for a resource item.
 *
 * @author Matt Coley
 */
public abstract class ItemInfo {
	private final String name;
	private final byte[] value;

	/**
	 * @param name
	 * 		Item name, used as key in resource.
	 * @param value
	 * 		Item value.
	 */
	public ItemInfo(String name, byte[] value) {
		this.name = name;
		this.value = value;
	}

	/**
	 * @return Item name, used as key in resource.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return Item value.
	 */
	public byte[] getValue() {
		return value;
	}
}
