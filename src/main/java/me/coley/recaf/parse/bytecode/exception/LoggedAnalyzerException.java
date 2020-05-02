package me.coley.recaf.parse.bytecode.exception;

import me.coley.recaf.parse.bytecode.RValue;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.Value;

public class LoggedAnalyzerException extends AnalyzerException {
	private final PostValidator postValidator;

	public LoggedAnalyzerException(PostValidator postValidator, AbstractInsnNode insn,
								   String message) {
		super(insn, message);
		this.postValidator = postValidator;
	}

	public LoggedAnalyzerException(PostValidator postValidator, AbstractInsnNode insn,
								   String message, Object expected, Value actual) {
		super(insn, message, expected, actual);
		this.postValidator = postValidator;
	}

	public boolean validate(MethodNode method, Frame<RValue>[] frames) {
		return postValidator.test(method, frames);
	}
}
