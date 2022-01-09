package me.coley.recaf.assemble;


import me.coley.recaf.assemble.ast.Element;
import me.coley.recaf.assemble.transformer.AstToMethodTransformer;

/**
 * Describes a problem that occurred in {@link AstToMethodTransformer}.
 *
 * @author Matt Coley
 */
public class MethodCompileException extends AstException {
	/**
	 * @param source
	 * 		Problematic AST node.
	 * @param message
	 * 		Details of the problem.
	 */
	public MethodCompileException(Element source, String message) {
		super(source, message);
	}

	/**
	 * @param source
	 * 		Problematic AST node.
	 * @param cause
	 * 		Cause exception.
	 * @param message
	 * 		Details of the problem.
	 */
	public MethodCompileException(Element source, Exception cause, String message) {
		super(source, cause, message);
	}
}
