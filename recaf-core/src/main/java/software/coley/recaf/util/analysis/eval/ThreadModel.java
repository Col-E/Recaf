package software.coley.recaf.util.analysis.eval;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.tree.MethodInsnNode;
import software.coley.recaf.util.ClassMethodPair;
import software.coley.recaf.util.analysis.value.IntValue;
import software.coley.recaf.util.analysis.value.LongValue;
import software.coley.recaf.util.analysis.value.ObjectValue;
import software.coley.recaf.util.analysis.value.ReValue;
import software.coley.recaf.util.analysis.value.StringValue;
import software.coley.recaf.util.analysis.value.UninitializedValue;

import java.util.List;

/**
 * Models {@link java.lang.Thread} operations.
 *
 * @author Matt Coley
 */
final class ThreadModel implements EvaluatorModel {
	private static final String THREAD = "java/lang/Thread";
	private final Evaluator evaluator;

	ThreadModel(@Nonnull Evaluator evaluator) {
		this.evaluator = evaluator;
	}

	@Override
	public boolean supportsAllocation(@Nonnull String type) {
		return evaluator.isAssignableFrom(THREAD, type);
	}

	@Override
	public boolean supportsConstructor(@Nonnull MethodInsnNode instruction) {
		return instruction.name.equals("<init>")
				&& evaluator.isAssignableFrom(THREAD, instruction.owner);
	}

	@Override
	public boolean supportsStatic(@Nonnull MethodInsnNode instruction) {
		return instruction.owner.equals(THREAD)
				&& (instruction.name.equals("currentThread")
				|| instruction.name.equals("sleep")
				|| instruction.name.equals("yield"));
	}

	@Override
	public boolean supportsInstance(@Nonnull MethodInsnNode instruction, @Nonnull ReValue receiver) {
		return receiver instanceof ObjectValue object
				&& evaluator.isAssignableFrom(THREAD, instruction.owner)
				&& evaluator.isAssignableFrom(THREAD, object.type().getInternalName());
	}

	@Override
	@Nullable
	public ObjectValue allocate(@Nonnull String type, @Nonnull EvaluationContext context) {
		return supportsAllocation(type) ? context.modelHeap.createThread(type, context.currentThread).value : null;
	}

	@Override
	@Nonnull
	public ModelResult invokeConstructor(@Nonnull MethodInsnNode instruction, @Nonnull ReValue receiver,
	                                     @Nonnull List<ReValue> arguments, @Nonnull EvaluationContext context) {
		EvaluationModelHeap.ThreadState state = context.modelHeap.thread(receiver);
		if (state == null)
			return ModelResult.failed("Thread constructor receiver is not model-owned");
		if (!arguments.isEmpty() && arguments.getFirst() instanceof InvokeDynamicExecutor.EvaluatedLambdaValue lambda)
			state.runnable = lambda;
		if (arguments.size() > 1 && arguments.get(1) instanceof StringValue name)
			state.name = name.getText().orElse(state.name);
		return voidResult();
	}

	@Override
	@Nonnull
	public ModelResult invokeStatic(@Nonnull MethodInsnNode instruction, @Nonnull List<ReValue> arguments,
	                                @Nonnull EvaluationContext context) {
		return switch (instruction.name) {
			case "currentThread" -> ModelResult.yielded(context.currentThread.value);
			case "sleep" -> result(arguments.size() > 1
					? context.scheduler.sleep(milliseconds(arguments), nanoseconds(arguments))
					: context.scheduler.sleep(milliseconds(arguments)));
			case "yield" -> voidResult();
			default -> ModelResult.NOT_HANDLED;
		};
	}

	@Override
	@Nonnull
	public ModelResult invokeInstance(@Nonnull MethodInsnNode instruction, @Nonnull ReValue receiver,
	                                  @Nonnull List<ReValue> arguments, @Nonnull EvaluationContext context) {
		EvaluationModelHeap.ThreadState state = context.modelHeap.thread(receiver);
		if (state == null)
			return ModelResult.NOT_HANDLED;
		return switch (instruction.name) {
			case "start" -> {
				// Start the thread, and yield control to the scheduler.
				// - Throws and failures are propagated to the caller (Handled by Evaluator).
				// - Yielding is ignored, as the thread is now running and will be scheduled when the scheduler is next ticked.
				EvaluationResult result = context.scheduler.start(state);
				yield switch (result) {
					case EvaluationThrowsResult thrown -> ModelResult.thrown(thrown.exception());
					case EvaluationFailureResult failure -> ModelResult.failed(failure.reason());
					case EvaluationYieldResult ignored -> voidResult();
				};
			}
			case "run" -> result(context.scheduler.runDirect(state));
			case "join" -> result(context.scheduler.join(state));
			case "isAlive" -> ModelResult.yielded(IntValue.of(state.status == EvaluationModelHeap.ThreadStatus.READY
					|| state.status == EvaluationModelHeap.ThreadStatus.RUNNING
					|| state.status == EvaluationModelHeap.ThreadStatus.WAITING ? 1 : 0));
			case "getName" -> ModelResult.yielded(ObjectValue.string(state.name));
			case "setName" -> {
				if (!arguments.isEmpty() && arguments.getFirst() instanceof StringValue name)
					state.name = name.getText().orElse(state.name);
				yield voidResult();
			}
			case "getId" -> ModelResult.yielded(LongValue.of(state.id));
			case "interrupt" -> {
				state.interrupted = true;
				yield voidResult();
			}
			case "isInterrupted" -> ModelResult.yielded(IntValue.of(state.interrupted ? 1 : 0));
			default -> ModelResult.NOT_HANDLED;
		};
	}

	@Nonnull
	@Override
	public EvaluationResult runTask(@Nonnull EvaluationModelHeap.ThreadState state,
	                                @Nonnull EvaluationContext context) throws Evaluator.UnknownValueException {
		ClassMethodPair run = evaluator.resolveConcreteMethod(state.type, "run", "()V");
		if (run != null)
			return evaluator.evaluate(run.classNode(), run.methodNode(), state.value, List.of(), context);
		if (state.runnable != null)
			return context.invokeCallable(state.runnable, List.of());
		return new EvaluationYieldResult(UninitializedValue.UNINITIALIZED_VALUE);
	}

	@Nonnull
	private static ModelResult result(@Nonnull EvaluationResult result) {
		return switch (result) {
			case EvaluationYieldResult yielded -> voidResult();
			case EvaluationThrowsResult thrown -> ModelResult.thrown(thrown.exception());
			case EvaluationFailureResult failure -> ModelResult.failed(failure.reason());
		};
	}

	@Nonnull
	private static ModelResult voidResult() {
		return ModelResult.yielded(UninitializedValue.UNINITIALIZED_VALUE);
	}

	private static long milliseconds(@Nonnull List<ReValue> arguments) {
		if (arguments.isEmpty() || !(arguments.getFirst() instanceof LongValue value))
			return 0;
		return value.value().orElse(0L);
	}

	private static int nanoseconds(@Nonnull List<ReValue> arguments) {
		return arguments.size() < 2 || !(arguments.get(1) instanceof IntValue value) ? 0 : value.value().orElse(0);
	}
}
