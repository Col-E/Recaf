package me.coley.recaf.plugin.tools;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Tool wrapper.
 * <br>
 * A tool is some process or feature that is executed with parameters and has some output.
 * This class gives each tool a common representation for Recaf to interact with.
 *
 * @param <T>
 * 		Tool option type.
 *
 * @author Matt Coley
 */
public abstract class Tool<T extends ToolOption<?>> implements Comparable<Tool<?>> {
	private final Map<String, T> defaultOptions = createDefaultOptions();
	private final String name;
	private final String version;

	protected Tool(String name, String version) {
		this.name = name;
		this.version = version;
	}

	/**
	 * @return Tool name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return Tool version.
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * @return Copy of default options used by the tool.
	 */
	public Map<String, T> getDefaultOptions() {
		return new HashMap<>(defaultOptions);
	}

	/**
	 * @return Map of options used by the tool as a default.
	 */
	protected abstract Map<String, T> createDefaultOptions();

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Tool)) return false;
		Tool<?> that = (Tool<?>) o;
		return getName().equals(that.getName()) &&
				getVersion().equals(that.getVersion());
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, version);
	}

	@Override
	public int compareTo(Tool o) {
		return getName().compareTo(o.getName());
	}
}
