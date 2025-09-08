package software.coley.recaf.services.deobfuscation.transform.generic;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Frame;
import software.coley.collections.Lists;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.services.inheritance.InheritanceGraph;
import software.coley.recaf.services.inheritance.InheritanceGraphService;
import software.coley.recaf.services.transform.ClassTransformer;
import software.coley.recaf.services.transform.JvmClassTransformer;
import software.coley.recaf.services.transform.JvmTransformerContext;
import software.coley.recaf.services.transform.TransformationException;
import software.coley.recaf.util.AsmInsnUtil;
import software.coley.recaf.util.BlwUtil;
import software.coley.recaf.util.analysis.value.DoubleValue;
import software.coley.recaf.util.analysis.value.FloatValue;
import software.coley.recaf.util.analysis.value.IntValue;
import software.coley.recaf.util.analysis.value.LongValue;
import software.coley.recaf.util.analysis.value.ReValue;
import software.coley.recaf.util.analysis.value.StringValue;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.objectweb.asm.Opcodes.*;

/**
 * A transformer that folds sequences of computed values into constants.
 *
 * @author Matt Coley
 */
@Dependent
public class OpaqueConstantFoldingTransformer implements JvmClassTransformer {
	private static final int[] ARG_1_SIZE = new int[255];
	private static final int[] ARG_2_SIZE = new int[255];
	private final InheritanceGraphService graphService;
	private InheritanceGraph inheritanceGraph;

	@Inject
	public OpaqueConstantFoldingTransformer(@Nonnull InheritanceGraphService graphService) {
		this.graphService = graphService;
	}

	@Override
	public void setup(@Nonnull JvmTransformerContext context, @Nonnull Workspace workspace) {
		inheritanceGraph = graphService.getOrCreateInheritanceGraph(workspace);
	}

	@Override
	public void transform(@Nonnull JvmTransformerContext context, @Nonnull Workspace workspace,
	                      @Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle,
	                      @Nonnull JvmClassInfo initialClassState) throws TransformationException {
		boolean dirty = false;
		String className = initialClassState.getName();
		ClassNode node = context.getNode(bundle, initialClassState);
		for (MethodNode method : node.methods) {
			InsnList instructions = method.instructions;
			if (instructions == null)
				continue;
			try {
				// This transformer runs in two passes.
				//
				// The first pass inlines various stack operations like DUP/SWAP/DUP_X.
				// By simplifying the stack we can mitigate some really annoying edge cases in the second pass.
				//
				// The second pass iteratively steps forwards and finds "operations" that act on stack values.
				// Where possible, if the result of the operation is known it will replace the operation instruction
				// and as many of the contributing instructions to it as possible.
				//
				// Any time either pass makes changes, it will generally replace instructions with NOP.
				// This is generally done over immediately removing them to reduce the risk of indexing errors.
				dirty |= pass1StackManipulation(context, node, method, instructions);
				dirty |= pass2SequenceFolding(context, node, method, instructions);

				// Now that we are done, we'll prune any NOP instructions if we made changes.
				if (dirty) {
					for (AbstractInsnNode insn : instructions.toArray()) {
						if (insn.getOpcode() == NOP)
							instructions.remove(insn);
					}
				}
			} catch (Throwable t) {
				throw new TransformationException("Error encountered when folding constants", t);
			}
		}
		if (dirty)
			context.setNode(bundle, initialClassState, node);
	}

