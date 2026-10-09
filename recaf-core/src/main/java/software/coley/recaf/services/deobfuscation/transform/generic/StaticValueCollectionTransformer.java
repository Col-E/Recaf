package software.coley.recaf.services.deobfuscation.transform.generic;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.Frame;
import software.coley.collections.Unchecked;
import software.coley.recaf.analytics.logging.DebuggingLogger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.services.inheritance.InheritanceGraph;
import software.coley.recaf.services.inheritance.InheritanceGraphService;
import software.coley.recaf.services.transform.CollectionTransformer;
import software.coley.recaf.services.transform.JvmClassTransformer;
import software.coley.recaf.services.transform.JvmTransformerContext;
import software.coley.recaf.services.transform.TransformationException;
import software.coley.recaf.services.transform.TransformationParameter;
import software.coley.recaf.util.ClassMethodPair;
import software.coley.recaf.util.AsmInsnUtil;
import software.coley.recaf.util.analysis.Nullness;
import software.coley.recaf.util.analysis.ReAnalyzer;
import software.coley.recaf.util.analysis.ReFrame;
import software.coley.recaf.util.analysis.ReInterpreter;
import software.coley.recaf.util.analysis.eval.EvaluationListener;
import software.coley.recaf.util.analysis.eval.EvaluationResult;
import software.coley.recaf.util.analysis.eval.EvaluationYieldResult;
import software.coley.recaf.util.analysis.eval.Evaluator;
import software.coley.recaf.util.analysis.eval.FieldCache;
import software.coley.recaf.util.analysis.eval.FieldCacheManager;
import software.coley.recaf.util.analysis.eval.InstancedObjectValue;
import software.coley.recaf.util.analysis.lookup.GetStaticLookup;
import software.coley.recaf.util.analysis.value.ArrayValue;
import software.coley.recaf.util.analysis.value.IllegalValueException;
import software.coley.recaf.util.analysis.value.ReValue;
import software.coley.recaf.util.analysis.value.UninitializedValue;
import software.coley.recaf.util.analysis.value.impl.ArrayValueImpl;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A transformer that collects values of {@code static final} field assignments.
 * <ul>
 *     <li>Intended to be used in combination with {@link StaticValueInliningTransformer}.</li>
 *     <li>Can also be used as a {@link GetStaticLookup}</li>
 * </ul>
 *
 * @author Matt Coley
 */
@Dependent
public class StaticValueCollectionTransformer implements JvmClassTransformer, CollectionTransformer, GetStaticLookup {
	public static final String IDENTIFIER = "peephole.data.staticcollect";
	public static final String KEY_MAX_STEPS = IDENTIFIER + ".max-steps";

	private static final int DEFAULT_MAX_STEPS = 20_000;
	private static final TransformationParameter<Integer> MAX_STEPS_PARAMETER =
			new TransformationParameter<>(KEY_MAX_STEPS, int.class, DEFAULT_MAX_STEPS);

	private static final DebuggingLogger logger = Logging.get(StaticValueCollectionTransformer.class);

	private final Map<String, StaticValues> classValues = new ConcurrentHashMap<>();
	private final Map<String, InitializerInfo> initializers = new ConcurrentHashMap<>();
	private final Set<String> mutableArrays = ConcurrentHashMap.newKeySet();
	private final InheritanceGraphService graphService;
	private InheritanceGraph inheritanceGraph;

	@Inject
	public StaticValueCollectionTransformer(@Nonnull InheritanceGraphService graphService) {
		this.graphService = graphService;
	}

	/**
	 * @param className
	 * 		Name of class defining the field.
	 * @param fieldName
	 * 		Field name.
	 * @param fieldDesc
	 * 		Field descriptor.
	 *
	 * @return Static value wrapper if known, otherwise {@code null}.
	 */
	@Nullable
	public ReValue getStaticValue(@Nonnull String className, @Nonnull String fieldName, @Nonnull String fieldDesc) {
		// A final array reference only means it cannot be re-assigned.
		// The array contents are still mutable, so we cannot treat the array as a constant.
		if (fieldDesc.startsWith("[") && mutableArrays.contains(arrayKey(className, fieldName, fieldDesc)))
			return null;

		// Otherwise, if the field is final, and we have a known value, return it.
		StaticValues values = classValues.get(className);
		if (values == null)
			return null;
		return values.get(fieldName, fieldDesc);
	}

	/**
	 * Reading a static field of a class initializes the class, which can have effects visible
	 * outside the class. Replacing such a read with a constant would then remove those effects.
	 *
	 * @param className
	 * 		Name of a class to check, which may have been collected by this transformer.
	 *
	 * @return {@code true} when initializing the class or any of its parents may have effects other
	 * than assigning the class's own static fields. Also {@code true} when this cannot be determined.
	 */
	public boolean isInitializationObservable(@Nonnull String className) {
		Set<String> visited = new HashSet<>();
		String current = className;
		while (current != null && !current.startsWith("java/")) {
			InitializerInfo info = initializers.get(current);
			if (info == null || info.observable() || !visited.add(current))
				return true;
			current = info.superName();
		}
		return false;
	}

	@Nonnull
	@Override
	public ReValue get(@Nonnull FieldInsnNode field) {
		ReValue value = getStaticValue(field.owner, field.name, field.desc);
		if (value == null) {
			try {
				return Objects.requireNonNull(ReValue.ofType(Type.getType(field.desc), Nullness.UNKNOWN));
			} catch (Exception ex) {
				// Should never fail since fields cannot have illegal types like primitive void.
				throw new IllegalStateException(ex);
			}
		}
		return value;
	}

