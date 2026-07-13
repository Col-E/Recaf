package software.coley.recaf.util;

import jakarta.annotation.Nonnull;

/**
 * Functional interface returning translation key.
 *
 * @author xDark
 */
public interface Translatable {
	/**
	 * @return Translation key.
	 */
	@Nonnull
	String getTranslationKey();
}