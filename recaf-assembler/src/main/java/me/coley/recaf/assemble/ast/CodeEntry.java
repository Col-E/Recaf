package me.coley.recaf.assemble.ast;

/**
 * Single entry of a {@link Code} block.
 *
 * @author Matt Coley
 */
public interface CodeEntry extends Element {
	/**
	 * Handles inserting the current entry into a given block.
	 *
	 * @param code
	 * 		Code block to be added to.
	 */
	void insertInto(Code code);
}