	@Override
	public boolean hasLookup(@Nonnull FieldInsnNode field) {
		return getStaticValue(field.owner, field.name, field.desc) != null;
	}

	@Override
	public void setup(@Nonnull JvmTransformerContext context, @Nonnull Workspace workspace) {
		inheritanceGraph = graphService.getOrCreateInheritanceGraph(workspace);
	}

	@Override
	public void transform(@Nonnull JvmTransformerContext context, @Nonnull Workspace workspace,
	                      @Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle,
	                      @Nonnull JvmClassInfo initialClassState) throws TransformationException {
		int maxSteps = context.getParameters().getInt(KEY_MAX_STEPS, DEFAULT_MAX_STEPS);

		StaticValues valuesContainer = new StaticValues();
		EffectivelyFinalFields finalContainer = new EffectivelyFinalFields();
		String className = initialClassState.getName();

		// Remove a previous pass's result before rebuilding this class's finalized state.
		classValues.remove(className);

		// TODO: Make some parameters for this
		//  - Option to make unsafe assumptions
		//    - treat all effectively final candidates as actually final
		//  - Option to scan other classes for references to our fields to have more thorough 'effective-final' checking
		//    - will be slower, but it will be opt-in and off by default

		// Populate initial values based on field's default value attribute.
		for (FieldMember field : initialClassState.getFields()) {
			if (!field.hasStaticModifier())
				continue;

			// Add to effectively-final container if it is 'static final'
			// If the field is private add it to the "maybe" effectively-final list, and we'll confirm it later
			if (field.hasFinalModifier())
				finalContainer.add(field.getName(), field.getDescriptor());
			else if (field.hasPrivateModifier())
				finalContainer.addMaybe(field.getName(), field.getDescriptor());

			// TODO: As mentioned above, we can add another 'else' case here for non-private fields
			//  but then we need to make sure no other classes write to those fields. So its more computational work...

			// Skip if there is no default value.
			Object defaultValue = field.getDefaultValue();
			if (defaultValue == null)
				continue;

			// Skip if the value cannot be mapped to our representation.
			ReValue mappedValue = Unchecked.getOr(() -> ReValue.ofConstant(defaultValue), null);
			if (mappedValue == null)
				continue;

			// Store the value.
			valuesContainer.put(field.getName(), field.getDescriptor(), mappedValue);
		}

		// Build a map of methods by their key for same-class traversal.
		ClassNode node = context.getNode(bundle, initialClassState);
		Map<String, MethodNode> methodsByKey = new HashMap<>();
		MethodNode clinit = null;
		for (MethodNode method : node.methods) {
			methodsByKey.put(key(method.name, method.desc), method);
			if ((method.access & Opcodes.ACC_STATIC) != 0 && method.name.equals("<clinit>") && method.desc.equals("()V"))
				clinit = method;
		}

		// Find private static helpers reachable from the class initializer.
		Set<String> initializationHelpers = new HashSet<>();
		Set<String> unresolvedHelperEdges = new HashSet<>();
		if (clinit != null)
			collectInitializationHelpers(clinit, className, methodsByKey, initializationHelpers, new HashSet<>(), unresolvedHelperEdges);

		// Find methods reachable from non-private entry points so helper writes remain conservative.
		Set<String> externallyReachableMethods = new HashSet<>();
		for (MethodNode method : node.methods) {
			if (!method.name.equals("<clinit>") && (method.access & Opcodes.ACC_PRIVATE) == 0)
				collectExternalMethods(method, className, methodsByKey, externallyReachableMethods, new HashSet<>());
		}

		// Find static arrays that are used in ways that allow their contents to change after initialization.
		// Uses by the initializer, and helpers only it can reach, are what populate the array in the first place.
		// Any other use must be a direct read, since something else could otherwise hold on to the reference.
		for (MethodNode method : node.methods) {
			if (method.instructions == null)
				continue;
			String methodKey = key(method.name, method.desc);
			boolean isInitialization = method == clinit
					|| (initializationHelpers.contains(methodKey) && !externallyReachableMethods.contains(methodKey));
			for (AbstractInsnNode instruction : method.instructions) {
				// Must be an array.
				if (instruction.getOpcode() != Opcodes.GETSTATIC
						|| !(instruction instanceof FieldInsnNode fieldInsn)
						|| !fieldInsn.desc.startsWith("["))
					continue;

				// If the array is used by the initializer, or any reachable helper,
				// then this is probably part of initialization.
				if (isInitialization && fieldInsn.owner.equals(className))
					continue;

				// Otherwise, the array mutation is happening outside of initialization, so its effectively mutable.
				if (!isReadOnlyArrayUse(method, fieldInsn))
					mutableArrays.add(arrayKey(fieldInsn.owner, fieldInsn.name, fieldInsn.desc));
			}
		}

		// Track every non-initializer writer and count direct initializer writes before analysis.
		Map<String, Set<String>> methodWrites = new HashMap<>();
		Map<String, Integer> directInitializerWrites = new HashMap<>();
		for (MethodNode method : node.methods) {
			if (method.instructions == null)
				continue;
			boolean isClinit = method == clinit;
			String methodKey = key(method.name, method.desc);
			for (AbstractInsnNode instruction : method.instructions) {
				if (instruction.getOpcode() != Opcodes.PUTSTATIC || !(instruction instanceof FieldInsnNode fieldInsn)
						|| !fieldInsn.owner.equals(className))
					continue;

				String fieldName = fieldInsn.name;
				String fieldDesc = fieldInsn.desc;
				String fieldKey = key(fieldName, fieldDesc);
				if (isClinit) {
					if (finalContainer.containsMaybe(fieldName, fieldDesc)) {
						int count = directInitializerWrites.merge(fieldKey, 1, Integer::sum);
						if (count > 1)
							finalContainer.disqualifyMaybe(fieldName, fieldDesc);
					}
				} else {
					// Any non-initializer writer removes a tentative field until a helper proof restores it.
					finalContainer.removeMaybe(fieldName, fieldDesc);
					methodWrites.computeIfAbsent(methodKey, ignored -> new HashSet<>()).add(fieldKey);
				}
			}
		}

		Set<String> initializationWrittenFields = new HashSet<>();
		for (String methodKey : initializationHelpers)
			initializationWrittenFields.addAll(methodWrites.getOrDefault(methodKey, Set.of()));
		Set<String> externalWrittenFields = new HashSet<>();
		for (String methodKey : externallyReachableMethods)
			externalWrittenFields.addAll(methodWrites.getOrDefault(methodKey, Set.of()));

		// Use current values for same-class reads and finalized values from other classes.
		GetStaticLookup staticLookup = new GetStaticLookup() {
			@Nonnull
			@Override
			public ReValue get(@Nonnull FieldInsnNode field) {
				if (field.owner.equals(className)) {
					ReValue localValue = valuesContainer.get(field.name, field.desc);
					if (localValue != null)
						return localValue;
				} else {
					ReValue externalValue = getStaticValue(field.owner, field.name, field.desc);
					if (externalValue != null)
						return externalValue;
				}
				try {
					return Objects.requireNonNull(ReValue.ofType(Type.getType(field.desc), Nullness.UNKNOWN));
				} catch (Exception ex) {
					// Should never fail since fields cannot have illegal types like primitive void.
					throw new IllegalStateException(ex);
				}
			}

			@Override
			public boolean hasLookup(@Nonnull FieldInsnNode field) {
				if (field.owner.equals(className))
					return valuesContainer.get(field.name, field.desc) != null;
				return getStaticValue(field.owner, field.name, field.desc) != null;
			}
		};

		// Evaluate the full initializer when it invokes any static method, keeping this cache local to the class.
		// This will let us feed the results of the evaluation into the final container for sharing with other transformers.
		FieldCacheManager evaluatedCache = null;
		EvaluationCapture evaluationCapture = null;
		boolean evaluatorCompleted = false;
		boolean helperEvaluationCompleted = false;
		if (clinit != null && hasStaticInvocations(clinit)) {
			evaluatedCache = new FieldCacheManager();
			FieldCache staticCache = evaluatedCache.getStaticFieldCache(className);

			// Initialize the static cache with any default values from the field attributes.
			for (FieldNode field : node.fields) {
				if ((field.access & Opcodes.ACC_STATIC) == 0)
					continue;
				ReValue value = field.value == null ? null : Unchecked.getOr(() -> ReValue.ofConstant(field.value), null);
				if (value == null)
					value = Unchecked.getOr(() -> ReValue.ofTypeDefaultValue(Type.getType(field.desc)), null);
				if (value != null)
					staticCache.setField(className, field.name, field.desc, value);
			}

			// Set up the evaluator with a listener to track writes and helper method lifecycle.
			ReInterpreter evaluatorInterpreter = context.newInterpreter(inheritanceGraph);
			evaluatorInterpreter.setGetStaticLookup(staticLookup);
			Evaluator evaluator = new Evaluator(workspace, evaluatorInterpreter, evaluatedCache, maxSteps, false, false);
			evaluationCapture = new EvaluationCapture(className, initializationHelpers);
			evaluator.addListener(evaluationCapture);

			// Empty locals ensure malformed or unused local slots cannot affect block execution.
			ReFrame originFrame = new ReFrame(null, clinit.maxLocals, clinit.maxStack);
			for (int i = 0; i < originFrame.getLocals(); i++)
				originFrame.setLocal(i, UninitializedValue.UNINITIALIZED_VALUE);

			// Evaluate the initializer. This will also evaluate any reachable helper
			// methods referenced by the initializer up to the max step limit.
			EvaluationResult evaluationResult = null;
			try {
				evaluationResult = evaluator.evaluateBlock(clinit.instructions, originFrame, clinit.access);
			} catch (Throwable t) {
				logger.debugging(l -> l.error("Error encountered when evaluating static initializer", t));
			}

			// Executed duplicate writes disqualify tentative fields even when evaluation later fails.
			for (String fieldKey : evaluationCapture.writtenFieldKeys()) {
				if (evaluationCapture.writeCount(fieldKey) > 1)
					finalContainer.disqualifyMaybe(fieldName(fieldKey), fieldDescriptor(fieldKey));
			}

			// Mark evaluation as completed if we get any yielded result.
			// - EvaluationFailureResult or EvaluationThrowsResult indicates the evaluation failed.
			evaluatorCompleted = evaluationResult instanceof EvaluationYieldResult;

			// Tentative fields need the stronger helper proof before their values become publishable.
			helperEvaluationCompleted = evaluatorCompleted
					&& unresolvedHelperEdges.isEmpty()
					&& evaluationCapture.helpersCompleted();
		}

		// Record whether the class initializer is observable (locally seen changes only, nothing external).
		initializers.put(className, new InitializerInfo(
				isInitializerObservable(clinit, className, evaluatorCompleted ? evaluationCapture : null),
				initialClassState.getSuperName()));

		// Commit tentative fields only after all static write checks have completed.
		if (helperEvaluationCompleted) {
			for (String fieldKey : initializationWrittenFields) {
				if (!externalWrittenFields.contains(fieldKey)
						&& !isWrittenOutsideInitialization(fieldKey, methodWrites, initializationHelpers)
						&& evaluationCapture.writeCount(fieldKey) <= 1)
					finalContainer.promoteMaybe(fieldName(fieldKey), fieldDescriptor(fieldKey));
			}
		}
		finalContainer.commitMaybeIntoEffectivelyFinals();

		// Preserve direct <clinit> analysis for arithmetic and assignments that do not need evaluation.
		if (clinit != null && hasStaticSetters(clinit)) {
			try {
				ReAnalyzer analyzer = context.newAnalyzer(inheritanceGraph, node, clinit);
				ReInterpreter interpreter = analyzer.getInterpreter();
				interpreter.setGetStaticLookup(staticLookup);
				Frame<ReValue>[] frames = analyzer.analyze(node.name, clinit);
				AbstractInsnNode[] instructions = clinit.instructions.toArray();
				for (int i = 0; i < instructions.length; i++) {
					AbstractInsnNode instruction = instructions[i];
					if (instruction.getOpcode() != Opcodes.PUTSTATIC || !(instruction instanceof FieldInsnNode fieldInsn)
							|| !fieldInsn.owner.equals(className))
						continue;

					String fieldName = fieldInsn.name;
					String fieldDesc = fieldInsn.desc;
					if (!finalContainer.contains(fieldName, fieldDesc))
						continue;

					Frame<ReValue> frame = frames[i];
					if (frame == null || frame.getStackSize() == 0)
						continue;
					ReValue existingValue = valuesContainer.get(fieldName, fieldDesc);
					ReValue stackValue = frame.getStack(frame.getStackSize() - 1);
					ReValue merged = existingValue == null ? stackValue : interpreter.merge(existingValue, stackValue);
					valuesContainer.put(fieldName, fieldDesc, merged);
				}
			} catch (Throwable t) {
				throw new TransformationException("Error encountered when computing static constants", t);
			}
		}

		// If the evaluator completed without any errors, we can trust the cache for all final fields.
		// We'll copy them from the cache used by the evaluator into the final container for sharing with other transformers.
		if (evaluatorCompleted)
			valuesContainer.collectFrom(evaluatedCache, className, node, finalContainer);

		// Any eligible static fields that remain unassigned retain their JVM default values.
		try {
			valuesContainer.commitRemainingAsDefaults(finalContainer);
		} catch (IllegalValueException ex) {
			throw new TransformationException("Error encountered when computing default constant values", ex);
		}

		// Remove values that lost finality and avoid leaking stale results across transformation passes.
		valuesContainer.retain(finalContainer);
		if (valuesContainer.staticFieldValues.isEmpty())
			classValues.remove(className);
		else
			classValues.put(className, valuesContainer);
	}

