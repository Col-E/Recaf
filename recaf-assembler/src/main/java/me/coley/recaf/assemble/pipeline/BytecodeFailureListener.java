package me.coley.recaf.assemble.pipeline;

import me.coley.recaf.assemble.BytecodeException;
import me.coley.recaf.assemble.MethodCompileException;
import me.coley.recaf.assemble.ast.Unit;

/**
 * Listener for responding to various failures at different steps in AST parsing.
 *
 * @author Matt Coley
 * @see AssemblerPipeline
 */
public interface BytecodeFailureListener {
	/**
	 * @param object
	 * 		The {@link org.objectweb.asm.tree.FieldNode}
	 * 		or {@link org.objectweb.asm.tree.MethodNode} that failed validation.
	 * @param ex
	 * 		Exception wrapper detailing the failure.
	 */
	void onValidationFailure(Object object, BytecodeException ex);

	/**
	 * @param unit
	 * 		Unit that failed compilation.
	 * @param ex
	 * 		Exception wrapper detailing the failure.
	 */
	void onCompileFailure(Unit unit, MethodCompileException ex);
}
