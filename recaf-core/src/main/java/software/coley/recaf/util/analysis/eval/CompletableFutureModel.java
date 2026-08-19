package software.coley.recaf.util.analysis.eval;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.tree.MethodInsnNode;
import software.coley.recaf.util.analysis.value.IntValue;
import software.coley.recaf.util.analysis.value.LongValue;
import software.coley.recaf.util.analysis.value.ObjectValue;
import software.coley.recaf.util.analysis.value.ReValue;
import software.coley.recaf.util.analysis.value.UninitializedValue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Models {@link CompletableFuture} operations.
 *
 * @author Matt Coley
 */
final class CompletableFutureModel implements EvaluatorModel {
	private static final String FUTURE = "java/util/concurrent/CompletableFuture";

	@Override
	public boolean supportsAllocation(@Nonnull String type) {
		return FUTURE.equals(type);
	}

	@Override
	public boolean supportsConstructor(@Nonnull MethodInsnNode instruction) {
		return instruction.owner.equals(FUTURE) && instruction.name.equals("<init>");
	}

	@Override
	public boolean supportsStatic(@Nonnull MethodInsnNode instruction) {
		return instruction.owner.equals(FUTURE) && switch (instruction.name) {
			case "completedFuture", "failedFuture", "supplyAsync", "runAsync" -> true;
			default -> false;
		};
	}

	@Override
	public boolean supportsInstance(@Nonnull MethodInsnNode instruction, @Nonnull ReValue receiver) {
		return receiver instanceof ObjectValue object
				&& FUTURE.equals(object.type().getInternalName());
	}

	@Override
	@Nullable
	public ObjectValue allocate(@Nonnull String type, @Nonnull EvaluationContext context) {
		return supportsAllocation(type) ? context.modelHeap.createFuture().value : null;
	}

	@Override
	@Nonnull
	public ModelResult invokeConstructor(@Nonnull MethodInsnNode instruction, @Nonnull ReValue receiver,
	                                     @Nonnull List<ReValue> arguments, @Nonnull EvaluationContext context) {
		return context.modelHeap.future(receiver) == null
				? ModelResult.failed("CompletableFuture constructor receiver is not model-owned") : voidResult();
	}

	@Override
	@Nonnull
	public ModelResult invokeStatic(@Nonnull MethodInsnNode instruction, @Nonnull List<ReValue> arguments,
	                                @Nonnull EvaluationContext context) {
		return switch (instruction.name) {
			case "completedFuture" -> {
				EvaluationModelHeap.FutureState future = context.modelHeap.createFuture();
				complete(future, arguments.isEmpty() ? ObjectValue.VAL_OBJECT_NULL : arguments.getFirst(), null, context);
				yield ModelResult.yielded(future.value);
			}
			case "failedFuture" -> {
				EvaluationModelHeap.FutureState future = context.modelHeap.createFuture();
				complete(future, null, arguments.isEmpty() ? context.throwable("java/lang/Throwable") : arguments.getFirst(), context);
				yield ModelResult.yielded(future.value);
			}
			case "supplyAsync" -> yieldAsync(arguments, context, false);
			case "runAsync" -> yieldAsync(arguments, context, true);
			default -> ModelResult.NOT_HANDLED;
		};
	}