	/**
	 * @param method
	 * 		Method to inspect for private helper calls.
	 * @param className
	 * 		Internal name of the current class.
	 * @param methodsByKey
	 * 		Methods declared by the current class.
	 * @param helpers
	 * 		Private helper methods reachable from the initializer.
	 * @param visited
	 * 		Methods already visited during traversal.
	 * @param unresolvedEdges
	 * 		Missing same-class static targets encountered during traversal.
	 */
	private static void collectInitializationHelpers(@Nonnull MethodNode method, @Nonnull String className,
	                                                 @Nonnull Map<String, MethodNode> methodsByKey,
	                                                 @Nonnull Set<String> helpers, @Nonnull Set<String> visited,
	                                                 @Nonnull Set<String> unresolvedEdges) {
		// Skip if already visited or if the method has no instructions.
		String methodKey = key(method.name, method.desc);
		if (!visited.add(methodKey) || method.instructions == null)
			return;

		for (AbstractInsnNode instruction : method.instructions) {
			// Only consider same-class static calls to private methods.
			if (instruction.getOpcode() != Opcodes.INVOKESTATIC
					|| !(instruction instanceof MethodInsnNode methodInsn)
					|| !methodInsn.owner.equals(className))
				continue;

			String targetKey = key(methodInsn.name, methodInsn.desc);
			MethodNode target = methodsByKey.get(targetKey);

			// If the target is missing, we cannot confirm it is a private helper. Track the edge for later evaluation.
			if (target == null) {
				unresolvedEdges.add(targetKey);
				continue;
			}

			// Otherwise only consider private static methods as helpers. Continue traversal from there.
			if ((target.access & Opcodes.ACC_PRIVATE) != 0
					&& (target.access & Opcodes.ACC_STATIC) != 0
					&& helpers.add(targetKey))
				collectInitializationHelpers(target, className, methodsByKey, helpers, visited, unresolvedEdges);
		}
	}

