package software.coley.recaf.services.search.query.structure.jvm;

import jakarta.annotation.Nonnull;
import me.darknet.dex.tree.definitions.instructions.Instruction;
import org.objectweb.asm.tree.AbstractInsnNode;
import software.coley.recaf.services.search.query.structure.InsnMatcher;

/**
 * JVM instruction matcher.
 *
 * @author Matt Coley
 */
public sealed interface JvmInsnMatcher extends InsnMatcher permits AnyJvmInsnMatcher, JvmOpcodeInsnMatcher,
		JvmInsnNodeMatcher, JvmIntInsnNodeMatcher, JvmVarInsnNodeMatcher, JvmTypeInsnNodeMatcher, JvmFieldInsnNodeMatcher,
		JvmMethodInsnNodeMatcher, JvmInvokeDynamicInsnNodeMatcher, JvmJumpInsnNodeMatcher, JvmLabelNodeMatcher,
		JvmLdcInsnNodeMatcher, JvmIincInsnNodeMatcher, JvmTableSwitchInsnNodeMatcher, JvmLookupSwitchInsnNodeMatcher,
		JvmMultiANewArrayInsnNodeMatcher, JvmLineNumberNodeMatcher {
	@Override
	boolean matchesJvm(@Nonnull AbstractInsnNode node);

	@Override
	default boolean matchesAndroid(@Nonnull Instruction instruction) {
		return false;
	}
}
