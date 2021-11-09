package me.coley.recaf.assemble;

import me.coley.recaf.assemble.ast.BytecodeAstGenerator;
import org.antlr.v4.runtime.tree.ParseTree;

/**
 * Describes a problem that occurred in {@link BytecodeAstGenerator}.
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
