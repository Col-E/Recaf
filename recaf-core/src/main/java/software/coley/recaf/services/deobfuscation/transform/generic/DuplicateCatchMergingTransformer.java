package software.coley.recaf.services.deobfuscation.transform.generic;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.services.transform.JvmClassTransformer;
import software.coley.recaf.services.transform.JvmTransformerContext;
import software.coley.recaf.services.transform.TransformationException;
import software.coley.recaf.util.BlwUtil;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.NOP;
import static software.coley.recaf.util.AsmInsnUtil.*;

/**
 * A transformer that removes duplicate code in try-catch handler blocks.
 *
 * @author Matt Coley
 */
@Dependent
public class DuplicateCatchMergingTransformer implements JvmClassTransformer {
	/**
	 * Allows us to skip blocks that are too simple to bother merging.
	 * For instance:
	 * <ul>
	 *     <li>{@code { throw e; }}</li>
	 *     <li>{@code { e.printStacktrace(); }}</li>
	 *     <li>{@code { no-op }}</li>
	 * </ul>
	 */
	private static final int MIN_BLOCK_THRESHOLD = 4;

	@Override
	public void transform(@Nonnull JvmTransformerContext context, @Nonnull Workspace workspace,
	                      @Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle,
	                      @Nonnull JvmClassInfo initialClassState) throws TransformationException {
		boolean isTransformed = false;
		ClassNode node = context.getNode(bundle, initialClassState);
		for (MethodNode method : node.methods) {
			InsnList instructions = method.instructions;
			if (instructions == null) continue;
			if (method.tryCatchBlocks == null || method.tryCatchBlocks.size() <= 1) continue;

			// Build model of catch block contents.
			Set<AbstractInsnNode> visitedHandlers = Collections.newSetFromMap(new IdentityHashMap<>());
			Map<TryCatchBlockNode, CodeBlock> catchBlocks = new IdentityHashMap<>();
			for (TryCatchBlockNode tryCatchBlock : method.tryCatchBlocks) {
				List<AbstractInsnNode> catchBlockInsns = new ArrayList<>();
				AbstractInsnNode insn = tryCatchBlock.handler;

				// Skip if we've already visited this block.
				if (!visitedHandlers.add(insn))
					continue;

				// Build block contents.
				while (insn != null) {
					// Only include non-metadata/filler instructions.
					if (insn.getType() != AbstractInsnNode.FRAME && insn.getOpcode() != NOP)
						catchBlockInsns.add(insn);

					// Abort when we encounter an exit or control flow.
					if (isReturn(insn) || isFlowControl(insn))
						break;

					insn = insn.getNext();
				}

				// Skip if the block is too small.
				if (catchBlockInsns.size() < MIN_BLOCK_THRESHOLD)
					continue;

				// Skip if there is control flow pointing to a contained label.
				if (hasExternalFlowIntoCatchBlock(method, catchBlockInsns))
					continue;

				catchBlocks.put(tryCatchBlock, new CodeBlock(catchBlockInsns));
			}

			// Sort the blocks by their position in the method code, and then redirect the control flow of earlier
			// catch blocks to the last catch block handler with equivalent code.
			List<CodeBlock> blocks = catchBlocks.values().stream()
					.sorted()
					.toList();
			for (int i = 0; i < blocks.size() - 1; i++) {
				CodeBlock block = blocks.get(i);
				for (int j = blocks.size() - 1; j > i; j--) {
					CodeBlock laterBlock = blocks.get(j);
					if (block.equals(laterBlock)) {
						block.pruneContent(instructions);
						block.redirectTo(instructions, (LabelNode) laterBlock.instructions.getFirst());
						isTransformed = true;
						break;
					}
				}
			}
		}

		if (isTransformed) {
			context.setRecomputeFrames(initialClassState.getName());
			context.setNode(bundle, initialClassState, node);
		}
	}

	private static boolean hasExternalFlowIntoCatchBlock(@Nonnull MethodNode method,
	                                                     @Nonnull List<AbstractInsnNode> block) {
		// We pass 'includeFirstInsn = false' because no catch block should be used to point to a label
		// that is in the middle of the block. However, if the handler is the start of the block, that is fine
		// as that is expected as a potential handler.
		if (hasHandlerFlowIntoBlock(method, block, false))
			return true;

		// No control flow instruction should point to this block *at all*.
		// If we observe this to be the case, it would be very hard to manipulate the contents of this block
		// without breaking the flow of the method.
		return hasInboundFlowReferences(method, block);
	}

	@Nonnull
	@Override
	public String name() {
		return "Duplicate catch merging";
	}

	/**
	 * Container for multiple instructions. Uses the disassembled presentation as a key
	 * because ASM does not implement equals/hashCodes for their instruction models.
	 */
	private final static class CodeBlock implements Comparable<CodeBlock> {
		private final List<AbstractInsnNode> instructions;
		private final String disassembled;
		private final int index;

		private CodeBlock(@Nonnull List<AbstractInsnNode> instructions) {
			this.instructions = instructions;
			this.disassembled = instructions.stream()
					.skip(1) // Skip first label, which will always be unique
					.map(BlwUtil::toString)
					.collect(Collectors.joining("\n"));
			this.index = indexOf(instructions.getFirst());
		}

		/**
		 * Prunes the instructions of this block that are not the initial label.
		 *
		 * @param container
		 * 		Method instruction container to remove the instructions from.
		 */
		public void pruneContent(@Nonnull InsnList container) {
			while (instructions.size() > 1) {
				AbstractInsnNode next = instructions.remove(1);
				container.remove(next);
			}
		}

		/**
		 * Redirects the control flow of this block to the given label.
		 *
		 * @param container
		 * 		Method instruction container to modify control flow of.
		 * @param target
		 * 		Target label to jump to.
		 */
		public void redirectTo(@Nonnull InsnList container, @Nonnull LabelNode target) {
			AbstractInsnNode first = instructions.getFirst();
			container.insert(first, new JumpInsnNode(GOTO, target));
		}

		/**
		 * @return Starting index in the method code of this block.
		 */
		public int getIndex() {
			return index;
		}

		/**
		 * @return Instructions contained by this block.
		 */
		@Nonnull
		public List<AbstractInsnNode> getInstructions() {
			return instructions;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof CodeBlock codeBlock)) return false;
			return disassembled.equals(codeBlock.disassembled);
		}

		@Override
		public int hashCode() {
			return disassembled.hashCode();
		}

		@Override
		public String toString() {
			return disassembled;
		}

		@Override
		public int compareTo(CodeBlock o) {
			// We already know only one code-block can exist per-each handler label
			// so our indices should always be unique.
			return Integer.compare(index, o.index);
		}
	}
}
