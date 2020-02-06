package me.coley.recaf.util;

import com.eclipsesource.json.*;
import org.apache.commons.io.IOUtils;

import java.io.FileInputStream;
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
	 * Load language from resource.
	 *
	 * @param resource
	 *            Resource wrapper for file.
	 */
	public static void load(Resource resource) {
		String file = resource.getPath();
		try {
			if(resource.isInternal()) {
				URL url = Thread.currentThread().getContextClassLoader().getResource(file);
				String jsStr = IOUtils.toString(url.openStream(), UTF_8);
				JsonObject json = Json.parse(jsStr).asObject();
				json.forEach(v -> MAP.put(v.getName(), v.getValue().asString()));
			} else {
				String jsStr = IOUtils.toString(new FileInputStream(file), UTF_8);
				JsonObject json = Json.parse(jsStr).asObject();
				json.forEach(v -> MAP.put(v.getName(), v.getValue().asString()));
			}
		} catch(Exception ex) {
			throw new IllegalStateException("Failed to fetch language file: " + file, ex);
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
		load(Resource.internal("translations/" + DEFAULT_LANGUAGE + ".json"));
	}
}
