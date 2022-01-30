package me.coley.recaf.assemble.pipeline;

import me.coley.recaf.assemble.AstException;
import me.coley.recaf.assemble.ParserException;
import me.coley.recaf.assemble.parser.BytecodeParser;
import me.coley.recaf.assemble.transformer.AntlrToAstTransformer;
import me.coley.recaf.assemble.validation.ast.AstValidator;
import org.antlr.v4.runtime.tree.ParseTree;

/**
 * Listener for responding to various failures at different steps in AST parsing.
 *
 * @author Matt Coley
 * @see AssemblerPipeline
 */
public interface ParserFailureListener {
	/**
	 * Called when {@link BytecodeParser.UnitContext#unit()} fails.
	 *
	 * @param ex
	 * 		Exception wrapper detailing the failure.
	 */
	void onAntlrParseFail(ParserException ex);

	/**
	 * Called when {@link AntlrToAstTransformer#visit(ParseTree)} fails.
	 *
	 * @param ex
	 * 		Exception wrapper detailing the failure.
	 */
	void onAntlrTransformFail(ParserException ex);

	/**
	 * Called when {@link AstValidator#visit()} fails.
	 * This is different from reporting validation errors, these are unhandled cases and should not generally occur.
	 *
	 * @param ex
	 * 		Exception wrapper detailing the failure.
	 */
	void onAstValidationError(AstException ex);
}
