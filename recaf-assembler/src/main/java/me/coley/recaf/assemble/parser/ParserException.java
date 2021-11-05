package me.coley.recaf.assemble.parser;

import org.antlr.v4.runtime.tree.ParseTree;

/**
 * Describes a problem that occured in {@link me.coley.recaf.assemble.parser.BytecodeVisitorImpl}.
 *
 * @author Matt Coley
 */
public class ParserException extends RuntimeException {
	private final ParseTree node;

	/**
	 * @param node
	 * 		Problematic node.
	 * @param message
	 * 		Details.
	 */
	public ParserException(ParseTree node, String message) {
		super(message);
		this.node = node;
	}

	/**
	 * @return Problematic node.
	 */
	public ParseTree getNode() {
		return node;
	}
}