	/**
	 * Replaces stack manipulation instructions in the method. This may involve shifting the order of instructions
	 * or placing copies of existing instructions in specific locations to achieve desired effects for operations
	 * like {@code dup_x} instructions.
	 *
	 * @param context
	 * 		Transformation context used to analyze methods for stack values.
	 * @param node
	 * 		Class defining the method.
	 * @param method
	 * 		The method to transform.
	 * @param instructions
	 * 		The instructions of the method.
	 *
	 * @return {@code true} when any stack operation was transformed.
	 *
	 * @throws TransformationException
	 * 		When the method code couldn't be analyzed.
	 */
	private boolean pass1StackManipulation(@Nonnull JvmTransformerContext context, @Nonnull ClassNode node,
	                                       @Nonnull MethodNode method, @Nonnull InsnList instructions) throws TransformationException {
		boolean dirty = false;
		int insertions = 0;
		Frame<ReValue>[] frames = context.analyze(inheritanceGraph, node, method);
		for (int i = 1; i < instructions.size() - 1; i++) {
			Frame<ReValue> frame = frames[i - insertions];
			if (frame == null || frame.getStackSize() == 0)
				continue;

			AbstractInsnNode instruction = instructions.get(i);
			int opcode = instruction.getOpcode();
			switch (opcode) {
				case DUP -> {
					if (getSlotsOccupied(frame) < 1)
						continue;
					ReValue top = frame.getStack(frame.getStackSize() - 1);
					if (top.hasKnownValue()) {
						AbstractInsnNode replacement = toInsn(top);
						if (replacement != null) {
							instructions.set(instruction, replacement);
							dirty = true;
						}
					} else if (isSupportedValueProducer(instruction.getPrevious())) {
						instructions.set(instruction, instruction.getPrevious().clone(Collections.emptyMap()));
						dirty = true;
					}
				}
				case DUP2 -> {
					if (getSlotsOccupied(frame) < 2)
						continue;
					BinaryOperationArguments arguments = getBinaryOperationArguments(instruction.getPrevious(), DUP2);
					if (arguments != null && arguments.combinedIntermediates().isEmpty()) {
						if (arguments.argument2().sameAs(arguments.argument1())) {
							// Arguments are same since input is wide (long/double)
							AbstractInsnNode arg = arguments.argument2().insn();
							if (isSupportedValueProducer(arg)) {
								instructions.set(instruction, arg.clone(Collections.emptyMap()));
								dirty = true;
							}
						} else {
							// Two separate arguments
							AbstractInsnNode arg1 = arguments.argument1().insn();
							AbstractInsnNode arg2 = arguments.argument2().insn();
							instructions.insert(instruction, arg2.clone(Collections.emptyMap()));
							instructions.set(instruction, arg1.clone(Collections.emptyMap()));
							insertions += 1;
							dirty = true;
						}

					}
				}
				case DUP_X1 -> {
					if (getSlotsOccupied(frame) < 2)
						continue;
					BinaryOperationArguments arguments = getBinaryOperationArguments(instruction.getPrevious(), DUP2_X1);
					if (arguments != null && arguments.combinedIntermediates().isEmpty()) {
						instructions.insertBefore(arguments.argument1().insn(), arguments.argument2().insn().clone(Collections.emptyMap()));
						instructions.set(instruction, new InsnNode(NOP));
						insertions += 1;
						dirty = true;
					}
				}
				case DUP_X2 -> {
					if (getSlotsOccupied(frame) < 3)
						continue;
					BinaryOperationArguments arguments = getBinaryOperationArguments(instruction.getPrevious(), DUP2_X1);
					if (arguments != null && arguments.combinedIntermediates().isEmpty()) {
						Argument prior = collectArgument(arguments.argument1().insn().getPrevious());
						if (prior == null)
							continue;
						instructions.insertBefore(prior.insn(), arguments.argument2().insn().clone(Collections.emptyMap()));
						instructions.set(instruction, new InsnNode(NOP));
						insertions += 1;
						dirty = true;
					}
				}
				case DUP2_X1 -> {
					if (getSlotsOccupied(frame) < 2)
						continue;
					BinaryOperationArguments arguments = getBinaryOperationArguments(instruction.getPrevious(), DUP2_X1);
					if (arguments == null || !arguments.combinedIntermediates().isEmpty())
						continue;
					Argument prior = collectArgument(arguments.argument1().insn().getPrevious());
					if (prior == null)
						continue;

					AbstractInsnNode target = prior.insn();
					if (arguments.argument2().sameAs(arguments.argument1())) {
						// Arguments are same since input is wide (long/double)
						AbstractInsnNode arg = arguments.argument2().insn();
						if (isSupportedValueProducer(arg)) {
							instructions.insertBefore(target, arg.clone(Collections.emptyMap()));
							instructions.set(instruction, new InsnNode(NOP));
							insertions += 1;
							dirty = true;
						}
					} else {
						// Two separate arguments
						AbstractInsnNode arg1 = arguments.argument1().insn();
						AbstractInsnNode arg2 = arguments.argument2().insn();
						instructions.insertBefore(target, arg1.clone(Collections.emptyMap()));
						instructions.insertBefore(target, arg2.clone(Collections.emptyMap()));
						instructions.set(instruction, new InsnNode(NOP));
						insertions += 2;
						dirty = true;
					}
				}
				case DUP2_X2 -> {
					if (getSlotsOccupied(frame) < 2)
						continue;
					BinaryOperationArguments arguments = getBinaryOperationArguments(instruction.getPrevious(), DUP2_X1);
					if (arguments == null || !arguments.combinedIntermediates().isEmpty())
						continue;
					BinaryOperationArguments prior = getBinaryOperationArguments(arguments.argument1().insn().getPrevious(), DUP2_X1);
					if (prior == null || !prior.combinedIntermediates().isEmpty())
						continue;

					AbstractInsnNode target = prior.argument1().insn();
					if (arguments.argument2().sameAs(arguments.argument1())) {
						// Arguments are same since input is wide (long/double)
						AbstractInsnNode arg = arguments.argument2().insn();
						if (isSupportedValueProducer(arg)) {
							instructions.insertBefore(target, arg.clone(Collections.emptyMap()));
							instructions.set(instruction, new InsnNode(NOP));
							insertions += 1;
							dirty = true;
						}
					} else {
						// Two separate arguments
						AbstractInsnNode arg1 = arguments.argument1().insn();
						AbstractInsnNode arg2 = arguments.argument2().insn();
						instructions.insertBefore(target, arg1.clone(Collections.emptyMap()));
						instructions.insertBefore(target, arg2.clone(Collections.emptyMap()));
						instructions.set(instruction, new InsnNode(NOP));
						insertions += 2;
						dirty = true;
					}
				}
				case POP -> {
					if (getSlotsOccupied(frame) < 1)
						continue;
					Argument argument = collectArgument(instruction.getPrevious());
					if (argument != null && argument.intermediates().isEmpty()) {
						instructions.set(instruction, new InsnNode(NOP));
						argument.replaceInsn(instructions);
						dirty = true;
					}
				}
				case POP2 -> {
					if (getSlotsOccupied(frame) < 2)
						continue;
					BinaryOperationArguments arguments = getBinaryOperationArguments(instruction.getPrevious(), POP2);
					if (arguments != null && arguments.combinedIntermediates().isEmpty()) {
						instructions.set(instruction, new InsnNode(NOP));
						arguments.replaceBinOp(instructions);
						dirty = true;
					}
				}
				case SWAP -> {
					if (getSlotsOccupied(frame) < 2)
						continue;
					BinaryOperationArguments arguments = getBinaryOperationArguments(instruction.getPrevious(), POP2);
					if (arguments != null && arguments.combinedIntermediates().isEmpty()) {
						instructions.remove(arguments.argument1().insn());
						instructions.insert(arguments.argument2().insn(), arguments.argument1().insn());
						instructions.set(instruction, new InsnNode(NOP));
						dirty = true;
					}
				}
			}
		}
		return dirty;
	}

