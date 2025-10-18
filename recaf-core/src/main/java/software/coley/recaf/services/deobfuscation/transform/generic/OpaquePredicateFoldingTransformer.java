package software.coley.recaf.services.deobfuscation.transform.generic;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.analysis.Frame;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.services.inheritance.InheritanceGraph;
import software.coley.recaf.services.inheritance.InheritanceGraphService;
import software.coley.recaf.services.transform.ClassTransformer;
import software.coley.recaf.services.transform.JvmClassTransformer;
import software.coley.recaf.services.transform.JvmTransformerContext;
import software.coley.recaf.services.transform.TransformationException;
import software.coley.recaf.util.AsmInsnUtil;
import software.coley.recaf.util.analysis.value.IntValue;
import software.coley.recaf.util.analysis.value.ObjectValue;
import software.coley.recaf.util.analysis.value.ReValue;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.Collections;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import static org.objectweb.asm.Opcodes.*;
import static software.coley.recaf.services.deobfuscation.transform.generic.OpaqueConstantFoldingTransformer.isSupportedValueProducer;
import static software.coley.recaf.util.AsmInsnUtil.isFlowControl;
import static software.coley.recaf.util.AsmInsnUtil.isSwitchEffectiveGoto;

/**
 * A transformer that folds opaque predicates into single-path control flows.
 *
 * @author Matt Coley
 */
@Dependent
public class OpaquePredicateFoldingTransformer implements JvmClassTransformer {
	private final InheritanceGraphService graphService;
	private InheritanceGraph inheritanceGraph;

