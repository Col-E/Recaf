package software.coley.recaf.util.analysis;

import jakarta.annotation.Nonnull;
import org.objectweb.asm.tree.analysis.Analyzer;
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
}
