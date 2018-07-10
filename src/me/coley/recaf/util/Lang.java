package me.coley.recaf.util;

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
			String prefix = "/resources/lang/";
			String file = prefix + lang + ".json";
			// FxWindow.logging.fine("Loading language from: " + file, 1);
			String jsStr = Streams.toString(Lang.class.getResourceAsStream(file));
			JsonObject json = Json.parse(jsStr).asObject();
			json.forEach(v -> {
				map.put(v.getName(), v.getValue().asString());
			});
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
		load(ConfDisplay.instance().language);
	}
}