package me.coley.recaf.ui.control.code.bytecode;

import me.coley.recaf.assemble.ast.Unit;
import me.coley.recaf.ui.control.code.ProblemTracking;

/**
 * Listener to receive updates from an {@link AssemblerArea} when the AST model changes.
 *
 * @author Matt Coley
 */
public interface AssemblerAstListener {
	/**
	 * Called when the assembler builds a new AST.
	 *
	 * @param unit
	 * 		Latest AST model.
	 */
	void onAstBuildPass(Unit unit);

	/**
	 * Called when the assembler fails to build a new AST.
	 *
	 * @param unit
	 * 		Latest AST model. If the problem occurred during the validation step, the AST is up-to-date.
	 * 		Otherwise, if the error occurred earlier in the parse process this may be the <i>prior</i> model.
	 */
	void onAstBuildFail(Unit unit, ProblemTracking problemTracking);

	/**
	 * Called when the assembler crashes while building a new AST.
	 *
	 * @param unit
	 * 		Latest AST model. If the crash occurred before the validation step the AST is the <i>prior</i> model.
	 * @param reason
	 * 		Crash reason.
	 */
	void onAstBuildCrash(Unit unit, Throwable reason);
}