	@Override
	@Nonnull
	public ModelResult invokeInstance(@Nonnull MethodInsnNode instruction, @Nonnull ReValue receiver,
	                                  @Nonnull List<ReValue> arguments, @Nonnull EvaluationContext context) {
		EvaluationModelHeap.FutureState state = context.modelHeap.future(receiver);
		if (state == null)
			return ModelResult.NOT_HANDLED;
		return switch (instruction.name) {
			case "complete" -> {
				boolean won = state.status == EvaluationModelHeap.FutureStatus.INCOMPLETE;
				if (won)
					complete(state, arguments.isEmpty() ? ObjectValue.VAL_OBJECT_NULL : arguments.getFirst(), null, context);
				yield ModelResult.yielded(IntValue.of(won ? 1 : 0));
			}
			case "completeExceptionally" -> {
				boolean won = state.status == EvaluationModelHeap.FutureStatus.INCOMPLETE;
				if (won)
					complete(state, null, arguments.isEmpty() ? context.throwable("java/lang/Throwable") : arguments.getFirst(), context);
				yield ModelResult.yielded(IntValue.of(won ? 1 : 0));
			}
			case "isDone" ->
					ModelResult.yielded(IntValue.of(state.status != EvaluationModelHeap.FutureStatus.INCOMPLETE ? 1 : 0));
			case "isCompletedExceptionally" ->
					ModelResult.yielded(IntValue.of(state.status == EvaluationModelHeap.FutureStatus.EXCEPTIONAL ? 1 : 0));
			case "isCancelled" ->
					ModelResult.yielded(IntValue.of(state.status == EvaluationModelHeap.FutureStatus.CANCELLED ? 1 : 0));
			case "cancel" -> cancel(state, context);
			case "join" -> join(state);
			case "get" -> arguments.isEmpty() ? join(state) : timedGet(state, arguments, context);
			case "getNow" -> getNow(state, arguments);
			case "orTimeout" -> timeout(state, arguments, context, false);
			case "completeOnTimeout" -> timeout(state, arguments, context, true);
			case "thenApply", "thenApplyAsync" ->
					dependent(state, arguments, EvaluationModelHeap.CompletionKind.APPLY, context);
			case "thenAccept", "thenAcceptAsync" ->
					dependent(state, arguments, EvaluationModelHeap.CompletionKind.ACCEPT, context);
			case "thenRun", "thenRunAsync" ->
					dependent(state, arguments, EvaluationModelHeap.CompletionKind.RUN, context);
			case "thenCompose", "thenComposeAsync" ->
					dependent(state, arguments, EvaluationModelHeap.CompletionKind.COMPOSE, context);
			case "exceptionally", "exceptionallyAsync" ->
					dependent(state, arguments, EvaluationModelHeap.CompletionKind.EXCEPTIONALLY, context);
			case "handle", "handleAsync" ->
					dependent(state, arguments, EvaluationModelHeap.CompletionKind.HANDLE, context);
			default -> ModelResult.NOT_HANDLED;
		};
	}

	@Nonnull
	private static ModelResult yieldAsync(@Nonnull List<ReValue> arguments, @Nonnull EvaluationContext context, boolean voidTask) {
		// Must have a modeled callable to run in the async task.
		if (arguments.isEmpty() || !(arguments.getFirst() instanceof InvokeDynamicExecutor.EvaluatedLambdaValue lambda))
			return ModelResult.failed("Async operation requires a modeled callable");

		// Set up a future to represent the async task, and a thread to run it.
		EvaluationModelHeap.FutureState future = context.modelHeap.createFuture();
		EvaluationModelHeap.ThreadState task = context.modelHeap.createThread("java/lang/Thread", context.currentThread);
		task.runnable = lambda;

		// Execute the task, and handle the result.
		EvaluationResult result = context.scheduler.start(task);
		switch (result) {
			case EvaluationYieldResult(ReValue value) -> {
				complete(future, voidTask ? UninitializedValue.UNINITIALIZED_VALUE : value, null, context);
				return ModelResult.yielded(future.value);
			}
			case EvaluationThrowsResult(ReValue exception) -> {
				complete(future, null, exception, context);
				return ModelResult.yielded(future.value);
			}
			case EvaluationFailureResult failure -> {
				return ModelResult.failed(failure.reason());
			}
			default -> {
				return ModelResult.yielded(future.value);
			}
		}
	}

	@Nonnull
	private static ModelResult dependent(@Nonnull EvaluationModelHeap.FutureState source, @Nonnull List<ReValue> arguments,
	                                     @Nonnull EvaluationModelHeap.CompletionKind kind,
	                                     @Nonnull EvaluationContext context) {
		// Must have a modeled callable to run in the async task.
		if (arguments.isEmpty() || !(arguments.getFirst() instanceof InvokeDynamicExecutor.EvaluatedLambdaValue lambda))
			return ModelResult.failed("Future dependent operation requires a modeled callable");

		// Set up a future to represent the dependent task, and register it as a dependent of the source future.
		EvaluationModelHeap.FutureState target = context.modelHeap.createFuture();
		source.dependents.add(new EvaluationModelHeap.Dependent(lambda, target, kind));
		if (source.status != EvaluationModelHeap.FutureStatus.INCOMPLETE)
			trigger(source, context);
		return ModelResult.yielded(target.value);
	}

