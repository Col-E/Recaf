package me.coley.recaf.assemble.pipeline;

import me.coley.recaf.assemble.AstException;
import me.coley.recaf.assemble.transformer.JasmToUnitTransformer;
import me.coley.recaf.assemble.validation.ast.AstValidator;
import me.darknet.assembler.exceptions.AssemblerException;
import me.darknet.assembler.parser.ParserContext;

/**
 * Listener for responding to various failures at different steps in AST parsing.
 *
 * @author Matt Coley
 * @see AssemblerPipeline
 */
public interface ParserFailureListener {

	/**
	 * Called when {@link AssemblerPipeline#updateAst(boolean)} fails due to the {@link ParserContext} not being
	 * parsable with the given input.
	 *
	 * @param ex
	 * 		Exception wrapper detailing the failure.
	 */
	void onParseFail(AssemblerException ex);

	/**
	 * Called when {@link AssemblerPipeline#updateAst(boolean)} fails due to the {@link JasmToUnitTransformer} not
	 * handling the contents of the parsed {@code List&gt;Group&lt;} from the prior AST parsing.
	 *
	 * @param ex
	 * 		Exception wrapper detailing the failure.
	 */
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