	/**
	 * Detects sequences of instructions that are passed to an "operation" like {@code iadd/dmul/fcml/etc}.
	 * Once a sequence is validated such that the inputs are aligned to the expected stack state of the operation inputs
	 * the entire sequence is replaced with the resulting pushed stack value.
	 *
	 * @param context
	 * 		Transformation context used to analyze methods for stack values.
	 * @param node
	 * 		Class defining the method.
	 * @param method
	 * 		The method to transform.
	 * @param instructions
	 * 		The instructions of the method.
	 *
	 * @return {@code true} when any stack operation was transformed.
	 *
	 * @throws TransformationException
	 * 		When the method code couldn't be analyzed.
	 */
	private boolean pass2SequenceFolding(@Nonnull JvmTransformerContext context, @Nonnull ClassNode node,
	                                     @Nonnull MethodNode method, @Nonnull InsnList instructions) throws TransformationException {
		boolean dirty = false;
		Frame<ReValue>[] frames = context.analyze(inheritanceGraph, node, method);
		int endIndex = instructions.size() - 1;
		for (int i = 1; i < endIndex; i++) {
			AbstractInsnNode instruction = instructions.get(i);
			int opcode = instruction.getOpcode();

			// Iterate until we find an instruction that consumes values off the stack as part of an "operation".
			int sizeConsumed = AsmInsnUtil.getSizeConsumed(instruction);
			if (sizeConsumed == 0 || (opcode >= POP && opcode <= DUP2_X2))
				continue;

			// Return instructions consume values off the stack but unlike operations do not produce an outcome.
			boolean isReturn = AsmInsnUtil.isReturn(opcode) && opcode != RETURN;

			// Grab the current and next frame for later. We want to pull values from these to determine
			// if operations on constant inputs can be inlined.
			// However, a "return" isn't an operation, so we have an edge case handling those.
			Frame<ReValue> frame = frames[i];
			if (frame == null)
				continue;
			Frame<ReValue> nextFrame = frames[i + 1];
			if ((nextFrame == null || nextFrame.getStackSize() <= 0) && !isReturn)
				continue;

			// Walk backwards from this point and try and find a sequence of instructions that
			// will create the expected stack state we see for this operation instruction.
			boolean validSequence = true;
			int netStackChange = 0;
			int j = i;
			List<AbstractInsnNode> sequence = new ArrayList<>();
			while (j >= 0) {
				AbstractInsnNode insn = instructions.get(j);
				int insnOp = insn.getOpcode();
				if (insnOp != NOP && insnOp != -1) // Skip adding NOP/Labels
					sequence.addFirst(insn);

				// Abort if we observe control flow. Both outbound and inbound breaks sequences.
				// If there is obfuscated control flow that is redundant use a control flow flattening transformer first.
				if (AsmInsnUtil.isFlowControl(insn) || AsmInsnUtil.hasInboundFlowReferences(method, Collections.singletonList(insn))) {
					validSequence = false;
					break;
				}

				// Update the net stack size change.
				int consumed = AsmInsnUtil.getSizeConsumed(insn);
				int produced = AsmInsnUtil.getSizeProduced(insn);
				int stackDiff = produced - consumed;
				netStackChange += stackDiff;

				// Step backwards.
				j--;

				// If we see the net stack change is positive, our sequence is "done".
				if (netStackChange >= 1)
					break;
			}

			// Skip if the completed sequence isn't a viable candidate for folding.
			// - Explicitly marked as invalid
			// - Too small
			// - The sequence isn't balanced, or requires a larger scope to include all "contributing" instructions
			if (!validSequence || sequence.size() < 2 || shouldContinueSequence(sequence))
				continue;

			// Additionally if the sequence does NOT end with 'xreturn' then it should
			// have a positive stack effect (the final operation result should push a value).
			if (netStackChange < (isReturn ? 0 : 1))
				continue;

			// Keep the return instruction in the sequence.
			if (isReturn && AsmInsnUtil.isReturn(sequence.getLast()))
				sequence.removeLast();

			// Replace the operation with a constant value, or simplified instruction pattern.
			ReValue topValue = isReturn ?
					frame.getStack(frame.getStackSize() - 1) :
					nextFrame.getStack(nextFrame.getStackSize() - 1);
			AbstractInsnNode replacement = toInsn(topValue);
			if (replacement == null) {
				// Skip if this isn't an operation we can support
				 if (frame.getStackSize() < 2)
				 	continue;

				// We don't know the result of the operation. But if it is something we know is redundant
				// we will want to remove it anyways. For instance:
				//  x * 1 = x
				//  x + 0 = x
				//  x | 0 = x
				//  x & -1 = x
				//  x ^ 0 = x
				//  x << 0 = x
				//  x >> 0 = x
				//  x >>> 0 = x
				ReValue top = frame.getStack(frame.getStackSize() - 1);
				ReValue topM1 = frame.getStack(frame.getStackSize() - 2);
				int targetValue = switch (opcode) {
					case IAND, LAND -> -1;
					case IMUL, FMUL, DMUL, LMUL -> 1;
					case IADD, FADD, DADD, LADD,
							IOR, LOR,
							IXOR, LXOR,
							ISHL, ISHR, IUSHR, LSHL, LSHR, LUSHR -> 0;
					default -> 25565;
				};

				// Skip if not an operation we can simplify.
				if (targetValue == 25565)
					continue;

				if (ReValue.isPrimitiveEqualTo(top, targetValue)) {
					// Scan for the instructions that provide the argument values for the current instruction/binary operation.
					// - Start with the instruction before this one as a potential provider for the 2nd argument (right value in an operation)
					BinaryOperationArguments arguments = getBinaryOperationArguments(instruction.getPrevious(), opcode);
					if (arguments == null)
						continue;

					// Remove redundant operation + top/right value provider.
					instructions.set(instruction, new InsnNode(NOP));
					instructions.set(arguments.argument2().insn(), new InsnNode(NOP));
					for (AbstractInsnNode intermediate : arguments.argument2().intermediates())
						instructions.set(intermediate, new InsnNode(NOP));
					dirty = true;
				} else if (ReValue.isPrimitiveEqualTo(topM1, targetValue)) {
					// Scan for the instructions that provide the argument values for the current instruction/binary operation.
					// - Start with the instruction before this one as a potential provider for the 2nd argument (right value in an operation)
					BinaryOperationArguments arguments = getBinaryOperationArguments(instruction.getPrevious(), opcode);
					if (arguments == null)
						continue;

					// Remove redundant operation + top-1/left value provider.
					instructions.set(instruction, new InsnNode(NOP));
					instructions.set(arguments.argument1().insn(), new InsnNode(NOP));
					for (AbstractInsnNode intermediate : arguments.argument1().intermediates())
						instructions.set(intermediate, new InsnNode(NOP));
					dirty = true;
				}
				continue;
			}

			// If we have a replacement, remove all instructions in the sequence and replace the
			// operation instruction with one that pushes a constant value of the result in its place.
			for (AbstractInsnNode item : sequence)
				instructions.set(item, new InsnNode(NOP));
			if (isReturn) {
				// We know the sequence size must be >= 2, so the instruction before
				// the return should have been replaced with a nop, and is safe to replace
				// with our constant.
				AbstractInsnNode old = instructions.get(i - 1);
				instructions.set(old, replacement);
			} else {
				instructions.set(instructions.get(i), replacement);
			}
			dirty = true;
		}
		return dirty;
	}

