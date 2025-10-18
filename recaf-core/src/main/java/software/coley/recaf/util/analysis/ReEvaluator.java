package software.coley.recaf.util.analysis;

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
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Interpreter;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.util.AccessFlag;
import software.coley.recaf.util.BlwUtil;
import software.coley.recaf.util.analysis.lookup.GetFieldLookup;
import software.coley.recaf.util.analysis.lookup.GetStaticLookup;
import software.coley.recaf.util.analysis.lookup.InvokeStaticLookup;
import software.coley.recaf.util.analysis.lookup.InvokeVirtualLookup;
import software.coley.recaf.util.analysis.value.IntValue;
import software.coley.recaf.util.analysis.value.ObjectValue;
import software.coley.recaf.util.analysis.value.ReValue;
import software.coley.recaf.util.visitors.MemberFilteringVisitor;
import software.coley.recaf.workspace.model.Workspace;

import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * Simple method evaluator.
 *
 * @author Matt Coley
 */
public class ReEvaluator {
	private final Workspace workspace;
	private final ReInterpreter interpreter;
	private final int maxSteps;

	/**
	 * @param workspace
	 * 		Workspace to pull classes from.
	 * @param interpreter
	 * 		Interpreter to evaluate instructions with.
	 * @param maxSteps
	 * 		Maximum number of steps to allow when evaluating a method.
	 */
	public ReEvaluator(@Nonnull Workspace workspace, @Nonnull ReInterpreter interpreter, int maxSteps) {
		this.workspace = workspace;
		this.interpreter = interpreter;
		this.maxSteps = maxSteps;
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
		ClassPathNode classPath = workspace.findClass(className);
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
	}

