package me.coley.recaf.assemble;

import me.coley.recaf.assemble.transformer.AntlrToAstTransformer;
import org.antlr.v4.runtime.ParserRuleContext;

/**
 * Describes a problem that occurred in {@link AntlrToAstTransformer}.
 *
 * @author Matt Coley
 */
public class ParserException extends RuntimeException {
	private final ParserRuleContext node;

	/**
	 * @param node
	 * 		Problematic node.
	 * @param message
	 * 		Details.
	 */
	public ParserException(ParserRuleContext node, String message) {
		super(message);
		this.node = node;
	}

	/**
	 * @return Problematic node.
	 */
	public ParserRuleContext getNode() {
		return node;
	}
}