	/**
	 * @param method
	 * 		Method to inspect for same-class static calls.
	 * @param className
	 * 		Internal name of the current class.
	 * @param methodsByKey
	 * 		Methods declared by the current class.
	 * @param externalMethods
	 * 		Methods reachable from non-private entry points.
	 * @param visited
	 * 		Methods already visited during traversal.
	 */
	private static void collectExternalMethods(@Nonnull MethodNode method, @Nonnull String className,
	                                           @Nonnull Map<String, MethodNode> methodsByKey,
	                                           @Nonnull Set<String> externalMethods, @Nonnull Set<String> visited) {
		// Skip if already visited or if the method has no instructions.
		String methodKey = key(method.name, method.desc);
		if (!visited.add(methodKey) || method.instructions == null)
			return;

		// Mark this method as externally reachable so that any writes it makes are considered non-initializer writes.
		externalMethods.add(methodKey);

		for (AbstractInsnNode instruction : method.instructions) {
			// Skip any non-static calls or calls to other classes.
			if (instruction.getOpcode() != Opcodes.INVOKESTATIC
					|| !(instruction instanceof MethodInsnNode methodInsn)
					|| !methodInsn.owner.equals(className))
				continue;

			// Only consider static calls to other methods in the same class. Continue traversal from there.
			String targetKey = key(methodInsn.name, methodInsn.desc);
			MethodNode target = methodsByKey.get(targetKey);
			if (target != null
					&& (target.access & Opcodes.ACC_STATIC) != 0
					&& !target.name.equals("<clinit>"))
				collectExternalMethods(target, className, methodsByKey, externalMethods, visited);
		}
	}

