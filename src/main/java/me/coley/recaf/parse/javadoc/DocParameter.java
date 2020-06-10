package me.coley.recaf.parse.javadoc;

/**
 * Javadoc method parameter wrapper.
 *
 * @author Matt
 */
public class DocParameter {
	// TODO: Record type?
	// - Given as signature types, generics don't have inlined type information
	// - Generally seems like a pain
	private final String name;
	private final String description;

	/**
	 * @param name
	 * 		Parameter's name.
	 * @param description
	 * 		Parameter's purpose.
	 */
	public DocParameter(String name, String description) {
		this.name = name;
		this.description = description;
	}

	/**
	 * @return Parameter's name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return Parameter's purpose.
	 */
	public String getDescription() {
		return description;
	}
}
