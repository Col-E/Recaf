package me.coley.recaf.config;

import java.io.File;
import java.io.StringWriter;
import java.lang.reflect.Field;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.WriterConfig;

import me.coley.recaf.util.Misc;

/**
 * Abstract class for automatically storing / loading values on launch / exit.
 * 
 * @author Matt
 */
public abstract class Config {
	/**
	 * File to store config in.
	 */
	private final File confFile;

	public Config(String confFile) {
		this.confFile = new File(confFile + ".json");
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				save();
			}
		});
	}

	/**
	 * Load from configuration.
	 */
	public void load() {
		// Return if the config file does not exist.
		if (!confFile.exists()) {
			return;
		}
		try {
			JsonObject json = Json.parse(Misc.readFile(confFile.getAbsolutePath())).asObject();
			for (Field field : this.getClass().getDeclaredFields()) {
				String name = field.getName();
				JsonValue value = json.get(name);
				if (value == null) {
					continue;
				}
				field.setAccessible(true);
				if (value.isBoolean()) {
					field.set(this, value.asBoolean());
				} else if (value.isNumber()) {
					field.set(this, value.asInt());
				} else if (value.isString()) {
					field.set(this, value.asString());
				}
			}
		} catch (Exception e) {
			// TODO: Propper logging
			e.printStackTrace();
		}
	}

	/**
	 * Save current settings to configuration.
	 */
	public void save() {
		try {
			if (!confFile.exists() && !confFile.createNewFile()) {
				// Return if config file cannot be found and cannot be created.
				return;
			}
			JsonObject json = Json.object();
			for (Field field : this.getClass().getDeclaredFields()) {
				field.setAccessible(true);
				String name = field.getName();
				Object value = field.get(this);
				if (value instanceof Boolean) {
					json.set(name, (boolean) value);
				} else if (value instanceof Integer) {
					json.set(name, (int) value);
				} else if (value instanceof String) {
					json.set(name, (String) value);
				}
			}
			StringWriter w = new StringWriter();
			json.writeTo(w, WriterConfig.PRETTY_PRINT);
			Misc.writeFile(confFile.getAbsolutePath(), w.toString());
		} catch (Exception e) {
			// TODO: Propper logging
			e.printStackTrace();
		}
	}
}
