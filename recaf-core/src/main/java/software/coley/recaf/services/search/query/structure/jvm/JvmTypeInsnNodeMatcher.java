package software.coley.recaf.services.search.query.structure.jvm;

import jakarta.annotation.Nonnull;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import software.coley.recaf.services.inheritance.InheritanceGraph;
import software.coley.recaf.services.search.match.Matcher;
import software.coley.recaf.services.search.match.Matchers;
import software.coley.recaf.services.search.query.structure.StructMatchUtils;

/**
 * Matches a type-operand JVM instruction.
 *
 * @param opcode
 * 		Opcode matcher.
 * @param type
 * 		Internal type name matcher.
 *
 * @author Matt Coley
 */
public record JvmTypeInsnNodeMatcher(@Nonnull Matcher<Integer> opcode,
                                     @Nonnull Matcher<String> type) implements JvmInsnMatcher {
	/**
	 * @param opcode
	 * 		Type instruction opcode to match.
	 * @param type
	 * 		Internal name of the type to match.
	 *
	 * @return Matcher against the given opcode and type.
	 */
	@Nonnull
	public static JvmTypeInsnNodeMatcher exact(int opcode, @Nonnull String type) {
		return new JvmTypeInsnNodeMatcher(Matchers.exact(opcode), Matchers.exact(type));
	}

	/**
	 * @param type
	 * 		Internal name of a class to check for assignability against the type.
	 * 		For instance, {@code java/util/List} will match {@code NEW} and {@code CHECKCAST}
	 * 		instructions using child types like {@code java/util/ArrayList}.
	 * @param graph
	 * 		Inheritance graph to use for checking assignability.
	 *
	 * @return Matcher against any type instruction that references a type that inherits from the given class.
	 */
	@Nonnull
	public static JvmTypeInsnNodeMatcher inherited(@Nonnull String type, @Nonnull InheritanceGraph graph) {
		return new JvmTypeInsnNodeMatcher(Matchers.any(),
				name -> name != null && (type.equals(name) || graph.isAssignableFrom(type, name)));
	}

	@Override
	public boolean matchesJvm(@Nonnull AbstractInsnNode node) {
		return node instanceof TypeInsnNode value
				&& StructMatchUtils.opcodeJvm(node, opcode)
				&& type.matches(value.desc);
	}
}
