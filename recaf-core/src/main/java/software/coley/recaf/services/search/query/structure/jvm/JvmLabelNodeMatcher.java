package software.coley.recaf.services.search.query.structure.jvm;

import jakarta.annotation.Nonnull;
import org.objectweb.asm.Label;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LabelNode;
import software.coley.recaf.services.search.match.Matcher;
import software.coley.recaf.services.search.match.Matchers;

/**
 * Matches a JVM label node by ASM label identity.
 *
 * @param label
 * 		Label matcher.
 *
 * @author Matt Coley
 */
public record JvmLabelNodeMatcher(@Nonnull Matcher<Label> label) implements JvmInsnMatcher {
	/** Matcher that matches any label node. */
	public static final JvmLabelNodeMatcher ANY = new JvmLabelNodeMatcher(Matchers.any());

	@Override
	public boolean matchesJvm(@Nonnull AbstractInsnNode node) {
		return node instanceof LabelNode value
				&& label.matches(value.getLabel());
	}
}