	/**
	 * Check if the given sequence is unbalanced, or is prefixed with an instruction that implies
	 * more instructions should be included for a <i>"full scope"</i> of <i>"contributing"</i> instructions.
	 *
	 * @param sequence
	 * 		Instruction sequence.
	 *
	 * @return {@code true} when the instruction sequence should continue expanding backwards.
	 */
	private static boolean shouldContinueSequence(@Nonnull List<AbstractInsnNode> sequence) {
		int stackDiff = 0;
		int consumed = 0;
		int produced = 0;
		int netStackChange = 0;
		for (AbstractInsnNode seq : sequence) {
			// DUP operations operate on values on the stack. While the most simple DUP case is fine, any other variant
			// such as DUP2, DUP_X, etc. have edge cases which mean we cannot have a 100% foolproof/isolated sequence.
			// These require us to continue scanning backwards to expand the sequence.
			int op = seq.getOpcode();
			if ((op == DUP && netStackChange < 1)
					|| (op == DUP_X1 && netStackChange < 2)
					|| (op == DUP_X2 && netStackChange < 3)
					|| (op == DUP2 && netStackChange < 2)
					|| (op == DUP2_X1 && netStackChange < 3)
					|| (op == DUP2_X2 && netStackChange < 4)
					|| (op == SWAP && netStackChange < 2)
					|| (op == POP && netStackChange < 1)
					|| (op == POP2 && netStackChange < 2)) {
				return true;
			}

			// Get stack change for this instruction in the sequence.
			consumed = AsmInsnUtil.getSizeConsumed(seq);
			produced = AsmInsnUtil.getSizeProduced(seq);

			// If we ever see the stack size in this sequence go negative, then it
			// cannot be treated as an "isolated" sequence. It implies that there is a reliance on a larger
			// stack size in the current sequence scope. We can't really "recover" this scope at this point.
			// However, if we create a new scope starting at a later instruction its possible it will give us
			// a larger scoped sequence which will include enough instructions to prevent this from occurring.
			if (consumed > netStackChange)
				return true;

			// Update net stack change.
			stackDiff = produced - consumed;
			netStackChange += stackDiff;
		}
		return netStackChange > consumed || netStackChange != produced;
	}