	@Nonnull
	private static ModelResult cancel(@Nonnull EvaluationModelHeap.FutureState state, @Nonnull EvaluationContext context) {
		// Cancellation is only possible if the future is still incomplete.
		if (state.status != EvaluationModelHeap.FutureStatus.INCOMPLETE)
			return ModelResult.yielded(IntValue.of(0));

		// Cancel the future and trigger dependents.
		state.status = EvaluationModelHeap.FutureStatus.CANCELLED;
		state.exception = context.throwable("java/util/concurrent/CancellationException");
		trigger(state, context);
		return ModelResult.yielded(IntValue.of(1));
	}

	@Nonnull
	private static ModelResult join(@Nonnull EvaluationModelHeap.FutureState state) {
		if (state.status == EvaluationModelHeap.FutureStatus.INCOMPLETE)
			return ModelResult.failed("Simulated future cannot complete");

		if (state.status == EvaluationModelHeap.FutureStatus.EXCEPTIONAL || state.status == EvaluationModelHeap.FutureStatus.CANCELLED)
			return ModelResult.thrown(state.exception);

		return ModelResult.yielded(state.result == null ? UninitializedValue.UNINITIALIZED_VALUE : state.result);
	}

	private static ModelResult timedGet(@Nonnull EvaluationModelHeap.FutureState state,
	                                    @Nonnull List<ReValue> arguments,
	                                    @Nonnull EvaluationContext context) {
		if (state.status == EvaluationModelHeap.FutureStatus.INCOMPLETE
				&& !arguments.isEmpty()
				&& arguments.getFirst() instanceof LongValue timeout) {
			long millis = timeout.value().orElse(0L);
			if (millis > 0)
				context.clock.advance(Math.min(Long.MAX_VALUE / 1_000_000L, millis) * 1_000_000L);
			if (state.status == EvaluationModelHeap.FutureStatus.INCOMPLETE)
				return ModelResult.thrown(context.throwable("java/util/concurrent/TimeoutException"));
		}
		return join(state);
	}

	@Nonnull
	private static ModelResult getNow(@Nonnull EvaluationModelHeap.FutureState state,
	                                  @Nonnull List<ReValue> arguments) {
		if (state.status == EvaluationModelHeap.FutureStatus.INCOMPLETE)
			return ModelResult.yielded(!arguments.isEmpty() ? arguments.getFirst() : ObjectValue.VAL_OBJECT_NULL);
		return join(state);
	}

	@Nonnull
	private static ModelResult timeout(@Nonnull EvaluationModelHeap.FutureState state,
	                                   @Nonnull List<ReValue> arguments,
	                                   @Nonnull EvaluationContext context,
	                                   boolean completeWithFallback) {
		if (state.status == EvaluationModelHeap.FutureStatus.INCOMPLETE) {
			int timeoutIndex = completeWithFallback ? 1 : 0;
			long millis = arguments.size() > timeoutIndex
					&& arguments.get(timeoutIndex) instanceof LongValue timeout
					? timeout.value().orElse(0L) : 0L;
			if (millis > 0)
				context.clock.advance(Math.min(Long.MAX_VALUE / 1_000_000L, millis) * 1_000_000L);
			if (state.status == EvaluationModelHeap.FutureStatus.INCOMPLETE) {
				if (completeWithFallback)
					complete(state, arguments.isEmpty() ? ObjectValue.VAL_OBJECT_NULL : arguments.getFirst(), null, context);
				else
					complete(state, null, context.throwable("java/util/concurrent/TimeoutException"), context);
			}
		}
		return ModelResult.yielded(state.value);
	}

