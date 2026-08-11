package software.coley.recaf.services.search.match;

import jakarta.annotation.Nonnull;

/**
 * Matches access masks with required and forbidden bits.
 *
 * @param required
 * 		Bits that must be present.
 * @param forbidden
 * 		Bits that must be absent.
 *
 * @author Matt Coley
 * @see Matcher
 */
public record AccessFlagMatcher(int required, int forbidden) {
	/**
	 * Validates that a bit cannot be both required and forbidden.
	 *
	 * @param required
	 * 		Bits that must be present.
	 * @param forbidden
	 * 		Bits that must be absent.
	 *
	 * @throws IllegalArgumentException
	 * 		When a bit appears in both masks.
	 */
	public AccessFlagMatcher {
		if ((required & forbidden) != 0)
			throw new IllegalArgumentException("Required and forbidden access bits overlap");
	}

	/**
	 * @param access
	 * 		Access mask to test.
	 *
	 * @return {@code true} when all constraints pass.
	 */
	public boolean matches(int access) {
		return (access & required) == required && (access & forbidden) == 0;
	}

	/**
	 * @return Matcher imposing no access constraint.
	 */
	@Nonnull
	public static AccessFlagMatcher any() {
		return new AccessFlagMatcher(0, 0);
	}

	/**
	 * @param flag
	 * 		Required bit.
	 *
	 * @return Matcher requiring the bit.
	 */
	@Nonnull
	public static AccessFlagMatcher with(int flag) {
		return new AccessFlagMatcher(flag, 0);
	}

	/**
	 * @param first
	 * 		First required bit.
	 * @param additional
	 * 		Additional required bits.
	 *
	 * @return Matcher requiring all supplied bits.
	 */
	@Nonnull
	public static AccessFlagMatcher with(int first, int... additional) {
		int required = first;
		for (int flag : additional)
			required |= flag;
		return with(required);
	}

	/**
	 * @param flag
	 * 		Forbidden bit.
	 *
	 * @return Matcher forbidding the bit.
	 */
	@Nonnull
	public static AccessFlagMatcher without(int flag) {
		return new AccessFlagMatcher(0, flag);
	}
}