	/**
	 * @param fieldKey
	 * 		Field key to check.
	 * @param methodWrites
	 * 		Fields written by each non-initializer method.
	 * @param initializationHelpers
	 * 		Private helper methods reached from the initializer.
	 *
	 * @return {@code true} when a non-helper method writes the field.
	 */
	private static boolean isWrittenOutsideInitialization(@Nonnull String fieldKey,
	                                                      @Nonnull Map<String, Set<String>> methodWrites,
	                                                      @Nonnull Set<String> initializationHelpers) {
		for (Map.Entry<String, Set<String>> entry : methodWrites.entrySet())
			if (!initializationHelpers.contains(entry.getKey()) && entry.getValue().contains(fieldKey))
				return true;
		return false;
	}

	/**
	 * @param clinit
	 * 		Static initializer of the class, if it has one.
	 * @param className
	 * 		Internal name of the class.
	 * @param completedEvaluation
	 * 		Capture of a completed evaluation of the initializer, or {@code null} if it was not evaluated successfully.
	 *
	 * @return {@code true} when the initializer may do anything other than computing values for the
	 * class's own static fields, such as writing another class's fields or calling methods with side effects.
	 */
	private static boolean isInitializerObservable(@Nullable MethodNode clinit, @Nonnull String className,
	                                               @Nullable EvaluationCapture completedEvaluation) {
		if (clinit == null || clinit.instructions == null)
			return false;

		// The evaluator only completes when every method it ran was understood, so calls it ran can be trusted.
		// What it cannot trust is anything that touches the static state of other classes, which includes
		// code in the helpers it ran, so that is tracked separately.
		boolean evaluated = completedEvaluation != null && !completedEvaluation.accessedForeignStatics();
		for (AbstractInsnNode instruction : clinit.instructions) {
			switch (instruction.getOpcode()) {
				case Opcodes.GETSTATIC, Opcodes.PUTSTATIC -> {
					if (instruction instanceof FieldInsnNode fieldInsn && isForeignStatic(fieldInsn, className))
						return true;
				}
				case Opcodes.MONITORENTER, Opcodes.MONITOREXIT, Opcodes.ATHROW -> {
					return true;
				}
				case Opcodes.INVOKESTATIC, Opcodes.INVOKEVIRTUAL, Opcodes.INVOKESPECIAL,
				     Opcodes.INVOKEINTERFACE, Opcodes.INVOKEDYNAMIC -> {
					if (!evaluated)
						return true;
				}
				default -> {
					// Nothing to do.
				}
			}
		}
		return false;
	}

	/**
	 * @param field
	 * 		Static field reference.
	 * @param className
	 * 		Internal name of the class using the field.
	 *
	 * @return {@code true} when the field belongs to some other class, excluding the JDK
	 * since initializing those classes has no effect that the program can observe.
	 */
	private static boolean isForeignStatic(@Nonnull FieldInsnNode field, @Nonnull String className) {
		return !field.owner.equals(className) && !field.owner.startsWith("java/");
	}

	/**
	 * @param method
	 * 		Method containing the instruction.
	 * @param arrayProducer
	 * 		Instruction pushing an array onto the stack.
	 *
	 * @return {@code true} when the array is only used to read one of its elements or its length,
	 * either directly or after being held in a local variable. Passing the array anywhere else,
	 * or writing to it, returns {@code false}.
	 */
	private static boolean isReadOnlyArrayUse(@Nonnull MethodNode method, @Nonnull AbstractInsnNode arrayProducer) {
		AbstractInsnNode next = arrayProducer.getNext();
		while (next != null && (next.getOpcode() == -1 || next.getOpcode() == Opcodes.NOP))
			next = next.getNext();
		if (next == null || next.getOpcode() != Opcodes.ASTORE)
			return isDirectArrayRead(arrayProducer);

		// The array is held in a local variable, so every use of the variable must also be a direct read.
		int variable = ((VarInsnNode) next).var;
		for (AbstractInsnNode insn : method.instructions)
			if (insn.getOpcode() == Opcodes.ALOAD && ((VarInsnNode) insn).var == variable && !isDirectArrayRead(insn))
				return false;
		return true;
	}

