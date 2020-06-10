package me.coley.recaf.config;

/**
 * Config base.
 *
 * @author Matt
 */
public abstract class Config implements Configurable{
	private final String name;

	/**
	 * @param name
	 * 		Group name.
	 */
	Config(String name) {
		this.name = name;
	}

	/**
	 * @return Group name.
	 */
	public String getName() {
		return name;
	}
}
