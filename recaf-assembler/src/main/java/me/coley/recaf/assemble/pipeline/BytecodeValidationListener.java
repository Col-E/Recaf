package me.coley.recaf.assemble.pipeline;

import me.coley.recaf.assemble.validation.Validator;

/**
 * Listener for responding to validation steps of a generated field or method.
 *
 * @author Matt Coley
 * @see AssemblerPipeline
 */
public interface BytecodeValidationListener {
	/**
	 * Called when the validation process starts.
	 *
	 * @param object
	 *        {@link org.objectweb.asm.tree.FieldNode} or {@link org.objectweb.asm.tree.MethodNode} being validated.
	 */
	void onBytecodeValidationBegin(Object object);

	/**
	 * Called when the validation process completes.
	 *
	 * @param object
	 *        {@link org.objectweb.asm.tree.FieldNode} or {@link org.objectweb.asm.tree.MethodNode} being validated.
	 * @param validator
	 * 		Validator. Contains any problem messages that occurred.
	 */
	void onBytecodeValidationComplete(Object object, Validator<?> validator);
}
