package me.coley.recaf.parse.bytecode;

import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.Frame;

/**
 * Analyzer that uses {@link RFrame} and is based on {@link RValue}s.
 *
 * @author Matt
 */
public class RAnalyzer extends Analyzer<RValue> {
	/**
	 * Create analyzer.
	 */
	public RAnalyzer() {
		super(new RInterpreter());
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
