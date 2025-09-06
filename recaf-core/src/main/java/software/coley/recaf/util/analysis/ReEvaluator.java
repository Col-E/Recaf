package software.coley.recaf.util.analysis;

import jakarta.annotation.Nonnull;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Interpreter;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.util.AccessFlag;
import software.coley.recaf.util.BlwUtil;
import software.coley.recaf.util.analysis.value.IntValue;
import software.coley.recaf.util.analysis.value.ObjectValue;
import software.coley.recaf.util.analysis.value.ReValue;
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
	private static final int MAX_ITERATIONS = 10_000;

	/**
	 * @param workspace
	 * 		Workspace to pull classes from.
	 * @param interpreter
	 * 		Interpreter to evaluate instructions with.
	 * @param className
	 * 		Name of class defining the target method.
	 * @param methodName
	 * 		Name of the target method.
	 * @param methodDescriptor
	 * 		Descriptor of the target method.
	 * @param parameters
	 * 		Parameters to pass to the target method.
	 *
	 * @return Return value of the target method when invoked with the given parameters.
	 *
	 * @throws ReEvaluationException
	 * 		When the target method could not be evaluated.
	 */
	@Nonnull
	public static ReValue evaluate(@Nonnull Workspace workspace,
	                               @Nonnull ReInterpreter interpreter,
	                               @Nonnull String className,
	                               @Nonnull String methodName,
	                               @Nonnull String methodDescriptor,
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

		for (MethodNode methodNode : classNode.methods) {
			if (methodName.equals(methodNode.name) && methodDescriptor.equals(methodNode.desc)) {
				return evaluate(workspace, interpreter, classNode, methodNode, parameters);
			}
		}

		throw new ReEvaluationException("Method exists in class model, but not in tree node representation");
	}

	/**
	 * @param workspace
	 * 		Workspace to pull classes from.
	 * @param interpreter
	 * 		Interpreter to evaluate instructions with.
	 * @param classNode
	 * 		Class defining the target method.
	 * @param methodNode
	 * 		Target method.
	 * @param parameters
	 * 		Parameters to pass to the target method.
	 *
	 * @return Return value of the target method when invoked with the given parameters.
	 *
	 * @throws ReEvaluationException
	 * 		When the target method could not be evaluated.
	 */
	@Nonnull
	public static ReValue evaluate(@Nonnull Workspace workspace,
	                               @Nonnull ReInterpreter interpreter,
	                               @Nonnull ClassNode classNode,
	                               @Nonnull MethodNode methodNode,
	                               @Nonnull List<ReValue> parameters) throws ReEvaluationException {
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
			frame.setLocal(0, interpreter.newValue(Type.getObjectType(classNode.name), Nullness.NOT_NULL));

		// Handle execution
		InsnList instructions = methodNode.instructions;
		AbstractInsnNode pc = instructions.getFirst();
		int it = MAX_ITERATIONS;
		while (it > 0) {
			try {
				pc = frame.evaluate(pc, interpreter);
				if (frame.returnValue != null)
					return frame.returnValue;
			} catch (AnalyzerException e) {
				throw new ReEvaluationException(e, "Failed executing instruction: " + BlwUtil.toString(pc));
			}
			it--;
		}
		throw new ReEvaluationException("Method did not yield an value in " + MAX_ITERATIONS + " steps");
	}

	/**
	 * Frame extension to support control flow processing of this evaluator.
	 */
	private static class ExecutingFrame extends ReFrame implements Opcodes {
		private AbstractInsnNode next;
		private ReValue returnValue;

		public ExecutingFrame(@Nonnull MethodNode method) {
			super(method.maxLocals, method.maxStack);
		}

		@Nonnull
		public AbstractInsnNode evaluate(AbstractInsnNode insn, Interpreter<ReValue> interpreter) throws AnalyzerException {
			return switch (insn.getOpcode()) {
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
}
