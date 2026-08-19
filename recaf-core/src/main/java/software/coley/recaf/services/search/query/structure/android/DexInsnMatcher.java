package software.coley.recaf.services.search.query.structure.android;

import jakarta.annotation.Nonnull;
import me.darknet.dex.tree.definitions.instructions.Instruction;
import org.objectweb.asm.tree.AbstractInsnNode;
import software.coley.recaf.services.search.query.structure.InsnMatcher;

/**
 * Android instruction matcher.
 *
 * @author Matt Coley
 */
public interface DexInsnMatcher extends InsnMatcher {
	// TODO: Once Android support is more fleshed out, implement the remainder of all instruction types,
	//  and make this interface sealed.

	@Override
	default boolean matchesJvm(@Nonnull AbstractInsnNode node) {
		return false;
	}

	@Override
	boolean matchesAndroid(@Nonnull Instruction instruction);
}
