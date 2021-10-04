package me.coley.recaf.ui.control.code;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.coley.recaf.util.ClasspathUtil;
import me.coley.recaf.util.IOUtil;
import me.coley.recaf.util.logging.Logging;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility for loading language style rulesets.
 *
 * @author Matt Coley
 */
public class Languages {
	private static final Logger logger = Logging.get(Languages.class);
	private static final Map<String, Language> CACHE = new HashMap<>();
	private static final Gson gson = new GsonBuilder().create();
	/**
	 * Java language.
	 */
	public static final Language JAVA = Languages.get("java");
	/**
	 * Dummy default language used as a fallback.
	 */
	public static final Language NONE = new Language("none", Collections.emptyList(), true);

	/**
	 * Add support for a language's syntax.
	 *
	 * @param key
	 * 		Key to associate with language. Should be lower case and match the standard file extension of the language.
	 * @param language
	 * 		Language defintion with rules.
	 */
	public static void register(String key, Language language) {
		logger.info("Registering language syntax for '{}'", language.getName());
		CACHE.put(key, language);
	}

	/**
	 * @param key
	 * 		Name of language
	 *
	 * @return Language ruleset for styling.
	 */
	public static Language get(String key) {
		key = key.toLowerCase();
		// Check if already fetched
		Language language = CACHE.get(key);
		if (language != null)
			return language;
		// Attempt to read language file
		language = loadBundled(key);
		return language;
	}

	private static Language loadBundled(String key) {
		String file = "languages/" + key + ".json";
		Language language;
		InputStream res = ClasspathUtil.resource(file);
		if (res == null)
			return NONE;
		try (BufferedReader reader = IOUtil.toBufferedReader(res)) {
			language = gson.fromJson(reader, Language.class);
		} catch (Exception ex) {
			logger.error("Failed parsing language json for type: " + key, ex);
			return NONE;
		}
		register(key, language);
		return language;
	}
}
