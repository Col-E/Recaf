package me.coley.recaf.plugin;

import java.io.File;

/**
 * A wrapper for error-handling in plugin loading.
 *
 * @author Matt
 */
public class PluginLoadException extends Exception {
	private final File jar;

	/**
	 * Create the load exception.
	 *
	 * @param jar
	 * 		The plugin jar that caused the load exception.
	 * @param ex
	 * 		The cause exception.
	 * @param message
	 * 		Exception message explaining what went wrong.
	 */
	public PluginLoadException(File jar, Exception ex, String message) {
		super(message, ex);
		this.jar = jar;
	}

	/**
	 * @return The plugin jar that caused the load exception.
	 */
	public final File getJar() {
		return jar;
	}
}
