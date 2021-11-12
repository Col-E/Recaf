package me.coley.recaf.assemble;


import me.coley.recaf.assemble.ast.Element;

/**
 * Describes a problem that occurred in {@link me.coley.recaf.assemble.generation.MethodBytecodeGenerator}.
 *
 * @author Matt Coley
 */
public class MethodCompileException extends Exception {
	private final Element source;

	/**
	 * @param source
	 * 		Problematic AST node.
	 * @param message
	 * 		Details of the problem.
	 */
	public MethodCompileException(Element source, String message) {
		super(message);
		this.source = source;
	}

	/**
	 * @return Problematic AST node.
	 */
	public Element getSource() {
		return source;
	}
}
