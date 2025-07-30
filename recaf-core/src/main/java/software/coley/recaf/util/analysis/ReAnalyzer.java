package software.coley.recaf.util.analysis;

import jakarta.annotation.Nonnull;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.Frame;
import software.coley.recaf.util.analysis.value.ReValue;

/**
 * Analyzer that takes in an interpreter for {@link ReValue enhanced value types}.
 *
 * @author Matt Coley
 */
public class ReAnalyzer extends Analyzer<ReValue> {
	private final ReInterpreter interpreter;

	/**
	 * @param interpreter
	 * 		Enhanced interpreter.
	 */
	public ReAnalyzer(@Nonnull ReInterpreter interpreter) {
		super(interpreter);
		this.interpreter = interpreter;
	}

	/**
	 * @return Interpreter backing this analyzer.
	 */
	@Nonnull
	public ReInterpreter getInterpreter() {
		return interpreter;
	}

	@Override
	protected Frame<ReValue> newFrame(int numLocals, int numStack) {
		return new ReFrame(numLocals, numStack);
	}

	@Override
	protected Frame<ReValue> newFrame(Frame<? extends ReValue> frame) {
		return new ReFrame(frame);
	}
}
