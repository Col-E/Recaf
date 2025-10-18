package software.coley.recaf.services.deobfuscation.transform.generic;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import software.coley.recaf.RecafConstants;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.services.transform.ClassTransformer;
import software.coley.recaf.services.transform.JvmClassTransformer;
import software.coley.recaf.services.transform.JvmTransformerContext;
import software.coley.recaf.services.transform.TransformationException;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static software.coley.recaf.util.AsmInsnUtil.*;

/**
 * A transformer that inlines control flow of <i>(redundant)</i> {@link Opcodes#GOTO} instructions.
 *
 * @author Matt Coley
 */
@Dependent
public class GotoInliningTransformer implements JvmClassTransformer {
	private static final int CATCH_VISIT_COUNT = 100;
	private static final boolean DO_WE_CARE_ABOUT_BACKWARDS_SWITCH_FLOW = false;

	@Override
	public void transform(@Nonnull JvmTransformerContext context, @Nonnull Workspace workspace,
	                      @Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle,
	                      @Nonnull JvmClassInfo initialClassState) throws TransformationException {
		boolean dirty = false;
		String className = initialClassState.getName();
		ClassNode node = context.getNode(bundle, initialClassState);

		for (int m = 0; m < node.methods.size(); m++) {
			MethodNode base = node.methods.get(m);

			// Skip if abstract.
			if (base.instructions == null)
				continue;

			// Because of the multiple "stages" we do, it's easier if we work on a copy of the method
			// and then write back our copy if we ended up making relevant changes. This way, if we
			// have changes from pre-processing, but there is no inlining work that gets done we can
			// throw away the changes and not worry about the changes accidentally being kept.
			MethodNode method = copyOf(base);
			InsnList instructions = method.instructions;

			// There are some obfuscators that put junk after the final 'return' instruction of methods.
			//
			// For example:
			//    return
			//    nop    // dead code removed by ASM
			//    athrow
			//
			// In this transformer, we ensure that removing blocks near the end of a method does not result in dangling code.
			// Without removing the dead code seen in the example, this transformer would see the 'athrow' and think it is
			// ok to move a block containing the 'return' somewhere else. However, the 'athrow' there is not valid because
			// there is nothing on the stack to throw.
			//
			// The simple fix is to do a dead-code removing pass before we run this transformer.
			context.pruneDeadCode(node, method);

			// Record where labels are visited from. Start with explicit control flow jumps.
			VisitCounters visitCounters = new VisitCounters();
			for (int i = 0; i < instructions.size(); i++) {
				AbstractInsnNode insn = instructions.get(i);
				if (insn instanceof JumpInsnNode cast) {
					visitCounters.of(cast.label).addExplicitSource(cast);
				} else if (insn instanceof TableSwitchInsnNode cast) {
					visitCounters.of(cast.dflt).addExplicitSource(cast);
					cast.labels.forEach(l -> visitCounters.of(l).addExplicitSource(cast));
				} else if (insn instanceof LookupSwitchInsnNode cast) {
					visitCounters.of(cast.dflt).addExplicitSource(cast);
					cast.labels.forEach(l -> visitCounters.of(l).addExplicitSource(cast));
				}
			}

			// Next record which labels are try-catch boundaries.
			if (method.tryCatchBlocks != null) {
				for (TryCatchBlockNode tryCatch : method.tryCatchBlocks) {
					visitCounters.of(tryCatch.start).markTryStart();
					visitCounters.of(tryCatch.end).markTryEnd();
					visitCounters.of(tryCatch.handler).markTryHandler();
				}
			}

			// Lastly record implicit flow into labels.
			// This just means if the code flows linearly from "A" into "B".
			for (int i = 0; i < instructions.size(); i++) {
				AbstractInsnNode insn = instructions.get(i);
				if (insn instanceof LabelNode targetLabel) {
					AbstractInsnNode prev = targetLabel.getPrevious();

					// If the target label has no previous instruction then it must be the first label of the method.
					// Thus, it makes sense to say "yeah, we can flow here".
					if (prev == null) {
						visitCounters.of(targetLabel).markImplicitFlow();
						continue;
					}

					// We want to see if we can naturally flow into this position.
					// Begin walking backwards linearly and see if we can end up at this target label.
					while (true) {
						// If we encounter an instruction that terminates linear flow we will stop walking backward.
						// This should imply that the target label cannot naturally be flowed into.
						if (isTerminalOrAlwaysTakeFlowControl(prev.getOpcode()))
							break;

						// If we encounter another label that is visited at least once while walking backwards
						// then the flow should continue from there to our target label.
						if (prev instanceof LabelNode prevLabel && visitCounters.of(prevLabel).isVisited()) {
							visitCounters.of(targetLabel).markImplicitFlow();
							break;
						}

						// Step backwards.
						prev = prev.getPrevious();

						// Same check that we had before the while loop. If we hit the start of the method,
						// then of course we can flow to here.
						if (prev == null) {
							visitCounters.of(targetLabel).markImplicitFlow();
							break;
						}
					}
				}
			}

			// Check for super-simple goto instruction patterns that can be inlined easily.
			boolean localDirty = false;
			for (int i = 0; i < instructions.size(); i++) {
				AbstractInsnNode insn = instructions.get(i);

				// Skip any non-goto instruction.
				if (insn.getOpcode() != GOTO)
					continue;

				// If the goto target label is just the next instruction then we can replace
				// the goto with a nop. The dead code pass later on will clean these up.
				JumpInsnNode jin = (JumpInsnNode) insn;
				VisitCounter counter = visitCounters.of(jin.label);
				if (jin.label == jin.getNext() && !counter.isTryTarget()) {
					localDirty = true;
					instructions.set(jin, new InsnNode(NOP));
					counter.removeExplicitSource(jin);
					counter.markImplicitFlow();
				}
			}

			// Check for goto instruction patterns that require more care.
			//  - Cannot result in creating end-of-method fall-through.
			//  - Cannot inline a block starting with a label that is flowed to both explicitly and implicitly.
			//  - Cannot inline a block that would break try-catch range contracts.
			for (int i = 0; i < instructions.size(); i++) {
				AbstractInsnNode insn = instructions.get(i);

				// Skip any non-goto instruction.
				if (insn.getOpcode() != GOTO)
					continue;

				// Skip any jump target labels that are visited more than once.
				JumpInsnNode jin = (JumpInsnNode) insn;
				if (visitCounters.of(jin.label).count() > 1)
					continue;

				// Attempt to re-arrange the code at the goto's destination to be inline here.
				doInline:
				{
					List<AbstractInsnNode> block = new ArrayList<>();
					AbstractInsnNode target = jin.label;
					while (target != null) {
						// Abort if we loop back around.
						if (target == jin)
							break doInline;

						// Abort if we see that relocating the GOTO destination's code would change the behavior
						// with try-catch blocks.
						if (method.tryCatchBlocks != null) {
							for (TryCatchBlockNode tryCatchBlock : method.tryCatchBlocks) {
								int gotoIndex = instructions.indexOf(jin);
								int targetIndex = instructions.indexOf(target);
								int tryStart = instructions.indexOf(tryCatchBlock.start);
								int tryEnd = instructions.indexOf(tryCatchBlock.end);

								// Skip if this would result in moving the targeted code inside a try-catch somewhere outside the try-catch.
								if (tryStart <= targetIndex && targetIndex < tryEnd) {
									if (tryStart > gotoIndex || gotoIndex >= tryEnd)
										break doInline;
								}

								// Skip if this would result in moving the code outside the try-catch into the try-catch
								if (tryStart <= gotoIndex && gotoIndex < tryEnd) {
									if (tryStart > targetIndex || targetIndex >= tryEnd)
										break doInline;
								}
							}
						}

						int targetOp = target.getOpcode();

						// TODO: Maybe remove this? Need to do more research into when it complains.
						if (DO_WE_CARE_ABOUT_BACKWARDS_SWITCH_FLOW) {
							// There are some weird cases where you're not allowed to jump backwards in switch instructions,
							// so it's better to abort if we see them so that we do not move them around in an illegal way.
							if (targetOp == TABLESWITCH || targetOp == LOOKUPSWITCH)
								break doInline;
						}

						// Record this instruction as part of the block if it isn't metadata/junk.
						if (target.getType() != AbstractInsnNode.FRAME)
							block.add(target);

						// Break out of this while loop if the target instruction is the end of a method's control flow,
						// or an always-branch instruction like goto/switch. This marks the end of our block.
						// We will do some final checks to see if this block can be inlined.
						if (isGotoBlockTerminator(targetOp)) {
							// Check if inlining this would cause dangling code (no return at the end of the method)
							AbstractInsnNode next = getNextInsn(target);
							if (instructions.getLast() == next || next == null) {
								// This block is the code at the end of the method.
								// Check if the code before this block has terminal control flow
								// (to prevent creation of dangling code at the end of the method)
								AbstractInsnNode prevBeforeBlock = getPreviousInsn(jin.label);
								if (prevBeforeBlock != null && !isTerminalOrAlwaysTakeFlowControl(prevBeforeBlock.getOpcode()))
									break doInline;
							}

							// Check if the current target instruction isn't the initial goto destination label,
							// and the current target is a label that has been visited more than once by control flow
							// originating from outside of this block.
							for (AbstractInsnNode blockInsn : block) {
								int blockInsnOp = blockInsn.getOpcode();
								if (blockInsn != jin.label && blockInsnOp == -1
										&& blockInsn instanceof LabelNode blockInsnLabel
										&& visitCounters.of(blockInsnLabel).getExplicitFlowSourcesExcluding(block) > 1)
									break doInline;
							}

							break;
						}

						// Move forward.
						target = target.getNext();
					}

					// Cut and paste those instructions into a temporary list.
					InsnList tempInsnList = new InsnList();
					for (AbstractInsnNode blockInsn : block) {
						instructions.remove(blockInsn);
						tempInsnList.add(blockInsn);
					}

					// Insert the block after the GOTO, then remove the GOTO.
					instructions.insert(jin, tempInsnList);
					instructions.remove(jin);
					localDirty = true;

					// Since we removed the original goto instruction, remove it from the label's visit counter.
					visitCounters.of(jin.label).removeExplicitSource(jin);

					// Start over from the beginning.
					i = 0;
				}
			}
			if (localDirty) {
				// Do another dead code removal pass.
				context.pruneDeadCode(node, method);

				// Fix references to labels that no longer exist in local variable debug metadata.
				fixMissingVariableLabels(method);
				dirty = true;

				// Update the class with our transformed copy of the method.
				node.methods.set(m, method);
			}
		}
		if (dirty) {
			context.setRecomputeFrames(className);
			context.setNode(bundle, initialClassState, node);
		}
	}

