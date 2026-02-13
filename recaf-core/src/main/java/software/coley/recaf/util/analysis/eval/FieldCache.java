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
	/** Maps field names/types to their values. */
	private final Map<String, ReValue> fields = new HashMap<>();

	/**
	 * Set a field value.
	 *
	 * @param name
	 * 		Field name.
	 * @param desc
	 * 		Field descriptor.
	 * @param value
	 * 		Value to set.
	 */
	public void setField(@Nonnull String name, @Nonnull String desc, @Nonnull ReValue value) {
		fields.put(getKey(name, desc), value);
	}

	/**
	 * Get a field value.
	 *
	 * @param name
	 * 		Field name.
	 * @param desc
	 * 		Field descriptor.
	 *
	 * @return Value of the field, or {@code null} if no value is known.
	 * An actual {@code null} value is represented by {@link ObjectValue#VAL_OBJECT_NULL}.
	 */
	@Nullable
	public ReValue getField(@Nonnull String name, @Nonnull String desc) {
		return fields.get(getKey(name, desc));
	}

	@Nonnull
	private static String getKey(@Nonnull String name, @Nonnull String desc) {
		return name + "." + desc;
	}
}
