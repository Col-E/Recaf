package software.coley.recaf.util.analysis.eval;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.tree.MethodInsnNode;
import software.coley.recaf.util.analysis.lookup.InvokeStaticLookup;
import software.coley.recaf.util.analysis.lookup.InvokeVirtualLookup;
import software.coley.recaf.util.analysis.value.ObjectValue;
import software.coley.recaf.util.analysis.value.ReValue;

import java.util.List;

/**
 * Contract for a model that specializes evaluation of a type's operations which require
 * more complex state management than the evaluator handles via the simpler stateless lookup models:
 * <ul>
 *     <li>{@link InvokeStaticLookup}</li>
 *     <li>{@link InvokeVirtualLookup}</li>
 * </ul>
 * Models are consulted in registration order by {@link EvaluationModelRegistry}. The first model
 * that reports support for an operation handles it, and a model may still refuse an operation it
 * supports by returning {@link ModelResult#NOT_HANDLED}.
 *
 * @author Matt Coley
 */
interface EvaluatorModel {
	/**
	 * @param type
	 * 		Internal name of the type being allocated.
	 *
	 * @return {@code true} when this model allocates instances of the type,
	 * {@code false} when ordinary allocation should run.
	 */
	boolean supportsAllocation(@Nonnull String type);

	/**
	 * @param instruction
	 * 		Constructor invocation.
	 *
	 * @return {@code true} when this model handles the constructor,
	 * {@code false} when ordinary constructor invocation should run.
	 */
	boolean supportsConstructor(@Nonnull MethodInsnNode instruction);

	/**
	 * @param instruction
	 * 		Static invocation.
	 *
	 * @return {@code true} when this model handles the static invocation,
	 * {@code false} when ordinary static invocation should run.
	 */
	boolean supportsStatic(@Nonnull MethodInsnNode instruction);

	/**
	 * @param instruction
	 * 		Instance invocation.
	 * @param receiver
	 * 		Receiver the invocation targets.
	 *
	 * @return {@code true} when this model handles the instance invocation,
	 * {@code false} when ordinary instance invocation should run.
	 */
	boolean supportsInstance(@Nonnull MethodInsnNode instruction, @Nonnull ReValue receiver);

	/**
	 * Allocates a modeled instance, giving the model a chance to attach its own bookkeeping state.
	 *
	 * @param type
	 * 		Internal name of the type to allocate.
	 * @param context
	 * 		Current evaluation context.
	 *
	 * @return Allocated value, or {@code null} when this model does not allocate the type.
	 */
	@Nullable
	ObjectValue allocate(@Nonnull String type, @Nonnull EvaluationContext context);

	/**
	 * Handles a constructor invocation this model supports.
	 *
	 * @param instruction
	 * 		Constructor invocation.
	 * @param receiver
	 * 		Constructor receiver.
	 * @param arguments
	 * 		Constructor arguments in descriptor order.
	 * @param context
	 * 		Current evaluation context.
	 *
	 * @return Dispatch result, or {@link ModelResult#NOT_HANDLED} when this model refuses after inspection.
	 */
	@Nonnull
	ModelResult invokeConstructor(@Nonnull MethodInsnNode instruction,
	                              @Nonnull ReValue receiver,
	                              @Nonnull List<ReValue> arguments,
	                              @Nonnull EvaluationContext context);

	/**
	 * Handles a static invocation this model supports.
	 *
	 * @param instruction
	 * 		Static invocation.
	 * @param arguments
	 * 		Invocation arguments in descriptor order.
	 * @param context
	 * 		Current evaluation context.
	 *
	 * @return Dispatch result, or {@link ModelResult#NOT_HANDLED} when this model refuses after inspection.
	 */
	@Nonnull
	ModelResult invokeStatic(@Nonnull MethodInsnNode instruction,
	                         @Nonnull List<ReValue> arguments,
	                         @Nonnull EvaluationContext context);

	/**
	 * Handles an instance invocation this model supports.
	 *
	 * @param instruction
	 * 		Instance invocation.
	 * @param receiver
	 * 		Receiver the invocation targets.
	 * @param arguments
	 * 		Invocation arguments in descriptor order.
	 * @param context
	 * 		Current evaluation context.
	 *
	 * @return Dispatch result, or {@link ModelResult#NOT_HANDLED} when this model refuses after inspection.
	 */
	@Nonnull
	ModelResult invokeInstance(@Nonnull MethodInsnNode instruction,
	                           @Nonnull ReValue receiver,
	                           @Nonnull List<ReValue> arguments,
	                           @Nonnull EvaluationContext context);

	/**
	 * Executes the task associated with the given thread state, used by models that simulate concurrency scheduling.
	 *
	 * @param state
	 * 		Thread state whose task should run.
	 * @param context
	 * 		Current evaluation context.
	 *
	 * @return Task result, or {@code null} when this model does not run tasks for the state.
	 *
	 * @throws Evaluator.UnknownValueException
	 * 		When the task depends on a value that cannot be evaluated.
	 */
	@Nullable
	default EvaluationResult runTask(@Nonnull EvaluationModelHeap.ThreadState state,
	                                 @Nonnull EvaluationContext context) throws Evaluator.UnknownValueException {
		// TODO: This being defined in the top-level interface is a bit of a hack.
		//  Its only supported/implemented by ThreadModel.
		return null;
	}
}
