package software.coley.recaf.services.search.query.structure.jvm;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.Label;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import software.coley.recaf.services.search.match.Matcher;
import software.coley.recaf.services.search.match.Matchers;

/**
 * Matches a JVM source line node.
 *
 * @param line
 * 		Source line matcher.
 * @param start
 * 		Start label matcher.
 *
 * @author Matt Coley
 */
public record JvmLineNumberNodeMatcher(@Nonnull Matcher<Integer> line,
                                       @Nonnull Matcher<Label> start) implements JvmInsnMatcher, Matcher<LineNumberNode> {
	/**
	 * @param line
	 * 		Line number to match.
	 *
	 * @return Matcher against the exact line number.
	 */
	@Nonnull
	public static JvmLineNumberNodeMatcher line(int line) {
		return new JvmLineNumberNodeMatcher(Matchers.exact(line), Matchers.any());
	}

	@Override
	public boolean matches(@Nullable LineNumberNode value) {
		return value != null && value.start != null
				&& line.matches(value.line)
				&& start.matches(value.start.getLabel());
	}

	@Override
	public boolean matchesJvm(@Nonnull AbstractInsnNode node) {
		return node instanceof LineNumberNode value && matches(value);
	}
}
