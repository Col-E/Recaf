package me.coley.recaf.config;

import java.io.*;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.WriterConfig;

import me.coley.recaf.Logging;
import me.coley.recaf.util.*;

/**
 * Persistent config handler. Saves on exit, loads on startup.
 * 
 * @author Matt
 */
public abstract class Config {
	/**
	 * Config subfolder to reduce clutter adjacent to the recaf jar.
	 */
	private final static File confFolder = new File("rc-config");
	/**
	 * Map of config instances.
	 */
	private final static Map<Class<?>, Config> instances = new HashMap<>();
	/**
	 * File to store data in.
	 */
	private final File confFile;

	protected Config(String fileName) {
		confFile = new File(confFolder, fileName + ".json");
		registerInstance();
		addSaveHook();
		setAccessible();
	}

	/**
	 * Save current settings to configuration.
	 */
	protected final void save() {
		try {
			if (!confFolder.exists()) {
				// Create subfolder for config files.
				confFolder.mkdirs();
			}
			if (!confFile.exists() && !confFile.createNewFile()) {
				// Return if config file cannot be found or cannot be created if
				// it does not exist.
				return;
			}
			JsonObject json = Json.object();
			for (Field field : Reflect.fields(getClass())) {
				// Skip if field does not represent a config option.
				String name = getValueName(field);
				if (name == null) {
					continue;
				}
				// Access via reflection, add value to json object.
				// field.get(this);
				Object value = Reflect.get(this, field);
				if (field.getType().equals(boolean.class)) {
					json.set(name, (boolean) value);
				} else if (field.getType().equals(int.class)) {
					json.set(name, (int) value);
				} else if (field.getType().equals(long.class)) {
					json.set(name, (long) value);
				} else if (field.getType().equals(float.class)) {
					json.set(name, (float) value);
				} else if (field.getType().equals(double.class)) {
					json.set(name, (double) value);
				} else if (field.getType().equals(String.class)) {
					json.set(name, (String) value);
				} else {
					JsonValue converted = convert(field.getType(), value);
					if (converted != null) {
						json.set(name, converted);
					}
				}
			}
			// Write json to file
			StringWriter w = new StringWriter();
			json.writeTo(w, WriterConfig.PRETTY_PRINT);
			FileIO.writeFile(confFile.getAbsolutePath(), w.toString());
		} catch (Exception e) {
			Logging.error(e);
		}
	}

	/**
	 * Load from configuration.
	 */
	protected final void load() {
		// Return if the config subfolder does not exist.
		if (!confFolder.exists()) {
			return;
		}
		// Return if the config file does not exist.
		if (!confFile.exists()) {
			return;
		}
		// Read values from file
		try {
			JsonObject json = Json.parse(FileIO.readFile(confFile.getAbsolutePath())).asObject();
			for (Field field : Reflect.fields(getClass())) {
				String name = getValueName(field);
				if (name == null) {
					continue;
				}
				JsonValue value = json.get(name);
				if (value == null) {
					continue;
				}

				try {
					if(field.getType().equals(boolean.class)) {
						field.set(this, value.asBoolean());
					} else if(field.getType().equals(int.class)) {
						field.set(this, value.asInt());
					} else if(field.getType().equals(long.class)) {
						field.set(this, value.asLong());
					} else if(field.getType().equals(float.class)) {
						field.set(this, value.asFloat());
					} else if(field.getType().equals(double.class)) {
						field.set(this, value.asDouble());
					} else if(field.getType().equals(String.class)) {
						field.set(this, value.asString());
					} else {
						Object parsed = parse(field.getType(), value);
						if(parsed != null) {
							field.set(this, parsed);
						}
					}
				} catch(Exception e) {
					Logging.error("Skipping bad option: " + confFile.getName() + " - " + name);
				}
			}
		} catch (Exception e) {
			Logging.error(e);
		}
	}

	/**
	 * Set all fields to accessible.
	 */
	private final void setAccessible() {
		for (Field field : Reflect.fields(getClass())) {
			field.setAccessible(true);
		}
	}

	/**
	 * Converts the value of the given type into a JsonValue.
	 * 
	 * @param type
	 *            Value's declared type.
	 * @param value
	 *            Current value.
	 * @return Serialized JsonValue representation of current value.
	 */
	protected JsonValue convert(Class<?> type, Object value) {
		return null;
	}

	/**
	 * Parses the value into the given type.
	 * 
	 * @param type
	 *            Value's declared type.
	 * @param value
	 *            Serialized json value.
	 * @return Deserialized value of JsonValue.
	 */
	protected Object parse(Class<?> type, JsonValue value) {
		return null;
	}

	/**
	 * Get name of value associated with the field by pulling data from the
	 * {@code @Conf} annotation.
	 * 
	 * @param field
	 * @return
	 */
	private final String getValueName(Field field) {
		Conf anno = field.getDeclaredAnnotation(Conf.class);
		if (anno == null) {
			return null;
		}
		return anno.category() + "." + anno.key();
	}

	/**
	 * Add shutdown hook to ensure config is saved when Recaf is closed.
	 */
	private final void addSaveHook() {
		if (!Misc.isTesting()) {
			Runtime.getRuntime().addShutdownHook(new Thread(() -> save()));
		}
	}

	/**
	 * Register current instance to the {@link #instances} map, accessible via
	 * {@link #instance(Class)}.
	 */
	private void registerInstance() {
		Class<?> key = getClass();
		if (!instances.containsKey(key)) {
			instances.put(key, this);
		} else {
			throw new RuntimeException("An instance of this config: '" + key + "' already exists!");
		}
	}

	/**
	 * Instance getter/setter.
	 * 
	 * @param key
	 * @return
	 */
	@SuppressWarnings("unchecked")
	protected static <T extends Config> T instance(Class<T> key) {
		T conf = (T) instances.get(key);
		if (conf == null) {
			try {
				// Call constructor, will call registerInstance() and add it to
				// the map.
				conf = key.getDeclaredConstructor().newInstance();
			} catch (Exception e) {
				Logging.fatal(e);
			}
		}
		return conf;
	}
}