	@Nonnull
	@Override
	public String name() {
		return "Opaque constant folding";
	}

	@Nonnull
	@Override
	public Set<Class<? extends ClassTransformer>> recommendedPredecessors() {
		// Basic goto obf will prevent this transformer from handling "obvious" cases.
		return Collections.singleton(GotoInliningTransformer.class);
	}

	/**
	 * Check if the instruction is responsible for providing some value we can possibly fold.
	 * This method doesn't tell us if the value is known though. The next frame after this
	 * instruction should have the provided value on the stack top.
	 *
	 * @param insn
	 * 		Instruction to check.
	 *
	 * @return {@code true} when the instruction will produce a single value.
	 */
	protected static boolean isSupportedValueProducer(@Nonnull AbstractInsnNode insn) {
		// Skip if this instruction consumes a value off the stack.
		if (AsmInsnUtil.getSizeConsumed(insn) > 0)
			return false;

		// The following cases are supported:
		//  - constants
		//  - variable loads (context will determine if value in variable is constant at the given position)
		//  - static field gets (context will determine if value in field is constant/known)
		//  - static method calls with 0 args (context will determine if returned value of method is constant/known)
		int op = insn.getOpcode();
		if (AsmInsnUtil.isConstValue(op))
			return true;
		if (op >= ILOAD && op <= ALOAD)
			return true;
		if (op == GETSTATIC)
			return true;
		return op == INVOKESTATIC
				&& insn instanceof MethodInsnNode min
				&& min.desc.startsWith("()")
				&& !min.desc.endsWith(")V");
	}

	/**
	 * @param value
	 * 		Value to convert.
	 *
	 * @return Instruction representing the value,
	 * or {@code null} if we don't/can't provide a mapping for the value content.
	 */
	@Nullable
	@SuppressWarnings("OptionalGetWithoutIsPresent")
	public static AbstractInsnNode toInsn(@Nonnull ReValue value) {
		// Skip if value is not known.
		if (!value.hasKnownValue())
			return null;

		// Map known value types to constant value instructions.
		return switch (value) {
			case IntValue intValue -> AsmInsnUtil.intToInsn(intValue.value().getAsInt());
			case FloatValue floatValue -> AsmInsnUtil.floatToInsn((float) floatValue.value().getAsDouble());
			case DoubleValue doubleValue -> AsmInsnUtil.doubleToInsn(doubleValue.value().getAsDouble());
			case LongValue longValue -> AsmInsnUtil.longToInsn(longValue.value().getAsLong());
			case StringValue stringValue -> new LdcInsnNode(stringValue.getText().get());
			default -> null;
		};
	}

	/**
	 * @param frame
	 * 		Frame to count true stack size of <i>(in terms of occupied slots)</i>
	 *
	 * @return Number of stack slots occupied in the frame.
	 */
	private static int getSlotsOccupied(@Nonnull Frame<ReValue> frame) {
		int valueCount = frame.getStackSize();
		int slots = 0;
		for (int i = 0; i < valueCount; i++) {
			ReValue value = frame.getStack(i);
			slots += value.getSize();
		}
		return slots;
	}

