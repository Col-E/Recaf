package me.coley.recaf.parse.assembly;

/**
 * Simple text visitor.
 *
 * @author Matt
 */
public interface Visitor {
	/**
	 * Visitor pass intended to collect debug information and other information.
	 *
	 * @param text
	 * 		Text to pre-parse.
	 *
	 * @throws LineParseException
	 * 		When the line of text could not be parsed by the visitor implementation.
	 */
	default void visitPre(String text) throws LineParseException {}

	/**
	 * Visitor pass intended to generate content.
	 *
	 * @param text
	 * 		Text to pre-parse.
	 *
	 * @throws LineParseException
	 * 		When the line of text could not be parsed by the visitor implementation.
	 */
	void visit(String text) throws LineParseException;
}
