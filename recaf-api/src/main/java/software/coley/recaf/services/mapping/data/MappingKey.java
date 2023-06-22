package software.coley.recaf.services.mapping.data;

/**
 * Mapping key, may be a class, method,
 * field or a local variable.
 *
 * @author xDark
 */
public interface MappingKey extends Comparable<MappingKey> {
	String getAsText();
}
