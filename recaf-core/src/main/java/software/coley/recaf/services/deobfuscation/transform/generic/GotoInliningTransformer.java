package software.coley.recaf.services.deobfuscation.transform.generic;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.services.transform.JvmClassTransformer;
import software.coley.recaf.services.transform.JvmTransformerContext;
import software.coley.recaf.services.transform.TransformationException;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static org.objectweb.asm.Opcodes.*;
import static software.coley.recaf.util.AsmInsnUtil.isReturn;

/**
 * A transformer that inlines control flow of <i>(redundant)</i> {@link Opcodes#GOTO} instructions.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class GotoInliningTransformer implements JvmClassTransformer {
	private static final int CATCH_VISIT_COUNT = 100;

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

			// Count how often each label in the method is visited by control flow.
			//  - We *never* want to mess around with try-catch range boundaries, so they get a large bump so we can
			//    clearly see them while debugging.
			Map<LabelNode, Integer> jumpCount = new IdentityHashMap<>();
			for (int i = 0; i < instructions.size(); i++) {
				AbstractInsnNode insn = instructions.get(i);
				if (insn instanceof JumpInsnNode cast) {
					jumpCount.merge(cast.label, 1, Integer::sum);
				} else if (insn instanceof TableSwitchInsnNode cast) {
					jumpCount.merge(cast.dflt, 1, Integer::sum);
					cast.labels.forEach(l -> jumpCount.merge(l, 1, Integer::sum));
				} else if (insn instanceof LookupSwitchInsnNode cast) {
					jumpCount.merge(cast.dflt, 1, Integer::sum);
					cast.labels.forEach(l -> jumpCount.merge(l, 1, Integer::sum));
				}
			}
			if (method.tryCatchBlocks != null) {
				for (TryCatchBlockNode tryCatch : method.tryCatchBlocks) {
					jumpCount.put(tryCatch.start, CATCH_VISIT_COUNT);
					jumpCount.put(tryCatch.end, CATCH_VISIT_COUNT);
					jumpCount.put(tryCatch.handler, CATCH_VISIT_COUNT);
				}
			}

			// Check instructions for GOTOs that can be inlined.
			for (int i = 0; i < instructions.size(); i++) {
				AbstractInsnNode insn = instructions.get(i);

				// Skip any non-goto instruction.
				if (insn.getOpcode() != GOTO)
					continue;

				// Skip any jump targets that are visited more than once.
				JumpInsnNode jin = (JumpInsnNode) insn;
				if (jumpCount.get(jin.label) > 1)
					continue;

				// Attempt to re-arrange the code at the GOTOs destination to be inline here.
				doInline:
				{
					AbstractInsnNode target = jin.label;
					while (target != null) {
						// Abort if we loop back around.
						if (target == jin)
							break doInline;

						// Abort if the current target isn't the initial GOTO destination label,
						// and the current target is a label that has been visited more than once by control flow.
						int targetOp = target.getOpcode();
						if (target != jin.label && targetOp == -1 && jumpCount.getOrDefault(target, 0) > 1)
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

						// Break out of this while loop if the target instruction is the end of a method's control flow,
						// or an always-branch instruction like goto/switch.
						if (isGotBlockTerminator(targetOp))
							break;

						// Move forward.
						target = target.getNext();
					}

					// Construct the list of instructions that represent the GOTO's destination block.
					target = jin.label;
					List<AbstractInsnNode> block = new ArrayList<>();
					while (target != null) {
						block.add(target);
						if (isGotBlockTerminator(target.getOpcode()))
							break;
						target = target.getNext();
					}

					// Cut and paste those instructions into a temporary list.
					InsnList tempInsnList = new InsnList();
					block.forEach(instructions::remove);
					block.forEach(tempInsnList::add);

					// Insert the block after the GOTO, then remove the GOTO.
					instructions.insert(jin, tempInsnList);
					instructions.remove(jin);
					dirty = true;

					// Start over from the beginning.
					i = 0;
				}
			}
		}
		if (dirty)
			context.setNode(bundle, initialClassState, node);
	}


	@Nonnull
	@Override
	public String name() {
		return "Goto inlining";
	}

	/**
	 * @param op
	 * 		Instruction opcode.
	 *
	 * @return {@code true} when the opcode represents an instruction
	 * that should mark the end of a {@code GOTO} destination block.
	 */
	private static boolean isGotBlockTerminator(int op) {
		return isReturn(op) || op == GOTO || op == TABLESWITCH || op == LOOKUPSWITCH || op == ATHROW;
	}
}