	/**
	 * @param arrayProducer
	 * 		Instruction pushing an array onto the stack.
	 *
	 * @return {@code true} when the array is only used to read one of its elements or its length,
	 * with indices simple enough to follow. Passing the array anywhere else, or writing to it, returns {@code false}.
	 */
	private static boolean isDirectArrayRead(@Nonnull AbstractInsnNode arrayProducer) {
		// Number of values pushed on top of the array since it was pushed.
		int above = 0;
		int jumps = 0;
		for (AbstractInsnNode insn = arrayProducer.getNext(); insn != null; insn = insn.getNext()) {
			int op = insn.getOpcode();
			if (op == -1 || op == Opcodes.NOP)
				continue;

			// Unconditional jumps are followed, as the index may be computed after paths merge.
			if (op == Opcodes.GOTO) {
				if (++jumps > 8)
					return false;
				insn = ((JumpInsnNode) insn).label;
				continue;
			}
			if (op == Opcodes.ARRAYLENGTH)
				return above == 0;
			if (AsmInsnUtil.isArrayLoad(op))
				return above == 1;

			// Anything else must be part of computing the index.
			if (AsmInsnUtil.isConstIntValue(insn) || op == Opcodes.ILOAD) {
				above++;
			} else if (AsmInsnUtil.isIntBinaryOperation(op)) {
				// Consumes two values and pushes one.
				if (above < 2)
					return false;
				above--;
			} else {
				return false;
			}
		}
		return false;
	}

	/**
	 * @param method
	 * 		Method to check for {@link Opcodes#INVOKESTATIC} use.
	 *
	 * @return {@code true} when the method has a static invocation.
	 */
	private static boolean hasStaticInvocations(@Nonnull MethodNode method) {
		if (method.instructions == null)
			return false;
		for (AbstractInsnNode instruction : method.instructions)
			if (instruction.getOpcode() == Opcodes.INVOKESTATIC)
				return true;
		return false;
	}

	/**
	 * @param fieldKey
	 * 		Field key in {@code name:descriptor} form.
	 *
	 * @return Field name.
	 */
	@Nonnull
	private static String fieldName(@Nonnull String fieldKey) {
		return fieldKey.substring(0, fieldKey.indexOf(':'));
	}

	/**
	 * @param fieldKey
	 * 		Field key in {@code name:descriptor} form.
	 *
	 * @return Field descriptor.
	 */
	@Nonnull
	private static String fieldDescriptor(@Nonnull String fieldKey) {
		return fieldKey.substring(fieldKey.indexOf(':') + 1);
	}

	/**
	 * @param evaluatedCache
	 * 		Cache populated by successful initializer evaluation.
	 * @param className
	 * 		Internal name of the current class.
	 * @param node
	 * 		Current class node.
	 * @param finalFields
	 * 		Finalized static fields eligible for publication.
	 */
	private static void collectEvaluatedValues(@Nonnull FieldCacheManager evaluatedCache, @Nonnull String className,
	                                           @Nonnull ClassNode node, @Nonnull EffectivelyFinalFields finalFields,
	                                           @Nonnull StaticValues values) {
		FieldCache staticCache = evaluatedCache.getStaticFieldCache(className);
		for (FieldNode field : node.fields) {
			if ((field.access & Opcodes.ACC_STATIC) == 0 || !finalFields.contains(field.name, field.desc))
				continue;
			ReValue value = staticCache.getField(className, field.name, field.desc);
			if (value != null)
				values.put(field.name, field.desc, snapshotValue(value));
		}
	}

	/**
	 * @param value
	 * 		Value owned by evaluator state.
	 *
	 * @return Immutable value safe to publish in the shared collector cache.
	 */
	@Nonnull
	private static ReValue snapshotValue(@Nonnull ReValue value) {
		if (value instanceof InstancedObjectValue<?> instanced) {
			if (instanced.getRealInstance() != null) {
				ReValue unmapped = instanced.unmap();
				if (unmapped != value)
					return snapshotValue(unmapped);
			}

			// Unsupported host values must not leave mutable evaluator objects in shared state.
			ReValue typedValue = Unchecked.getOr(() -> ReValue.ofType(instanced.type(), instanced.nullness()), null);
			if (typedValue != null)
				return typedValue;
		}

		if (value instanceof ArrayValue array) {
			if (array.getFirstDimensionLength().isEmpty())
				return new ArrayValueImpl(array.type(), array.nullness());

			int length = array.getFirstDimensionLength().getAsInt();
			return new ArrayValueImpl(array.type(), array.nullness(), length, index -> {
				ReValue element = array.getValue(index);
				return element == null ? UninitializedValue.UNINITIALIZED_VALUE : snapshotValue(element);
			});
		}
		return value;
	}

	/**
	 * Tracks evaluator writes and the lifecycle of private initializer helpers.
	 */
	private static final class EvaluationCapture implements EvaluationListener {
		private final String className;
		private final Set<String> helperKeys;
		private final Map<String, Integer> writeCounts = new HashMap<>();
		private final Map<String, Integer> helperEntries = new HashMap<>();
		private final Map<String, Integer> helperReturns = new HashMap<>();
		private final Set<String> helperThrows = new HashSet<>();
		private boolean foreignStaticAccess;

