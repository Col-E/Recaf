package software.coley.recaf.services.search.query.structure.jvm;

import jakarta.annotation.Nonnull;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import software.coley.recaf.services.search.match.Matcher;
import software.coley.recaf.services.search.match.Matchers;
import software.coley.recaf.services.search.query.structure.StructMatchUtils;

/**
 * Matches a field-reference JVM instruction.
 *
 * @param opcode
 * 		Opcode matcher.
 * @param owner
 * 		Field owner matcher.
 * @param name
 * 		Field name matcher.
 * @param descriptor
 * 		Field descriptor matcher.
 *
 * @author Matt Coley
 */
public record JvmFieldInsnNodeMatcher(@Nonnull Matcher<Integer> opcode,
                                      @Nonnull Matcher<String> owner,
                                      @Nonnull Matcher<String> name,
                                      @Nonnull Matcher<String> descriptor) implements JvmInsnMatcher {
	/**
	 * @param opcode
	 * 		Field instruction opcode.
	 * @param owner
	 * 		Field owner.
	 * @param name
	 * 		Field name.
	 * @param desc
	 * 		Field descriptor.
	 *
	 * @return Matcher against the exact field definition.
	 */
	@Nonnull
	public static JvmFieldInsnNodeMatcher exact(int opcode, @Nonnull String owner, @Nonnull String name, @Nonnull String desc) {
		return new JvmFieldInsnNodeMatcher(Matchers.exact(opcode), Matchers.exact(owner), Matchers.exact(name), Matchers.exact(desc));
	}

	/**
	 * @param owner
	 * 		Internal name of the class that owns the field.
	 *
	 * @return Matcher against any field instruction that references the given class as the owner.
	 */
	@Nonnull
	public static JvmFieldInsnNodeMatcher exactOwner(@Nonnull String owner) {
		return new JvmFieldInsnNodeMatcher(Matchers.any(), Matchers.exact(owner), Matchers.any(), Matchers.any());
	}

	/**
	 * @param packageName
	 * 		Name of the package to match.
	 *
	 * @return Matcher against any field instruction that references a class in the given package <i>(or subpackage)</i>.
	 */
	@Nonnull
	public static JvmFieldInsnNodeMatcher containsPackage(@Nonnull String packageName) {
		packageName = packageName.replace('.', '/');
		return new JvmFieldInsnNodeMatcher(Matchers.any(), Matchers.startsWith(packageName), Matchers.any(), Matchers.any());
	}

	@Override
	public boolean matchesJvm(@Nonnull AbstractInsnNode node) {
		return node instanceof FieldInsnNode value
				&& StructMatchUtils.opcodeJvm(node, opcode)
				&& owner.matches(value.owner)
				&& name.matches(value.name)
				&& descriptor.matches(value.desc);
	}
}