	@Nonnull
	@Override
	public Set<Class<? extends ClassTransformer>> dependencies() {
		return Collections.singleton(DeadCodeRemovingTransformer.class);
	}

	@Nonnull
	@Override
	public String name() {
		return "Goto inlining";
	}

	/**
	 * @param base
	 * 		Method model to copy.
	 *
	 * @return Copy of method model.
	 */
	@Nonnull
	private static MethodNode copyOf(@Nonnull MethodNode base) {
		MethodNode copy = new MethodNode(RecafConstants.getAsmVersion(), base.access, base.name, base.desc, base.signature, base.exceptions.toArray(String[]::new)) {
			@Override
			public void visitFrame(int type, int numLocal, Object[] local, int numStack, Object[] stack) {
				// Skip frames
			}
		};
		base.accept(copy);
		return copy;
	}

	/**
	 * @param op
	 * 		Instruction opcode.
	 *
	 * @return {@code true} when the opcode represents an instruction
	 * that should mark the end of a {@code GOTO} destination block.
	 */
	private static boolean isGotoBlockTerminator(int op) {
		return isReturn(op) || op == GOTO || op == TABLESWITCH || op == LOOKUPSWITCH || op == ATHROW;
	}

	/**
	 * Map of {@link LabelNode} to visit/flow data.
	 */
	private static class VisitCounters {
		private final Map<LabelNode, VisitCounter> visitCounters = new IdentityHashMap<>();

