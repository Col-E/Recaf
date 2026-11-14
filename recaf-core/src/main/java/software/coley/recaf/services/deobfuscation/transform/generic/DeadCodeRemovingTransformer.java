package software.coley.recaf.services.deobfuscation.transform.generic;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.services.transform.ClassTransformer;
import software.coley.recaf.services.transform.JvmClassTransformer;
import software.coley.recaf.services.transform.JvmTransformerContext;
import software.coley.recaf.services.transform.TransformationException;
import software.coley.recaf.util.AsmInsnUtil;
import software.coley.recaf.util.collect.primitive.Int2ObjectMap;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
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
	public static final String IDENTIFIER = "cleanup.deadcode";

	@Override
	public void transform(@Nonnull JvmTransformerContext context, @Nonnull Workspace workspace,
	                      @Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle,
	                      @Nonnull JvmClassInfo initialClassState) throws TransformationException {
		boolean dirty = false;
		String className = initialClassState.getName();
		ClassNode node = context.getNode(bundle, initialClassState);
		for (MethodNode method : node.methods)
			dirty |= prune(node, method);
		if (dirty) {
			// Removed blocks take their frames with them, but later delta frames can be relative to them.
			context.setRecomputeFrames(className);
			context.setNode(bundle, initialClassState, node);
		}
	}

	public boolean prune(@Nonnull ClassNode node, @Nonnull MethodNode method) throws TransformationException {
		InsnList instructions = method.instructions;
		if (instructions == null || instructions.size() == 0)
			return false;

		// Collect all labels that are part of try-catch blocks so they can be preserved even if they are "dead".
		List<TryCatchBlockNode> tryCatches = method.tryCatchBlocks;
		Set<AbstractInsnNode> protectedLabels = Collections.newSetFromMap(new IdentityHashMap<>());
		for (TryCatchBlockNode block : tryCatches) {
			protectedLabels.add(block.start);
			protectedLabels.add(block.end);
			protectedLabels.add(block.handler);
		}

		boolean dirty = false;
		try {
			// Compute which instructions are reachable from the method entry and every exception handler.
			int size = instructions.size();
			Int2ObjectMap<List<Integer>> successors = new Int2ObjectMap<>(size);
			AsmInsnUtil.populateSuccessors(method, successors, AsmInsnUtil.ExceptionFlow.NONE);
			int[] entries = new int[tryCatches.size() + 1];
			for (int i = 0; i < tryCatches.size(); i++)
				entries[i + 1] = instructions.indexOf(tryCatches.get(i).handler);
			BitSet visited = AsmInsnUtil.computeReachable(size, successors, entries);

			// Compute a prefix sum of kept instructions so we can determine if
			// a try-catch block has any executable instructions left after pruning.
			int end = instructions.size() - 1;
			int[] keptPrefix = new int[end + 2];
			Map<LabelNode, Integer> labelIndices = new IdentityHashMap<>();
			for (int i = 0; i <= end; i++) {
				AbstractInsnNode insn = instructions.get(i);
				if (insn instanceof LabelNode label)
					labelIndices.put(label, i);

				// Keep when the instruction is visited and not a NOP, or is an ending protected label for a try-catch block.
				// Later usage of 'fixMissingVariableLabels' will add trailing labels back, so removing here would be redundant.
				boolean keep = visited.get(i) && insn.getOpcode() != NOP;
				if (!keep && (i == end && insn.getType() == AbstractInsnNode.LABEL || insn.getType() == AbstractInsnNode.LABEL && protectedLabels.contains(insn)))
					keep = true;
				keptPrefix[i + 1] = keptPrefix[i] + (keep && insn.getOpcode() >= 0 ? 1 : 0);
			}

			// Determine which try-catch blocks have any executable instructions left after pruning.
			boolean[] catchHasKeptNode = new boolean[tryCatches.size()];
			for (int i = 0; i < tryCatches.size(); i++) {
				TryCatchBlockNode block = tryCatches.get(i);
				Integer start = labelIndices.get(block.start);
				Integer finish = labelIndices.get(block.end);
				catchHasKeptNode[i] = start == null || finish == null || start >= finish || keptPrefix[finish] > keptPrefix[start + 1];
			}

			// Prune any instructions not visited.
			for (int i = end; i >= 0; i--) {
				if (i >= instructions.size())
					continue;
				AbstractInsnNode insn = instructions.get(i);
				if (insn == null)
					continue;
				if (!visited.get(i) || insn.getOpcode() == NOP) {
					// Don't prune the tail label even if it's "dead" because the last method instruction
					// is terminal like 'return' or 'athrow'. The label will just get added back automatically
					// and cause the transform process to loop on repeat.
					if (i == end && insn.getType() == AbstractInsnNode.LABEL)
						continue;

					// Keep try-catch labels for now.
					if (insn.getType() == AbstractInsnNode.LABEL && protectedLabels.contains(insn))
						continue;

					instructions.remove(insn);
					dirty = true;
				}
			}

			// Remove entries whose protected range has no executable instructions left after pruning.
			List<TryCatchBlockNode> keptTryCatches = new ArrayList<>(tryCatches.size());
			for (int i = 0; i < tryCatches.size(); i++)
				if (catchHasKeptNode[i])
					keptTryCatches.add(tryCatches.get(i));
			if (keptTryCatches.size() != tryCatches.size()) {
				tryCatches.clear();
				tryCatches.addAll(keptTryCatches);
				dirty = true;
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

	@Nonnull
	@Override
	public Set<Class<? extends ClassTransformer>> recommendedPredecessors() {
		// Having opaque predicates replaced with direct GOTO or fall-through
		// will allow  this transformer to properly detect dead code.
		return Collections.singleton(OpaquePredicateFoldingTransformer.class);
	}

	@Nonnull
	@Override
	public String identifier() {
		return IDENTIFIER;
	}

	@Nonnull
	@Override
	public String beforeExample() {
		return """
				goto after; // Java doesn't have 'goto' in source, but this is the general idea
				dead:
				  System.out.println("Dead code");
				after:
				  return;
				""";
	}

	@Nonnull
	@Override
	public String afterExample() {
		return """
				goto after;
				// Dead block removed
				after:
				  return;
				""";
	}

}