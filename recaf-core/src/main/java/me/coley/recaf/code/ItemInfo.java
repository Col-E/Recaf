package me.coley.recaf.code;

/**
 * Base outline for a resource item.
 *
 * @author Matt Coley
 */
public abstract class ItemInfo {
	private final String name;

	/**
	 * @param name
	 * 		Item name, used as key in resource.
	 */
	public ItemInfo(String name) {
		this.name = name;
	}

	/**
	 * @return Item name, used as key in resource.
	 */
	public String getName() {
		return name;
	}
}
