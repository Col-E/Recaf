package software.coley.recaf.util;

import jakarta.annotation.Nonnull;

/**
 * Functional interface returning
 * translation key.
 *
 * @author xDark
 */
public interface Translatable {
	/**
	 * @return translation key.
	 */
	@Nonnull
	String getTranslationKey();
}