	/**
	 * This is essentially {@link #collectArgument(AbstractInsnNode)} but run twice, then wrapped up in a box.
	 * The main difference is edge case handling for wide types and some sanity checks for edge cases applicable
	 * only to cases where there are two arguments rather than just one.
	 *
	 * @param insnBeforeOp
	 * 		Starting instruction representing a {@link #isSupportedValueProducer(AbstractInsnNode) value producer}
	 * 		to an operation instruction <i>(like an {@code iconst_1} as part of an {@code iadd} operation)</i>.
	 * @param binOperationOpcode
	 * 		The opcode for the operation instruction. Generally something like {@code iadd}, {@code dmul}, etc.
	 * 		Used to determine how to treat arguments in some wide-type edge cases.
	 *
	 * @return Wrapper containing the arguments <i>(and their instructions)</i> if found. Otherwise {@code null}.
	 */
	@Nullable
	public static BinaryOperationArguments getBinaryOperationArguments(@Nonnull AbstractInsnNode insnBeforeOp, int binOperationOpcode) {
		// Get instruction of the top stack's contributing instruction.
		Argument argument2 = collectArgument(insnBeforeOp);
		if (argument2 == null)
			return null;

		// Get instruction of the 2nd-to-top stack's contributing instruction.
		// In some cases this may be the same value as the instruction we grabbed above.
		// Consider the case:
		//  iconst_2
		//  dup2
		//  iadd
		// When we see "iadd" has arguments "dup2" it will satisfy both values in the addition.
		Argument argument1;
		if (argument2.providesBinaryOpValuesFor(binOperationOpcode)) {
			// If the instruction before produces a larger value than required we have
			// encountered a case that follows the example case above (likely a dup2).
			argument1 = argument2;
		} else {
			argument1 = collectArgument(argument2.insn().getPrevious());

			// If we didn't find a value for argument 1, we cannot handle this binary argument.
			if (argument1 == null)
				return null;

			// If argument 1 was found, but is too wide (a double or dup2) for the binary argument considering
			// that we already have argument 2 resolved, then we also cannot handle this binary argument.
			//
			// Example:
			//   sipush 20
			//   sipush 10
			//   dup2       <---- Pushes [20, 10] onto stack, resulting in [20, 10, 20, 10]
			//   sipush -10
			//   swap       <---- If we are here, and want to see what instructions provide "arguments"
			//   ...              then the "dup2" provides [20, 10] on the stack, while we only operate on [10].
			//   ...              This makes it so that we can't correctly say "dup2" is 100% responsible for operands
			//   ...              in "swap" because it also produces "20" which isn't an operand for our "swap.
			if (argument1.providesBinaryOpValuesFor(binOperationOpcode))
				return null;
		}

		// If we saw an odd number of "swap" before we got (arg2) then we want to swap the references.
		int swapCount = (int) argument2.intermediates().stream()
				.filter(i -> i.getOpcode() == SWAP)
				.count();
		if (swapCount % 2 == 1) {
			Argument temp = argument1;
			argument1 = argument2;
			argument2 = temp;
		}

		// If we have recorded intermediate instructions that result in stack consumption
		// we need to remove the instructions they have consumed.
		// Track any intermediate instructions between the operation instruction
		// and the first argument instruction (arg2).
		List<AbstractInsnNode> combinedIntermediates = new ArrayList<>(argument1.getCombinedIntermediates(argument2));
		if (!canConsumeAccumulatedStackConsumption(argument1, argument2, combinedIntermediates))
			return null;
		return new BinaryOperationArguments(argument2, argument1, combinedIntermediates);
	}

	/**
	 * Starting with the provided instruction <i>(inclusive)</i>, we walk backwards until a valid
	 * {@link #isSupportedValueProducer(AbstractInsnNode) value producer} is found. There are certain
	 * instructions which we will support as intermediates between the starting point and the final chosen instruction.
	 * Intermediate instructions are generally stack manipulations which we want to remove as they are between the
	 * actual instruction providing the value and the place where the value is used.
	 *
	 * @param insnBeforeOp
	 * 		Starting instruction representing a {@link #isSupportedValueProducer(AbstractInsnNode) value producer}
	 * 		to an operation instruction <i>(like an {@code iconst_1} as part of an {@code ineg} operation)</i>.
	 *
	 * @return Wrapper containing the instruction if it is a value producer was found. Otherwise {@code null}.
	 */
	@Nullable
	public static Argument collectArgument(@Nullable AbstractInsnNode insnBeforeOp) {
		if (insnBeforeOp == null)
			return null;
		List<AbstractInsnNode> intermediates = null;
		int intermediateStackConsumption = 0;
		while (insnBeforeOp != null) {
			int argumentOp = insnBeforeOp.getOpcode();
			if (argumentOp == NOP) {
				insnBeforeOp = insnBeforeOp.getPrevious();
			} else if (argumentOp == SWAP || argumentOp == POP || argumentOp == POP2) {
				// We already know the values in our frame, so these intermediate instructions
				// between the operation instruction and the instructions pushing those values
				// onto the stack can be recorded for removal.
				if (intermediates == null)
					intermediates = new ArrayList<>();
				intermediates.add(insnBeforeOp);
				intermediateStackConsumption += AsmInsnUtil.getSizeConsumed(insnBeforeOp);
				insnBeforeOp = insnBeforeOp.getPrevious();
			} else {
				break;
			}
		}
		if (insnBeforeOp == null || !isSupportedValueProducer(insnBeforeOp))
			return null;
		return new Argument(insnBeforeOp, Objects.requireNonNullElse(intermediates, Collections.emptyList()), intermediateStackConsumption);
	}

