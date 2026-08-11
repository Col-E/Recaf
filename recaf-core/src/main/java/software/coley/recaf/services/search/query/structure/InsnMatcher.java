package software.coley.recaf.services.search.query.structure;

import jakarta.annotation.Nonnull;
import me.darknet.dex.tree.definitions.instructions.Instruction;
import org.objectweb.asm.tree.AbstractInsnNode;
import software.coley.recaf.services.search.query.structure.android.AnyDexInsnMatcher;
import software.coley.recaf.services.search.query.structure.jvm.AnyJvmInsnMatcher;

/**
 * Backend-neutral instruction matcher.
 *
 * @author Matt Coley
 */
public interface InsnMatcher {
	/**
	 * Matches any instruction.
	 */
	InsnMatcher ANY = new InsnMatcher() {
		@Override
		public boolean matchesJvm(@Nonnull AbstractInsnNode node) {
			return true;
		}

		@Override
		public boolean matchesAndroid(@Nonnull Instruction instruction) {
			return true;
		}
	};

	/**
	 * @param node
	 * 		JVM instruction node.
	 *
	 * @return {@code true} when this matcher accepts the JVM instruction.
	 */
	boolean matchesJvm(@Nonnull AbstractInsnNode node);

	/**
	 * @param instruction
	 * 		Dex instruction.
	 *
	 * @return {@code true} when this matcher accepts the dex instruction.
	 */
	boolean matchesAndroid(@Nonnull Instruction instruction);
}
