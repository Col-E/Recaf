package software.coley.recaf.services.search.query.structure.jvm;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.Label;
import org.objectweb.asm.tree.LocalVariableNode;
import software.coley.recaf.services.search.match.Matcher;
import software.coley.recaf.services.search.match.Matchers;

/**
 * Matches a JVM local-variable debug entry.
 *
 * @param index
 * 		Local slot matcher.
 * @param name
 * 		Local name matcher.
 * @param descriptor
 * 		Local type descriptor matcher.
 * @param signature
 * 		Generic signature matcher.
 * @param start
 * 		Live-range start label matcher.
 * @param end
 * 		Live-range end label matcher.
 *
 * @author Matt Coley
 */
public record JvmLocalVariableMatcher(@Nonnull Matcher<Integer> index,
                                      @Nonnull Matcher<String> name,
                                      @Nonnull Matcher<String> descriptor,
                                      @Nonnull Matcher<String> signature,
                                      @Nonnull Matcher<Label> start,
                                      @Nonnull Matcher<Label> end) implements Matcher<LocalVariableNode> {
	/**
	 * @param index
	 * 		Local slot index.
	 * @param name
	 * 		Local name.
	 * @param descriptor
	 * 		Local type descriptor.
	 *
	 * @return Matcher for the exact local slot, name, and descriptor, ignoring generic signature and live-range labels.
	 */
	@Nonnull
	public static JvmLocalVariableMatcher exact(int index, @Nonnull String name, @Nonnull String descriptor) {
		return new JvmLocalVariableMatcher(Matchers.exact(index), Matchers.exact(name), Matchers.exact(descriptor),
				Matchers.any(), Matchers.any(), Matchers.any());
	}

	/**
	 * @param index
	 * 		Local slot index.
	 * @param name
	 * 		Local name.
	 * @param descriptor
	 * 		Local type descriptor.
	 * @param signature
	 * 		Generic signature, or {@code null} when absent.
	 *
	 * @return Matcher for the exact local declaration, ignoring live-range labels.
	 */
	@Nonnull
	public static JvmLocalVariableMatcher exact(int index, @Nonnull String name,
	                                           @Nonnull String descriptor, @Nullable String signature) {
		return new JvmLocalVariableMatcher(Matchers.exact(index), Matchers.exact(name), Matchers.exact(descriptor),
				Matchers.exact(signature), Matchers.any(), Matchers.any());
	}

	@Override
	public boolean matches(@Nullable LocalVariableNode value) {
		return value != null && value.start != null && value.end != null
				&& index.matches(value.index)
				&& name.matches(value.name)
				&& descriptor.matches(value.desc)
				&& signature.matches(value.signature)
				&& start.matches(value.start.getLabel())
				&& end.matches(value.end.getLabel());
	}
}