	private static void complete(@Nonnull EvaluationModelHeap.FutureState state, @Nullable ReValue result,
	                             @Nullable ReValue exception, @Nonnull EvaluationContext context) {
		if (state.status != EvaluationModelHeap.FutureStatus.INCOMPLETE)
			return;
		if (exception != null) {
			state.status = EvaluationModelHeap.FutureStatus.EXCEPTIONAL;
			state.exception = exception;
		} else {
			state.status = EvaluationModelHeap.FutureStatus.SUCCESS;
			state.result = result == null ? UninitializedValue.UNINITIALIZED_VALUE : result;
		}
		trigger(state, context);
	}

	private static void trigger(@Nonnull EvaluationModelHeap.FutureState source, @Nonnull EvaluationContext context) {
		List<EvaluationModelHeap.Dependent> dependents = new ArrayList<>(source.dependents);
		source.dependents.clear();
		for (EvaluationModelHeap.Dependent dependent : dependents) {
			if (dependent.callable == null) {
				copy(source, dependent.target, context);
				continue;
			}
			boolean exceptional = source.status == EvaluationModelHeap.FutureStatus.EXCEPTIONAL
					|| source.status == EvaluationModelHeap.FutureStatus.CANCELLED;
			if (exceptional && dependent.kind != EvaluationModelHeap.CompletionKind.EXCEPTIONALLY
					&& dependent.kind != EvaluationModelHeap.CompletionKind.HANDLE) {
				complete(dependent.target, null, source.exception, context);
				continue;
			}
			if (!exceptional && dependent.kind == EvaluationModelHeap.CompletionKind.EXCEPTIONALLY) {
				complete(dependent.target, source.result, null, context);
				continue;
			}

			List<ReValue> args = switch (dependent.kind) {
				case APPLY, COMPOSE, EXCEPTIONALLY -> List.of(exceptional ? source.exception : source.result);
				case ACCEPT -> List.of(source.result);
				case RUN -> List.of();
				case HANDLE -> List.of(exceptional ? ObjectValue.VAL_OBJECT_NULL : source.result,
						exceptional ? source.exception : ObjectValue.VAL_OBJECT_NULL);
			};
			EvaluationResult callback = context.invokeCallable(dependent.callable, args);
			if (callback instanceof EvaluationYieldResult(ReValue value)) {
				if (dependent.kind == EvaluationModelHeap.CompletionKind.COMPOSE) {
					EvaluationModelHeap.FutureState inner = context.modelHeap.future(value);
					if (inner == null) {
						complete(dependent.target, null, context.throwable("java/lang/ClassCastException"), context);
					} else if (inner.status == EvaluationModelHeap.FutureStatus.INCOMPLETE) {
						inner.dependents.add(new EvaluationModelHeap.Dependent(null, dependent.target,
								EvaluationModelHeap.CompletionKind.COMPOSE));
					} else {
						copy(inner, dependent.target, context);
					}
				} else {
					complete(dependent.target,
							dependent.kind == EvaluationModelHeap.CompletionKind.ACCEPT
									|| dependent.kind == EvaluationModelHeap.CompletionKind.RUN
									? UninitializedValue.UNINITIALIZED_VALUE : value, null, context);
				}
			} else if (callback instanceof EvaluationThrowsResult(ReValue exception)) {
				complete(dependent.target, null, exception, context);
			} else if (callback instanceof EvaluationFailureResult failure) {
				complete(dependent.target, null, context.throwable("java/lang/IllegalStateException"), context);
			}
		}
	}

	private static void copy(@Nonnull EvaluationModelHeap.FutureState source,
	                         @Nonnull EvaluationModelHeap.FutureState target,
	                         @Nonnull EvaluationContext context) {
		if (source.status == EvaluationModelHeap.FutureStatus.SUCCESS)
			complete(target, source.result, null, context);
		else
			complete(target, null, source.exception, context);
	}

	@Nonnull
	private static ModelResult voidResult() {
		return ModelResult.yielded(UninitializedValue.UNINITIALIZED_VALUE);
	}
}
