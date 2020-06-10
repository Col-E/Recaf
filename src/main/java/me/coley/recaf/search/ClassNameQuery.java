package me.coley.recaf.search;

/**
 * Query to find classes matching the given name.
 *
 * @author Matt
 */
public class ClassNameQuery extends Query {
	private final String name;

	/**
	 * Constructs a class name matching query.
	 *
	 * @param name
	 * 		Class name pattern.
	 * @param stringMode
	 * 		How to match strings.
	 */
	public ClassNameQuery(String name, StringMatchMode stringMode) {
		super(QueryType.CLASS_NAME, stringMode);
		this.name = name;
	}

	/**
	 * Adds a result if the given class matches the specified name pattern.
	 *
	 * @param access
	 * 		Class modifiers.
	 * @param name
	 * 		Name of class.
	 */
	public void match(int access, String name) {
		if (stringMode.match(this.name, name)) {
			getMatched().add(new ClassResult(access, name));
		}
	}
}