	private static boolean canConsumeAccumulatedStackConsumption(@Nonnull Argument argument1, @Nonnull Argument argument2,
	                                                             @Nonnull List<AbstractInsnNode> intermediates) {
		// The first argument (provides value beneath the 2nd argument on the stack) is where
		// our backwards search (exclusive) for instructions will begin.
		AbstractInsnNode insn = argument1.insn();

		// Combine the stack consumption of both arguments and begin consumption.
		int intermediateStackConsumption = argument1.getCombinedStackConsumption(argument2);
		return canConsumeAccumulatedStackConsumption(intermediateStackConsumption, intermediates, insn);
	}

	private static boolean canConsumeAccumulatedStackConsumption(int intermediateStackConsumption,
	                                                             @Nonnull List<AbstractInsnNode> intermediates,
	                                                             @Nonnull AbstractInsnNode start) {
		// If we have recorded intermediate instructions that result in stack consumption
		// we need to remove the instructions they have consumed. To do this, we will add them
		// to the intermediate instruction list.
		AbstractInsnNode insn = start;
		while (intermediateStackConsumption > 0) {
			insn = insn.getPrevious();
			if (insn == null)
				break;
			if (insn.getOpcode() == NOP)
				continue;
			if (isSupportedValueProducer(insn)) {
				intermediates.add(insn);
				intermediateStackConsumption -= AsmInsnUtil.getSizeProduced(insn);
			} else {
				// We don't know how to handle this instruction, bail out.
				return false;
			}
		}

		return intermediateStackConsumption == 0;
	}

	static {
		Arrays.fill(ARG_1_SIZE, -1);
		Arrays.fill(ARG_2_SIZE, -1);

		ARG_1_SIZE[IADD] = 1;
		ARG_1_SIZE[FADD] = 1;
		ARG_1_SIZE[ISUB] = 1;
		ARG_1_SIZE[FSUB] = 1;
		ARG_1_SIZE[IMUL] = 1;
		ARG_1_SIZE[FMUL] = 1;
		ARG_1_SIZE[IDIV] = 1;
		ARG_1_SIZE[FDIV] = 1;
		ARG_1_SIZE[IREM] = 1;
		ARG_1_SIZE[FREM] = 1;
		ARG_1_SIZE[ISHL] = 1;
		ARG_1_SIZE[ISHR] = 1;
		ARG_1_SIZE[IUSHR] = 1;
		ARG_1_SIZE[IAND] = 1;
		ARG_1_SIZE[IXOR] = 1;
		ARG_1_SIZE[IOR] = 1;
		ARG_1_SIZE[DREM] = 2;
		ARG_1_SIZE[DDIV] = 2;
		ARG_1_SIZE[DMUL] = 2;
		ARG_1_SIZE[DSUB] = 2;
		ARG_1_SIZE[DADD] = 2;
		ARG_1_SIZE[LUSHR] = 2;
		ARG_1_SIZE[LSHR] = 2;
		ARG_1_SIZE[LSHL] = 2;
		ARG_1_SIZE[LREM] = 2;
		ARG_1_SIZE[LDIV] = 2;
		ARG_1_SIZE[LMUL] = 2;
		ARG_1_SIZE[LSUB] = 2;
		ARG_1_SIZE[LADD] = 2;
		ARG_1_SIZE[LAND] = 2;
		ARG_1_SIZE[LOR] = 2;
		ARG_1_SIZE[LXOR] = 2;
		ARG_1_SIZE[FCMPL] = 1;
		ARG_1_SIZE[FCMPG] = 1;
		ARG_1_SIZE[LCMP] = 2;
		ARG_1_SIZE[DCMPL] = 2;
		ARG_1_SIZE[DCMPG] = 2;

		System.arraycopy(ARG_1_SIZE, 0, ARG_2_SIZE, 0, ARG_1_SIZE.length);
		ARG_2_SIZE[LUSHR] = 1;
		ARG_2_SIZE[LSHR] = 1;
		ARG_2_SIZE[LSHL] = 1;

		// The rest of these aren't "operations" like the above
		ARG_1_SIZE[DUP] = 1;
		ARG_1_SIZE[DUP_X1] = 1;
		ARG_1_SIZE[DUP_X2] = 1;
		ARG_1_SIZE[DUP2] = 1;
		ARG_2_SIZE[DUP2] = 1;
		ARG_1_SIZE[DUP2_X1] = 1;
		ARG_2_SIZE[DUP2_X1] = 1;
		ARG_1_SIZE[DUP2_X2] = 1;
		ARG_2_SIZE[DUP2_X2] = 1;
		ARG_1_SIZE[POP] = 1;
		ARG_1_SIZE[POP2] = 1;
		ARG_2_SIZE[POP2] = 1;

	}

