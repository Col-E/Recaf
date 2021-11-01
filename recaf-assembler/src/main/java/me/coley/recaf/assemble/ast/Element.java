package me.coley.recaf.assemble.ast;

/**
 * Base AST element which exposes position information of the element within the document text
 * and a dissassemble of the element via {@link #print()}.
 *
 * @author Matt Coley
 */
public interface Element extends Printable {
	/**
	 * @return Line number the element appears on.
	 */
	int getLine();

	/**
	 * @return Position in the line the element appears at.
	 */
	int getStart();

	/**
	 * @return Position in the line the element ends at.
	 */
	int getStop();
}
