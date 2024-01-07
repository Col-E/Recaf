package software.coley.recaf.services.cell;

import jakarta.annotation.Nonnull;

import java.util.function.Predicate;

/**
 * Basic context source with whitelisting for {@link #allow(String)}.
 *
 * @author Matt Coley
 */
public class BasicBlacklistingContextSource extends BasicWhitelistingContextSource {
	/**
	 * @param isDeclaration
	 *        {@code true} for the source to model a declaration.
	 *        {@code false} for the source to model a reference.
	 * @param whitelist
	 * 		Blacklist function, where {@code false} is allowed.
	 */
	public BasicBlacklistingContextSource(boolean isDeclaration, @Nonnull Predicate<String> whitelist) {
		super(isDeclaration, whitelist.negate());
	}
}