	/**
	 * Wrapper of two {@link Argument}.
	 *
	 * @param argument2
	 * 		Argument providing the left side value of a binary operation.
	 * @param argument1
	 * 		Argument providing the right side value of a binary operation.
	 * @param combinedIntermediates
	 * 		Track any intermediate instructions between the operation instruction and the argument's instructions.
	 */
	public record BinaryOperationArguments(@Nonnull Argument argument2, @Nonnull Argument argument1,
	                                       @Nonnull List<AbstractInsnNode> combinedIntermediates) {
		/**
		 * Replace the instructions from the wrapped arguments with {@code nop}
		 * or other value providing instructions if the stack state necessitates it.
		 *
		 * @param instructions
		 * 		Instructions list to modify.
		 */
		public void replaceBinOp(@Nonnull InsnList instructions) {
			// Replace right binary operation value.
			argument2.replaceInsn(instructions);

			// Replace left binary operation value (If the right argument hasn't provided for both arguments).
			if (!argument1.sameAs(argument2))
				argument1.replaceInsn(instructions);

			// Remove any intermediate instructions.
			for (AbstractInsnNode intermediate : combinedIntermediates)
				if (instructions.contains(intermediate))
					instructions.set(intermediate, new InsnNode(NOP));
		}
	}

	/**
	 * @param insn
	 * 		Instruction that may act as a provider for the operation instruction.
	 * @param intermediates
	 * 		Instructions between the operation instruction and the argument instruction.
	 * @param intermediateStackConsumption
	 * 		Track any intermediate instructions between the operation instruction and the argument instruction.
	 */
	public record Argument(@Nonnull AbstractInsnNode insn,
	                       @Nonnull List<AbstractInsnNode> intermediates,
	                       int intermediateStackConsumption) {
		/**
		 * @return {@code true} when {@link #intermediates()} is empty.
		 */
		public boolean hasIntermediates() {
			return !intermediates.isEmpty();
		}

		/**
		 * @param other
		 * 		Some other argument.
		 *
		 * @return {@code true} when both this and the other arg wrap the same instruction.
		 */
		public boolean sameAs(@Nonnull Argument other) {
			return insn == other.insn;
		}

		/**
		 * @param other
		 * 		Some other argument.
		 *
		 * @return Combined stack consumption of this and the other argument.
		 */
		public int getCombinedStackConsumption(@Nonnull Argument other) {
			return sameAs(other) ?
					intermediateStackConsumption :
					intermediateStackConsumption + other.intermediateStackConsumption;
		}

		/**
		 * @param other
		 * 		Some other argument.
		 *
		 * @return Combined intermediates of both this and the other argument.
		 */
		@Nonnull
		public List<AbstractInsnNode> getCombinedIntermediates(@Nonnull Argument other) {
			if (!hasIntermediates() && !other.hasIntermediates())
				return Collections.emptyList();
			if (sameAs(other) || !other.hasIntermediates())
				return intermediates;
			return Lists.combine(intermediates, other.intermediates);
		}

		@Override
		public String toString() {
			String string = BlwUtil.toString(insn);
			if (intermediates.isEmpty())
				return string;
			return string + "\n - " + intermediates.stream().map(BlwUtil::toString).collect(Collectors.joining("\n - "));
		}

		/**
		 * Check if this {@link #insn()} provides values for the given {@code opcode}'s operation.
		 *
		 * @param opcode
		 * 		Some binary operation <i>(Instruction operating on two stack values)</i>
		 *
		 * @return {@code true} if this argument {@link #insn() instruction} supplies the binary operation with <b>both</b> stack values.
		 * {@code false} if this argument provides only one or none of the values.
		 */
		public boolean providesBinaryOpValuesFor(int opcode) {
			// Get the required sizes of arguments for the given instruction.
			int arg1Size = ARG_1_SIZE[opcode];
			int arg2Size = ARG_2_SIZE[opcode];
			if (arg1Size < 0 || arg2Size < 0)
				throw new IllegalStateException("Missing arg sizes for op: " + opcode);

			// Cover cases like long/doubles
			int totalArgSize = arg1Size + arg2Size;
			if (AsmInsnUtil.getSizeProduced(insn) == totalArgSize)
				return true;

			// The ONLY case where this is valid is DUP2 for some non-wide op (like IADD).
			// Other stack modifying instructions like DUP_X1/X2 + DUP2_X1/X2 move the values below the stack.
			return insn.getOpcode() == DUP2
					&& arg1Size == 1
					&& arg2Size == 1;
		}

		/**
		 * Replace the {@link #insn()} with either a {@code nop} or some other value providing instruction <i>(Depending on calling circumstances)</i>.
		 *
		 * @param instructions
		 * 		Instructions list to modify.
		 *
		 * @return {@code true} when the instructions list was successfully modified.
		 * {@code false} when no replacement could be made.
		 */
		public boolean replaceInsn(@Nonnull InsnList instructions) {
			instructions.set(insn, new InsnNode(NOP));
			return true;
		}
	}
}
