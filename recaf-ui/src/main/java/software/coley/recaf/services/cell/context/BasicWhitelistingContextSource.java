package software.coley.recaf.services.cell.context;

import jakarta.annotation.Nonnull;

import java.util.function.Predicate;

/**
 * Basic context source with whitelisting for {@link #allow(String)}.
 *
 * @author Matt Coley
 */
public class BasicWhitelistingContextSource extends BasicContextSource {
	private final Predicate<String> allowed;

	/**
	 * @param isDeclaration
	 *        {@code true} for the source to model a declaration.
	 *        {@code false} for the source to model a reference.
	 * @param whitelist
	 * 		Whitelist function, where {@code true} is allowed.
	 */
	public BasicWhitelistingContextSource(boolean isDeclaration, @Nonnull Predicate<String> whitelist) {
		super(isDeclaration);
		this.allowed = whitelist;
	}

	@Override
	public boolean allow(@Nonnull String key) {
		return allowed.test(key);
	}
}
