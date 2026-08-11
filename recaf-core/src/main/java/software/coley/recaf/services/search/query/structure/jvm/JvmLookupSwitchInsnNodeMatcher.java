package software.coley.recaf.services.search.query.structure.jvm;

import jakarta.annotation.Nonnull;
import org.objectweb.asm.Label;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import software.coley.recaf.services.search.match.Matcher;
import software.coley.recaf.services.search.match.Matchers;
import software.coley.recaf.services.search.query.structure.ListMatcher;

import java.util.ArrayList;
import java.util.List;

/**
 * Matches a JVM lookup-switch instruction.
 *
 * @param defaultLabel
 * 		Default target label matcher.
 * @param keys
 * 		Switch key matcher.
 * @param labels
 * 		Target label matcher.
 *
 * @author Matt Coley
 */
public record JvmLookupSwitchInsnNodeMatcher(@Nonnull Matcher<Label> defaultLabel,
                                             @Nonnull ListMatcher<Matcher<Integer>> keys,
                                             @Nonnull ListMatcher<Matcher<Label>> labels) implements JvmInsnMatcher {
	/**
	 * @param keys
	 * 		Exact keys to match.
	 *
	 * @return Matcher against the exact switch keys.
	 */
	@Nonnull
	public static JvmLookupSwitchInsnNodeMatcher exactKeys(int... keys) {
		List<Matcher<Integer>> keyMatchers = new ArrayList<>(keys.length);
		for (int key : keys)
			keyMatchers.add(Matchers.exact(key));
		return new JvmLookupSwitchInsnNodeMatcher(Matchers.any(), ListMatcher.exactly(keyMatchers), ListMatcher.any());
	}

	@Override
	public boolean matchesJvm(@Nonnull AbstractInsnNode node) {
		if (!(node instanceof LookupSwitchInsnNode value)
				|| !defaultLabel.matches(value.dflt.getLabel())
				|| !keys.matches(value.keys))
			return false;
		List<Label> targets = new ArrayList<>();
		for (LabelNode label : value.labels)
			targets.add(label.getLabel());
		return labels.matches(targets);
	}
}
