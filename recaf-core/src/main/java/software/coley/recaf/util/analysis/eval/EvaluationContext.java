package software.coley.recaf.util.analysis.eval;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import software.coley.recaf.util.ClassMethodPair;
import software.coley.recaf.util.analysis.value.ReValue;
import software.coley.recaf.util.analysis.value.ThrowableValue;
import software.coley.recaf.util.analysis.value.impl.ThrowableValueImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * State shared by every nested operation in one top-level evaluation.
 *
 * @author Matt Coley
 */
public final class EvaluationContext {
	final List<ClassMethodPair> callStack;
	final Set<String> initializedClasses;
	final Set<String> initializingClasses;
	final Map<String, EvaluationResult> failedClassInitializers;
	final EvaluationModelHeap modelHeap;
	final VirtualClock clock;
	final SimulatedScheduler scheduler;
	final EvaluationModelRegistry models;
	int stepAllocation;
	EvaluationModelHeap.ThreadState currentThread;

	EvaluationContext(@Nonnull Evaluator evaluator, int stepAllocation) {
		this(evaluator, stepAllocation, null);
	}

	EvaluationContext(@Nonnull Evaluator evaluator, int stepAllocation, @Nullable List<ClassMethodPair> callStackSeed) {
		this.stepAllocation = stepAllocation;
		callStack = callStackSeed == null ? new ArrayList<>() : new ArrayList<>(callStackSeed);
		initializedClasses = new HashSet<>();
		initializingClasses = new HashSet<>();
		failedClassInitializers = new HashMap<>();
		modelHeap = new EvaluationModelHeap();
		clock = new VirtualClock();
		models = new EvaluationModelRegistry(evaluator);
		scheduler = new SimulatedScheduler(this, evaluator);
		currentThread = modelHeap.createMainThread();
	}

	/**
	 * Invokes a retained evaluator lambda through the normal workspace path.
	 *
	 * @param lambda
	 * 		Lambda to invoke.
	 * @param arguments
	 * 		SAM arguments in descriptor order.
	 *
	 * @return Nested evaluator result.
	 */
	@Nonnull
	EvaluationResult invokeCallable(@Nonnull InvokeDynamicExecutor.EvaluatedLambdaValue lambda,
	                                @Nonnull List<ReValue> arguments) {
		return scheduler.invokeCallable(lambda, arguments);
	}

	/**
	 * Creates a throwable carrying the current evaluator stack.
	 *
	 * @param internalName
	 * 		Throwable internal name.
	 *
	 * @return Evaluator throwable value.
	 */
	@Nonnull
	ThrowableValue throwable(@Nonnull String internalName) {
		return new ThrowableValueImpl(Type.getObjectType(internalName), stackTrace(), null);
	}

	/**
	 * @return Current root-relative evaluator stack in throwable order.
	 */
	@Nonnull
	List<StackTraceElement> stackTrace() {
		List<StackTraceElement> trace = new ArrayList<>(callStack.size());
		for (int i = callStack.size() - 1; i >= 0; i--) {
			ClassMethodPair pair = callStack.get(i);
			ClassNode classNode = pair.classNode();
			MethodNode methodNode = pair.methodNode();
			trace.add(new StackTraceElement(classNode.name.replace('/', '.'), methodNode.name, null, -1));
		}
		return trace;
	}
}