	/**
	 * @param method
	 * 		Method to check for evaluation support.
	 *
	 * @return {@code true} when all instructions in the method can be evaluated.
	 */
	public boolean canEvaluate(@Nonnull MethodNode method) {
		// Cannot be abstract / have no instructions.
		if (method.instructions == null)
			return false;

		// Must not have any unsupported instructions
		ExecutingFrame frame = new ExecutingFrame(method);
		for (AbstractInsnNode instruction : method.instructions)
			if (!frame.canEvaluate(instruction, interpreter))
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
			if (!frame.canEvaluate(instruction, interpreter))
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
	 * @return Return value of the target method when invoked with the given parameters.
	 *
	 * @throws ReEvaluationException
	 * 		When the target method could not be evaluated.
	 */
	@Nonnull
	public ReValue evaluate(@Nonnull String className,
	                        @Nonnull String methodName,
	                        @Nonnull String methodDescriptor,
	                        @Nullable ReValue classInstance,
	                        @Nonnull List<ReValue> parameters) throws ReEvaluationException {
		Type methodType = Type.getMethodType(methodDescriptor);
		if (methodType.getReturnType() == Type.VOID_TYPE)
			throw new ReEvaluationException("Method must yield a value");

		ClassPathNode classPath = workspace.findClass(true, className);
		if (classPath == null)
			throw new ReEvaluationException("Class not found in workspace: " + className);

		JvmClassInfo classInfo = classPath.getValue().asJvmClass();
		if (classInfo.getDeclaredMethod(methodName, methodDescriptor) == null)
			throw new ReEvaluationException("Method not found in class: " + className + "." + methodName + methodDescriptor);

		ClassNode classNode = new ClassNode();
		ClassReader reader = classInfo.getClassReader();
		reader.accept(classNode, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);

		for (MethodNode methodNode : classNode.methods)
			if (methodName.equals(methodNode.name) && methodDescriptor.equals(methodNode.desc))
				return evaluate(classNode, methodNode, classInstance, parameters);

		throw new ReEvaluationException("Method exists in class model, but not in tree node representation");
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
	 * @return Return value of the target method when invoked with the given parameters.
	 *
	 * @throws ReEvaluationException
	 * 		When the target method could not be evaluated.
	 */
	@Nonnull
	public ReValue evaluate(@Nonnull ClassNode classNode,
	                        @Nonnull MethodNode methodNode,
	                        @Nullable ReValue classInstance,
	                        @Nonnull List<ReValue> parameters) throws ReEvaluationException {
		// Must support evaluation
		if (!canEvaluate(methodNode))
			throw new ReEvaluationException("Target method does not support evaluation: " + classNode.name + "." + methodNode.name + methodNode.desc);

		// Sanity check parameters
		Type methodType = Type.getMethodType(methodNode.desc);
		if (parameters.size() != methodType.getArgumentCount())
			throw new ReEvaluationException("Mismatched parameter count, method expects "
					+ methodType.getArgumentCount() + " but was given " + parameters.size() + " parameters");
		Type[] argumentTypes = methodType.getArgumentTypes();
		for (int i = 0; i < argumentTypes.length; i++) {
			int expectedSort = argumentTypes[i].getSort();
			int actualSort = parameters.get(i).type().getSort();
			if (expectedSort != actualSort && !(expectedSort <= Type.INT && actualSort < Type.INT))
				throw new ReEvaluationException("Mismatched parameter type at index " + i);
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
		int it = maxSteps;
		while (it > 0) {
			try {
				pc = frame.evaluate(pc, interpreter);
				if (frame.returnValue != null)
					return frame.returnValue;
			} catch (AnalyzerException e) {
				throw new ReEvaluationException(e, "Failed executing instruction: " + BlwUtil.toString(pc));
			} catch (NoNextException e) {
				throw new ReEvaluationException(e, "Execution falls through end");
			}
			it--;
		}
		throw new ReEvaluationException("Method did not yield an value in " + maxSteps + " steps");
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
	 * @return Return value of the instruction block, or stack top value at the end of execution.
	 *
	 * @throws ReEvaluationException
	 * 		When the target block could not be evaluated.
	 */
	@Nonnull
	public ReValue evaluateBlock(@Nonnull InsnList instructionBlock,
	                             @Nonnull ReFrame originFrame,
	                             int methodAccess) throws ReEvaluationException {
		// Must support evaluation
		if (!canEvaluateBlock(instructionBlock, originFrame, methodAccess))
			throw new ReEvaluationException("Target block does not support evaluation");

		// Create initial frame
		ExecutingFrame frame = new ExecutingFrame(originFrame.getLocals(), originFrame.getMaxStackSize(), methodAccess);
		for (int i = 0; i < originFrame.getLocals(); i++)
			frame.setLocal(i, originFrame.getLocal(i));

		// Handle execution
		AbstractInsnNode pc = instructionBlock.getFirst();
		int it = maxSteps;
		while (it > 0) {
			try {
				pc = frame.evaluate(pc, interpreter);

				// Check if return instruction assigned a value.
				if (frame.returnValue != null)
					return frame.returnValue;
			} catch (AnalyzerException e) {
				throw new ReEvaluationException(e, "Failed executing instruction: " + BlwUtil.toString(pc));
			} catch (NoNextException e) {
				// If there is no next instruction from the given block, then control flow has exited the block.
				// The intended use case for this is to be given incomplete segments of code and see what's on the
				// top at the end, so we will yield that here.
				return frame.getStack(frame.getStackSize() - 1);
			}
			it--;
		}
		throw new ReEvaluationException("Block did not yield an value in " + maxSteps + " steps");
	}

	/**
	 * Frame extension to support control flow processing of this evaluator.
	 */
	private static class ExecutingFrame extends ReFrame implements Opcodes {
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
		 * @param insn
		 * 		Instruction to evaluate.
		 * @param interpreter
		 * 		Interpreter to evaluate with.
		 *
		 * @return {@code true} when the given instruction can be evaluated via {@link #evaluate(AbstractInsnNode, ReInterpreter)}.
		 */
		public boolean canEvaluate(@Nonnull AbstractInsnNode insn, @Nonnull ReInterpreter interpreter) {
			return switch (insn.getOpcode()) {
				case JSR, RET, // Legacy instructions
						INVOKEDYNAMIC, // Dynamic linking not supported
						ATHROW, // Need to finish control-flow handling for this
						NEW, PUTFIELD, // Need to make form of instance tracking for these
						PUTSTATIC // Need to wrap interpreter with one that also has support for this
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
				// Methods + Fields must have lookup support
				case INVOKEINTERFACE, INVOKESPECIAL, INVOKEVIRTUAL -> {
					InvokeVirtualLookup lookup = interpreter.getInvokeVirtualLookup();
					yield lookup != null && lookup.hasLookup((MethodInsnNode) insn);
				}
				case INVOKESTATIC -> {
					InvokeStaticLookup lookup = interpreter.getInvokeStaticLookup();
					yield lookup != null && lookup.hasLookup((MethodInsnNode) insn);
				}
				case GETFIELD -> {
					GetFieldLookup lookup = interpreter.getGetFieldLookup();
					yield lookup != null && lookup.hasLookup((FieldInsnNode) insn);
				}
				case GETSTATIC -> {
					GetStaticLookup lookup = interpreter.getGetStaticLookup();
					yield lookup != null && lookup.hasLookup((FieldInsnNode) insn);
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
					// TODO: Find handler and move there with exception being only stack element
					//  - Need to also support this for math operations that fail (div by zero)
					throw new UnsupportedOperationException();
				}
				case NEW -> {
					// TODO: Handle creating instances, calling constructor to facilitate "state" on values
					//  - Used for put/get-field + invokevirtual(this) instancing
					execute(insn, interpreter);
					yield insn.getNext();
				}
				case PUTFIELD -> {
					// TODO: Store value in interpreter lookup
					execute(insn, interpreter);
					yield insn.getNext();
				}
				case PUTSTATIC -> {
					// TODO: Store value in interpreter lookup
					execute(insn, interpreter);
					yield insn.getNext();
				}
				case JSR, RET -> {
					throw new UnsupportedOperationException();
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
	}

	/** Dummy exception to signal out-of-bounds flow in the evaluate methos. */
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
