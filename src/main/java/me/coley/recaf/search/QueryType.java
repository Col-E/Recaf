package me.coley.recaf.search;

/**
 * Type of query, useful for switching over query implementation types.
 *
 * @author Matt
 */
public enum QueryType {
	/**
	 * Match a class if it matches a given name.
	 */
	CLASS_NAME,
	/**
	 * Match a class if it inherits a given class.
	 */
	CLASS_INHERITANCE,
	/**
	 * Match a member if it matches a given definition.
	 */
	MEMBER_DEFINITION,
	/**
	 * Match an instruction if it references a given class.
	 */
	CLASS_REFERENCE,
	/**
	 * Match an instruction if it references a given member.
	 */
	MEMBER_REFERENCE,
	/**
	 * Match an instruction / field-constant if it's string value matches a given pattern.
	 */
	STRING,
	/**
	 * Match an instruction if it contains a given value.
	 */
	VALUE,
	/**
	 * Match a method if it contains the given sequence of instruction patterns.
	 */
	INSTRUCTION_TEXT
}
