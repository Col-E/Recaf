package me.coley.recaf.util;

import com.eclipsesource.json.*;
import org.apache.commons.io.IOUtils;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static me.coley.recaf.util.Log.*;
import static java.nio.charset.StandardCharsets.UTF_8;


/**
 * Simplistic language translation loader.
 *
 * @author Matt
 */
public class LangUtil {
	private static final Map<String, String> MAP = new HashMap<>();
	private static final boolean DEBUG = true;
	public static final String DEFAULT_LANGUAGE = "en";

	/**
	 * Get translated string.
	 *
	 * @param key
	 *            Translation key.
	 * @return Translated string.
	 */
	public static String translate(String key) {
		if (DEBUG && !MAP.containsKey(key))
			error("\"{}\": \"missing_translation\",", key);
		return MAP.getOrDefault(key, key);
	}

	/**
	 * Load language from file by given name.
	 *
	 * @param lang
	 *            Name of file <i>(without extension)</i>.
	 */
	public static void load(String lang) {
		try {
			String file = "translations/" + lang + ".json";
			URL url = Thread.currentThread().getContextClassLoader().getResource(file);
			String jsStr = IOUtils.toString(url.openStream(), UTF_8);
			JsonObject json = Json.parse(jsStr).asObject();
			json.forEach(v -> MAP.put(v.getName(), v.getValue().asString()));
		} catch (Exception ex) {
			throw new IllegalStateException(ex);
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
		MAP.put(key, value);
	}

	static {
		load(DEFAULT_LANGUAGE);
	}
}
