package software.coley.recaf.util.analysis.eval;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import software.coley.recaf.util.analysis.ReFrame;
import software.coley.recaf.util.analysis.ReInterpreter;
import software.coley.recaf.util.analysis.value.ReValue;

import java.util.List;

/**
 * Handler for invoking a static method.
 *
 * @author Matt Coley
 * @see MethodInvokeHandler
 */
@FunctionalInterface
public interface MethodInvokeStaticHandler {
	/**
	 * @param frame
	 * 		Current frame of the evaluator.
	 * @param interpreter
	 * 		Evaluator interpreter used to resolve modeled type relationships.
	 * @param instruction
	 * 		Static method invocation instruction.
	 * @param args
	 * 		Values of the arguments on the stack passed to the method.
	 *
	 * @return Value returned by the method, or {@code null} if the method is {@code void}.
	 *
	 * @throws AnalyzerException
	 * 		When evaluator state is too unknown to model the method invocation.
	 * @throws Throwable
	 * 		When the modeled method throws an ordinary host/runtime exception.
	 */
	@Nullable
	ReValue invoke(@Nonnull ReFrame frame, @Nonnull ReInterpreter interpreter, @Nonnull MethodInsnNode instruction, @Nonnull List<ReValue> args) throws Throwable;
}
