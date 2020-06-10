package me.coley.recaf.decompile;

import me.coley.recaf.control.Controller;

import java.util.*;

/**
 * Decompiler base.
 *
 * @param <OptionType>
 * 		Type used by decompiler implementation for options.
 *
 * @author Matt.
 */
public abstract class Decompiler<OptionType> {
	private final Controller controller;
	private final Map<String, OptionType> defaultOptions;
	private Map<String, OptionType> options;

	/**
	 * Initialize the decompiler wrapper.
	 *
	 * @param controller
	 * 		Controller with configuration to pull from and the workspace to pull classes from.
	 */
	public Decompiler(Controller controller) {
		this.controller = controller;
		this.defaultOptions = Collections.unmodifiableMap(generateDefaultOptions());
		this.options = new HashMap<>(defaultOptions);
	}

	/**
	 * @return Controller with configuration to pull from and the workspace to pull classes from.
	 */
	protected Controller getController() {
		return controller;
	}

	/**
	 * @return Map of the current options.
	 */
	public Map<String, OptionType> getOptions() {
		return options;
	}

	/**
	 * @param options
	 * 		Map of the options to use.
	 */
	public void setOptions(Map<String, OptionType> options) {
		this.options = options;
	}

	/**
	 * @return Map of the default decompiler options.
	 */
	public Map<String, OptionType> getDefaultOptions() {
		return defaultOptions;
	}

	/**
	 * @return Map of the default decompiler options.
	 */
	protected abstract Map<String, OptionType> generateDefaultOptions();

	/**
	 * @param name
	 * 		Name of the class to decompile.
	 *
	 * @return Decompiled text of the class.
	 */
	public abstract String decompile(String name);
}