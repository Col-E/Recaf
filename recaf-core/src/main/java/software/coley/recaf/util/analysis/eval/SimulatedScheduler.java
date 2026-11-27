package software.coley.recaf.util.analysis.eval;

import jakarta.annotation.Nonnull;
import software.coley.recaf.util.analysis.eval.model.EvaluationModelHeap;
import software.coley.recaf.util.analysis.value.ReValue;
import software.coley.recaf.util.analysis.value.UninitializedValue;

import java.util.List;

/**
 * Eager deterministic scheduler for simulated threads and futures.
 *
 * @author Matt Coley
 */
public final class SimulatedScheduler {
	private final EvaluationContext context;
	private final Evaluator evaluator;

	SimulatedScheduler(@Nonnull EvaluationContext context, @Nonnull Evaluator evaluator) {
		this.context = context;
		this.evaluator = evaluator;
	}

	/**
	 * Starts a thread exactly once and drains its task immediately.
	 *
	 * @param state
	 * 		Thread to start.
	 *
	 * @return Task result, including modeled throws and failures.
	 */
	@Nonnull
	public EvaluationResult start(@Nonnull EvaluationModelHeap.ThreadState state) {
		// Check if already started, and if so, throw an exception to match the JVM's behavior.
		if (state.isStarted())
			return new EvaluationThrowsResult(context.throwable("java/lang/IllegalThreadStateException"));
		state.setStarted(true);
		state.setStatus(EvaluationModelHeap.ThreadStatus.READY);
		return run(state);
	}

	/**
	 * Runs a thread task under its simulated identity.
	 *
	 * @param state
	 * 		Thread task.
	 *
	 * @return Task result.
	 */
	@Nonnull
	private EvaluationResult run(@Nonnull EvaluationModelHeap.ThreadState state) {
		EvaluationModelHeap.ThreadState previous = context.getCurrentThread();

		// Mark the thread as running and set it as the current thread for the duration of its execution.
		state.setStatus(EvaluationModelHeap.ThreadStatus.RUNNING);
		context.setCurrentThread(state);
		try {
			EvaluationResult result;
			try {
				result = context.getModels().runTask(state, context);
			} catch (Evaluator.UnknownValueException e) {
				result = EvaluationResult.cannotEvaluate("Unknown value while executing simulated thread", e);
			}

			// Retain erroneous completion so a later Thread.join() can observe the task's failure.
			state.setFailure(result instanceof EvaluationYieldResult ? null : result);
			state.setStatus(result instanceof EvaluationFailureResult || result instanceof EvaluationThrowsResult
					? EvaluationModelHeap.ThreadStatus.FAILED : EvaluationModelHeap.ThreadStatus.TERMINATED);
			return result;
		} finally {
			// Resume the previous thread.
			context.setCurrentThread(previous);
		}
	}

	/**
	 * Executes direct {@code run()} without changing lifecycle state or current-thread identity.
	 *
	 * @param state
	 * 		Thread whose target should run.
	 *
	 * @return Direct invocation result.
	 */
	@Nonnull
	public EvaluationResult runDirect(@Nonnull EvaluationModelHeap.ThreadState state) {
		try {
			return context.getModels().runTask(state, context);
		} catch (Evaluator.UnknownValueException e) {
			return EvaluationResult.cannotEvaluate("Unknown value while executing simulated thread", e);
		}
	}

	/**
	 * Executes a deferred callable while preserving the current simulated thread.
	 *
	 * @param lambda
	 * 		Callable to invoke.
	 * @param arguments
	 * 		SAM arguments.
	 *
	 * @return Callable result.
	 */
	@Nonnull
	public EvaluationResult invokeCallable(@Nonnull InvokeDynamicExecutor.EvaluatedLambdaValue lambda,
	                                       @Nonnull List<ReValue> arguments) {
		try {
			return evaluator.evaluateLambda(lambda, arguments, context);
		} catch (Evaluator.UnknownValueException e) {
			return EvaluationResult.cannotEvaluate("Unknown value while invoking simulated callable", e);
		}
	}

	/**
	 * Joins a simulated thread.
	 * <p>
	 * Ok, technically this is a <i>"join"</i> in the sense that it waits for a thread to complete,
	 * but since this is an eager scheduler, the thread has <i>already completed</i> by the time this method is called.
	 * So this method just checks the retained result of the thread and returns it.
	 *
	 * @param state
	 * 		Thread being joined.
	 *
	 * @return Retained task failure, or a void result ({@link UninitializedValue}) after completion or for a new thread.
	 */
	@Nonnull
	public EvaluationResult join(@Nonnull EvaluationModelHeap.ThreadState state) {
		// If the thread has not been started, then joining it is a no-op.
		if (!state.isStarted())
			return new EvaluationYieldResult(UninitializedValue.UNINITIALIZED_VALUE);

		// Eager execution has already reached a terminal state by this synchronization point.
		return state.getFailure() == null
				? new EvaluationYieldResult(UninitializedValue.UNINITIALIZED_VALUE)
				: state.getFailure();
	}

	/**
	 * Advances virtual time for a blocking duration.
	 *
	 * @param millis
	 * 		Duration in milliseconds.
	 *
	 * @return Normal void result, or a modeled argument failure.
	 */
	@Nonnull
	public EvaluationResult sleep(long millis) {
		if (millis < 0)
			return new EvaluationThrowsResult(context.throwable("java/lang/IllegalArgumentException"));
		if (millis != 0)
			context.getClock().advance(Math.min(Long.MAX_VALUE / 1_000_000L, millis) * 1_000_000L);
		return new EvaluationYieldResult(software.coley.recaf.util.analysis.value.UninitializedValue.UNINITIALIZED_VALUE);
	}

	/**
	 * Advances virtual time for the millisecond/nanosecond sleep overload.
	 *
	 * @param millis
	 * 		Millisecond duration.
	 * @param nanos
	 * 		Additional nanoseconds from zero through 999999.
	 *
	 * @return Normal void result, or a modeled argument failure.
	 */
	@Nonnull
	public EvaluationResult sleep(long millis, int nanos) {
		// Validate nanos.
		if (nanos < 0 || nanos > 999_999)
			return new EvaluationThrowsResult(context.throwable("java/lang/IllegalArgumentException"));

		// Sleep the milliseconds first, then advance the nanoseconds.
		EvaluationResult result = sleep(millis);
		if (result instanceof EvaluationThrowsResult)
			return result;
		if (nanos != 0)
			context.getClock().advance(nanos);

		return result;
	}
}
