package me.coley.recaf.search;

import java.util.function.IntSupplier;

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
	 *
	 */
	public ClassReferenceQuery(String name) {
		this(name, StringMatchMode.EQUALS);
	}

	/**
	 * Constructs a class referencing query.
	 *
	 * @param name
	 * 		Class name pattern.
	 * @param stringMode
	 * 		How to match class names.
	 */
	public ClassReferenceQuery(String name, StringMatchMode stringMode) {
		super(QueryType.CLASS_REFERENCE, stringMode);
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
	public void match(IntSupplier access, String name) {
		if (stringMode.match(this.name, name)) {
			getMatched().add(new ClassResult(access.getAsInt(), name));
		}
	}
}
