package software.coley.recaf.services.deobfuscation.transform.generic;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.services.transform.ClassTransformer;
import software.coley.recaf.services.transform.JvmClassTransformer;
import software.coley.recaf.services.transform.JvmTransformerContext;
import software.coley.recaf.services.transform.TransformationException;
import software.coley.recaf.util.AsmInsnUtil;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import static org.objectweb.asm.Opcodes.NOP;
import static software.coley.recaf.util.AsmInsnUtil.fixMissingVariableLabels;

/**
 * A transformer that removes dead code.
 *
 * @author Matt Coley
 */
@Dependent
public class DeadCodeRemovingTransformer implements JvmClassTransformer {
	@Override
	public void transform(@Nonnull JvmTransformerContext context, @Nonnull Workspace workspace,
	                      @Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle,
	                      @Nonnull JvmClassInfo initialClassState) throws TransformationException {
		boolean dirty = false;
		String className = initialClassState.getName();
		ClassNode node = context.getNode(bundle, initialClassState);
		for (MethodNode method : node.methods)
			dirty |= prune(node, method);
		if (dirty)
			context.setNode(bundle, initialClassState, node);
	}

	public boolean prune(@Nonnull ClassNode node, @Nonnull MethodNode method) throws TransformationException {
		InsnList instructions = method.instructions;
		if (instructions == null || instructions.size() == 0)
			return false;

		// Collect try blocks and the instructions within the start-end range (exclusive)
		List<TryCatch> tryCatches = new ArrayList<>(method.tryCatchBlocks.size());
		for (TryCatchBlockNode block : method.tryCatchBlocks) {
			List<AbstractInsnNode> blockInsns = new ArrayList<>();
			AbstractInsnNode insn = block.start;
			LabelNode end = block.end;
			while (insn != null) {
				insn = insn.getNext();
				if (insn == end || insn == null)
					break;
				blockInsns.add(insn);
			}
			tryCatches.add(new TryCatch(block, blockInsns));
		}

		boolean dirty = false;
		try {
			// Compute which instructions are visited by walking the method's control flow.
			Set<AbstractInsnNode> visited = Collections.newSetFromMap(new IdentityHashMap<>());
			List<AbstractInsnNode> flowStarts = new ArrayList<>();
			flowStarts.add(instructions.getFirst());
			for (TryCatch tryCatch : tryCatches)
				flowStarts.add(tryCatch.block.handler);
			visit(visited, flowStarts);

			// Prune any instructions not visited.
			int end = instructions.size() - 1;
			for (int i = end; i >= 0; i--) {
				AbstractInsnNode insn = instructions.get(i);
				if (!visited.contains(insn) || insn.getOpcode() == NOP) {
					// Don't prune the tail label even if it is "dead" because the last method instruction
					// is terminal like 'return' or 'athrow'. The label will just get added back automatically
					// and cause the transform process to loop on repeat.
					if (i == end && insn.getType() == AbstractInsnNode.LABEL)
						continue;

					// Keep try-catch labels for now.
					if (insn.getType() == AbstractInsnNode.LABEL && method.tryCatchBlocks.stream().anyMatch(tryCatch -> {
						if (insn == tryCatch.start) return true;
						if (insn == tryCatch.end) return true;
						return insn == tryCatch.handler;
					})) continue;

					// Remove instruction from method.
					instructions.remove(insn);

					// Remove from any catch block's visited instructions.
					for (TryCatch tryCatch : tryCatches)
						tryCatch.visitedInstructions.remove(insn);

					// Mark as dirty.
					dirty = true;
				}
			}

			// If we have removed all the instructions of a try block's start-end range (because they're dead code)
			// then the try-catch block entry can be removed.
			for (TryCatch tryCatch : tryCatches) {
				if (tryCatch.visitedInstructions.isEmpty()) {
					method.tryCatchBlocks.remove(tryCatch.block);
				}
			}

			// Ensure that after dead code removal (or any other transformers not cleaning up)
			// that all variables have labels that reside in the method code list.
			List<LocalVariableNode> variables = method.localVariables;
			if (variables != null && variables.stream().anyMatch(l -> !instructions.contains(l.start) || !instructions.contains(l.end))) {
				fixMissingVariableLabels(method);
				dirty = true;
			}
		} catch (Throwable t) {
			throw new TransformationException("Error encountered when removing dead code", t);
		}
		return dirty;
	}

	private static void visit(@Nonnull Set<AbstractInsnNode> visited, @Nonnull List<AbstractInsnNode> startingPoints) {
		Queue<AbstractInsnNode> todo = new ArrayDeque<>(startingPoints);
		while (!todo.isEmpty()) {
			AbstractInsnNode insn = todo.remove();
			while (insn != null && visited.add(insn)) {
				handleNext:
				{
					switch (insn.getType()) {
						case AbstractInsnNode.INSN -> {
							if (AsmInsnUtil.isTerminalOrAlwaysTakeFlowControl(insn.getOpcode()))
								break handleNext;
						}
						case AbstractInsnNode.JUMP_INSN -> {
							JumpInsnNode jin = (JumpInsnNode) insn;
							todo.add(jin.label);
							if (AsmInsnUtil.isTerminalOrAlwaysTakeFlowControl(jin.getOpcode()))
								break handleNext;
						}
						case AbstractInsnNode.TABLESWITCH_INSN -> {
							TableSwitchInsnNode tsin = (TableSwitchInsnNode) insn;
							todo.add(tsin.dflt);
							todo.addAll(tsin.labels);
							break handleNext;
						}
						case AbstractInsnNode.LOOKUPSWITCH_INSN -> {
							LookupSwitchInsnNode lsin = (LookupSwitchInsnNode) insn;
							todo.add(lsin.dflt);
							todo.addAll(lsin.labels);
							break handleNext;
						}
					}
					insn = insn.getNext();
				}
			}
		}
	}

	@Nonnull
	@Override
	public Set<Class<? extends ClassTransformer>> recommendedPredecessors() {
		// Having opaque predicates replaced with direct GOTO or fall-through
		// will allow  this transformer to properly detect dead code.
		return Collections.singleton(OpaquePredicateFoldingTransformer.class);
	}

	@Nonnull
	@Override
	public String name() {
		return "Dead code removal";
	}

	record TryCatch(@Nonnull TryCatchBlockNode block, @Nonnull List<AbstractInsnNode> visitedInstructions) {
		boolean hasInsn(@Nonnull AbstractInsnNode insn) {
			return visitedInstructions.contains(insn);
		}
	}
}