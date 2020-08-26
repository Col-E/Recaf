package me.coley.recaf.util;

import com.eclipsesource.json.*;
import org.apache.commons.io.IOUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
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
	 * Load language from {@link InputStream}.
	 *
	 * @param in
	 *            {@link InputStream} to load language from.
	 */
	public static void load(InputStream in) {
		try {
			String jsStr = IOUtils.toString(in, UTF_8);
			JsonObject json = Json.parse(jsStr).asObject();
			json.forEach(v -> MAP.put(v.getName(), v.getValue().asString()));
		} catch (Exception ex) {
			throw new IllegalStateException("Failed to fetch language from input stream", ex);
		}
	}

	/**
	 * Load language from URL.
	 *
	 * @param url
	 *            URL to load language from.
	 */
	public static void load(URL url) {
		try (InputStream in = url.openStream()) {
			load(in);
		} catch (IOException ex) {
			throw new IllegalStateException("Failed to fetch language from URL: " + url, ex);
		}
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
				Enumeration<URL> urls = LangUtil.class.getClassLoader().getResources(file);
				if (!urls.hasMoreElements()) {
					throw new IOException(file);
				}
				load(urls.nextElement());
				while (urls.hasMoreElements()) {
					load(urls.nextElement());
				}
			} else {
				try (InputStream in  = new FileInputStream(file)) {
					load(in);
				}
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

	/**
	 * Clears available translations.
	 */
	public static void clear() {
		MAP.clear();
	}

	static {
		load(Resource.internal("translations/" + DEFAULT_LANGUAGE + ".json"));
	}
}
