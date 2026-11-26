package software.coley.recaf.services.search.query.structure.android;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import me.darknet.dex.tree.definitions.instructions.Instruction;
import me.darknet.dex.tree.definitions.instructions.Label;
import software.coley.recaf.services.search.match.Matcher;
import software.coley.recaf.services.search.match.Matchers;

/**
 * Matches a DEX label.
 *
 * @param label
 * 		Label matcher.
 *
 * @author Matt Coley
 */
public record DexLabelMatcher(@Nonnull Matcher<Label> label) implements DexInsnMatcher, Matcher<Label> {
	/** Matcher that matches any DEX label. */
	public static final DexLabelMatcher ANY = new DexLabelMatcher(Matchers.any());

	/**
	 * @param label
	 * 		Label to match.
	 *
	 * @return Matcher against the exact label.
	 */
	@Nonnull
	public static DexLabelMatcher exact(@Nonnull Label label) {
		return new DexLabelMatcher(Matchers.exact(label));
	}

	/**
	 * @param index
	 * 		Label index to match.
	 * @param position
	 * 		Label position to match.
	 *
	 * @return Matcher against a label with the given index and position.
	 */
	@Nonnull
	public static DexLabelMatcher exact(int index, int position) {
		return new DexLabelMatcher(value -> value != null
				&& value.index() == index
				&& value.position() == position);
	}

	/**
	 * @param index
	 * 		Label index to match.
	 * @param position
	 * 		Label position to match.
	 * @param lineNumber
	 * 		Source line number to match.
	 *
	 * @return Matcher against a label with the given coordinates.
	 */
	@Nonnull
	public static DexLabelMatcher exact(int index, int position, int lineNumber) {
		return new DexLabelMatcher(value -> value != null
				&& value.index() == index
				&& value.position() == position
				&& value.lineNumber() == lineNumber);
	}

	@Override
	public boolean matches(@Nullable Label value) {
		return label.matches(value);
	}

	@Override
	public boolean matchesAndroid(@Nonnull Instruction instruction) {
		return instruction instanceof Label lbl && label.matches(lbl);
	}
}
