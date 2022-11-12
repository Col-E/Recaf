package me.coley.recaf.assemble;

import me.coley.recaf.assemble.ast.Element;

/**
 * Describes a problem occurring in the {@link me.coley.recaf.assemble.analysis.Analyzer} process.
 *
 * @author Matt Coley
 */
public class AnalysisException extends AstException {
	/**
	 * @param source
	 * 		Problematic AST node.
	 * @param message
	 * 		Details of the problem.
	 */
	public AnalysisException(Element source, String message) {
		super(source, message);
	}

	/**
	 * @param source
	 * 		Problematic AST node.
	 * @param cause
	 * 		Delegated exception.
	 */
	public AnalysisException(Element source, Exception cause) {
		super(source, cause, cause.getMessage());
	}

	/**
	 * @param source
	 * 		Problematic AST node.
	 * @param cause
	 * 		Delegated exception.
	 *          @param message
	 * 	 		Details of the problem.
	 */
	public AnalysisException(Element source, Exception cause, String message) {
		super(source, cause, message);
	}
}
