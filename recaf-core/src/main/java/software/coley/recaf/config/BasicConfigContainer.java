package software.coley.recaf.config;

import jakarta.annotation.Nonnull;

import java.util.Map;
import java.util.TreeMap;

/**
 * Basic implementation of {@link ConfigContainer}
 *
 * @author Matt Coley
 */
public class BasicConfigContainer implements ConfigContainer {
	private final Map<String, ConfigValue<?>> configMap = new TreeMap<>();
	private final String group;
	private final String id;

	/**
	 * @param group
	 * 		Container group.
	 * @param id
	 * 		Container ID.
	 */
	public BasicConfigContainer(@Nonnull String group, @Nonnull String id) {
		this.group = group;
		this.id = id;
	}

	/**
	 * @param value
	 * 		Value to add.
	 */
	protected void addValue(@Nonnull ConfigValue<?> value) {
		configMap.put(value.getId(), value);
	}

	@Nonnull
	@Override
	public String getGroup() {
		return group;
	}

	@Nonnull
	@Override
	public String getId() {
		return id;
	}

	@Nonnull
	@Override
	public Map<String, ConfigValue<?>> getValues() {
		return configMap;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		BasicConfigContainer that = (BasicConfigContainer) o;

		if (!configMap.equals(that.configMap)) return false;
		return id.equals(that.id);
	}

	@Override
	public int hashCode() {
		int result = configMap.hashCode();
		result = 31 * result + id.hashCode();
		return result;
	}
}
