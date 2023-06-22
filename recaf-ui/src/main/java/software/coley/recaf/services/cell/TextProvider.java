package software.coley.recaf.services.cell;

import jakarta.annotation.Nullable;

/**
 * Provides some text. Primarily used when wanting to provide an expected text pattern lazily.
 *
 * @author Matt Coley
 */
public interface TextProvider {
	/**
	 * @return Provided text. May be {@code null}.
	 */
	@Nullable
	String makeText();
}
