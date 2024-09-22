package software.coley.recaf.services.mapping.gen.naming;

import jakarta.annotation.Nonnull;
import software.coley.recaf.config.BasicConfigContainer;

/**
 * Abstract base provider for a {@link NameGenerator} implementation.
 *
 * @param <T>
 * 		Name generator implementation type.
 *
 * @author Matt Coley
 */
public abstract class AbstractNameGeneratorProvider<T extends NameGenerator>
		extends BasicConfigContainer implements NameGeneratorProvider<T> {
	/**
	 * @param id
	 * 		Name generator ID.
	 */
	public AbstractNameGeneratorProvider(@Nonnull String id) {
		super(GROUP_ID, id);
	}
}
