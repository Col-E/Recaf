package software.coley.recaf.util.analysis.eval;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import me.darknet.assembler.printer.JvmPrinterUtil;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Interpreter;
import org.slf4j.Logger;
import software.coley.collections.Unchecked;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.util.AccessFlag;
import software.coley.recaf.util.ClassMethodPair;
import software.coley.recaf.util.analysis.Branching;
import software.coley.recaf.util.analysis.Nullness;
import software.coley.recaf.util.analysis.ReFrame;
import software.coley.recaf.util.analysis.ReInterpreter;
import software.coley.recaf.util.analysis.lookup.InvokeStaticLookup;
import software.coley.recaf.util.analysis.lookup.InvokeVirtualLookup;
import software.coley.recaf.util.analysis.value.ArrayValue;
import software.coley.recaf.util.analysis.value.IntValue;
import software.coley.recaf.util.analysis.value.ObjectValue;
import software.coley.recaf.util.analysis.value.ReValue;
import software.coley.recaf.util.analysis.value.ThrowableValue;
import software.coley.recaf.util.analysis.value.UninitializedValue;
import software.coley.recaf.util.analysis.value.impl.ArrayValueImpl;
import software.coley.recaf.util.visitors.MemberFilteringVisitor;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.RuntimeWorkspaceResource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * Simple method evaluator.
 *
 * @author Matt Coley
 */
public class Evaluator {
	private static final String UNKNOWN_VALUE_REASON = "Encountered unknown value while evaluating branch";

	private static final Logger logger = Logging.get(Evaluator.class);
	private static final InstanceFactory instanceFactory = new InstanceFactory();
	private final List<EvaluationListener> listeners = new CopyOnWriteArrayList<>();
	private final Workspace workspace;
	private final ReInterpreter interpreter;
	private final FieldCacheManager fieldCacheManager;
	private final boolean evaluateInternals;
	private final boolean evaluateClassInitializers;
	private final int maxSteps;

	/**
	 * @param workspace
	 * 		Workspace to pull classes from.
	 * @param interpreter
	 * 		Interpreter to evaluate instructions with.
	 * @param fieldCacheManager
	 * 		Support class for tracking instance data.
	 * @param maxSteps
	 * 		Maximum number of steps to allow when evaluating a method.
	 * @param evaluateInternals
	 * 		Flag to allow evaluation of methods defined by classes of internal resources
	 * 		<i>(Mainly the {@link RuntimeWorkspaceResource} to facilitate emulating core JDK methods)</i>
	 * @param evaluateClassInitializers
	 * 		Flag to allow evaluation of workspace class initializers.
	 */
	public Evaluator(@Nonnull Workspace workspace, @Nonnull ReInterpreter interpreter,
	                 @Nonnull FieldCacheManager fieldCacheManager, int maxSteps, boolean evaluateInternals,
	                 boolean evaluateClassInitializers) {
		this.workspace = workspace;
		this.interpreter = interpreter;
		this.fieldCacheManager = fieldCacheManager;
		this.maxSteps = maxSteps;
		this.evaluateInternals = evaluateInternals;
		this.evaluateClassInitializers = evaluateClassInitializers;
	}

	/**
	 * Registers a listener for successfully executed instructions.
	 *
	 * @param listener
	 * 		Listener to invoke after each successfully executed instruction.
	 */
	public void addListener(@Nonnull EvaluationListener listener) {
		listeners.add(listener);
	}

