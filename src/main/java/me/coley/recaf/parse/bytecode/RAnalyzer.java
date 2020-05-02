package me.coley.recaf.parse.bytecode;

import me.coley.recaf.parse.bytecode.exception.LoggedAnalyzerException;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;

import java.util.HashSet;
import java.util.Map;

/**
 * Analyzer that uses {@link RFrame} and is based on {@link RValue}s.
 *
 * @author Matt
 */
public class RAnalyzer extends Analyzer<RValue> {
	private final RInterpreter interpreter;

	/**
	 * Create analyzer.
	 */
	public RAnalyzer(RInterpreter interpreter) {
		super(interpreter);
		this.interpreter = interpreter;
	}

	@Override
	public Frame<RValue>[] analyze(String owner, MethodNode method) throws AnalyzerException {
		Frame<RValue>[] values = super.analyze(owner, method);
		// If the interpeter has problems, check if they've been resolved by checking frames
		if (interpreter.hasReportedProblems()) {
			// Check if the error logged no longer applies given the stack analysis results (due to flow control most likely)
			for(Map.Entry<AbstractInsnNode, AnalyzerException> e : new HashSet<>(interpreter.getProblemInsns().entrySet())) {
				if (e.getValue() instanceof LoggedAnalyzerException) {
					if (((LoggedAnalyzerException) e.getValue()).validate(method, values)) {
						interpreter.getProblemInsns().remove(e.getKey());
					}
				}
			}
			// Check one last time
			if (!interpreter.getProblemInsns().isEmpty())
				throw interpreter.getProblemInsns().values().iterator().next();
		}
		return values;
	}

	@Override
	protected RFrame newFrame(final int numLocals, final int numStack) {
		return new RFrame(numLocals, numStack);
	}

	@Override
	protected RFrame newFrame(final Frame<? extends RValue> frame) {
		return new RFrame((RFrame) frame);
	}
}
