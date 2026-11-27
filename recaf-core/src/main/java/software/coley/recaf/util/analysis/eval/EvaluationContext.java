package software.coley.recaf.util.analysis.eval;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodNode;
import software.coley.recaf.util.ClassMethodPair;
import software.coley.recaf.util.analysis.eval.model.EvaluationModelHeap;
import software.coley.recaf.util.analysis.eval.model.EvaluationModelRegistry;
import software.coley.recaf.util.analysis.value.ReValue;
import software.coley.recaf.util.analysis.value.ThrowableValue;
import software.coley.recaf.util.analysis.value.impl.ThrowableValueImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * State shared by every nested operation in one top-level evaluation.
 *
 * @author Matt Coley
 */
public final class EvaluationContext {
	private final List<ClassMethodPair> callStack;
	private final Set<String> initializedClasses;
	private final Set<String> initializingClasses;
	private final Map<String, EvaluationResult> failedClassInitializers;
	private final Map<String, ClassNode> classNodeOverrides;
	private final Map<String, ClassNode> workspaceNodeCache;
	private final Map<FieldInsnNode, String> fieldOwners;
	private final EvaluationModelHeap modelHeap;
	private final VirtualClock clock;
	private final SimulatedScheduler scheduler;
	private final EvaluationModelRegistry models;
	private final int callStackSeedSize;
	private int stepAllocation;
	private EvaluationModelHeap.ThreadState currentThread;

	EvaluationContext(@Nonnull Evaluator evaluator, int stepAllocation) {
		this(evaluator, stepAllocation, null);
	}

	EvaluationContext(@Nonnull Evaluator evaluator, int stepAllocation, @Nullable List<ClassMethodPair> callStackSeed) {
		this.stepAllocation = stepAllocation;
		callStack = callStackSeed == null ? new ArrayList<>() : new ArrayList<>(callStackSeed);
		callStackSeedSize = callStack.size();
		initializedClasses = new HashSet<>();
		initializingClasses = new HashSet<>();
		failedClassInitializers = new HashMap<>();
		classNodeOverrides = new HashMap<>(evaluator.getClassNodeOverrides()); // Snapshot overrides so later caller changes cannot switch bytecode halfway through an evaluation.
		workspaceNodeCache = new HashMap<>(); // Cache, as constant re-reads from the workspace can be expensive.
		fieldOwners = new IdentityHashMap<>(); // Cache, as finding the class declaring a field takes a workspace lookup for each class searched.
		modelHeap = new EvaluationModelHeap();
		clock = new VirtualClock();
		models = new EvaluationModelRegistry(evaluator);
		scheduler = new SimulatedScheduler(this, evaluator);
		currentThread = modelHeap.createMainThread();
	}

	/**
	 * @return Call stack of the current evaluation, including any seeded frames.
	 */
	@Nonnull
	public List<ClassMethodPair> getCallStack() {
		return callStack;
	}

	/**
	 * @return Number of seeded frames at the start of the call stack, which do not count towards the call depth limit.
	 */
	public int getCallStackSeedSize() {
		return callStackSeedSize;
	}

	/**
	 * @return Set of classes whose static initializers have completed.
	 */
	@Nonnull
	public Set<String> getInitializedClasses() {
		return initializedClasses;
	}

	/**
	 * @return Set of classes whose static initializers are currently running.
	 */
	@Nonnull
	public Set<String> getInitializingClasses() {
		return initializingClasses;
	}

	/**
	 * @return Map of classes to the failure result of their static initializer.
	 */
	@Nonnull
	public Map<String, EvaluationResult> getFailedClassInitializers() {
		return failedClassInitializers;
	}

	/**
	 * @return Map of class node overrides for this evaluation.
	 */
	@Nonnull
	public Map<String, ClassNode> getClassNodeOverrides() {
		return classNodeOverrides;
	}

	/**
	 * @return Map of cached class nodes read from the workspace.
	 */
	@Nonnull
	public Map<String, ClassNode> getWorkspaceNodeCache() {
		return workspaceNodeCache;
	}

	/**
	 * @return Map of cached resolved field owners.
	 */
	@Nonnull
	public Map<FieldInsnNode, String> getFieldOwners() {
		return fieldOwners;
	}

	/**
	 * @return Identity-based heap of model-owned concurrency objects.
	 */
	@Nonnull
	public EvaluationModelHeap getModelHeap() {
		return modelHeap;
	}

	/**
	 * @return Virtual clock of this evaluation.
	 */
	@Nonnull
	public VirtualClock getClock() {
		return clock;
	}

	/**
	 * @return Scheduler for simulated threads and futures.
	 */
	@Nonnull
	public SimulatedScheduler getScheduler() {
		return scheduler;
	}

	/**
	 * @return Registry of evaluator models.
	 */
	@Nonnull
	public EvaluationModelRegistry getModels() {
		return models;
	}

	/**
	 * @return Remaining instruction steps available to the evaluation.
	 */
	public int getStepAllocation() {
		return stepAllocation;
	}

	/**
	 * Consumes one instruction step. Callers must check {@link #getStepAllocation()} is positive first.
	 */
	public void consumeStep() {
		stepAllocation--;
	}

	/**
	 * @return Simulated thread currently executing.
	 */
	@Nonnull
	public EvaluationModelHeap.ThreadState getCurrentThread() {
		return currentThread;
	}

	/**
	 * @param currentThread
	 * 		Simulated thread that is now executing.
	 */
	public void setCurrentThread(@Nonnull EvaluationModelHeap.ThreadState currentThread) {
		this.currentThread = currentThread;
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
	public EvaluationResult invokeCallable(@Nonnull InvokeDynamicExecutor.EvaluatedLambdaValue lambda,
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
	public ThrowableValue throwable(@Nonnull String internalName) {
		return new ThrowableValueImpl(Type.getObjectType(internalName), stackTrace(), null);
	}

	/**
	 * @return Current root-relative evaluator stack in throwable order.
	 */
	@Nonnull
	public List<StackTraceElement> stackTrace() {
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
