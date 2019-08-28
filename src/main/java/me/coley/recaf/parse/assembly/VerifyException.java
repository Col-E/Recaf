package me.coley.recaf.parse.assembly;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;

/**
 * Assembly exception for when the generated method failed verification.
 *
 * @author Matt
 */
public class VerifyException extends Exception {
	public VerifyException(Exception parent, String message) {
		super(message, parent);
	}

	/**
	 * @return Cause of verification failure. Will be null if the {@link #getCause() cause} of
	 * this exception is not an {@link org.objectweb.asm.tree.analysis.AnalyzerException}.
	 */
	public AbstractInsnNode getInsn() {
		if(getCause() instanceof AnalyzerException) {
			return ((AnalyzerException) getCause()).node;
		}
		return null;
	}

}
