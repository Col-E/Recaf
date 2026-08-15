package software.coley.recaf.util.analysis.eval;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodInsnNode;
import software.coley.recaf.util.analysis.Nullness;
import software.coley.recaf.util.analysis.lookup.InvokeStaticLookup;
import software.coley.recaf.util.analysis.lookup.InvokeVirtualLookup;
import software.coley.recaf.util.analysis.value.ObjectValue;
import software.coley.recaf.util.analysis.value.ReValue;
import software.coley.recaf.util.analysis.value.UninitializedValue;

import java.util.List;

/**
 * Generic dispatch boundary for evaluator models of specific types of operations.
 * <p>
 * Models are used when {@link InvokeStaticLookup} / {@link InvokeVirtualLookup} are
 * insufficient to handle a type of operation. For example, {@link ThreadModel} and {@link CompletableFutureModel}
 * which require more complex state management than a single method invocation.
 *
 * @author Matt Coley
 */
public final class EvaluationModelRegistry {
	private final List<EvaluatorModel> models;

	EvaluationModelRegistry(@Nonnull Evaluator evaluator) {
		models = List.of(new ThreadModel(evaluator), new CompletableFutureModel(), new TimeModel());
	}

	/**
	 * @param type
	 * 		Allocated type.
	 *
	 * @return {@code true} when a model supports allocation of the type,
	 * {@code false} when ordinary allocation should run.
	 */
	public boolean supportsAllocation(@Nonnull String type) {
		return models.stream().anyMatch(model -> model.supportsAllocation(type));
	}

	/**
	 * @param type
	 * 		Allocated type.
	 * @param context
	 * 		Current evaluation context.
	 *
	 * @return Allocated value, or {@code null} when ordinary allocation should run.
	 */
	@Nullable
	public ObjectValue allocate(@Nonnull String type, @Nonnull EvaluationContext context) {
		for (EvaluatorModel model : models) {
			ObjectValue value = model.allocate(type, context);
			if (value != null)
				return value;
		}
		return null;
	}

	/**
	 * @param instruction
	 * 		Constructor invocation.
	 *
	 * @return {@code true} when a model supports the constructor,
	 * {@code false} when ordinary constructor invocation should run.
	 */
	public boolean supportsConstructor(@Nonnull MethodInsnNode instruction) {
		return models.stream().anyMatch(model -> model.supportsConstructor(instruction));
	}

	/**
	 * @param instruction
	 * 		Constructor invocation.
	 * @param receiver
	 * 		Constructor receiver.
	 * @param arguments
	 * 		Constructor arguments.
	 * @param context
	 * 		Current context.
	 *
	 * @return Model dispatch result.
	 */
	@Nonnull
	public ModelResult invokeConstructor(@Nonnull MethodInsnNode instruction, @Nonnull ReValue receiver,
	                                     @Nonnull List<ReValue> arguments, @Nonnull EvaluationContext context) {
		for (EvaluatorModel model : models) {
			if (model.supportsConstructor(instruction))
				return model.invokeConstructor(instruction, receiver, arguments, context);
		}
		return ModelResult.NOT_HANDLED;
	}

	/**
	 * @param instruction
	 * 		Static invocation.
	 *
	 * @return {@code true} when a model supports the static invocation,
	 * {@code false} when ordinary static invocation should run.
	 */
	public boolean supportsStatic(@Nonnull MethodInsnNode instruction) {
		return models.stream().anyMatch(model -> model.supportsStatic(instruction));
	}

	/**
	 * @param instruction
	 * 		Static invocation.
	 * @param arguments
	 * 		Invocation arguments.
	 * @param context
	 * 		Current context.
	 *
	 * @return Model dispatch result.
	 */
	@Nonnull
	public ModelResult invokeStatic(@Nonnull MethodInsnNode instruction, @Nonnull List<ReValue> arguments,
	                                @Nonnull EvaluationContext context) {
		for (EvaluatorModel model : models) {
			if (model.supportsStatic(instruction))
				return model.invokeStatic(instruction, arguments, context);
		}
		return ModelResult.NOT_HANDLED;
	}

	/**
	 * @param instruction
	 * 		Instance invocation.
	 * @param receiver
	 * 		Invocation receiver.
	 *
	 * @return {@code true} when a model supports the instance invocation,
	 * {@code false} when ordinary instance invocation should run.
	 */
	public boolean supportsInstance(@Nonnull MethodInsnNode instruction, @Nonnull ReValue receiver) {
		return models.stream().anyMatch(model -> model.supportsInstance(instruction, receiver));
	}

	/**
	 * @param instruction
	 * 		Instance invocation.
	 *
	 * @return {@code true} when a model supports the instance invocation,
	 * {@code false} when ordinary instance invocation should run.
	 */
	public boolean supportsInstance(@Nonnull MethodInsnNode instruction) {
		ObjectValue receiver = ObjectValue.object(Type.getObjectType(instruction.owner), Nullness.NOT_NULL);
		return models.stream().anyMatch(model -> model.supportsInstance(instruction, receiver));
	}

	/**
	 * @param instruction
	 * 		Instance invocation.
	 * @param receiver
	 * 		Invocation receiver.
	 * @param arguments
	 * 		Invocation arguments.
	 * @param context
	 * 		Current context.
	 *
	 * @return Model dispatch result.
	 */
	@Nonnull
	public ModelResult invokeInstance(@Nonnull MethodInsnNode instruction, @Nonnull ReValue receiver,
	                                  @Nonnull List<ReValue> arguments, @Nonnull EvaluationContext context) {
		for (EvaluatorModel model : models) {
			if (model.supportsInstance(instruction, receiver))
				return model.invokeInstance(instruction, receiver, arguments, context);
		}
		return ModelResult.NOT_HANDLED;
	}

	/**
	 * Handles thread task execution for models that support it.
	 *
	 * @param state
	 * 		Task state.
	 * @param context
	 * 		Shared evaluation context.
	 *
	 * @return Task result.
	 *
	 * @throws Evaluator.UnknownValueException
	 * 		When task evaluation encounters an unknown value.
	 * @see SimulatedScheduler Thread scheduler usage.
	 */
	@Nonnull
	EvaluationResult runTask(@Nonnull EvaluationModelHeap.ThreadState state,
	                         @Nonnull EvaluationContext context) throws Evaluator.UnknownValueException {
		for (EvaluatorModel model : models) {
			EvaluationResult result = model.runTask(state, context);
			if (result != null)
				return result;
		}
		return new EvaluationYieldResult(UninitializedValue.UNINITIALIZED_VALUE);
	}
}