package software.coley.recaf.ui.control.richtext.suggest.typeindex;

import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

/**
 * Precomputes simple-name humps and matches typed camel-hump fragments against them.
 *
 * @author Matt Coley
 */
public final class TypeNameMatcher {
	private TypeNameMatcher() {}

	/**
	 * @param simpleName
	 * 		Source-facing simple type name.
	 *
	 * @return Indexed name with its word starts and initials precomputed.
	 */
	@Nonnull
	public static IndexedName index(@Nonnull String simpleName) {
		if (simpleName.isEmpty())
			return new IndexedName(simpleName, "", List.of());

		// An example: ConcurrentHashMap -> C,H,M
		// - User can search for "CoHaM" and get the full name.
		List<String> humps = new ArrayList<>();
		int start = 0;
		for (int i = 1; i < simpleName.length(); i++) {
			char previous = simpleName.charAt(i - 1);
			char current = simpleName.charAt(i);
			char next = i + 1 < simpleName.length() ? simpleName.charAt(i + 1) : 0;
			boolean boundary = Character.isUpperCase(current)
					&& (Character.isLowerCase(previous) || Character.isDigit(previous)
					|| Character.isUpperCase(previous) && Character.isLowerCase(next));
			if (boundary) {
				humps.add(simpleName.substring(start, i));
				start = i;
			}
		}
		humps.add(simpleName.substring(start));

		// And for just the initials, CHM also would work.
		StringBuilder initials = new StringBuilder(humps.size());
		for (String hump : humps)
			initials.append(hump.charAt(0));
		return new IndexedName(simpleName, initials.toString(), humps);
	}

	/**
	 * Splits a typed camel-hump fragment at uppercase characters.
	 * For example, {@code StrB} becomes {@code [Str, B]} and {@code SB} becomes {@code [S, B]}.
	 *
	 * @param prefix
	 * 		Typed simple-name fragment.
	 *
	 * @return Typed hump fragments.
	 */
	@Nonnull
	public static List<String> splitQuery(@Nonnull String prefix) {
		if (prefix.isEmpty())
			return List.of();

		List<String> parts = new ArrayList<>();
		int start = 0;
		for (int i = 1; i < prefix.length(); i++) {
			if (Character.isUpperCase(prefix.charAt(i))) {
				parts.add(prefix.substring(start, i));
				start = i;
			}
		}
		parts.add(prefix.substring(start));
		return List.copyOf(parts);
	}

	/**
	 * @param queryParts
	 * 		Typed hump fragments.
	 *
	 * @return Initials of the typed fragments, suitable for an indexed lookup.
	 */
	@Nonnull
	public static String initials(@Nonnull List<String> queryParts) {
		StringBuilder initials = new StringBuilder(queryParts.size());
		for (String part : queryParts)
			if (!part.isEmpty())
				initials.append(part.charAt(0));
		return initials.toString();
	}

	/**
	 * Indexed match metadata for a single type name.
	 *
	 * @param simpleName
	 * 		Source-facing simple type name.
	 * @param initials
	 * 		Initials of the camel humps.
	 * @param humps
	 * 		Name segments beginning at each camel hump.
	 */
	public record IndexedName(@Nonnull String simpleName,
	                          @Nonnull String initials,
	                          @Nonnull List<String> humps) {
		/**
		 * @param queryParts
		 * 		Typed fragments split at uppercase characters.
		 *
		 * @return {@code true} if each fragment is a prefix of the corresponding name hump.
		 */
		public boolean matches(@Nonnull List<String> queryParts) {
			if (queryParts.size() < 2 || queryParts.size() > humps.size())
				return false;

			for (int i = 0; i < queryParts.size(); i++)
				if (!humps.get(i).startsWith(queryParts.get(i)))
					return false;
			return true;
		}
	}
}