	@Inject
	public OpaquePredicateFoldingTransformer(@Nonnull InheritanceGraphService graphService) {
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

			// Skip if method is abstract.
			if (instructions == null)
				continue;

			// Some obfuscators will use 'switch' instructions with all labels being the same in order to
			// recreate the behavior of 'goto'. We will just replace these if we see them.
			// We do this in a pre-pass here since we end up inserting an additional 'POP' for any matched switch.
			for (int i = 1; i < instructions.size() - 1; i++) {
				AbstractInsnNode instruction = instructions.get(i);
				if (instruction instanceof TableSwitchInsnNode switchInsn && isSwitchEffectiveGoto(switchInsn)) {
					AbstractInsnNode previous = switchInsn.getPrevious();
					if (isValueProducerOrTopDup(previous))
						instructions.remove(previous);
					else
						instructions.insertBefore(switchInsn, new InsnNode(POP));
					instructions.set(switchInsn, new JumpInsnNode(GOTO, switchInsn.dflt));
					dirty = true;
				} else if (instruction instanceof LookupSwitchInsnNode switchInsn && isSwitchEffectiveGoto(switchInsn)) {
					AbstractInsnNode previous = switchInsn.getPrevious();
					if (isValueProducerOrTopDup(previous))
						instructions.remove(previous);
					else
						instructions.insertBefore(switchInsn, new InsnNode(POP));
					instructions.set(switchInsn, new JumpInsnNode(GOTO, switchInsn.dflt));
					dirty = true;
				}
			}

			try {
				boolean localDirty = false;
				Frame<ReValue>[] frames = context.analyze(inheritanceGraph, node, method);
				for (int i = 1; i < instructions.size() - 1; i++) {
					AbstractInsnNode instruction = instructions.get(i);

					// Skip if this isn't a control flow instruction.
					// We are only flattening control flow here.
					if (!isFlowControl(instruction))
						continue;

					// Skip goto, branch is always taken.
					// Use the goto inliner if you want to clean these up.
					if (instruction.getOpcode() == GOTO)
						continue;

					// Skip if there is no frame for this instruction.
					if (i >= frames.length)
						continue; // Can happen if there is dead code at the end
					Frame<ReValue> frame = frames[i];
					if (frame == null || frame.getStackSize() == 0)
						continue;

					// Skip if stack top is not known.
					ReValue stackTop = frame.getStack(frame.getStackSize() - 1);
					if (!stackTop.hasKnownValue() && !(stackTop instanceof ObjectValue ov && ov.isNull()))
						continue;

					// Get instruction of the top stack's contributing instruction.
					// It must also be a value producing instruction.
					// If this is something that isn't value producing, another transformer needs to simplify it first.
					AbstractInsnNode prevInstruction = AsmInsnUtil.getPreviousInsn(instruction);
					if (prevInstruction == null || !isValueProducerOrTopDup(prevInstruction))
						continue;

					// Handle any control flow instruction and see if we know based on the frame contents if a specific
					// path is always taken.
					int insnType = instruction.getType();
					if (insnType == AbstractInsnNode.JUMP_INSN) {
						JumpInsnNode jin = (JumpInsnNode) instruction;
						int opcode = instruction.getOpcode();
						if ((opcode >= IFEQ && opcode <= IFLE) || opcode == IFNULL || opcode == IFNONNULL) {
							// Replace single argument binary control flow.
							localDirty |= switch (opcode) {
								case IFEQ ->
										replaceIntValue(instructions, prevInstruction, stackTop, jin, v -> v.isEqualTo(0));
								case IFNE ->
										replaceIntValue(instructions, prevInstruction, stackTop, jin, v -> v.isNotEqualTo(0));
								case IFLT ->
										replaceIntValue(instructions, prevInstruction, stackTop, jin, v -> v.isLessThan(0));
								case IFGE ->
										replaceIntValue(instructions, prevInstruction, stackTop, jin, v -> v.isGreaterThanOrEqual(0));
								case IFGT ->
										replaceIntValue(instructions, prevInstruction, stackTop, jin, v -> v.isGreaterThan(0));
								case IFLE ->
										replaceIntValue(instructions, prevInstruction, stackTop, jin, v -> v.isLessThanOrEqual(0));
								case IFNULL ->
										replaceObjValue(instructions, prevInstruction, stackTop, jin, ObjectValue::isNull);
								case IFNONNULL ->
										replaceObjValue(instructions, prevInstruction, stackTop, jin, ObjectValue::isNotNull);
								default -> localDirty;
							};
						} else if (opcode >= IF_ICMPEQ && opcode <= IF_ACMPNE) {
							// Skip if the other argument to compare with is not available or known.
							if (frame.getStackSize() < 2)
								continue;
							ReValue stack2ndTop = frame.getStack(frame.getStackSize() - 2);
							if (!stack2ndTop.hasKnownValue() && !(stack2ndTop instanceof ObjectValue ov && ov.isNull()))
								continue;

							// Skip if the other argument to compare with is not immediately backed by
							// a value supplying instruction.
							AbstractInsnNode prevPrevInstruction = prevInstruction.getPrevious();
							if (prevPrevInstruction == null || !isValueProducerOrTopDup(prevPrevInstruction))
								continue;

							// Replace double argument binary control flow.
							localDirty |= switch (opcode) {
								case IF_ICMPEQ ->
										replaceIntIntValue(instructions, prevPrevInstruction, prevInstruction, stack2ndTop, stackTop, jin, IntValue::isEqualTo);
								case IF_ICMPNE ->
										replaceIntIntValue(instructions, prevPrevInstruction, prevInstruction, stack2ndTop, stackTop, jin, IntValue::isNotEqualTo);
								case IF_ICMPLT ->
										replaceIntIntValue(instructions, prevPrevInstruction, prevInstruction, stack2ndTop, stackTop, jin, IntValue::isLessThan);
								case IF_ICMPGE ->
										replaceIntIntValue(instructions, prevPrevInstruction, prevInstruction, stack2ndTop, stackTop, jin, IntValue::isGreaterThanOrEqual);
								case IF_ICMPGT ->
										replaceIntIntValue(instructions, prevPrevInstruction, prevInstruction, stack2ndTop, stackTop, jin, IntValue::isGreaterThan);
								case IF_ICMPLE ->
										replaceIntIntValue(instructions, prevPrevInstruction, prevInstruction, stack2ndTop, stackTop, jin, IntValue::isLessThanOrEqual);
								case IF_ACMPEQ ->
										replaceObjObjValue(instructions, prevPrevInstruction, prevInstruction, stack2ndTop, stackTop, jin,
												(a, b) -> a.isNull() && b.isNull(), // Both null --> both are equal
												(a, b) -> (a.isNull() && b.isNotNull()) || (a.isNotNull() && b.isNull())); // Nullability conflict, both cannot be equal
								case IF_ACMPNE ->
										replaceObjObjValue(instructions, prevPrevInstruction, prevInstruction, stack2ndTop, stackTop, jin,
												(a, b) -> (a.isNull() && b.isNotNull()) || (a.isNotNull() && b.isNull()), // Nullability conflict, both cannot be equal
												(a, b) -> a.isNull() && b.isNull()); // Both null --> both are equal
								default -> localDirty;
							};
						}
					} else if (insnType == AbstractInsnNode.LOOKUPSWITCH_INSN) {
						LookupSwitchInsnNode lsin = (LookupSwitchInsnNode) instruction;

						// Skip if stack top is not an integer.
						if (!(stackTop instanceof IntValue intValue))
							continue;

						// Find matching key in switch.
						int keyIndex = -1;
						for (int j = 0; j < lsin.keys.size(); j++) {
							int key = lsin.keys.get(j);
							if (intValue.isEqualTo(key)) {
								keyIndex = j;
								break;
							}
						}

						// Replace switch with goto for the appropriate control flow path.
						JumpInsnNode replacement = keyIndex == -1 ?
								new JumpInsnNode(GOTO, lsin.dflt) :
								new JumpInsnNode(GOTO, lsin.labels.get(keyIndex));
						instructions.set(lsin, replacement);
						instructions.set(prevInstruction, new InsnNode(NOP));
						localDirty = true;
					} else if (insnType == AbstractInsnNode.TABLESWITCH_INSN) {
						TableSwitchInsnNode tsin = (TableSwitchInsnNode) instruction;

						// Skip if stack top is not an integer.
						if (!(stackTop instanceof IntValue intValue))
							continue;

						// Find matching key in switch.
						int arg = intValue.value().getAsInt();
						int keyIndex = (arg > tsin.max || arg < tsin.min) ?
								-1 : (arg - tsin.min);

						// Replace switch with goto for the appropriate control flow path.
						JumpInsnNode replacement = keyIndex == -1 ?
								new JumpInsnNode(GOTO, tsin.dflt) :
								new JumpInsnNode(GOTO, tsin.labels.get(keyIndex));
						instructions.set(tsin, replacement);
						instructions.set(prevInstruction, new InsnNode(NOP));
						localDirty = true;
					}
				}

				// Clear any code that is no longer accessible. If we don't do this step ASM's auto-cleanup
				// will likely leave some ugly artifacts like "athrow" in dead code regions.
				if (localDirty) {
					context.pruneDeadCode(node, method);
					dirty = true;
				}
			} catch (Throwable t) {
				throw new TransformationException("Error encountered when folding opaque predicates", t);
			}
		}
		if (dirty) {
			context.setRecomputeFrames(initialClassState.getName());
			context.setNode(bundle, initialClassState, node);
		}
	}

	private static boolean replaceIntValue(@Nonnull InsnList instructions,
	                                       @Nonnull AbstractInsnNode stackValueProducerInsn,
	                                       @Nonnull ReValue stackTopValue,
	                                       @Nonnull JumpInsnNode jump,
	                                       @Nonnull Predicate<IntValue> gotoCondition) {
		if (stackTopValue instanceof IntValue intValue) {
			AbstractInsnNode replacement = gotoCondition.test(intValue) ?
					new JumpInsnNode(GOTO, jump.label) :
					new InsnNode(NOP);
			instructions.set(jump, replacement);
			instructions.set(stackValueProducerInsn, new InsnNode(NOP));
			return true;
		}
		return false;
	}

	private static boolean replaceIntIntValue(@Nonnull InsnList instructions,
	                                          @Nonnull AbstractInsnNode stackValueProducerInsnA,
	                                          @Nonnull AbstractInsnNode stackValueProducerInsnB,
	                                          @Nonnull ReValue stackTopValueA,
	                                          @Nonnull ReValue stackTopValueB,
	                                          @Nonnull JumpInsnNode jump,
	                                          @Nonnull BiPredicate<IntValue, IntValue> gotoCondition) {
		if (stackTopValueA instanceof IntValue intValueA && stackTopValueB instanceof IntValue intValueB) {
			AbstractInsnNode replacement = gotoCondition.test(intValueA, intValueB) ?
					new JumpInsnNode(GOTO, jump.label) :
					new InsnNode(NOP);
			instructions.set(jump, replacement);
			instructions.set(stackValueProducerInsnA, new InsnNode(NOP));
			instructions.set(stackValueProducerInsnB, new InsnNode(NOP));
			return true;
		}
		return false;
	}

	private static boolean replaceObjValue(@Nonnull InsnList instructions,
	                                       @Nonnull AbstractInsnNode stackValueProducerInsn,
	                                       @Nonnull ReValue stackTopValue,
	                                       @Nonnull JumpInsnNode jump,
	                                       @Nonnull Predicate<ObjectValue> gotoCondition) {
		if (stackTopValue instanceof ObjectValue objectValue) {
			AbstractInsnNode replacement = gotoCondition.test(objectValue) ?
					new JumpInsnNode(GOTO, jump.label) :
					new InsnNode(NOP);
			instructions.set(jump, replacement);
			instructions.set(stackValueProducerInsn, new InsnNode(NOP));
			return true;
		}
		return false;
	}

	private static boolean replaceObjObjValue(@Nonnull InsnList instructions,
	                                          @Nonnull AbstractInsnNode stackValueProducerInsnA,
	                                          @Nonnull AbstractInsnNode stackValueProducerInsnB,
	                                          @Nonnull ReValue stackTopValueA,
	                                          @Nonnull ReValue stackTopValueB,
	                                          @Nonnull JumpInsnNode jump,
	                                          @Nonnull BiPredicate<ObjectValue, ObjectValue> gotoCondition,
	                                          @Nonnull BiPredicate<ObjectValue, ObjectValue> fallCondition) {
		if (stackTopValueA instanceof ObjectValue objValueA && stackTopValueB instanceof ObjectValue objValueB) {
			// Objects are a bit more complicated than primitives, so we have separate checks for replacing as a goto
			// versus a fallthrough case. Additionally, if neither conditions pass we must be in a state where the values
			// are technically known, but not well enough to the point where we can make a decision.
			AbstractInsnNode replacement = gotoCondition.test(objValueA, objValueB) ? new JumpInsnNode(GOTO, jump.label) : null;
			if (replacement == null) replacement = fallCondition.test(objValueA, objValueB) ? new InsnNode(NOP) : null;
			if (replacement == null) return false;
			instructions.set(jump, replacement);
			instructions.set(stackValueProducerInsnA, new InsnNode(NOP));
			instructions.set(stackValueProducerInsnB, new InsnNode(NOP));
			return true;
		}
		return false;
	}

	private static boolean isValueProducerOrTopDup(@Nonnull AbstractInsnNode insnNode) {
		if (isSupportedValueProducer(insnNode))
			return true;

		int op = insnNode.getOpcode();
		return op == DUP || op == DUP2;
	}

	@Nonnull
	@Override
	public Set<Class<? extends ClassTransformer>> recommendedPredecessors() {
		return Set.of(
				// Folding opaque constants like "1 + 1" into "2"
				OpaqueConstantFoldingTransformer.class,
				// Folding static constants into inline-usages (some obfuscators use fields to store opaque flow values)
				StaticValueInliningTransformer.class,
				// Folding constant values stored in variables
				VariableFoldingTransformer.class
		);
	}

	@Nonnull
	@Override
	public Set<Class<? extends ClassTransformer>> dependencies() {
		return Collections.singleton(DeadCodeRemovingTransformer.class);
	}

	@Nonnull
	@Override
	public String name() {
		return "Opaque predicate simplification";
	}
}
