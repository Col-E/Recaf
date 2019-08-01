package me.coley.recaf.search;

/**
 * Query to find references to the given class.
 *
 * @author Matt
 */
public class ClassReferenceQuery extends Query {
	private final String name;

	/**
	 * Constructs a class referencing query.
	 *
	 * @param name
	 * 		Class name pattern.
	 */
	public ClassReferenceQuery(String name) {
		super(QueryType.USAGE, StringMatchMode.EQUALS);
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
