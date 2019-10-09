package me.coley.recaf.config;

import java.io.File;
import java.io.IOException;

/**
 * Config base.
 *
 * @author Matt
 */
public abstract class Config {
	private final String name;

	/**
	 * @param name
	 * 		Group name.
	 */
	Config(String name) {
		this.name = name;
	}

	/**
	 * @return Group name.
	 */
	public String getName() {
		return name;
	}

	void load(File file) throws IOException {
		// TODO: config loading
	}

	void save(File file) throws IOException {
		// TODO: config saving
	}
}
