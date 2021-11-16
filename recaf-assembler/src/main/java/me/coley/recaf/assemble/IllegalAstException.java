package me.coley.recaf.assemble;

import me.coley.recaf.assemble.ast.Element;

/**
 * Describes various problem when analyzing usage of the AST.
 * For example, looking up a label by name in the last stage and not finding it
 * <i>(Which should not occur after validation step)</i>.
 *
 * @author Matt Coley
 */
public class IllegalAstException extends AstException {
	/**
	 * @param source
	 * 		Problematic AST node.
	 * @param message
	 * 		Details of the problem.
	 */
	public IllegalAstException(Element source, String message) {
		super(source, message);
	}
}