		private EvaluationCapture(@Nonnull String className, @Nonnull Set<String> helperKeys) {
			this.className = className;
			this.helperKeys = helperKeys;
		}

		@Override
		public void onInstruction(@Nullable ClassNode classNode, @Nullable MethodNode methodNode,
		                          @Nonnull AbstractInsnNode instruction, @Nonnull ReFrame frame) {
			if (instruction.getOpcode() == Opcodes.PUTSTATIC && instruction instanceof FieldInsnNode fieldInsn
					&& fieldInsn.owner.equals(className)) {
				String fieldKey = key(fieldInsn.name, fieldInsn.desc);
				writeCounts.merge(fieldKey, 1, (oldCount, ignored) -> Math.min(2, oldCount + 1));
			}
			if ((instruction.getOpcode() == Opcodes.PUTSTATIC || instruction.getOpcode() == Opcodes.GETSTATIC)
					&& instruction instanceof FieldInsnNode fieldInsn && isForeignStatic(fieldInsn, className))
				foreignStaticAccess = true;
		}

		@Override
		public void onMethodEnter(@Nonnull ClassNode classNode, @Nonnull MethodNode methodNode,
		                          @Nonnull ReFrame frame, @Nonnull List<ClassMethodPair> stack) {
			String methodKey = key(methodNode.name, methodNode.desc);
			if (classNode.name.equals(className) && helperKeys.contains(methodKey))
				helperEntries.merge(methodKey, 1, Integer::sum);
		}

		@Override
		public void onMethodReturn(@Nonnull ClassNode classNode, @Nonnull MethodNode methodNode,
		                           @Nonnull ReFrame frame, @Nonnull ReValue value,
		                           @Nonnull List<ClassMethodPair> stack) {
			String methodKey = key(methodNode.name, methodNode.desc);
			if (classNode.name.equals(className) && helperKeys.contains(methodKey))
				helperReturns.merge(methodKey, 1, Integer::sum);
		}

		@Override
		public void onMethodThrow(@Nonnull ClassNode classNode, @Nonnull MethodNode methodNode,
		                          @Nonnull ReFrame frame, @Nonnull ReValue exception,
		                          @Nonnull List<ClassMethodPair> stack) {
			String methodKey = key(methodNode.name, methodNode.desc);
			if (classNode.name.equals(className) && helperKeys.contains(methodKey))
				helperThrows.add(methodKey);
		}

		private boolean accessedForeignStatics() {
			return foreignStaticAccess;
		}

		private boolean helpersCompleted() {
			for (String helperKey : helperKeys) {
				int entries = helperEntries.getOrDefault(helperKey, 0);
				if (entries == 0
						|| helperReturns.getOrDefault(helperKey, 0) != entries
						|| helperThrows.contains(helperKey))
					return false;
			}
			return true;
		}

		private int writeCount(@Nonnull String fieldKey) {
			return writeCounts.getOrDefault(fieldKey, 0);
		}

		@Nonnull
		private Set<String> writtenFieldKeys() {
			return writeCounts.keySet();
		}
	}

	/**
	 * @param method
	 * 		Method to check for {@link Opcodes#PUTSTATIC} use.
	 *
	 * @return {@code true} when the method has a {@link Opcodes#PUTSTATIC} instruction.
	 */
	private static boolean hasStaticSetters(@Nonnull MethodNode method) {
		if (method.instructions == null)
			return false;
		for (AbstractInsnNode abstractInsnNode : method.instructions)
			if (abstractInsnNode.getOpcode() == Opcodes.PUTSTATIC) return true;
		return false;
	}

	@Nonnull
	private static String arrayKey(@Nonnull String owner, @Nonnull String name, @Nonnull String desc) {
		return owner + '.' + key(name, desc);
	}

	/**
	 * @param name
	 * 		Field name.
	 * @param desc
	 * 		Field descriptor.
	 *
	 * @return Field key.
	 */
	@Nonnull
	private static String key(@Nonnull String name, @Nonnull String desc) {
		return name + ':' + desc;
	}

	@Nonnull
	@Override
	public String identifier() {
		return IDENTIFIER;
	}

	@Nonnull
	@Override
	public List<TransformationParameter<?>> getParameterDefinitions() {
		return List.of(MAX_STEPS_PARAMETER);
	}

	/**
	 * @param observable
	 * 		Whether initializing the class may have effects beyond assigning its own static fields.
	 * @param superName
	 * 		Name of the parent class, initialized before the class itself.
	 */
	private record InitializerInfo(boolean observable, @Nullable String superName) {}

	/**
	 * Wrapper/utility for field finality storage/lookups.
	 */
	private static class EffectivelyFinalFields {
		private Set<String> finalFieldKeys;
		private Set<String> originalMaybeFinalFieldKeys;
		private Set<String> maybeFinalFieldKeys;
		private Set<String> disqualifiedMaybeFieldKeys;

		/**
		 * Add a {@code static final} field.
		 *
		 * @param name
		 * 		Field name.
		 * @param desc
		 * 		Field descriptor.
		 */
		public void add(@Nonnull String name, @Nonnull String desc) {
			if (finalFieldKeys == null)
				finalFieldKeys = new HashSet<>();
			finalFieldKeys.add(key(name, desc));
		}

