package software.coley.recaf.util.analysis.eval;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.tree.MethodInsnNode;
import software.coley.recaf.util.analysis.value.LongValue;
import software.coley.recaf.util.analysis.value.ObjectValue;
import software.coley.recaf.util.analysis.value.ReValue;

import java.util.List;

/**
 * Models system clock operations.
 * <ul>
 * <li>{@link System#currentTimeMillis()}</li>
 * <li>{@link System#nanoTime()}</li>
 * </ul>
 *
 * @author Matt Coley
 */
final class TimeModel implements EvaluatorModel {
	@Override
	public boolean supportsAllocation(@Nonnull String type) {
		return false;
	}

	@Override
	public boolean supportsConstructor(@Nonnull MethodInsnNode instruction) {
		return false;
	}

	@Override
	public boolean supportsStatic(@Nonnull MethodInsnNode instruction) {
		// Just handle the two clock methods, everything else is handled by the default evaluator.
		return instruction.owner.equals("java/lang/System")
				&& (instruction.name.equals("currentTimeMillis") || instruction.name.equals("nanoTime"));
	}

	@Override
	public boolean supportsInstance(@Nonnull MethodInsnNode instruction, @Nonnull ReValue receiver) {
		return false;
	}

	@Override
	@Nullable
	public ObjectValue allocate(@Nonnull String type,
	                            @Nonnull EvaluationContext context) {
		return null;
	}

	@Override
	@Nonnull
	public ModelResult invokeConstructor(@Nonnull MethodInsnNode instruction,
	                                     @Nonnull ReValue receiver,
	                                     @Nonnull List<ReValue> arguments,
	                                     @Nonnull EvaluationContext context) {
		return ModelResult.NOT_HANDLED;
	}

	@Override
	@Nonnull
	public ModelResult invokeStatic(@Nonnull MethodInsnNode instruction,
	                                @Nonnull List<ReValue> arguments,
	                                @Nonnull EvaluationContext context) {
		// Will either be millis or nanos based on supported method check above.
		return ModelResult.yielded(LongValue.of(instruction.name.equals("nanoTime")
				? context.clock.nanos() : context.clock.millis()));
	}

	@Override
	@Nonnull
	public ModelResult invokeInstance(@Nonnull MethodInsnNode instruction,
	                                  @Nonnull ReValue receiver,
	                                  @Nonnull List<ReValue> arguments,
	                                  @Nonnull EvaluationContext context) {
		return ModelResult.NOT_HANDLED;
	}
}
