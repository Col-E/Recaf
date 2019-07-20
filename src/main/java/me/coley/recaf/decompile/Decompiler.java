package me.coley.recaf.decompile;

import me.coley.recaf.workspace.Workspace;

import java.util.Map;

/**
 * Decompiler base.
 *
 * @author Matt.
 */
public abstract class Decompiler {
	private final Map<String, Object> defaultOptions = generateDefaultOptions();
	private Map<String, Object> options = defaultOptions;

	/**
	 * @return Map of the current options.
	 */
	public Map<String, Object> getOptions() {
		return options;
	}

	/**
	 * @param options
	 * 		Map of the options to use.
	 */
	public void setOptions(Map<String, Object> options) {
		this.options = options;
	}

	/**
	 * @return Map of the default decompiler options.
	 */
	public Map<String, Object> getDefaultOptions() {
		return defaultOptions;
	}

	/**
	 * @return Map of the default decompiler options.
	 */
	protected abstract Map<String, Object> generateDefaultOptions();

	/**
	 * @param workspace
	 * 		Workspace to pull classes from.
	 * @param name
	 * 		Name of the class to decompile.
	 *
	 * @return Decompiled text of the class.
	 */
	public abstract String decompile(Workspace workspace, String name);
}