		/**
		 * Add a {@code static} field that <i>may be</i> effectively final.
		 *
		 * @param name
		 * 		Field name.
		 * @param desc
		 * 		Field descriptor.
		 */
		public void addMaybe(@Nonnull String name, @Nonnull String desc) {
			String fieldKey = key(name, desc);
			if (originalMaybeFinalFieldKeys == null)
				originalMaybeFinalFieldKeys = new HashSet<>();
			originalMaybeFinalFieldKeys.add(fieldKey);
			if (maybeFinalFieldKeys == null)
				maybeFinalFieldKeys = new HashSet<>();
			maybeFinalFieldKeys.add(fieldKey);
		}

		/**
		 * Remove a field from being considered possibly effectively final.
		 *
		 * @param name
		 * 		Field name.
		 * @param desc
		 * 		Field descriptor.
		 */
		public void removeMaybe(@Nonnull String name, @Nonnull String desc) {
			if (maybeFinalFieldKeys != null)
				maybeFinalFieldKeys.remove(key(name, desc));
		}

		/**
		 * Permanently disqualify a tentative field after observing multiple possible writes.
		 *
		 * @param name
		 * 		Field name.
		 * @param desc
		 * 		Field descriptor.
		 */
		public void disqualifyMaybe(@Nonnull String name, @Nonnull String desc) {
			String fieldKey = key(name, desc);
			if (disqualifiedMaybeFieldKeys == null)
				disqualifiedMaybeFieldKeys = new HashSet<>();
			disqualifiedMaybeFieldKeys.add(fieldKey);
			removeMaybe(name, desc);
		}

		/**
		 * Restore a tentative field after proving its only writer is an initializer helper.
		 *
		 * @param name
		 * 		Field name.
		 * @param desc
		 * 		Field descriptor.
		 */
		public void promoteMaybe(@Nonnull String name, @Nonnull String desc) {
			String fieldKey = key(name, desc);
			if (originalMaybeFinalFieldKeys != null && originalMaybeFinalFieldKeys.contains(fieldKey)
					&& (disqualifiedMaybeFieldKeys == null || !disqualifiedMaybeFieldKeys.contains(fieldKey))) {
				if (maybeFinalFieldKeys == null)
					maybeFinalFieldKeys = new HashSet<>();
				maybeFinalFieldKeys.add(fieldKey);
			}
		}

		/**
		 * @param name
		 * 		Field name.
		 * @param desc
		 * 		Field descriptor.
		 *
		 * @return {@code true} when the field is still a tentative candidate.
		 */
		public boolean containsMaybe(@Nonnull String name, @Nonnull String desc) {
			return maybeFinalFieldKeys != null && maybeFinalFieldKeys.contains(key(name, desc));
		}

		/**
		 * Commit all possible effectively final fields into the final fields set.
		 */
		public void commitMaybeIntoEffectivelyFinals() {
			if (maybeFinalFieldKeys != null)
				if (finalFieldKeys == null)
					finalFieldKeys = new HashSet<>(maybeFinalFieldKeys);
				else
					finalFieldKeys.addAll(maybeFinalFieldKeys);
		}

		/**
		 * @param name
		 * 		Field name.
		 * @param desc
		 * 		Field descriptor.
		 *
		 * @return {@code true} when the field is {@code final} or effectively {@code final}.
		 */
		public boolean contains(@Nonnull String name, @Nonnull String desc) {
			if (finalFieldKeys == null)
				return false;
			return finalFieldKeys.contains(key(name, desc));
		}
	}

	/**
	 * Wrapper/utility for field value storage/lookups.
	 */
	private static class StaticValues {
		private final Map<String, ReValue> staticFieldValues = new ConcurrentHashMap<>();

		private void put(@Nonnull String name, @Nonnull String desc, @Nonnull ReValue value) {
			staticFieldValues.put(key(name, desc), value);
		}

		@Nullable
		private ReValue get(@Nonnull String name, @Nonnull String desc) {
			return staticFieldValues.get(key(name, desc));
		}

		private void collectFrom(@Nonnull FieldCacheManager evaluatedCache, @Nonnull String className,
		                         @Nonnull ClassNode node, @Nonnull EffectivelyFinalFields finalFields) {
			collectEvaluatedValues(evaluatedCache, className, node, finalFields, this);
		}

		private void commitRemainingAsDefaults(@Nonnull EffectivelyFinalFields finalFields) throws IllegalValueException {
			if (finalFields.finalFieldKeys == null)
				return;

			// By the point this is called, the final fields container will have committed any "maybe" candidates
			// that are valid to being actually final. Thus, if we see anything in the final field set, we will
			// initialize them with default values here.
			for (String key : finalFields.finalFieldKeys)
				if (!staticFieldValues.containsKey(key)) {
					Type fieldType = Type.getType(key.substring(key.indexOf(':') + 1));
					staticFieldValues.put(key, ReValue.ofTypeDefaultValue(fieldType));
				}
		}

		private void retain(@Nonnull EffectivelyFinalFields finalFields) {
			if (finalFields.finalFieldKeys == null) {
				staticFieldValues.clear();
				return;
			}

			staticFieldValues.keySet().removeIf(key -> !finalFields.finalFieldKeys.contains(key));
		}
	}
}
