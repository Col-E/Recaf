package software.coley.recaf.services.search.query.structure.jvm;

import jakarta.annotation.Nonnull;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import software.coley.recaf.services.inheritance.InheritanceGraph;
import software.coley.recaf.services.search.match.Matcher;
import software.coley.recaf.services.search.match.Matchers;
import software.coley.recaf.services.search.query.structure.StructMatchUtils;

/**
 * Matches a method-reference JVM instruction.
 *
 * @param opcode
 * 		Opcode matcher.
 * @param owner
 * 		Method owner matcher.
 * @param name
 * 		Method name matcher.
 * @param descriptor
 * 		Method descriptor matcher.
 * @param isInterface
 * 		Interface-call flag matcher.
 *
 * @author Matt Coley
 */
public record JvmMethodInsnNodeMatcher(@Nonnull Matcher<Integer> opcode,
                                       @Nonnull Matcher<String> owner,
                                       @Nonnull Matcher<String> name,
                                       @Nonnull Matcher<String> descriptor,
                                       @Nonnull Matcher<Boolean> isInterface) implements JvmInsnMatcher {
	/**
	 * @param opcode
	 * 		Method instruction opcode.
	 * @param owner
	 * 		Method owner.
	 * @param name
	 * 		Method name.
	 * @param desc
	 * 		Method descriptor.
	 *
	 * @return Matcher against the exact method definition.
	 */
	@Nonnull
	public static JvmMethodInsnNodeMatcher exact(int opcode, @Nonnull String owner, @Nonnull String name, @Nonnull String desc) {
		return new JvmMethodInsnNodeMatcher(Matchers.exact(opcode), Matchers.exact(owner), Matchers.exact(name), Matchers.exact(desc), Matchers.any());
	}

	/**
	 * @param owner
	 * 		Internal name of the class that owns the method.
	 *
	 * @return Matcher against any method instruction that references the given class as the owner.
	 */
	@Nonnull
	public static JvmMethodInsnNodeMatcher exactOwner(@Nonnull String owner) {
		return new JvmMethodInsnNodeMatcher(Matchers.any(), Matchers.exact(owner), Matchers.any(), Matchers.any(), Matchers.any());
	}

	/**
	 * @param packageName
	 * 		Name of the package to match.
	 *
	 * @return Matcher against any method instruction that references a class in the given package <i>(or subpackage)</i>.
	 */
	@Nonnull
	public static JvmMethodInsnNodeMatcher containsPackage(@Nonnull String packageName) {
		packageName = packageName.replace('.', '/');
		return new JvmMethodInsnNodeMatcher(Matchers.any(), Matchers.startsWith(packageName), Matchers.any(), Matchers.any(), Matchers.any());
	}

	/**
	 * @param owner
	 * 		Internal name of a class to check for assignability against the method owner.
	 * 		For instance, {@code java/util/List} will match direct calls against child types like {@code java/util/ArrayList}.
	 * @param graph
	 * 		Inheritance graph to use for checking assignability.
	 *
	 * @return Matcher against any method instruction that references a type that inherits from the given class.
	 */
	@Nonnull
	public static JvmMethodInsnNodeMatcher inheritedOwner(@Nonnull String owner, @Nonnull InheritanceGraph graph) {
		return new JvmMethodInsnNodeMatcher(Matchers.any(),
				name -> name != null && (owner.equals(name) || graph.isAssignableFrom(owner, name)),
				Matchers.any(), Matchers.any(), Matchers.any());
	}

	@Override
	public boolean matchesJvm(@Nonnull AbstractInsnNode node) {
		return node instanceof MethodInsnNode value
				&& StructMatchUtils.opcodeJvm(node, opcode)
				&& owner.matches(value.owner)
				&& name.matches(value.name)
				&& descriptor.matches(value.desc)
				&& isInterface.matches(value.itf);
	}
}
