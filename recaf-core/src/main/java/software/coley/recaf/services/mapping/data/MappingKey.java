package software.coley.recaf.services.mapping.data;

import jakarta.annotation.Nonnull;

/**
 * Mapping key, may be a class, method,
 * field or a local variable.
 *
 * @author xDark
 */
public interface MappingKey extends Comparable<MappingKey> {
	@Nonnull
	String getAsText();
}