	/**
	 * @param className
	 * 		Name of class defining the target method.
	 * @param methodName
	 * 		Name of the target method.
	 * @param methodDescriptor
	 * 		Descriptor of the target method.
	 *
	 * @return {@code true} when all instructions in the method can be evaluated.
	 */
	public boolean canEvaluate(@Nonnull String className,
	                           @Nonnull String methodName,
	                           @Nonnull String methodDescriptor) {
		// Find class in workspace.
		ClassPathNode classPath = workspace.findClass(evaluateInternals, className);
		if (classPath == null)
			return false;

		// Ensure method exists in class.
		JvmClassInfo jvmClass = classPath.getValue().asJvmClass();
		MethodMember method = jvmClass.getDeclaredMethod(methodName, methodDescriptor);
		if (method == null || AccessFlag.isAbstract(method.getAccess()) || AccessFlag.isNative(method.getAccess()))
			return false;

		// Extract method-node model and delegate to evaluate check.
		ClassNode node = new ClassNode();
		jvmClass.getClassReader().accept(new MemberFilteringVisitor(node, method), ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
		return node.methods.size() == 1 && canEvaluate(node.methods.getFirst());
	}

	/**
	 * @param method
	 * 		Method to check for evaluation support.
	 *
	 * @return {@code true} when all instructions in the method can be evaluated.
	 */
	public boolean canEvaluate(@Nonnull MethodNode method) {
		// Cannot be abstract, native, or have no instructions.
		if (AccessFlag.isAbstract(method.access) || AccessFlag.isNative(method.access)
				|| method.instructions == null || method.instructions.size() == 0)
			return false;

		// Must not have any unsupported instructions
		ExecutingFrame frame = new ExecutingFrame(method, new EvaluationContext(maxSteps), null);
		for (AbstractInsnNode instruction : method.instructions)
			if (!frame.canEvaluateInsn(instruction, interpreter, method.instructions))
				return false;
		return true;
	}

	/**
	 * @param instructionBlock
	 * 		Block of instructions to evaluate.
	 * 		This may be an incomplete expression with no {@code return} instruction.
	 * 		In such cases, the resulting stack top value will be returned.
	 * @param originFrame
	 * 		The origin frame to initiate evaluation state from.
	 * @param methodAccess
	 * 		The access flags of the method defining the given instruction block.
	 *
	 * @return {@code true} when all instructions in the given list can be evaluated.
	 */
	public boolean canEvaluateBlock(@Nonnull InsnList instructionBlock,
	                                @Nonnull ReFrame originFrame,
	                                int methodAccess) {
		// Must not have any unsupported instructions
		ExecutingFrame frame = new ExecutingFrame(null, 0xFF, 0xFF, methodAccess, new EvaluationContext(maxSteps));
		for (AbstractInsnNode instruction : instructionBlock)
			if (!frame.canEvaluateInsn(instruction, interpreter, instructionBlock))
				return false;
		return true;
	}

	/**
	 * @param className
	 * 		Name of class defining the target method.
	 * @param methodName
	 * 		Name of the target method.
	 * @param methodDescriptor
	 * 		Descriptor of the target method.
	 * @param classInstance
	 * 		Instance of {@code this} for instance methods.
	 * 		Can be {@code null} for {@code static} methods.
	 * @param parameters
	 * 		Parameters to pass to the target method.
	 *
	 * @return Result of evaluating the target method with the given parameters.
	 */
	@Nonnull
	public EvaluationResult evaluate(@Nonnull String className,
	                                 @Nonnull String methodName,
	                                 @Nonnull String methodDescriptor,
	                                 @Nullable ObjectValue classInstance,
	                                 @Nonnull List<ReValue> parameters) {
		if (Type.getReturnType(methodDescriptor) == Type.VOID_TYPE)
			return EvaluationResult.cannotEvaluate("Method must yield a value");
		try {
			return evaluate(className, methodName, methodDescriptor, classInstance, parameters, new EvaluationContext(maxSteps));
		} catch (UnknownValueException e) {
			return EvaluationResult.cannotEvaluate(UNKNOWN_VALUE_REASON, e);
		}
	}

	@Nonnull
	private EvaluationResult evaluate(@Nonnull String className,
	                                  @Nonnull String methodName,
	                                  @Nonnull String methodDescriptor,
	                                  @Nullable ObjectValue classInstance,
	                                  @Nonnull List<ReValue> parameters,
	                                  @Nonnull EvaluationContext context) throws UnknownValueException {
		ClassPathNode classPath = workspace.findClass(evaluateInternals, className);
		if (classPath == null)
			return EvaluationResult.cannotEvaluate("Class not found in workspace: " + className);

		JvmClassInfo classInfo = classPath.getValue().asJvmClass();
		if (classInfo.getDeclaredMethod(methodName, methodDescriptor) == null)
			return EvaluationResult.cannotEvaluate("Method not found in class: " + className + "." + methodName + methodDescriptor);

		ClassNode classNode = new ClassNode();
		ClassReader reader = classInfo.getClassReader();
		reader.accept(classNode, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);

		for (MethodNode methodNode : classNode.methods)
			if (methodName.equals(methodNode.name) && methodDescriptor.equals(methodNode.desc))
				return evaluate(classNode, methodNode, classInstance, parameters, context);

		return EvaluationResult.cannotEvaluate("Method exists in class model, but not in tree node representation");
	}

	/**
	 * @param classNode
	 * 		Class defining the target method.
	 * @param methodNode
	 * 		Target method.
	 * @param classInstance
	 * 		Instance of {@code this} for instance methods.
	 * 		Can be {@code null} for {@code static} methods.
	 * @param parameters
	 * 		Parameters to pass to the target method.
	 *
	 * @return Result of evaluating the target method with the given parameters.
	 */
	@Nonnull
	public EvaluationResult evaluate(@Nonnull ClassNode classNode,
	                                 @Nonnull MethodNode methodNode,
	                                 @Nullable ObjectValue classInstance,
	                                 @Nonnull List<ReValue> parameters) {
		if (Type.getReturnType(methodNode.desc) == Type.VOID_TYPE)
			return EvaluationResult.cannotEvaluate("Method must yield a value");
		try {
			return evaluate(classNode, methodNode, classInstance, parameters, new EvaluationContext(maxSteps));
		} catch (UnknownValueException e) {
			return EvaluationResult.cannotEvaluate(UNKNOWN_VALUE_REASON, e);
		}
	}

	@Nonnull
	private EvaluationResult evaluate(@Nonnull ClassNode classNode,
	                                  @Nonnull MethodNode methodNode,
	                                  @Nullable ObjectValue classInstance,
	                                  @Nonnull List<ReValue> parameters,
	                                  @Nonnull EvaluationContext context) throws UnknownValueException {
		// Active method entry is a JVM class-initialization boundary.
		if (!methodNode.name.equals("<clinit>")) {
			EvaluationResult initializationResult = initializeClassIfNeeded(classNode.name, context);
			if (initializationResult != null)
				return initializationResult;
		}

		// Must support evaluation.
		if (!canEvaluate(methodNode))
			return EvaluationResult.cannotEvaluate("Target method does not support evaluation: " + classNode.name + "." + methodNode.name + methodNode.desc);

		// Sanity check parameters.
		Type methodType = Type.getMethodType(methodNode.desc);
		if (parameters.size() != methodType.getArgumentCount())
			return EvaluationResult.cannotEvaluate("Mismatched parameter count, method expects "
					+ methodType.getArgumentCount() + " but was given " + parameters.size() + " parameters");
		Type[] argumentTypes = methodType.getArgumentTypes();
		for (int i = 0; i < argumentTypes.length; i++) {
			Type parameterType = parameters.get(i).type();
			if (parameterType == null)
				return EvaluationResult.cannotEvaluate("Unknown passed parameter type at index " + i);
			int expectedSort = argumentTypes[i].getSort();
			int actualSort = parameterType.getSort();
			if (expectedSort != actualSort && !(expectedSort <= Type.INT && actualSort < Type.INT))
				return EvaluationResult.cannotEvaluate("Mismatched parameter type at index " + i);
		}

		// Instance methods need a concrete receiver ("this" reference) before their local frame is created.
		if (!AccessFlag.isStatic(methodNode.access) && classInstance == null)
			return EvaluationResult.cannotEvaluate("Instance method requires a class instance");

		// For instance methods, initialize the instance fields of the receiver so that they have a known state.
		if (!AccessFlag.isStatic(methodNode.access) && classInstance instanceof InstancedObjectValue<?>)
			initializeInstanceFields(classInstance, classNode.name);

		// Create initial frame with every slot empty so unused and wide-value slots have valid state.
		ExecutingFrame frame = new ExecutingFrame(methodNode, context, classNode.name);
		for (int i = 0; i < methodNode.maxLocals; i++)
			frame.setLocal(i, interpreter.newEmptyValue(i));

		// Reserve local zero for the receiver before placing descriptor arguments.
		int local = AccessFlag.isStatic(methodNode.access) ? 0 : 1;
		if (!AccessFlag.isStatic(methodNode.access))
			frame.setLocal(0, classInstance);

		// Advance by slot width so arguments after long and double land correctly.
		for (int i = 0; i < argumentTypes.length; i++) {
			frame.setLocal(local, parameters.get(i));
			local += argumentTypes[i].getSize();
		}

		// Handle execution
		context.callStack.add(new EvaluationFrame(classNode.name, methodNode.name));
		try {
			InsnList instructions = methodNode.instructions;
			AbstractInsnNode pc = instructions.getFirst();
			while (context.stepAllocation > 0) {
				try {
					// Evaluate the instruction and advance the program counter.
					AbstractInsnNode executedInstruction = pc;
					pc = frame.evaluate(executedInstruction, interpreter);

					// Notify observers after state changes so they see the completed instruction.
					if (!listeners.isEmpty())
						Unchecked.checkedForEach(listeners,
								listener -> listener.onInstruction(classNode, methodNode, executedInstruction, frame),
								(listener, error) -> {
									logger.error("Listener {} failed to observe instruction {} in {}.{}{}",
											listener.getClass().getName(),
											JvmPrinterUtil.toString(executedInstruction),
											classNode.name, methodNode.name, methodNode.desc,
											error);
								});

					// Check for a return value after the instruction executes, since the instruction may have been a return.
					ReValue retVal = frame.returnValue;
					if (retVal != null) {
						if (retVal instanceof InstancedObjectValue<?> instanced && instanced.getRealInstance() != null)
							retVal = instanced.unmap();
						return new EvaluationYieldResult(retVal);
					}
				} catch (NestedEvaluationFailure e) {
					return e.result;
				} catch (AnalyzerException e) {
					return EvaluationResult.cannotEvaluate("Failed executing instruction: " + JvmPrinterUtil.toString(pc), e);
				} catch (NoNextException e) {
					return EvaluationResult.cannotEvaluate("Execution falls through end", e);
				} catch (ExceptionHandler.ThrownException e) {
					return new EvaluationThrowsResult(e.getExceptionValue());
				}
				context.stepAllocation--;
			}
			return EvaluationResult.cannotEvaluate("Method did not yield an value in " + maxSteps + " steps");
		} finally {
			context.callStack.removeLast();
		}
	}

	/**
	 * Initializes all fields declared by an allocated class and its resolvable parents.
	 * Defaults are inserted once so a cached known-null remains distinguishable from an absent field.
	 *
	 * @param instance
	 * 		Value to initialize fields for.
	 * @param allocatedClassName
	 * 		Internal name of the class that was allocated for the given instance.
	 */
	private void initializeInstanceFields(@Nonnull ReValue instance, @Nonnull String allocatedClassName) {
		FieldCache cache = fieldCacheManager.getInstanceFieldCache(instance);
		Set<String> visited = new HashSet<>();
		String className = allocatedClassName;
		while (className != null && visited.add(className)) {
			ClassPathNode classPath = workspace.findClass(evaluateInternals, className);
			if (classPath == null)
				return;
			JvmClassInfo classInfo = classPath.getValue().asJvmClass();
			for (FieldMember field : classInfo.getFields()) {
				if (AccessFlag.isStatic(field.getAccess()))
					continue;
				if (!cache.containsField(className, field.getName(), field.getDescriptor())) {
					try {
						cache.setField(className, field.getName(), field.getDescriptor(),
								ReValue.ofTypeDefaultValue(Type.getType(field.getDescriptor())));
					} catch (Exception ex) {
						throw new IllegalStateException("Failed to initialize field " + className + '.' + field.getName(), ex);
					}
				}
			}
			className = classInfo.getSuperName();
		}
	}

	/**
	 * Evaluates the static initializer once for the given class in the current evaluation context.
	 * Since the initializers are... well, initializers, the return values are just used to evaluate success or failure.
	 * <p>
	 * Initialization is opt-in, see {@link #evaluateClassInitializers}.
	 *
	 * @param className
	 * 		Internal name of the class to initialize.
	 * @param context
	 * 		Per-evaluation context sharing initialization state across nested calls.
	 *
	 * @return {@code null} when initialization succeeded or was skipped, otherwise the failure or
	 * thrown result produced while initializing the class.
	 */
	@Nullable
	private EvaluationResult initializeClassIfNeeded(@Nonnull String className, @Nonnull EvaluationContext context) {
		// Skip if we aren't evaluating class initializers or the class is not in the workspace.
		if (!evaluateClassInitializers || workspace.findClass(evaluateInternals, className) == null)
			return null;

		// Skip if the class has already been initialized in this evaluation.
		if (context.initializedClasses.contains(className))
			return null;

		// An initializer that previously failed isn't going to magically work the next time around.
		// Skip if we already tried and saw a failure, and return the same failure result.
		EvaluationResult failed = context.failedClassInitializers.get(className);
		if (failed != null)
			return failed;

		// Skip if the class is already being initialized in this evaluation (cycle-breaking).
		if (!context.initializingClasses.add(className))
			return null;

		try {
			ClassNode classNode = getNode(className);
			if (classNode == null)
				return null;

			// Seed JVM field defaults before the initializer can read or increment them.
			initializeStaticFields(classNode);

			// JVM initialization runs a resolvable superclass before the current class.
			if (classNode.superName != null) {
				EvaluationResult superclassResult = initializeClassIfNeeded(classNode.superName, context);
				if (superclassResult != null) {
					context.failedClassInitializers.put(className, superclassResult);
					return superclassResult;
				}
			}

			// Find the static initializer method, if it exists.
			MethodNode clinit = null;
			for (MethodNode methodNode : classNode.methods) {
				if (methodNode.name.equals("<clinit>") && methodNode.desc.equals("()V")) {
					clinit = methodNode;
					break;
				}
			}

			// Classes without executable static initialization are complete after default seeding.
			if (clinit == null) {
				context.initializedClasses.add(className);
				return null;
			}

			// Run the static initializer and cache the result for future calls.
			EvaluationResult result;
			try {
				result = evaluate(classNode, clinit, null, List.of(), context);
			} catch (UnknownValueException e) {
				result = EvaluationResult.cannotEvaluate(UNKNOWN_VALUE_REASON, e);
			}
			if (result instanceof EvaluationYieldResult) {
				context.initializedClasses.add(className);
				return null;
			}
			context.failedClassInitializers.put(className, result);
			return result;
		} finally {
			context.initializingClasses.remove(className);
		}
	}

	/**
	 * Seeds declared static fields with their JVM defaults unless the static cache already knows them.
	 *
	 * @param classNode
	 * 		Class whose static fields should be seeded.
	 */
	private void initializeStaticFields(@Nonnull ClassNode classNode) {
		FieldCache cache = fieldCacheManager.getStaticFieldCache(classNode.name);
		for (FieldNode field : classNode.fields) {
			// Skip instance fields.
			if (!AccessFlag.isStatic(field.access))
				continue;

			// Skip if the field is already known in the cache.
			if (cache.containsField(classNode.name, field.name, field.desc))
				continue;

			// Seed the field with its constant value if it has one,
			// otherwise seed it with the JVM default for its type.
			ReValue value;
			try {
				if (field.value != null) {
					try {
						value = ReValue.ofConstant(field.value);
					} catch (Exception ignored) {
						// Invalid constants keep JVM default semantics instead of invoking host conversion.
						value = ReValue.ofTypeDefaultValue(Type.getType(field.desc));
					}
				} else {
					value = ReValue.ofTypeDefaultValue(Type.getType(field.desc));
				}
			} catch (Exception ex) {
				throw new IllegalStateException("Failed to initialize static field "
						+ classNode.name + '.' + field.name, ex);
			}
			cache.setField(classNode.name, field.name, field.desc, value);
		}
	}

	/**
	 * @param instructionBlock
	 * 		Block of instructions to evaluate.
	 * 		This may be an incomplete expression with no {@code return} instruction.
	 * 		In such cases, the resulting stack top value will be returned.
	 * @param originFrame
	 * 		The origin frame to initiate evaluation state from.
	 * @param methodAccess
	 * 		The access flags of the method defining the given instruction block.
	 *
	 * @return Result of evaluating the given block of instructions.
	 */
	@Nonnull
	public EvaluationResult evaluateBlock(@Nonnull InsnList instructionBlock,
	                                      @Nonnull ReFrame originFrame,
	                                      int methodAccess) {
		// Must support evaluation
		if (!canEvaluateBlock(instructionBlock, originFrame, methodAccess))
			return EvaluationResult.cannotEvaluate("Target block does not support evaluation");

		// Create initial frame
		EvaluationContext context = new EvaluationContext(maxSteps);
		ExecutingFrame frame = new ExecutingFrame(null, originFrame.getLocals(), originFrame.getMaxStackSize(), methodAccess, context);
		for (int i = 0; i < originFrame.getLocals(); i++)
			frame.setLocal(i, originFrame.getLocal(i));
		for (int i = 0; i < originFrame.getStackSize(); i++)
			frame.push(originFrame.getStack(i));

		// Handle execution
		AbstractInsnNode pc = instructionBlock.getFirst();
		while (context.stepAllocation > 0) {
			try {
				// Evaluate the instruction and advance the program counter.
				AbstractInsnNode executedInstruction = pc;
				pc = frame.evaluate(executedInstruction, interpreter);

				// Notify observers after state changes so they see the completed instruction.
				if (!listeners.isEmpty())
					Unchecked.checkedForEach(listeners,
							listener -> listener.onInstruction(null, null, executedInstruction, frame),
							(listener, error) -> {
								logger.error("Listener {} failed to observe instruction {} in block evaluation",
										listener.getClass().getName(),
										JvmPrinterUtil.toString(executedInstruction),
										error);
							});

				// Check for a return value after the instruction executes, since the instruction may have been a return.
				if (frame.returnValue != null)
					return new EvaluationYieldResult(frame.returnValue);
			} catch (NestedEvaluationFailure e) {
				return e.result;
			} catch (UnknownValueException e) {
				return EvaluationResult.cannotEvaluate(UNKNOWN_VALUE_REASON, e);
			} catch (AnalyzerException e) {
				return EvaluationResult.cannotEvaluate("Failed executing instruction: " + JvmPrinterUtil.toString(pc), e);
			} catch (NoNextException e) {
				// If there is no next instruction from the given block, then control flow has exited the block.
				// The intended use case for this is to be given incomplete segments of code and see what's on the
				// top at the end, so we will yield that here.
				return new EvaluationYieldResult(frame.getStack(frame.getStackSize() - 1));
			} catch (ExceptionHandler.ThrownException e) {
				return new EvaluationThrowsResult(e.getExceptionValue());
			}
			context.stepAllocation--;
		}
		return EvaluationResult.cannotEvaluate("Block did not yield an value in " + maxSteps + " steps");
	}

	/**
	 * @param insn
	 * 		Instruction to follow.
	 * @param branching
	 * 		Branching result to follow.
	 *
	 * @return Next instruction to evaluate.
	 *
	 * @throws UnknownValueException
	 * 		When the branching result is {@link Branching#UNKNOWN}.
	 */
	@Nonnull
	private AbstractInsnNode followBranch(@Nonnull AbstractInsnNode insn, @Nonnull Branching branching)
			throws UnknownValueException {
		return switch (branching) {
			case TAKEN -> ((JumpInsnNode) insn).label;
			case NOT_TAKEN -> insn.getNext();
			case UNKNOWN -> throw UnknownValueException.INSTANCE;
		};
	}

	/**
	 * @param value
	 * 		Value to check.
	 *
	 * @return The value as an {@link ObjectValue} if it is one.
	 *
	 * @throws NestedEvaluationFailure
	 * 		When the value is not an {@link ObjectValue}.
	 */
	@Nonnull
	private ObjectValue requireObject(@Nonnull ReValue value) throws NestedEvaluationFailure {
		if (value instanceof ObjectValue objectValue)
			return objectValue;
		throw new NestedEvaluationFailure(new EvaluationFailureResult("Expected object value, but got: " + value, null));
	}

	/**
	 * @param value
	 * 		Value to check.
	 *
	 * @return The value as an {@link IntValue} if it has a known value.
	 *
	 * @throws UnknownValueException
	 * 		When the value is not an {@link IntValue} or has unknown value.
	 */
	@Nonnull
	private IntValue requireKnownInt(@Nonnull ReValue value) throws UnknownValueException {
		if (value instanceof IntValue intValue && intValue.hasKnownValue())
			return intValue;
		throw UnknownValueException.INSTANCE;
	}

	/**
	 * @param value
	 * 		Value to check.
	 *
	 * @return The value as an {@link ObjectValue} if it has known nullness.
	 *
	 * @throws UnknownValueException
	 * 		When the value is not an {@link ObjectValue} or has unknown nullness.
	 */
	@Nonnull
	private ObjectValue requireKnownNullness(@Nonnull ReValue value) throws UnknownValueException {
		if (value instanceof ObjectValue objectValue && objectValue.nullness() != Nullness.UNKNOWN)
			return objectValue;
		throw UnknownValueException.INSTANCE;
	}

	/**
	 * Handles {@code IF_ACMPEQ} and {@code IF_ACMPNE} instructions.
	 *
	 * @param left
	 * 		Left value to compare.
	 * @param right
	 * 		Right value to compare.
	 *
	 * @return Branching result of the comparison.
	 *
	 * @throws UnknownValueException
	 * 		When the comparison cannot be determined due to unknown values.
	 */
	@Nonnull
	private Branching referenceBranching(@Nonnull ReValue left, @Nonnull ReValue right)
			throws UnknownValueException {
		// The illusion of free choice...
		if (left == right)
			return Branching.TAKEN;

		if (left instanceof ObjectValue leftObject && right instanceof ObjectValue rightObject) {
			// If both values are known to be null, the branch is taken.
			if (leftObject.isNull() && rightObject.isNull())
				return Branching.TAKEN;

			// Distinct known non-null wrappers cannot be the same reference (see the `==` check above).
			if (leftObject.isNotNull() && rightObject.isNotNull())
				return Branching.NOT_TAKEN;
		}

		throw UnknownValueException.INSTANCE;
	}

	/**
	 * @param className
	 * 		Internal name of the class to get.
	 *
	 * @return Class node for the given class, or {@code null} if the class is not in the workspace.
	 */
	@Nullable
	private ClassNode getNode(@Nonnull String className) {
		ClassPathNode classPath = workspace.findClass(evaluateInternals, className);
		if (classPath == null)
			return null;

		ClassNode classNode = new ClassNode();
		classPath.getValue().asJvmClass().getClassReader().accept(classNode, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
		return classNode;
	}

	/**
	 * @param className
	 * 		Internal name of the class to check.
	 *
	 * @return {@code true} if the class is concrete <i>(not abstract or an interface)</i> and can be instantiated.
	 */
	private boolean isConcreteClass(@Nonnull String className) {
		ClassPathNode classPath = workspace.findClass(evaluateInternals, className);
		if (classPath == null)
			return false;
		int access = classPath.getValue().asJvmClass().getAccess();
		return !AccessFlag.isAbstract(access) && !AccessFlag.isInterface(access);
	}

	/**
	 * Resolves a workspace implementation for an instance call:
	 * <ul>
	 *     <li>INVOKEVIRTUAL: Uses the receiver's concrete type to find a concrete implementation of the method.</li>
	 *     <li>INVOKEINTERFACE: Uses the receiver's concrete type to find a concrete implementation of the method, or a default method from an interface.</li>
	 *     <li>INVOKESPECIAL: Uses the symbolic owner to find a concrete implementation of the method.</li>
	 * </ul>
	 *
	 * @param instruction
	 * 		Instance method instruction to resolve.
	 * @param receiver
	 * 		Receiver value being invoked.
	 * @param receiverType
	 * 		Internal name of the receiver class when the receiver is known to be {@code this},
	 * 		or {@code null} when that attribution is unavailable.
	 *
	 * @return Resolved method, or {@code null} if no concrete, unambiguous workspace implementation is available.
	 */
	@Nullable
	private ClassMethodPair resolveMethod(@Nonnull MethodInsnNode instruction,
	                                      @Nonnull ReValue receiver,
	                                      @Nullable String receiverType) {
		// Special calls use the symbolic owner exactly. No special resolution needed.
		if (instruction.getOpcode() == Opcodes.INVOKESPECIAL)
			return resolveConcreteMethod(getNode(instruction.owner), instruction.name, instruction.desc);

		// Virtual and interface calls use the receiver's concrete type when available, otherwise the symbolic owner.
		// First get the receiver's concrete type, if it is known.
		String receiverClassName = receiver instanceof InstancedObjectValue<?> instanced
				? instanced.type().getInternalName() : receiverType;
		if (receiverClassName == null)
			return null;

		// Walk up the class hierarchy to find a concrete implementation of the method.
		Set<String> visitedClasses = new HashSet<>();
		String className = receiverClassName;
		while (className != null && visitedClasses.add(className)) {
			ClassNode classNode = getNode(className);
			if (classNode == null)
				break;

			// Check for a concrete implementation of the method in this class.
			ClassMethodPair method = resolveConcreteMethod(classNode, instruction.name, instruction.desc);
			if (method != null)
				return method;

			// Continue looking in the parent class.
			className = classNode.superName;
		}

		// Interface defaults are selected only when one candidate is strictly most specific.
		List<ClassMethodPair> interfaceMethods = new ArrayList<>();
		Set<String> visitedInterfaces = new HashSet<>();
		Set<String> interfaceClassChain = new HashSet<>();
		className = receiverClassName;
		while (className != null && interfaceClassChain.add(className)) {
			ClassNode classNode = getNode(className);
			if (classNode == null)
				break;

			// Collect concrete implementations of the method in this class's interfaces and their parents.
			for (String interfaceName : classNode.interfaces)
				collectInterfaceMethods(interfaceName, instruction, visitedInterfaces, interfaceMethods);

			className = classNode.superName;
		}
		if (interfaceMethods.isEmpty())
			return null;

		// We have at least one candidate, but we need to find the most specific one.
		ClassMethodPair selected = null;
		for (ClassMethodPair candidate : interfaceMethods) {
			boolean isMostSpecific = true;
			for (ClassMethodPair other : interfaceMethods) {
				if (candidate == other)
					continue;

				// If the candidate is not assignable to the other, then it is not the most specific.
				if (!interpreter.isAssignableFrom(candidate.classNode().name, other.classNode().name)) {
					isMostSpecific = false;
					break;
				}
			}

			// If the candidate is not the most specific, skip it.
			if (!isMostSpecific)
				continue;

			// If we already have a selected candidate, then there is no unique most specific candidate.
			if (selected != null)
				return null;

			selected = candidate;
		}
		return selected;
	}

	/**
	 * Recursively collects concrete implementations of the given method in the given interface and its parent interfaces.
	 *
	 * @param interfaceName
	 * 		Internal name of the interface to check.
	 * @param instruction
	 * 		Method instruction to resolve.
	 * @param visited
	 * 		Set of visited interfaces to avoid cycles.
	 * @param methods
	 * 		Result list to add any found concrete methods to.
	 */
	private void collectInterfaceMethods(@Nonnull String interfaceName, @Nonnull MethodInsnNode instruction,
	                                     @Nonnull Set<String> visited, @Nonnull List<ClassMethodPair> methods) {
		// Skip visited to avoid cycles.
		if (!visited.add(interfaceName))
			return;

		// Must be a known interface in the workspace.
		ClassNode interfaceNode = getNode(interfaceName);
		if (interfaceNode == null)
			return;

		// Check for a concrete implementation of the method in this interface.
		ClassMethodPair method = resolveConcreteMethod(interfaceNode, instruction.name, instruction.desc);
		if (method != null)
			methods.add(method);

		// Continue looking in the parent interfaces.
		for (String parentName : interfaceNode.interfaces)
			collectInterfaceMethods(parentName, instruction, visited, methods);
	}

	/**
	 * Resolves a concrete implementation of the given method in the given class.
	 *
	 * @param classNode
	 * 		Class to check for a concrete implementation of the method.
	 * @param methodName
	 * 		Name of the method to check for.
	 * @param descriptor
	 * 		Descriptor of the method to check for.
	 *
	 * @return The concrete method if found, or {@code null} if no concrete implementation exists in the given class.
	 */
	@Nullable
	private ClassMethodPair resolveConcreteMethod(@Nullable ClassNode classNode,
	                                              @Nonnull String methodName,
	                                              @Nonnull String descriptor) {
		if (classNode == null)
			return null;

		// Check for a concrete implementation of the method in this class.
		for (MethodNode methodNode : classNode.methods) {
			if (methodName.equals(methodNode.name)
					&& descriptor.equals(methodNode.desc)
					&& !AccessFlag.isAbstract(methodNode.access)
					&& !AccessFlag.isNative(methodNode.access)
					&& methodNode.instructions != null
					&& methodNode.instructions.size() > 0)
				return new ClassMethodPair(classNode, methodNode);
		}

		return null;
	}

	/** Frame extension to support control flow processing of this evaluator. */
	private class ExecutingFrame extends ReFrame implements Opcodes {
		@Nullable
		private final MethodNode method;
		private final EvaluationContext context;
		private final ExceptionHandler exceptionHandler;
		private AbstractInsnNode next;
		private ReValue returnValue;
		private final boolean isStatic;
		@Nullable
		private final String currentClassName;

		public ExecutingFrame(@Nonnull MethodNode method, @Nonnull EvaluationContext context,
		                      @Nullable String currentClassName) {
			this(method, method.maxLocals, method.maxStack, method.access, context, currentClassName);
		}

		public ExecutingFrame(@Nullable MethodNode method, int maxLocals, int maxStack, int access,
		                      @Nonnull EvaluationContext context) {
			this(method, maxLocals, maxStack, access, context, null);
		}

		public ExecutingFrame(@Nullable MethodNode method, int maxLocals, int maxStack, int access,
		                      @Nonnull EvaluationContext context, @Nullable String currentClassName) {
			super(null, maxLocals, maxStack);

			this.method = method;
			this.context = context;
			this.exceptionHandler = new ExceptionHandler(interpreter, method, context::stackTrace);
			this.currentClassName = currentClassName;

			isStatic = AccessFlag.isStatic(access);
		}

		/**
		 * @return Top value of the stack without popping it.
		 */
		@Nonnull
		public ReValue peek() {
			return getStack(getStackSize() - 1);
		}

		/**
		 * Determines if the given instruction can be evaluated by {@link #evaluate(AbstractInsnNode, ReInterpreter)}.
		 *
		 * @param insn
		 * 		Instruction to evaluate.
		 * @param interpreter
		 * 		Interpreter to evaluate with.
		 * @param instructionScope
		 * 		Method or block instructions used for scoped support checks.
		 *
		 * @return {@code true} when the given instruction can be evaluated via {@link #evaluate(AbstractInsnNode, ReInterpreter)}.
		 */
		public boolean canEvaluateInsn(@Nonnull AbstractInsnNode insn, @Nonnull ReInterpreter interpreter,
		                               @Nonnull InsnList instructionScope) {
			return switch (insn.getOpcode()) {
				case JSR, RET // Legacy instructions
						-> false;
				case ALOAD -> true;
				case LDC -> {
					// Dynamic linking + method handles not supported
					Object cst = ((LdcInsnNode) insn).cst;
					yield !(cst instanceof ConstantDynamic || cst instanceof Handle);
				}
				case ATHROW -> true;
				case NEW -> insn instanceof TypeInsnNode tin && (instanceFactory.isSupportedType(tin.desc)
						|| exceptionHandler.isThrowableType(tin.desc)
						|| isConcreteClass(tin.desc));
				case INVOKESPECIAL, INVOKEINTERFACE, INVOKEVIRTUAL -> {
					if (insn instanceof MethodInsnNode min) {
						// Object initializer is no-op and inherently safe to evaluate.
						if (min.getOpcode() == INVOKESPECIAL && min.owner.equals("java/lang/Object")
								&& min.name.equals("<init>") && min.desc.equals("()V"))
							yield true;

						// Check if the method is a known exception constructor or stack trace getter.
						// These are special cases in our evaluation engine and are safe to evaluate.
						if (exceptionHandler.isThrowableConstructor(min) || exceptionHandler.isThrowableGetStackTrace(min))
							yield true;

						// Check if the method can be instanced.
						if (instanceFactory.getMethodHandler(min) != null || instanceFactory.getMapper(min) != null)
							yield true;
						// Lambda calls are eligible only when this scope creates the matching functional value.
						if (min.getOpcode() != INVOKESPECIAL
								&& InvokeDynamicExecutor.canEvaluateLambdaInvocation(min, instructionScope))
							yield true;

						// Check if the symbolic owner exists for runtime receiver dispatch.
						ClassPathNode targetClassPath = workspace.findClass(evaluateInternals, min.owner);
						if (targetClassPath != null)
							yield true;

						// Check if we have a value lookup for the method.
						InvokeVirtualLookup lookup = interpreter.getInvokeVirtualLookup();
						yield lookup != null && lookup.hasLookup(min);
					}
					yield false;
				}
				case INVOKEDYNAMIC ->
						insn instanceof InvokeDynamicInsnNode indy && InvokeDynamicExecutor.canEvaluate(indy);
				case INVOKESTATIC -> {
					if (insn instanceof MethodInsnNode min) {
						// Check if the method can be instanced.
						if (instanceFactory.getMapper(min) != null || instanceFactory.getMethodHandler(min) != null)
							yield true;

						// Check if the method is declared in the workspace, meaning we can evaluate it.
						ClassPathNode targetClassPath = workspace.findClass(evaluateInternals, min.owner);
						if (targetClassPath != null)
							yield true;

						// Check if we have a value lookup for the method.
						InvokeStaticLookup lookup = interpreter.getInvokeStaticLookup();
						yield lookup != null && lookup.hasLookup(min);
					}
					yield false;
				}
				default -> true;
			};
		}

		/**
		 * Wrapper for {@link #execute(AbstractInsnNode, Interpreter)}.
		 *
		 * @param insn
		 * 		Instruction to evaluate.
		 * @param interpreter
		 * 		Interpreter to evaluate with.
		 *
		 * @return Next instruction to evaluate <i>(following control flow rules)</i>.
		 *
		 * @throws AnalyzerException
		 * 		When the instruction cannot be evaluated.
		 * @throws NoNextException
		 * 		When there is no next instruction to execute.
		 * @throws UnknownValueException
		 * 		When a branch depends on an unknown value.
		 * @throws ExceptionHandler.ThrownException
		 * 		When an exception is thrown during evaluation.
		 * @throws NestedEvaluationFailure
		 * 		When a nested evaluation fails and yields a result.
		 */
		@Nonnull
		public AbstractInsnNode evaluate(@Nonnull AbstractInsnNode insn, @Nonnull ReInterpreter interpreter)
				throws AnalyzerException, NoNextException, UnknownValueException,
				ExceptionHandler.ThrownException, NestedEvaluationFailure {
			ReValue implicitException = exceptionHandler.knownFault(insn, this);
			if (implicitException != null)
				return exceptionHandler.routeException(this, implicitException, insn);

			AbstractInsnNode next = switch (insn.getOpcode()) {
				case GOTO -> ((JumpInsnNode) insn).label;
				case IFEQ -> conditional(insn, i -> i.isEqualTo(0));
				case IFNE -> conditional(insn, i -> i.isNotEqualTo(0));
				case IFLT -> conditional(insn, i -> i.isLessThan(0));
				case IFGE -> conditional(insn, i -> i.isGreaterThanOrEqual(0));
				case IFGT -> conditional(insn, i -> i.isGreaterThan(0));
				case IFLE -> conditional(insn, i -> i.isLessThanOrEqual(0));
				case IFNULL -> {
					ObjectValue value = requireKnownNullness(pop());
					yield followBranch(insn, value.isNull() ? Branching.TAKEN : Branching.NOT_TAKEN);
				}
				case IFNONNULL -> {
					ObjectValue value = requireKnownNullness(pop());
					yield followBranch(insn, value.isNotNull() ? Branching.TAKEN : Branching.NOT_TAKEN);
				}
				case IF_ICMPEQ -> conditional(insn, IntValue::isEqualTo);
				case IF_ICMPNE -> conditional(insn, IntValue::isNotEqualTo);
				case IF_ICMPLT -> conditional(insn, IntValue::isLessThan);
				case IF_ICMPGE -> conditional(insn, IntValue::isGreaterThanOrEqual);
				case IF_ICMPGT -> conditional(insn, IntValue::isGreaterThan);
				case IF_ICMPLE -> conditional(insn, IntValue::isLessThanOrEqual);
				case IF_ACMPEQ -> {
					ReValue value2 = pop();
					ReValue value1 = pop();
					yield followBranch(insn, referenceBranching(value1, value2));
				}
				case IF_ACMPNE -> {
					ReValue value2 = pop();
					ReValue value1 = pop();
					yield followBranch(insn, referenceBranching(value1, value2).invert());
				}
				case TABLESWITCH -> {
					ReValue value = pop();
					if (insn instanceof TableSwitchInsnNode table) {
						IntValue iv = requireKnownInt(value);
						int arg = iv.value().getAsInt();
						int keyIndex = (arg > table.max || arg < table.min) ? -1 : (arg - table.min);
						yield keyIndex == -1 ? table.dflt : table.labels.get(keyIndex);
					} else {
						throw new AnalyzerException(insn, "Invalid table-switch state");
					}
				}
				case LOOKUPSWITCH -> {
					ReValue value = pop();
					if (insn instanceof LookupSwitchInsnNode table) {
						IntValue iv = requireKnownInt(value);
						int arg = iv.value().getAsInt();
						int keyIndex = -1;
						for (int j = 0; j < table.keys.size(); j++) {
							int key = table.keys.get(j);
							if (arg == key) {
								keyIndex = j;
								break;
							}
						}
						yield keyIndex == -1 ? table.dflt : table.labels.get(keyIndex);
					} else {
						throw new AnalyzerException(insn, "Invalid lookup-switch state");
					}
				}
				case IRETURN, LRETURN, FRETURN, DRETURN, ARETURN -> {
					returnValue = peek();
					yield insn;
				}
				case RETURN -> {
					returnValue = UninitializedValue.UNINITIALIZED_VALUE;
					yield insn;
				}
				case ATHROW -> {
					ReValue value = pop();
					if (value instanceof ObjectValue object && object.isNull())
						yield exceptionHandler.routeException(this,
								exceptionHandler.newThrowable("java/lang/NullPointerException", null), insn);
					if (!(value instanceof ObjectValue) || !exceptionHandler.isThrowableType(value.type().getInternalName()))
						throw new AnalyzerException(insn, "ATHROW value is not throwable");
					yield exceptionHandler.routeException(this, value, insn);
				}
				case NEW -> {
					if (insn instanceof TypeInsnNode tin) {
						// NEW is a JVM active-use boundary, so run static initialization before allocation.
						EvaluationResult initializationResult = initializeClassIfNeeded(tin.desc, context);
						if (initializationResult instanceof EvaluationFailureResult failure)
							throw new NestedEvaluationFailure(failure);
						if (initializationResult instanceof EvaluationThrowsResult(ReValue exception))
							yield exceptionHandler.routeException(this, exception, insn);

						// Now allocate the instance.
						// If the type is a known throwable, we will use the exception handler to create it.
						Type type = Type.getObjectType(tin.desc);
						if (exceptionHandler.isThrowableType(tin.desc)) {
							push(exceptionHandler.newThrowable(tin.desc, null));
						} else {
							InstancedObjectValue<?> instance = new InstancedObjectValue<>(type);
							initializeInstanceFields(instance, tin.desc);
							push(instance);
						}
						yield insn.getNext();
					}
					throw new AnalyzerException(insn, "Invalid new state");
				}
				case GETFIELD -> {
					if (insn instanceof FieldInsnNode fieldInsn) {
						// Try to get the field value from the instance cache.
						ReValue receiver = peek();
						ReValue value = fieldCacheManager.getInstanceFieldCache(receiver)
								.getField(fieldInsn.owner, fieldInsn.name, fieldInsn.desc);
						if (value != null) {
							pop(); // Pop receiver
							push(value); // Push field value
							yield insn.getNext();
						}

						// Fall back to normal execution, which can handle some remaining cases.
						execute(insn, interpreter);
						yield insn.getNext();
					}
					throw new AnalyzerException(insn, "Invalid getfield state");
				}
				case GETSTATIC -> {
					if (insn instanceof FieldInsnNode fieldInsn) {
						// GETSTATIC is a JVM active-use boundary,so run static initialization before cache lookup.
						EvaluationResult initializationResult = initializeClassIfNeeded(fieldInsn.owner, context);
						if (initializationResult instanceof EvaluationFailureResult failure)
							throw new NestedEvaluationFailure(failure);
						if (initializationResult instanceof EvaluationThrowsResult(ReValue exception))
							yield exceptionHandler.routeException(this, exception, insn);

						// Try to get the field value from the static cache.
						ReValue value = fieldCacheManager.getStaticFieldCache(fieldInsn.owner)
								.getField(fieldInsn.owner, fieldInsn.name, fieldInsn.desc);
						if (value != null) {
							push(value);
							yield insn.getNext();
						}

						// Fall back to normal execution, which can handle some remaining cases.
						execute(insn, interpreter);
						yield insn.getNext();
					}
					throw new AnalyzerException(insn, "Invalid getstatic state");
				}
				case PUTFIELD -> {
					// Assign the top value to the instance field in the cache.
					if (insn instanceof FieldInsnNode fin) {
						ReValue value = pop();
						ReValue receiver = pop();
						fieldCacheManager.getInstanceFieldCache(receiver)
								.setField(fin.owner, fin.name, fin.desc, value);
						yield insn.getNext();
					}
					throw new AnalyzerException(insn, "Invalid putfield state");
				}
				case PUTSTATIC -> {
					if (insn instanceof FieldInsnNode fin) {
						// PUTSTATIC is a JVM active-use boundary, so run static initialization before caching the write.
						EvaluationResult initializationResult = initializeClassIfNeeded(fin.owner, context);
						if (initializationResult instanceof EvaluationFailureResult failure)
							throw new NestedEvaluationFailure(failure);
						if (initializationResult instanceof EvaluationThrowsResult(ReValue exception))
							yield exceptionHandler.routeException(this, exception, insn);

						// Assign the top value to the static field in the cache.
						ReValue value = pop();
						fieldCacheManager.getStaticFieldCache(fin.owner)
								.setField(fin.owner, fin.name, fin.desc, value);
						yield insn.getNext();
					}
					throw new AnalyzerException(insn, "Invalid putstatic state");
				}
				case INVOKESPECIAL -> {
					if (insn instanceof MethodInsnNode min) {
						String methodDescriptor = min.desc;

						// Throwable constructors are modeled so their stack traces remain evaluator-owned.
						if (exceptionHandler.isThrowableConstructor(min)) {
							for (int i = Type.getArgumentCount(methodDescriptor); i > 0; --i)
								pop();
							pop();
							yield insn.getNext();
						}

						// Collect workspace-call arguments and the receiver in their original order.
						List<ReValue> valueList = new ArrayList<>();
						for (int i = Type.getArgumentCount(methodDescriptor); i > 0; --i)
							valueList.addFirst(pop());
						ObjectValue receiver = requireObject(pop());

						// Registered host mappers remain the only allocation path for supported host-backed types.
						InstanceMapper mapper = instanceFactory.getMapper(min);
						if (mapper != null) {
							if (receiver instanceof InstancedObjectValue<?> instancedReceiver) {
								try {
									Object instance = mapper.map(instancedReceiver, valueList);
									instancedReceiver.setRealInstance(Unchecked.cast(instance));
								} catch (Throwable t) {
									// A failed registered mapper must surface instead of leaving the host-backed receiver uninitialized.
									yield exceptionHandler.routeException(this, exceptionHandler.newThrowable(t), insn);
								}
							}
							yield insn.getNext();
						}

						// Object initialization is the one unconditional no-op constructor.
						if (min.owner.equals("java/lang/Object") && min.name.equals("<init>") && methodDescriptor.equals("()V"))
							yield insn.getNext();

						// Extract current class name for method resolution using 'this' receiver.
						String resolutionClassName = !isStatic
								&& currentClassName != null
								&& getLocals() > 0
								&& receiver == getLocal(0)
								? currentClassName : null;

						// Resolve the method to evaluate, preferring the receiver type when available, and falling back to the current class.
						ClassMethodPair resolvedMethod = resolveMethod(min, receiver, resolutionClassName);
						if (resolvedMethod != null) {
							EvaluationResult result = Evaluator.this.evaluate(resolvedMethod.classNode(), resolvedMethod.methodNode(),
									receiver, valueList, context);
							switch (result) {
								case EvaluationYieldResult yielded -> {
									if (Type.getReturnType(min.desc) != Type.VOID_TYPE)
										push(yielded.value());
									yield insn.getNext();
								}
								case EvaluationThrowsResult thrown -> {
									yield exceptionHandler.routeException(this, thrown.exception(), insn);
								}
								case EvaluationFailureResult failure -> throw new NestedEvaluationFailure(failure);
							}
						}
						if (isWorkspaceClass(receiver, resolutionClassName))
							throw new NestedEvaluationFailure(EvaluationResult.cannotEvaluate(
									"Invoke-special call could not be resolved: " + min.owner + '.' + min.name + min.desc));

						// Fall back to normal interpreter execution, which can handle some remaining cases, including value lookups.
						// - Need to unmap values here since the underlying lookup system doesn't know how to handle our wrapped values.
						valueList.addFirst(receiver);
						List<ReValue> unmappedValueList = unmapValues(valueList);
						if (Type.getReturnType(min.desc) != Type.VOID_TYPE)
							push(interpreter.naryOperation(insn, unmappedValueList));
						else
							interpreter.naryOperation(insn, unmappedValueList);
						yield insn.getNext();
					}
					throw new AnalyzerException(insn, "Invalid invokespecial state");
				}
				case INVOKEVIRTUAL, INVOKEINTERFACE -> {
					if (insn instanceof MethodInsnNode min) {
						// Collect parameters.
						List<ReValue> valueList = new ArrayList<>();
						for (int i = Type.getArgumentCount(min.desc); i > 0; --i)
							valueList.addFirst(pop());

						// Get the receiver and check if it's a throwable, in which case we can handle the getStackTrace call.
						ObjectValue receiver = requireObject(pop());
						if (exceptionHandler.isThrowableGetStackTrace(min) && receiver instanceof ThrowableValue throwable) {
							// TODO: Not all throwable types fill in the stack trace, we just assume they do here.
							push(exceptionHandler.createStackTrace(throwable));
							yield insn.getNext();
						}

						// Dispatch modeled lambdas through workspace evaluation instead of host metafactory execution.
						if (receiver instanceof InvokeDynamicExecutor.EvaluatedLambdaValue lambda
								&& lambda.supportsInvocation(min)) {
							EvaluationResult result = evaluateLambda(lambda, valueList, context);
							switch (result) {
								case EvaluationYieldResult yielded -> {
									push(yielded.value());
									yield insn.getNext();
								}
								case EvaluationThrowsResult thrown -> {
									yield exceptionHandler.routeException(this, thrown.exception(), insn);
								}
								case EvaluationFailureResult failure -> throw new NestedEvaluationFailure(failure);
							}
						}

						// Check if we can handle the invoke with instance support or a value lookup.
						boolean isVoid = Type.getReturnType(min.desc) == Type.VOID_TYPE;
						if (receiver instanceof InstancedObjectValue<?> instancedReceiver && instancedReceiver.getRealInstance() != null) {
							MethodInvokeHandler<?> handler = instanceFactory.getMethodHandler(min);
							if (handler != null) {
								try {
									ReValue result = handler.invoke(this, instancedReceiver, Unchecked.cast(instancedReceiver.getRealInstance()), valueList);
									if (isVoid)
										yield insn.getNext();
									if (result != null) {
										push(result);
										yield insn.getNext();
									}
								} catch (Throwable t) {
									yield exceptionHandler.routeException(this, exceptionHandler.newThrowable(t), insn);
								}
							}
						}

						// Extract current class name for method resolution using 'this' receiver.
						String resolutionClassName = !isStatic
								&& currentClassName != null
								&& getLocals() > 0
								&& receiver == getLocal(0)
								? currentClassName : null;

						// Resolve the method to evaluate, preferring the receiver type when available, and falling back to the current class.
						ClassMethodPair resolvedMethod = resolveMethod(min, receiver, resolutionClassName);
						if (resolvedMethod != null) {
							EvaluationResult result = Evaluator.this.evaluate(resolvedMethod.classNode(), resolvedMethod.methodNode(),
									receiver, valueList, context);
							switch (result) {
								case EvaluationYieldResult yielded -> {
									if (isVoid)
										yield insn.getNext();
									push(yielded.value());
									yield insn.getNext();
								}
								case EvaluationThrowsResult thrown -> {
									yield exceptionHandler.routeException(this, thrown.exception(), insn);
								}
								case EvaluationFailureResult failure -> throw new NestedEvaluationFailure(failure);
							}
						}

						if (isWorkspaceClass(receiver, resolutionClassName))
							throw new NestedEvaluationFailure(EvaluationResult.cannotEvaluate(
									"Invoke-Virtual/Interface call could not be resolved: " + min.owner + '.' + min.name + min.desc));

						// Fall back to normal interpreter execution, which can handle some remaining cases, including value lookups.
						// - Need to unmap values here since the underlying lookup system doesn't know how to handle our wrapped values.
						valueList.addFirst(receiver);
						List<ReValue> unmappedValueList = unmapValues(valueList);
						if (isVoid) {
							interpreter.naryOperation(insn, unmappedValueList);
						} else {
							push(interpreter.naryOperation(insn, unmappedValueList));
						}
						yield insn.getNext();
					}
					throw new AnalyzerException(insn, "Invalid invokevirtual/interface state");
				}
				case INVOKESTATIC -> {
					if (insn instanceof MethodInsnNode min) {
						// INVOKESTATIC is a JVM active-use boundary, so run static initialization before static dispatch.
						EvaluationResult initializationResult = initializeClassIfNeeded(min.owner, context);
						if (initializationResult instanceof EvaluationFailureResult failure)
							throw new NestedEvaluationFailure(failure);
						if (initializationResult instanceof EvaluationThrowsResult(ReValue exception))
							yield exceptionHandler.routeException(this, exception, insn);

						// Collect parameters.
						List<ReValue> valueList = new ArrayList<>();
						for (int i = Type.getArgumentCount(min.desc); i > 0; --i)
							valueList.addFirst(pop());

						// Check if we have a mapper for this method (assuming it is a static factory for a supported type)
						Type returnType = Type.getReturnType(min.desc);
						boolean isVoid = returnType == Type.VOID_TYPE;
						if (!isVoid) {
							InstanceMapper mapper = instanceFactory.getMapper(min);
							if (mapper != null) {
								try {
									InstancedObjectValue<?> returnValue = new InstancedObjectValue<>(returnType);
									Object value = mapper.map(returnValue, valueList);
									returnValue.setRealInstance(Unchecked.cast(value));
									push(returnValue);
									yield insn.getNext();
								} catch (Throwable t) {
									yield exceptionHandler.routeException(this, exceptionHandler.newThrowable(t), insn);
								}
							}
						}

						// Check if the method is defined in the workspace and can be evaluated.
						if (canEvaluate(min.owner, min.name, min.desc)) {
							EvaluationResult result = Evaluator.this.evaluate(min.owner, min.name, min.desc, null, valueList, context);
							switch (result) {
								case EvaluationYieldResult yielded -> {
									if (isVoid)
										yield insn.getNext();
									push(yielded.value());
									yield insn.getNext();
								}
								case EvaluationThrowsResult thrown -> {
									yield exceptionHandler.routeException(this, thrown.exception(), insn);
								}
								case EvaluationFailureResult failure -> {
									// No-op, fallthrough will attempt to handle this.
								}
							}
						}

						// Fall back to normal interpreter execution, which can handle some remaining cases, including value lookups.
						// - Need to unmap values here since the underlying lookup system doesn't know how to handle our wrapped values.
						List<ReValue> unmappedValueList = unmapValues(valueList);
						if (isVoid) {
							interpreter.naryOperation(insn, unmappedValueList);
						} else {
							push(interpreter.naryOperation(insn, unmappedValueList));
						}
						yield insn.getNext();
					}
					throw new AnalyzerException(insn, "Invalid invokestatic state");

				}
				case INVOKEDYNAMIC -> {
					if (insn instanceof InvokeDynamicInsnNode indy) {
						List<ReValue> valueList = new ArrayList<>();
						for (int i = Type.getArgumentCount(indy.desc); i > 0; --i)
							valueList.addFirst(pop());

						ReValue result = InvokeDynamicExecutor.evaluate(indy, valueList);
						push(result != null ? result : interpreter.naryOperation(insn, valueList));
						yield insn.getNext();
					}
					throw new AnalyzerException(insn, "Invalid invokedynamic state");
				}
				case JSR, RET -> {
					throw new UnsupportedOperationException();
				}

				case AASTORE -> {
					execute(insn, interpreter);
					yield insn.getNext();
				}
				default -> {
					if (insn.getOpcode() != -1) // Skip labels
						execute(insn, interpreter);
					yield insn.getNext();
				}
			};
			if (next == null)
				throw NoNextException.INSTANCE;
			return next;
		}

		/**
		 * Evaluates a modeled lambda invocation through the normal workspace method path.
		 *
		 * @param lambda
		 * 		Modeled lambda receiver.
		 * @param arguments
		 * 		Functional-interface invocation arguments.
		 * @param context
		 * 		Current evaluation context.
		 *
		 * @return Result of evaluating the static implementation method.
		 */
		@Nonnull
		private EvaluationResult evaluateLambda(@Nonnull InvokeDynamicExecutor.EvaluatedLambdaValue lambda,
		                                        @Nonnull List<ReValue> arguments,
		                                        @Nonnull EvaluationContext context) throws UnknownValueException {
			// Abort if the lambda implementation handle is not a static method.
			// At some later point we may want to support non-static lambda implementations, but for now we only support static ones.
			Handle handle = lambda.implementationHandle();
			if (handle.getTag() != Opcodes.H_INVOKESTATIC)
				return EvaluationResult.cannotEvaluate("Lambda implementation handle is not static");

			// Abort if the lambda invocation argument count does not match the static implementation method argument count.
			int invocationArgumentCount = lambda.instantiatedMethodType().getArgumentCount();
			if (arguments.size() != invocationArgumentCount)
				return EvaluationResult.cannotEvaluate("Mismatched lambda invocation argument count");

			// Capture preceding invocation arguments in the static implementation method descriptor.
			List<ReValue> targetArguments = new ArrayList<>(lambda.capturedValues().size() + arguments.size());
			targetArguments.addAll(lambda.capturedValues());
			targetArguments.addAll(arguments);

			// Abort if the descriptor of the static implementation method does not match the expected argument count.
			Type handleType = Type.getMethodType(handle.getDesc());
			if (handleType.getArgumentCount() != targetArguments.size())
				return EvaluationResult.cannotEvaluate("Mismatched lambda implementation argument count");

			// Delegate lookup, initialization, step accounting, and exception behavior to nested evaluation.
			return Evaluator.this.evaluate(handle.getOwner(), handle.getName(), handle.getDesc(),
					null, targetArguments, context);
		}

		/**
		 * @param insn
		 * 		Instruction to evaluate.
		 * @param cmp
		 * 		Comparison predicate to evaluate the popped value.
		 *
		 * @return Next instruction to evaluate, following the branch decision.
		 *
		 * @throws UnknownValueException
		 * 		When the popped value is unknown.
		 */
		@Nonnull
		private AbstractInsnNode conditional(@Nonnull AbstractInsnNode insn, @Nonnull Predicate<IntValue> cmp)
				throws UnknownValueException {
			IntValue value = requireKnownInt(pop());
			return followBranch(insn, cmp.test(value) ? Branching.TAKEN : Branching.NOT_TAKEN);
		}

		/**
		 * @param insn
		 * 		Instruction to evaluate.
		 * @param cmp
		 * 		Comparison predicate to evaluate the two popped values.
		 *
		 * @return Next instruction to evaluate, following the branch decision.
		 *
		 * @throws UnknownValueException
		 * 		When either of the two popped values is unknown.
		 */
		@Nonnull
		private AbstractInsnNode conditional(@Nonnull AbstractInsnNode insn, @Nonnull BiPredicate<IntValue, IntValue> cmp)
				throws UnknownValueException {
			IntValue value2 = requireKnownInt(pop());
			IntValue value1 = requireKnownInt(pop());
			return followBranch(insn, cmp.test(value1, value2) ? Branching.TAKEN : Branching.NOT_TAKEN);
		}

		/**
		 * Checks whether an unresolved instance call supposedly should exist in the workspace.
		 * This is called after {@link #resolveMethod(MethodInsnNode, ReValue, String)}, which means
		 * we only call this after a resolution attempt has failed.
		 *
		 * @param receiver
		 * 		Receiver of the unresolved instance call.
		 * @param currentClassName
		 * 		Current workspace class when the receiver is known to be {@code this}, or {@code null}.
		 *
		 * @return {@code true} when the receiver is a class defined in the workspace,
		 * and so the unresolved call must be treated as a conservative failure.
		 */
		private boolean isWorkspaceClass(@Nonnull ReValue receiver, @Nullable String currentClassName) {
			if (receiver instanceof InstancedObjectValue<?> instanced
					&& workspace.findClass(evaluateInternals, instanced.type().getInternalName()) != null)
				return true;
			return currentClassName != null && workspace.findClass(evaluateInternals, currentClassName) != null;
		}

		@Nonnull
		private static List<ReValue> unmapValues(@Nonnull List<ReValue> values) {
			return values.stream()
					.map(v -> {
						if (v instanceof InstancedObjectValue<?> instanced) return instanced.unmap();
						if (v instanceof ArrayValue array) {
							OptionalInt dimension = array.getFirstDimensionLength();
							if (dimension.isPresent()) {
								int length = dimension.getAsInt();
								List<ReValue> arrayValues = new ArrayList<>(length);
								for (int i = 0; i < length; i++) {
									ReValue element = array.getValue(i);
									arrayValues.add(element);
								}
								List<ReValue> unmappedArrayValues = unmapValues(arrayValues);
								return new ArrayValueImpl(array.type(), Nullness.NOT_NULL, length, unmappedArrayValues::get);
							}
							return array;
						}
						return v;
					})
					.toList();
		}
	}

	/**
	 * Frame for evaluation. Consider it like a {@link StackTraceElement}.
	 *
	 * @param className
	 * 		Name of the class being evaluated.
	 * @param methodName
	 * 		Name of the method being evaluated.
	 */
	private record EvaluationFrame(@Nonnull String className, @Nonnull String methodName) {}

	/** Context for evaluation, including the call stack, step allocation, and class initialization state. */
	private static final class EvaluationContext {
		private final List<EvaluationFrame> callStack = new ArrayList<>();
		private final Set<String> initializedClasses;
		private final Set<String> initializingClasses;
		private final Map<String, EvaluationResult> failedClassInitializers;
		private int stepAllocation;

		private EvaluationContext(int stepAllocation) {
			this.stepAllocation = stepAllocation;

			initializedClasses = new HashSet<>();
			initializingClasses = new HashSet<>();
			failedClassInitializers = new HashMap<>();
		}

		@Nonnull
		private List<StackTraceElement> stackTrace() {
			List<StackTraceElement> trace = new ArrayList<>(callStack.size());
			for (int i = callStack.size() - 1; i >= 0; i--) {
				EvaluationFrame frame = callStack.get(i);
				trace.add(new StackTraceElement(frame.className().replace('/', '.'), frame.methodName(), null, -1));
			}
			return trace;
		}
	}

	/** Dummy exception to signal a nested evaluation failure. */
	private static final class NestedEvaluationFailure extends Exception {
		private final EvaluationFailureResult result;

		private NestedEvaluationFailure(@Nonnull EvaluationFailureResult result) {
			this.result = result;
		}
	}

	/** Dummy exception to signal out-of-bounds flow in the evaluate methods. */
	private static class NoNextException extends Exception {
		private static final NoNextException INSTANCE = new NoNextException();

		private NoNextException() {}

		@Override
		public synchronized Throwable fillInStackTrace() {
			// Don't care.
			return this;
		}
	}

	/** Dummy exception to signal an unknown branch decision. */
	private static final class UnknownValueException extends Exception {
		private static final UnknownValueException INSTANCE = new UnknownValueException();

		private UnknownValueException() {}

		@Override
		public synchronized Throwable fillInStackTrace() {
			return this;
		}
	}
}
