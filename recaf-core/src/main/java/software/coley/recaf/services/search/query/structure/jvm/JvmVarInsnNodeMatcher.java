package software.coley.recaf.services.search.query.structure.jvm;

import jakarta.annotation.Nonnull;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import software.coley.recaf.services.search.match.Matcher;
import software.coley.recaf.services.search.match.Matchers;
import software.coley.recaf.services.search.query.structure.StructMatchUtils;
import software.coley.recaf.util.AsmInsnUtil;
import software.coley.recaf.util.Types;

import java.util.Objects;

/**
 * Matches a local-variable JVM instruction.
 *
 * @param opcode
 * 		Opcode matcher.
 * @param variable
 * 		Local variable index matcher.
 *
 * @author Matt Coley
 */
public record JvmVarInsnNodeMatcher(@Nonnull Matcher<Integer> opcode,
                                    @Nonnull Matcher<Integer> variable) implements JvmInsnMatcher {
	/**
	 * @param type
	 * 		Type to match sort of.
	 *
	 * @return Matcher for any variable instruction that uses the given type group.
	 */
	@Nonnull
	public static JvmVarInsnNodeMatcher type(@Nonnull Type type) {
		return type(type.getSort());
	}

	/**
	 * @param typeSort
	 * 		Type sort to match.
	 *
	 * @return Matcher for any variable instruction that uses the given type group.
	 */
	@Nonnull
	public static JvmVarInsnNodeMatcher type(int typeSort) {
		return new JvmVarInsnNodeMatcher(op -> op != null
				&& Objects.requireNonNullElse(Types.fromVarOpcode(op), Types.OBJECT_TYPE).getSort() == typeSort,
				Matchers.any());
	}

	/**
	 * @param slot
	 * 		Local variable index to match.
	 *
	 * @return Matcher for a store instruction of the given local variable index.
	 */
	@Nonnull
	public static JvmVarInsnNodeMatcher store(int slot) {
		return new JvmVarInsnNodeMatcher(op -> op != null && AsmInsnUtil.isVarStore(op), Matchers.exact(slot));
	}

	/**
	 * @param slot
	 * 		Local variable index to match.
	 *
	 * @return Matcher for a load instruction of the given local variable index.
	 */
	@Nonnull
	public static JvmVarInsnNodeMatcher load(int slot) {
		return new JvmVarInsnNodeMatcher(op -> op != null && AsmInsnUtil.isVarLoad(op), Matchers.exact(slot));
	}

	@Override
	public boolean matchesJvm(@Nonnull AbstractInsnNode node) {
		return node instanceof VarInsnNode value
				&& StructMatchUtils.opcodeJvm(node, opcode)
				&& variable.matches(value.var);
	}
}
