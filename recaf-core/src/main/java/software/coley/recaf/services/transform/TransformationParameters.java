package software.coley.recaf.services.transform;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Per-run parameters for transformers.
 *
 * @author Matt Coley
 */
public class TransformationParameters {
	private final Map<String, Object> values;

	/**
	 * @param values
	 * 		Parameter key-value pairs.
	 */
	public TransformationParameters(@Nonnull Map<String, Object> values) {
		this.values = Map.copyOf(values);
	}

	/**
	 * @return Parameters with no values set.
	 */
	@Nonnull
	public static TransformationParameters empty() {
		return new TransformationParameters(Collections.emptyMap());
	}

	/**
	 * @param key
	 * 		Parameter key.
	 *
	 * @return Raw value associated with the key, or {@code null} if not present.
	 */
	@Nullable
	public Object get(@Nonnull String key) {
		return values.get(key);
	}

	/**
	 * @param key
	 * 		Parameter key.
	 *
	 * @return {@code true} when the key is present.
	 */
	public boolean has(@Nonnull String key) {
		return values.containsKey(key);
	}

	/**
	 * @param key
	 * 		Parameter key.
	 * @param defaultValue
	 * 		Value to return when the key is absent or holds a non-numeric value.
	 *
	 * @return Integer value of the parameter, or {@code defaultValue}.
	 */
	public int getInt(@Nonnull String key, int defaultValue) {
		Object value = values.get(key);
		if (value instanceof Number number)
			return number.intValue();
		return defaultValue;
	}

	/**
	 * @param key
	 * 		Parameter key.
	 * @param defaultValue
	 * 		Value to return when the key is absent or holds a non-numeric value.
	 *
	 * @return Long value of the parameter, or {@code defaultValue}.
	 */
	public long getLong(@Nonnull String key, long defaultValue) {
		Object value = values.get(key);
		if (value instanceof Number number)
			return number.longValue();
		return defaultValue;
	}

	/**
	 * @param key
	 * 		Parameter key.
	 * @param defaultValue
	 * 		Value to return when the key is absent or holds a non-boolean value.
	 *
	 * @return Boolean value of the parameter, or {@code defaultValue}.
	 */
	public boolean getBoolean(@Nonnull String key, boolean defaultValue) {
		Object value = values.get(key);
		if (value instanceof Boolean bool)
			return bool;
		return defaultValue;
	}

	/**
	 * @param key
	 * 		Parameter key.
	 * @param defaultValue
	 * 		Value to return when the key is absent or holds a non-string value.
	 *
	 * @return String value of the parameter, or {@code defaultValue}.
	 */
	@Nonnull
	public String getString(@Nonnull String key, @Nonnull String defaultValue) {
		Object value = values.get(key);
		if (value instanceof String string)
			return string;
		return defaultValue;
	}
}
