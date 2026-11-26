package software.coley.recaf.services.search.query.structure.android;

import jakarta.annotation.Nonnull;
import me.darknet.dex.tree.definitions.instructions.Instruction;
import me.darknet.dex.tree.definitions.instructions.Label;
import me.darknet.dex.tree.definitions.instructions.SparseSwitchInstruction;
import software.coley.recaf.services.search.match.Matcher;
import software.coley.recaf.services.search.match.Matchers;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Matches a DEX sparse-switch instruction.
 *
 * @param register
 * 		Switch register matcher.
 * @param targets
 * 		Key-to-target mapping matcher. DEX sparse-switch entries are a map, so matching preserves key/target pairs
 * 		and does not depend on map iteration order.
 *
 * @author Matt Coley
 */
public record DexSparseSwitchInstructionMatcher(@Nonnull Matcher<Integer> register,
                                                @Nonnull Matcher<Map<Integer, Label>> targets) implements DexInsnMatcher {
	/**
	 * @return Matcher that matches any DEX sparse-switch instruction.
	 */
	@Nonnull
	public static DexSparseSwitchInstructionMatcher any() {
		return new DexSparseSwitchInstructionMatcher(Matchers.any(), Matchers.any());
	}

	/**
	 * @param register
	 * 		Switch register to match.
	 * @param entries
	 * 		Exact switch key-to-target mappings.
	 *
	 * @return Matcher against the exact sparse-switch operands.
	 */
	@Nonnull
	public static DexSparseSwitchInstructionMatcher exact(int register, @Nonnull Map<Integer, Label> entries) {
		return new DexSparseSwitchInstructionMatcher(Matchers.exact(register), Matchers.exact(Map.copyOf(entries)));
	}

	/**
	 * @param register
	 * 		Switch register to match.
	 * @param keys
	 * 		Exact switch keys. Target labels are unconstrained.
	 *
	 * @return Matcher against the exact sparse-switch key set.
	 */
	@Nonnull
	public static DexSparseSwitchInstructionMatcher exactKeys(int register, int... keys) {
		Set<Integer> expectedKeys = new HashSet<>(keys.length);
		for (int key : keys)
			expectedKeys.add(key);
		Set<Integer> immutableKeys = Set.copyOf(expectedKeys);
		return new DexSparseSwitchInstructionMatcher(Matchers.exact(register),
				actual -> actual != null && actual.keySet().equals(immutableKeys));
	}

	@Override
	public boolean matchesAndroid(@Nonnull Instruction instruction) {
		return instruction instanceof SparseSwitchInstruction(int iregister, Map<Integer, Label> itargets)
				&& register.matches(iregister)
				&& targets.matches(itargets);
	}
}
