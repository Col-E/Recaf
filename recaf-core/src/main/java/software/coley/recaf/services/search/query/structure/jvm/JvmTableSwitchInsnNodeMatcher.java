package software.coley.recaf.services.search.query.structure.jvm;

import jakarta.annotation.Nonnull;
import org.objectweb.asm.Label;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import software.coley.recaf.services.search.match.Matcher;
import software.coley.recaf.services.search.query.structure.ListMatcher;

import java.util.ArrayList;
import java.util.List;

/**
 * Matches a JVM table-switch instruction.
 *
 * @param minimum
 * 		Minimum key matcher.
 * @param maximum
 * 		Maximum key matcher.
 * @param defaultLabel
 * 		Default target label matcher.
 * @param labels
 * 		Target label matcher.
 *
 * @author Matt Coley
 */
public record JvmTableSwitchInsnNodeMatcher(@Nonnull Matcher<Integer> minimum,
                                            @Nonnull Matcher<Integer> maximum,
                                            @Nonnull Matcher<Label> defaultLabel,
                                            @Nonnull ListMatcher<Matcher<Label>> labels) implements JvmInsnMatcher {
	@Override
	public boolean matchesJvm(@Nonnull AbstractInsnNode node) {
		if (!(node instanceof TableSwitchInsnNode value)
				|| !minimum.matches(value.min)
				|| !maximum.matches(value.max)
				|| !defaultLabel.matches(value.dflt.getLabel()))
			return false;
		List<Label> targets = new ArrayList<>();
		for (LabelNode label : value.labels)
			targets.add(label.getLabel());
		return labels.matches(targets);
	}
}
