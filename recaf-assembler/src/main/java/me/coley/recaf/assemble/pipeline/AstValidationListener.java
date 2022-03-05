package me.coley.recaf.assemble.pipeline;

import me.coley.recaf.assemble.ast.Unit;
import me.coley.recaf.assemble.validation.Validator;

/**
 * Listener for responding to validation steps of an AST {@link Unit}.
 *
 * @author Matt Coley
 * @see AssemblerPipeline
 */
public interface AstValidationListener {
	/**
	 * Called when the validation process starts.
	 *
	 * @param unit
	 * 		Unit being validated.
	 */
	void onAstValidationBegin(Unit unit);

	/**
	 * Called when the validation process completes.
	 *
	 * @param unit
	 * 		Unit validated.
	 * @param validator
	 * 		Validator. Contains any problem messages that occurred.
	 */
	void onAstValidationComplete(Unit unit, Validator<?> validator);
}
