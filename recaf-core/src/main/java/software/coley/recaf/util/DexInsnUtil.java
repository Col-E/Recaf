package software.coley.recaf.util;

import jakarta.annotation.Nonnull;
import me.darknet.dex.tree.definitions.code.Code;
import me.darknet.dex.tree.definitions.code.Handler;
import me.darknet.dex.tree.definitions.code.TryCatch;
import me.darknet.dex.tree.definitions.instructions.BranchInstruction;
import me.darknet.dex.tree.definitions.instructions.GotoInstruction;
import me.darknet.dex.tree.definitions.instructions.Instruction;
import me.darknet.dex.tree.definitions.instructions.Label;
import me.darknet.dex.tree.definitions.instructions.NopInstruction;
import me.darknet.dex.tree.definitions.instructions.PackedSwitchInstruction;
import me.darknet.dex.tree.definitions.instructions.ReturnInstruction;
import me.darknet.dex.tree.definitions.instructions.SparseSwitchInstruction;
import me.darknet.dex.tree.definitions.instructions.ThrowInstruction;
import org.objectweb.asm.tree.MethodNode;
import software.coley.recaf.util.collect.primitive.Int2ObjectMap;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Dex instruction utilities.
 *
 * @author Matt Coley
 */
public class DexInsnUtil {
	/**
	 * @param instruction
	 * 		Some instruction.
	 *
	 * @return {@code true} if the instruction is an actual executable instruction, {@code false} if it's a label or nop.
	 */
	public static boolean isDexExecutable(@Nonnull Instruction instruction) {
		return !(instruction instanceof Label) && !(instruction instanceof NopInstruction);
	}

	/**
	 * Populate control flow maps for a method.
	 *
	 * @param code
	 * 		Method code to analyze.
	 * @param successorMap
	 * 		Output successor map.
	 * @param predecessorMap
	 * 		Output predecessor map.
	 *
	 * @see AsmInsnUtil#populateFlowMaps(MethodNode, Int2ObjectMap, Int2ObjectMap)
	 */
	@SuppressWarnings("StatementWithEmptyBody")
	public static void populateFlowMaps(@Nonnull Code code,
	                                    @Nonnull Int2ObjectMap<List<Integer>> successorMap,
	                                    @Nonnull Int2ObjectMap<List<Integer>> predecessorMap) {
		List<Instruction> instructions = code.getInstructions();
		Map<Label, Integer> labelIndices = buildDexLabelIndex(instructions);
		for (int i = 0; i < instructions.size(); i++) {
			Instruction instruction = instructions.get(i);
			Set<Integer> successors = new LinkedHashSet<>();
			if (instruction instanceof ReturnInstruction || instruction instanceof ThrowInstruction) {
				// Terminal instructions.
			} else if (instruction instanceof GotoInstruction gotoInstruction) {
				// Directed jump, no fallthrough.
				addDexTarget(successors, labelIndices, gotoInstruction.jump());
			} else if (instruction instanceof BranchInstruction branchInstruction) {
				// Conditional jump, has both a directed jump and fallthrough.
				addDexTarget(successors, labelIndices, branchInstruction.label());
				addDexFallthrough(successors, instructions.size(), i);
			} else if (instruction instanceof PackedSwitchInstruction packedSwitchInstruction) {
				// Switch instructions have all their targets as successors.
				for (Label label : packedSwitchInstruction.targets())
					addDexTarget(successors, labelIndices, label);
				addDexFallthrough(successors, instructions.size(), i);
			} else if (instruction instanceof SparseSwitchInstruction sparseSwitchInstruction) {
				// Same as above.
				for (Label label : sparseSwitchInstruction.targets().values())
					addDexTarget(successors, labelIndices, label);
				addDexFallthrough(successors, instructions.size(), i);
			} else {
				// All other instructions just flow to the next instruction.
				addDexFallthrough(successors, instructions.size(), i);
			}

			// TODO: This is a loose translation of the variant in AsmInsnUtil, which itself isn't perfect/complete.
			if (!(instruction instanceof Label)) {
				for (TryCatch tryCatch : code.tryCatch()) {
					Integer start = labelIndices.get(tryCatch.begin());
					Integer end = labelIndices.get(tryCatch.end());
					if (start == null || end == null || !(start <= i && i < end))
						continue;
					for (Handler handler : tryCatch.handlers())
						addDexTarget(successors, labelIndices, handler.handler());
				}
			}

			successorMap.put(i, List.copyOf(successors));
		}

		// Populate predecessor map from successor map.
		for (int i = 0; i < instructions.size(); i++)
			for (int successor : successorMap.getOrDefault(i, Collections.emptyList()))
				predecessorMap.computeIfAbsent(successor, ignored -> new ArrayList<>()).add(i);
	}

	/**
	 * Compute reachable instructions in a method.
	 *
	 * @param size
	 * 		Size of {@link Code} for a method's instructions.
	 * @param successors
	 * 		Control flow successor map for the method. See {@link #populateFlowMaps(Code, Int2ObjectMap, Int2ObjectMap)}
	 *
	 * @return BitSet of reachable instructions in the method.
	 */
	@Nonnull
	public static BitSet computeReachable(int size, @Nonnull Int2ObjectMap<List<Integer>> successors) {
		return AsmInsnUtil.computeReachable(size, successors);
	}

	/**
	 * @param instructions
	 * 		Dex instructions to build an index for.
	 *
	 * @return Map of labels to their instruction index in the method.
	 */
	@Nonnull
	public static Map<Label, Integer> buildDexLabelIndex(@Nonnull List<Instruction> instructions) {
		Map<Label, Integer> labelIndices = new IdentityHashMap<>();
		for (int i = 0; i < instructions.size(); i++) {
			if (instructions.get(i) instanceof Label label)
				labelIndices.put(label, i);
		}
		return labelIndices;
	}

	private static void addDexTarget(@Nonnull Set<Integer> successors,
	                                 @Nonnull Map<Label, Integer> labelIndices,
	                                 @Nonnull Label label) {
		Integer index = labelIndices.get(label);
		if (index != null)
			successors.add(index);
	}

	private static void addDexFallthrough(@Nonnull Set<Integer> successors, int size, int index) {
		if (index + 1 < size)
			successors.add(index + 1);
	}
}
