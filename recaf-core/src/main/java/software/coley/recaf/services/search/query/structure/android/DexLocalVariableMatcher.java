package software.coley.recaf.services.search.query.structure.android;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import me.darknet.dex.tree.definitions.debug.DebugInformation;
import me.darknet.dex.tree.definitions.instructions.Label;
import software.coley.recaf.services.search.match.Matcher;
import software.coley.recaf.services.search.match.Matchers;

/**
 * Matches a DEX local-variable debug entry.
 *
 * @param register
 * 		Register matcher.
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
public record DexLocalVariableMatcher(@Nonnull Matcher<Integer> register,
                                      @Nonnull Matcher<String> name,
                                      @Nonnull Matcher<String> descriptor,
                                      @Nonnull Matcher<String> signature,
                                      @Nonnull Matcher<Label> start,
                                      @Nonnull Matcher<Label> end) implements Matcher<DebugInformation.LocalVariable> {
	/**
	 * @param register
	 * 		Register index.
	 * @param name
	 * 		Local name.
	 * @param descriptor
	 * 		Local type descriptor.
	 *
	 * @return Matcher for the exact register, name, and type, ignoring signature and live-range labels.
	 */
	@Nonnull
	public static DexLocalVariableMatcher exact(int register, @Nonnull String name, @Nonnull String descriptor) {
		return new DexLocalVariableMatcher(Matchers.exact(register), Matchers.exact(name), Matchers.exact(descriptor),
				Matchers.any(), Matchers.any(), Matchers.any());
	}

	@Override
	public boolean matches(@Nullable DebugInformation.LocalVariable variable) {
		return variable != null
				&& register.matches(variable.register())
				&& name.matches(variable.name())
				&& variable.type() != null
				&& descriptor.matches(variable.type().descriptor())
				&& signature.matches(variable.signature())
				&& start.matches(variable.start())
				&& end.matches(variable.end());
	}
}
