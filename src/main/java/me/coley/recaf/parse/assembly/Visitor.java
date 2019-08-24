package me.coley.recaf.parse.assembly;

/**
 * Simple type visitor.
 *
 * @param <T> type of content to visit
 *
 * @author Matt
 */
public interface Visitor<T> {
	/**
	 * Visitor pass intended to collect debug information and other information.
	 *
	 * @param value
	 * 		Item to pre-parse.
	 *
	 * @throws LineParseException
	 * 		When the item could not be parsed by the visitor implementation.
	 */
	default void visitPre(T value) throws LineParseException {}

	/**
	 * Visitor pass intended to generate content.
	 *
	 * @param value
	 * 		Item to parse.
	 *
	 * @throws LineParseException
	 * 		When the item could not be parsed by the visitor implementation.
	 */
	void visit(T value) throws LineParseException;
}
