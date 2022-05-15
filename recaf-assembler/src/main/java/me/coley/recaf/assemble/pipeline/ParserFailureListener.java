package me.coley.recaf.assemble.pipeline;

import me.coley.recaf.assemble.AstException;
import me.coley.recaf.assemble.validation.ast.AstValidator;
import me.darknet.assembler.parser.AssemblerException;

/**
 * Listener for responding to various failures at different steps in AST parsing.
 *
 * @author Matt Coley
 * @see AssemblerPipeline
 */
public interface ParserFailureListener {

	void onParseFail(AssemblerException ex);


	void onParserTransformFail(AssemblerException ex);

	/**
	 * Called when {@link AstValidator#visit()} fails.
	 * This is different from reporting validation errors, these are unhandled cases and should not generally occur.
	 *
	 * @param ex
	 * 		Exception wrapper detailing the failure.
	 */
	void onAstValidationError(AstException ex);
}
