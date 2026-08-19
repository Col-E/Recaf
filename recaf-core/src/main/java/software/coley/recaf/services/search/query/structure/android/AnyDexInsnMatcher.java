package software.coley.recaf.services.search.query.structure.android;

import jakarta.annotation.Nonnull;
import me.darknet.dex.tree.definitions.instructions.Instruction;

/**
 * Matches any dex instruction.
 *
 * @author Matt Coley
 */
public record AnyDexInsnMatcher() implements DexInsnMatcher {
	/** Singleton instance. */
	public static final AnyDexInsnMatcher INSTANCE = new AnyDexInsnMatcher();

	@Override
	public boolean matchesAndroid(@Nonnull Instruction instruction) {
		return true;
	}
}
