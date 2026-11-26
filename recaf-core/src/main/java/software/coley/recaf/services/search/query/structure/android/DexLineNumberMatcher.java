package software.coley.recaf.services.search.query.structure.android;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import me.darknet.dex.tree.definitions.debug.DebugInformation;
import me.darknet.dex.tree.definitions.instructions.Label;
import software.coley.recaf.services.search.match.Matcher;
import software.coley.recaf.services.search.match.Matchers;

/**
 * Matches a DEX line-number entry.
 *
 * @param label
 * 		Instruction label associated with the line.
 * @param line
 * 		Source line number.
 *
 * @author Matt Coley
 */
public record DexLineNumberMatcher(@Nonnull Matcher<Label> label,
                                   @Nonnull Matcher<Integer> line) implements Matcher<DebugInformation.LineNumber> {
	/**
	 * @param line
	 * 		Source line number.
	 *
	 * @return Matcher for any DEX line-number entry with the given line.
	 */
	@Nonnull
	public static DexLineNumberMatcher line(int line) {
		return new DexLineNumberMatcher(Matchers.any(), Matchers.exact(line));
	}

	@Override
	public boolean matches(@Nullable DebugInformation.LineNumber value) {
		return value != null && value.label() != null && label.matches(value.label()) && line.matches(value.line());
	}
}