		@Nonnull
		public VisitCounter of(@Nonnull LabelNode label) {
			return visitCounters.computeIfAbsent(label, VisitCounter::new);
		}
	}

	/**
	 * Model of control flow interactions with a {@link LabelNode}.
	 */
	private static class VisitCounter {
		private final LabelNode label;
		private final Set<AbstractInsnNode> explicitFlowSources = Collections.newSetFromMap(new IdentityHashMap<>());
		private boolean implicitFlow;
		private boolean tryTarget;

		private VisitCounter(@Nonnull LabelNode label) {
			this.label = label;
		}

		public void addExplicitSource(@Nonnull AbstractInsnNode insn) {
			explicitFlowSources.add(insn);
		}

		public void removeExplicitSource(@Nonnull AbstractInsnNode insn) {
			explicitFlowSources.remove(insn);
		}

		public void markImplicitFlow() {
			implicitFlow = true;
		}

		public boolean isVisited() {
			if (!explicitFlowSources.isEmpty())
				return true;
			return implicitFlow || tryTarget;
		}

		public boolean isImplicitFlow() {
			return implicitFlow;
		}

		public boolean isTryTarget() {
			return tryTarget;
		}

		public long getExplicitFlowSourcesExcluding(@Nonnull Collection<AbstractInsnNode> block) {
			return explicitFlowSources.stream()
					.filter(insn -> !block.contains(insn))
					.count();
		}

		public int count() {
			return explicitFlowSources.size() + (implicitFlow ? 1 : 0) + (tryTarget ? 1 : 0);
		}

		public void markTryStart() {
			tryTarget = true;
		}

		public void markTryEnd() {
			tryTarget = true;
		}

		public void markTryHandler() {
			tryTarget = true;
		}

		@Override
		public String toString() {
			return "VisitCounter{" +
					"label=" + indexOf(label) +
					", explicitFlowSources=" + explicitFlowSources.size() +
					", implicitFlow=" + implicitFlow +
					", tryTarget=" + tryTarget +
					'}';
		}
	}
}
