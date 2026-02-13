package software.coley.recaf.util.analysis.eval;

import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Interpreter;
import software.coley.collections.Unchecked;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.util.AccessFlag;
import software.coley.recaf.util.BlwUtil;
import software.coley.recaf.util.analysis.Nullness;
import software.coley.recaf.util.analysis.ReFrame;
import software.coley.recaf.util.analysis.ReInterpreter;
import software.coley.recaf.util.analysis.lookup.InvokeStaticLookup;
import software.coley.recaf.util.analysis.lookup.InvokeVirtualLookup;
import software.coley.recaf.util.analysis.value.ArrayValue;
import software.coley.recaf.util.analysis.value.IntValue;
import software.coley.recaf.util.analysis.value.ObjectValue;
import software.coley.recaf.util.analysis.value.ReValue;
import software.coley.recaf.util.analysis.value.impl.ArrayValueImpl;
import software.coley.recaf.util.visitors.MemberFilteringVisitor;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.RuntimeWorkspaceResource;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * Simple method evaluator.
 *
 * @author Matt Coley
 */
public class Evaluator {
	private static final Object2BooleanMap<String> evaluationSupportCache = new Object2BooleanOpenHashMap<>();
	private static final InstanceFactory instanceFactory = new InstanceFactory();
	private final Workspace workspace;
	private final ReInterpreter interpreter;
	private final FieldCacheManager fieldCacheManager;
	private final boolean evaluateInternals;
	private final int maxSteps;
	private int stepAllocation;

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
	 */
	public Evaluator(@Nonnull Workspace workspace, @Nonnull ReInterpreter interpreter,
	                 @Nonnull FieldCacheManager fieldCacheManager, int maxSteps, boolean evaluateInternals) {
		this.workspace = workspace;
		this.interpreter = interpreter;
		this.fieldCacheManager = fieldCacheManager;
		this.maxSteps = maxSteps;
		this.stepAllocation = maxSteps;
		this.evaluateInternals = evaluateInternals;
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
		String key = className + '.' + methodName + methodDescriptor;
		return evaluationSupportCache.computeIfAbsent(key, k -> {
			// Find class in workspace.
			ClassPathNode classPath = workspace.findClass(evaluateInternals, className);
			if (classPath == null)
				return false;

			// Ensure method exists in class.
			JvmClassInfo jvmClass = classPath.getValue().asJvmClass();
			MethodMember method = jvmClass.getDeclaredMethod(methodName, methodDescriptor);
			if (method == null)
				return false;

			// Extract method-node model and delegate to evaluate check.
			ClassNode node = new ClassNode();
			jvmClass.getClassReader().accept(new MemberFilteringVisitor(node, method), ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
			return node.methods.size() == 1 && canEvaluate(node.methods.getFirst());
		});
	}

	/**
	 * @param method
	 * 		Method to check for evaluation support.
	 *
	 * @return {@code true} when all instructions in the method can be evaluated.
	 */
	public boolean canEvaluate(@Nonnull MethodNode method) {
		// Cannot be abstract / have no instructions.
		if (method.instructions == null || method.instructions.size() == 0)
			return false;

		// Must not have any unsupported instructions
		ExecutingFrame frame = new ExecutingFrame(method);
		for (AbstractInsnNode instruction : method.instructions)
			if (!frame.canEvaluateInsn(instruction, interpreter))
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
		ExecutingFrame frame = new ExecutingFrame(0xFF, 0xFF, methodAccess);
		for (AbstractInsnNode instruction : instructionBlock)
			if (!frame.canEvaluateInsn(instruction, interpreter))
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
	                                 @Nullable ReValue classInstance,
	                                 @Nonnull List<ReValue> parameters) {
		Type methodType = Type.getMethodType(methodDescriptor);
		if (methodType.getReturnType() == Type.VOID_TYPE)
			return EvaluationResult.cannotEvaluate("Method must yield a value");

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
				return evaluate(classNode, methodNode, classInstance, parameters);

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
	                                 @Nullable ReValue classInstance,
	                                 @Nonnull List<ReValue> parameters) {
		// Must support evaluation
		if (!canEvaluate(methodNode))
			return EvaluationResult.cannotEvaluate("Target method does not support evaluation: " + classNode.name + "." + methodNode.name + methodNode.desc);

		// Sanity check parameters
		Type methodType = Type.getMethodType(methodNode.desc);
		if (parameters.size() != methodType.getArgumentCount())
			return EvaluationResult.cannotEvaluate("Mismatched parameter count, method expects "
					+ methodType.getArgumentCount() + " but was given " + parameters.size() + " parameters");
		Type[] argumentTypes = methodType.getArgumentTypes();
		for (int i = 0; i < argumentTypes.length; i++) {
			int expectedSort = argumentTypes[i].getSort();
			int actualSort = parameters.get(i).type().getSort();
			if (expectedSort != actualSort && !(expectedSort <= Type.INT && actualSort < Type.INT))
				return EvaluationResult.cannotEvaluate("Mismatched parameter type at index " + i);
		}

		// Create initial frame
		ExecutingFrame frame = new ExecutingFrame(methodNode);
		for (int i = 0; i < methodNode.maxLocals; i++)
			frame.setLocal(i, i < parameters.size() ? parameters.get(i) : interpreter.newEmptyValue(i));
		if (!AccessFlag.isStatic(methodNode.access))
			frame.setLocal(0, classInstance);

		// Handle execution
		InsnList instructions = methodNode.instructions;
		AbstractInsnNode pc = instructions.getFirst();
		while (stepAllocation > 0) {
			try {
				pc = frame.evaluate(pc, interpreter);
				ReValue retVal = frame.returnValue;
				if (retVal != null) {
					if (retVal instanceof InstancedObjectValue<?> instanced && instanced.getRealInstance() != null)
						retVal = instanced.unmap();
					return new EvaluationYieldResult(retVal);
				}
			} catch (AnalyzerException e) {
				return EvaluationResult.cannotEvaluate("Failed executing instruction: " + BlwUtil.toString(pc), e);
			} catch (NoNextException e) {
				return EvaluationResult.cannotEvaluate("Execution falls through end", e);
			}
			stepAllocation--;
		}
		return EvaluationResult.cannotEvaluate("Method did not yield an value in " + maxSteps + " steps");
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
		ExecutingFrame frame = new ExecutingFrame(originFrame.getLocals(), originFrame.getMaxStackSize(), methodAccess);
		for (int i = 0; i < originFrame.getLocals(); i++)
			frame.setLocal(i, originFrame.getLocal(i));

		// Handle execution
		AbstractInsnNode pc = instructionBlock.getFirst();
		while (stepAllocation > 0) {
			try {
				pc = frame.evaluate(pc, interpreter);

				// Check if return instruction assigned a value.
				if (frame.returnValue != null)
					return new EvaluationYieldResult(frame.returnValue);
			} catch (AnalyzerException e) {
				return EvaluationResult.cannotEvaluate("Failed executing instruction: " + BlwUtil.toString(pc), e);
			} catch (NoNextException e) {
				// If there is no next instruction from the given block, then control flow has exited the block.
				// The intended use case for this is to be given incomplete segments of code and see what's on the
				// top at the end, so we will yield that here.
				return new EvaluationYieldResult(frame.getStack(frame.getStackSize() - 1));
			}
			stepAllocation--;
		}
		return EvaluationResult.cannotEvaluate("Block did not yield an value in " + maxSteps + " steps");
	}

	/** Frame extension to support control flow processing of this evaluator. */
	private class ExecutingFrame extends ReFrame implements Opcodes {
		private AbstractInsnNode next;
		private ReValue returnValue;
		private final boolean isStatic;

		public ExecutingFrame(@Nonnull MethodNode method) {
			this(method.maxLocals, method.maxStack, method.access);
		}

		public ExecutingFrame(int maxLocals, int maxStack, int access) {
			super(null, maxLocals, maxStack);

			isStatic = AccessFlag.isStatic(access);
		}

		/**
		 * Determines if the given instruction can be evaluated by {@link #evaluate(AbstractInsnNode, ReInterpreter)}.
		 *
		 * @param insn
		 * 		Instruction to evaluate.
		 * @param interpreter
		 * 		Interpreter to evaluate with.
		 *
		 * @return {@code true} when the given instruction can be evaluated via {@link #evaluate(AbstractInsnNode, ReInterpreter)}.
		 */
		public boolean canEvaluateInsn(@Nonnull AbstractInsnNode insn, @Nonnull ReInterpreter interpreter) {
			return switch (insn.getOpcode()) {
				case JSR, RET, // Legacy instructions
				     INVOKEDYNAMIC // Dynamic linking not supported
						-> false;
				case ALOAD -> {
					// Local variable 'this' is not supported until we make some form of instance tracking
					int local = ((VarInsnNode) insn).var;
					yield isStatic || local != 0;
				}
				case LDC -> {
					// Dynamic linking + method handles not supported
					Object cst = ((LdcInsnNode) insn).cst;
					yield !(cst instanceof ConstantDynamic || cst instanceof Handle);
				}
				case ATHROW -> {
					// TODO: Need to finish control-flow handling for this, then this would yield true.
					yield false;
				}
				case NEW -> insn instanceof TypeInsnNode tin && instanceFactory.isSupportedType(tin.desc);
				case INVOKESPECIAL, INVOKEINTERFACE, INVOKEVIRTUAL -> {
					if (insn instanceof MethodInsnNode min) {
						// Check if the method can be instanced.
						if (instanceFactory.getMethodHandler(min) != null || instanceFactory.getMapper(min) != null)
							yield true;

						// Check if the method is declared in the workspace, meaning we can evaluate it.
						ClassPathNode targetClassPath = workspace.findClass(evaluateInternals, min.owner);
						if (targetClassPath != null)
							yield true;

						// Check if we have a value lookup for the method.
						InvokeVirtualLookup lookup = interpreter.getInvokeVirtualLookup();
						yield lookup != null && lookup.hasLookup(min);
					}
					yield false;
				}
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
		 */
		@Nonnull
		public AbstractInsnNode evaluate(@Nonnull AbstractInsnNode insn, @Nonnull ReInterpreter interpreter) throws AnalyzerException, NoNextException {
			AbstractInsnNode next = switch (insn.getOpcode()) {
				case GOTO -> ((JumpInsnNode) insn).label;
				case IFEQ -> conditional(insn, i -> i.isEqualTo(0));
				case IFNE -> conditional(insn, i -> i.isNotEqualTo(0));
				case IFLT -> conditional(insn, i -> i.isLessThan(0));
				case IFGE -> conditional(insn, i -> i.isGreaterThanOrEqual(0));
				case IFGT -> conditional(insn, i -> i.isGreaterThan(0));
				case IFLE -> conditional(insn, i -> i.isLessThanOrEqual(0));
				case IFNULL -> {
					ReValue value = pop();
					if (value instanceof ObjectValue ov && ov.isNull())
						yield ((JumpInsnNode) insn).label;
					yield insn.getNext();
				}
				case IFNONNULL -> {
					ReValue value = pop();
					if (value instanceof ObjectValue ov && ov.isNotNull())
						yield ((JumpInsnNode) insn).label;
					yield insn.getNext();
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
					if (value1 == value2)
						yield ((JumpInsnNode) insn).label;
					yield insn.getNext();
				}
				case IF_ACMPNE -> {
					ReValue value2 = pop();
					ReValue value1 = pop();
					if (value1 != value2)
						yield ((JumpInsnNode) insn).label;
					yield insn.getNext();
				}
				case TABLESWITCH -> {
					ReValue value = pop();
					if (insn instanceof TableSwitchInsnNode table && value instanceof IntValue iv && iv.hasKnownValue()) {
						int arg = iv.value().getAsInt();
						int keyIndex = (arg > table.max || arg < table.min) ? -1 : (arg - table.min);
						yield keyIndex == -1 ? table.dflt : table.labels.get(keyIndex);
					} else {
						throw new AnalyzerException(insn, "Invalid table-switch state");
					}
				}
				case LOOKUPSWITCH -> {
					ReValue value = pop();
					if (insn instanceof LookupSwitchInsnNode table && value instanceof IntValue iv && iv.hasKnownValue()) {
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
				case ATHROW -> {
					// TODO: Find handler and set instruction pointer 'insn' there with exception being only stack element
					//  - Need to also support this for other exception throwing behavior for other instructions
					//    - Math operations that fail (div by zero)
					//    - Null pointer exceptions from field/method ops
					//    - Array ops (index out of bounds, null pointer)
					throw new UnsupportedOperationException();
				}
				case NEW -> {
					if (insn instanceof TypeInsnNode tin) {
						push(new InstancedObjectValue<>(Type.getObjectType(tin.desc)));
						yield insn.getNext();
					}
					throw new AnalyzerException(insn, "Invalid new state");
				}
				case GETFIELD -> {
					if (insn instanceof FieldInsnNode fieldInsn) {
						// Try to get the field value from the instance cache.
						ReValue receiver = peek();
						ReValue value = fieldCacheManager.getInstanceFieldCache(receiver).getField(fieldInsn.name, fieldInsn.desc);
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
						// Try to get the field value from the static cache.
						ReValue value = fieldCacheManager.getStaticFieldCache(fieldInsn.owner).getField(fieldInsn.name, fieldInsn.desc);
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
						fieldCacheManager.getInstanceFieldCache(receiver).setField(fin.name, fin.desc, value);
						yield insn.getNext();
					}
					throw new AnalyzerException(insn, "Invalid putfield state");
				}
				case PUTSTATIC -> {
					// Assign the top value to the static field in the cache.
					if (insn instanceof FieldInsnNode fin) {
						ReValue value = pop();
						fieldCacheManager.getStaticFieldCache(fin.owner).setField(fin.name, fin.desc, value);
						yield insn.getNext();
					}
					throw new AnalyzerException(insn, "Invalid putstatic state");
				}
				case INVOKESPECIAL -> {
					if (insn instanceof MethodInsnNode min) {
						String methodDescriptor = min.desc;

						// Handle instance initialization for supported types.
						InstanceMapper mapper = instanceFactory.getMapper(min);
						if (mapper != null) {
							// Collect parameters.
							List<ReValue> valueList = new ArrayList<>();
							for (int i = Type.getArgumentCount(methodDescriptor); i > 0; --i)
								valueList.addFirst(pop());

							// Get the receiver and populate the instance if it's a supported type.
							ReValue receiver = pop();
							if (receiver instanceof InstancedObjectValue<?> instancedReceiver) {
								try {
									Object instance = mapper.map(instancedReceiver, valueList);
									instancedReceiver.setRealInstance(Unchecked.cast(instance));
								} catch (Throwable t) {
									// If the mapper fails, we can still fall back to normal execution, which may be able to handle some cases.
								}
							}
						} else {
							// Fall back to normal execution, which can handle some remaining cases, including value lookups.
							execute(insn, interpreter);
						}
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

						// Get the receiver and check if we can handle the invoke with instance support or a value lookup.
						ReValue receiver = pop();
						boolean isVoid = Type.getReturnType(min.desc) == Type.VOID_TYPE;
						if (receiver instanceof InstancedObjectValue<?> instancedReceiver && instancedReceiver.getRealInstance() != null) {
							MethodInvokeHandler<?> handler = instanceFactory.getMethodHandler(min);
							if (handler != null) {
								try {
									ReValue result = handler.invoke(instancedReceiver, Unchecked.cast(instancedReceiver.getRealInstance()), valueList);
									if (isVoid)
										yield insn.getNext();
									if (result != null) {
										push(result);
										yield insn.getNext();
									}
								} catch (Throwable t) {
									// TODO: Need to handle exception throwing control flow
									//  - Yield appropriate exception block handler instead of normal next instruction
									//  - Need to have some way to determine if the exception should be thrown (bad usage of method)
									//    vs a problem with our handler logic itself.
								}
							}
						}

						// Check if the method is defined in the workspace and can be evaluated.
						if (canEvaluate(min.owner, min.name, min.desc)) {
							EvaluationResult result = Evaluator.this.evaluate(min.owner, min.name, min.desc, receiver, valueList);
							switch (result) {
								case EvaluationYieldResult yielded -> {
									push(yielded.value());
									yield insn.getNext();
								}
								case EvaluationThrowsResult thrown -> {
									// TODO: Need to handle exception throwing control flow
									//  - Yield appropriate exception block handler instead of normal next instruction
								}
								case EvaluationFailureResult failure -> {
									// No-op, fallthrough will attempt to handle this.
								}
							}
						}

						// Fall back to normal execution, which can handle some remaining cases, including value lookups.
						// - Need to unmap values here since the underlying lookup system doesn't know how to handle our wrapped values.
						valueList.addFirst(receiver);
						List<ReValue> unmappedValueList = unmapValues(valueList);
						if (isVoid) {
							interpreter.naryOperation(insn, valueList);
						} else {
							push(interpreter.naryOperation(insn, valueList));
						}
						yield insn.getNext();
					}
					throw new AnalyzerException(insn, "Invalid invokevirtual/interface state");
				}
				case INVOKESTATIC -> {
					if (insn instanceof MethodInsnNode min) {
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
									// TODO: Need to handle exception throwing control flow
									//  - Yield appropriate exception block handler instead of normal next instruction
									//  - Need to have some way to determine if the exception should be thrown (bad usage of method)
									//    vs a problem with our handler logic itself.
								}
							}
						}

						// Check if the method is defined in the workspace and can be evaluated.
						if (canEvaluate(min.owner, min.name, min.desc)) {
							EvaluationResult result = Evaluator.this.evaluate(min.owner, min.name, min.desc, null, valueList);
							switch (result) {
								case EvaluationYieldResult yielded -> {
									push(yielded.value());
									yield insn.getNext();
								}
								case EvaluationThrowsResult thrown -> {
									// TODO: Need to handle exception throwing control flow
									//  - Yield appropriate exception block handler instead of normal next instruction
								}
								case EvaluationFailureResult failure -> {
									// No-op, fallthrough will attempt to handle this.
								}
							}
						}

						// Fall back to normal execution, which can handle some remaining cases, including value lookups.
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

		@Nonnull
		private AbstractInsnNode conditional(@Nonnull AbstractInsnNode insn, @Nonnull Predicate<IntValue> cmp) {
			ReValue value = pop();
			if (value instanceof IntValue iv && cmp.test(iv))
				return ((JumpInsnNode) insn).label;
			return insn.getNext();
		}

		@Nonnull
		private AbstractInsnNode conditional(@Nonnull AbstractInsnNode insn, @Nonnull BiPredicate<IntValue, IntValue> cmp) {
			ReValue value2 = pop();
			ReValue value1 = pop();
			if (value1 instanceof IntValue i1 && value2 instanceof IntValue i2 && cmp.test(i1, i2))
				return ((JumpInsnNode) insn).label;
			return insn.getNext();
		}

		@Nonnull
		public ReValue peek() {
			return peek(0);
		}

		@Nonnull
		public ReValue peek(int offset) {
			return getStack(getStackSize() - 1 - offset);
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
}
