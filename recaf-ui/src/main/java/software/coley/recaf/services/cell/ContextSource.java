package software.coley.recaf.services.cell;

import jakarta.annotation.Nonnull;

/**
 * Allows the {@link ContextMenuProviderFactory} types to know additional information about the context of the inputs.
 * For instance, if the request to provide a context menu for some data is based on <b>the declaration</b> of the data
 * or <b>a reference to</b> the data.
 *
 * @author Matt Coley
 */
public interface ContextSource {
	/**
	 * Constant describing declaration context sources.
	 */
	ContextSource DECLARATION = new BasicContextSource(true);

	/**
	 * Constant describing reference context sources.
	 */
	ContextSource REFERENCE = new BasicContextSource(false);

	/**
	 * @return {@code true} if the context is of a declaration.
	 */
	boolean isDeclaration();

	/**
	 * @return {@code true} if the context is of a reference.
	 */
	boolean isReference();

	/**
	 * @param key
	 * 		A key denoting a context menu capability.
	 *
	 * @return {@code true} if the context action associated with the key should be allowed to be shown.
	 */
	default boolean allow(@Nonnull String key) {
		return true;
	}
}
