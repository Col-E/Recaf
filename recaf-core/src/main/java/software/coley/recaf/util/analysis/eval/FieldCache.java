package software.coley.recaf.util.analysis.eval;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.util.analysis.value.ObjectValue;
import software.coley.recaf.util.analysis.value.ReValue;

import java.util.HashMap;
import java.util.Map;

/**
 * Support for tracking field values.
 *
 * @author Matt Coley
 */
public class FieldCache {
	/** Maps owner-qualified field names and descriptors to their values. */
	private final Map<String, ReValue> fields = new HashMap<>();

	/**
	 * Set a field value.
	 *
	 * @param owner
	 * 		Internal name of the field owner.
	 * @param name
	 * 		Field name.
	 * @param desc
	 * 		Field descriptor.
	 * @param value
	 * 		Value to set.
	 */
	public void setField(@Nonnull String owner, @Nonnull String name, @Nonnull String desc, @Nonnull ReValue value) {
		fields.put(getKey(owner, name, desc), value);
	}

	/**
	 * Get a field value.
	 *
	 * @param owner
	 * 		Internal name of the field owner.
	 * @param name
	 * 		Field name.
	 * @param desc
	 * 		Field descriptor.
	 *
	 * @return Value of the field, or {@code null} if the field is absent.
	 * A known {@code null} value is stored as {@link ObjectValue#VAL_OBJECT_NULL}.
	 */
	@Nullable
	public ReValue getField(@Nonnull String owner, @Nonnull String name, @Nonnull String desc) {
		return fields.get(getKey(owner, name, desc));
	}

	/**
	 * Checks whether a field has been assigned a cached value.
	 *
	 * @param owner
	 * 		Internal name of the field owner.
	 * @param name
	 * 		Field name.
	 * @param desc
	 * 		Field descriptor.
	 *
	 * @return {@code true} when the cache contains the field.
	 */
	public boolean containsField(@Nonnull String owner, @Nonnull String name, @Nonnull String desc) {
		return fields.containsKey(getKey(owner, name, desc));
	}

	@Nonnull
	private static String getKey(@Nonnull String owner, @Nonnull String name, @Nonnull String desc) {
		// Key includes the owner to address potential name shadowing.
		return owner + "." + name + "." + desc;
	}
}

