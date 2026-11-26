package software.coley.recaf.services.search.query.structure.android;

import jakarta.annotation.Nonnull;
import me.darknet.dex.tree.definitions.instructions.Instruction;
import me.darknet.dex.tree.definitions.instructions.Label;
import me.darknet.dex.tree.definitions.instructions.PackedSwitchInstruction;
import software.coley.recaf.services.search.match.Matcher;
import software.coley.recaf.services.search.match.Matchers;
import software.coley.recaf.services.search.query.structure.ListMatcher;

import java.util.ArrayList;
import java.util.List;

/**
 * Matches a DEX packed-switch instruction.
 *
 * @param register
 * 		Switch register matcher.
 * @param firstKey
 * 		First packed key matcher.
 * @param targets
 * 		Target label matcher.
 *
 * @author Matt Coley
 */
public record DexPackedSwitchInstructionMatcher(@Nonnull Matcher<Integer> register,
                                                @Nonnull Matcher<Integer> firstKey,
                                                @Nonnull ListMatcher<Matcher<Label>> targets) implements DexInsnMatcher {
	/**
	 * @return Matcher that matches any DEX packed-switch instruction.
	 */
	@Nonnull
	public static DexPackedSwitchInstructionMatcher any() {
		return new DexPackedSwitchInstructionMatcher(Matchers.any(), Matchers.any(), ListMatcher.any());
	}

	/**
	 * @param register
	 * 		Switch register to match.
	 * @param firstKey
	 * 		First packed key to match.
	 * @param targets
	 * 		Target labels to match in sequence.
	 *
	 * @return Matcher against the exact packed-switch operands.
	 */
	@Nonnull
	public static DexPackedSwitchInstructionMatcher exact(int register, int firstKey, @Nonnull List<Label> targets) {
		return new DexPackedSwitchInstructionMatcher(
				Matchers.exact(register),
				Matchers.exact(firstKey),
				exactTargets(targets)
		);
	}

	/**
	 * @param register
	 * 		Switch register to match.
	 * @param firstKey
	 * 		First packed key to match.
	 * @param targets
	 * 		Target labels to match in sequence.
	 *
	 * @return Matcher against the exact packed-switch operands.
	 */
	@SafeVarargs
	@Nonnull
	public static DexPackedSwitchInstructionMatcher exact(int register, int firstKey, @Nonnull Label... targets) {
		List<Label> labels = List.of(targets);
		return exact(register, firstKey, labels);
	}

	/**
	 * @param targets
	 * 		Target labels to match in sequence.
	 *
	 * @return Exact sequence matcher for the supplied labels.
	 */
	@Nonnull
	private static ListMatcher<Matcher<Label>> exactTargets(@Nonnull List<Label> targets) {
		List<Matcher<Label>> matchers = new ArrayList<>(targets.size());
		for (Label target : targets)
			matchers.add(Matchers.exact(target));
		return ListMatcher.sequence(matchers);
	}

	@Override
	public boolean matchesAndroid(@Nonnull Instruction instruction) {
		return instruction instanceof PackedSwitchInstruction(int iregister, int ifirstKey, List<Label> itargets)
				&& register.matches(iregister)
				&& firstKey.matches(ifirstKey)
				&& targets.matches(itargets);
	}
}
