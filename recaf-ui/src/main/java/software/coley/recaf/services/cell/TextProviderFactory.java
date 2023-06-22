package software.coley.recaf.services.cell;

import jakarta.annotation.Nonnull;

/**
 * Base text provider factory.
 *
 * @author Matt Coley
 */
public interface TextProviderFactory {
	/**
	 * @return Text provider that provides {@code null}.
	 */
	@Nonnull
	default TextProvider emptyProvider() {
		return () -> null;
	}

	/**
	 * @return Text provider that provides {@code ""}.
	 */
	@Nonnull
	default TextProvider blankProvider() {
		return () -> "";
	}
}
