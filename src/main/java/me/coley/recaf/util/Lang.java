package me.coley.recaf.util;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

import me.coley.recaf.Logging;
import me.coley.recaf.config.impl.ConfDisplay;

/**
 * Simplistic language translation loader.
 * 
 * @author Matt
 */
public class Lang {
	public static final String DEFAULT_LANGUAGE = "en";
	private static final Map<String, String> map = new HashMap<>();
	private final static boolean DEBUG = true;

	/**
	 * Get translated string.
	 * 
	 * @param key
	 *            Translation key.
	 * @return Translated string.
	 */
	public static String get(String key) {
		if (DEBUG && !map.containsKey(key)) {
			Logging.error("\t\"" + key + "\": \"missing_translation\",");
		}
		return map.getOrDefault(key, key);
	}

	/**
	 * Load language from file by given name.
	 * 
	 * @param lang
	 *            Name of file <i>(without extension)</i>.
	 */
	public static void load(String lang) {
		// If this is crashing you, add **/*.json to your source filters.
		// The assets need to be on the class-path.
		// Refresh the project when that's done.
		try {
			String file = "resources/lang/" + lang + ".json";
			URL url = Thread.currentThread().getContextClassLoader().getResource(file);
			String jsStr = Streams.toString(url.openStream());
			JsonObject json = Json.parse(jsStr).asObject();
			json.forEach(v -> map.put(v.getName(), v.getValue().asString()));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Insert a translation key-value pair.
	 * 
	 * @param key
	 *            Translation key.
	 * @param value
	 *            Translation text.
	 */
	public static void load(String key, String value) {
		map.put(key, value);
	}

	static {
		load(DEFAULT_LANGUAGE);
		load(ConfDisplay.instance().language);
	}
}