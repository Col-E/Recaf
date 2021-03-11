package me.coley.recaf.ui.util;

import me.coley.recaf.util.logging.Logging;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Simple translation utility, tracking a bundle instance in the future may be a better choice.
 *
 * @author Matt Coley
 */
public class Lang {
	public static final String DEFAULT_LANGUAGE = "en";

	private static final Logger logger = Logging.get(Lang.class);
	private static final Map<String, String> map = new HashMap<>();

	/**
	 * @param translationKey
	 * 		Key name.
	 *
	 * @return Translated value, based on current loaded mappings.
	 */
	public static String get(String translationKey) {
		String value = map.get(translationKey);
		if (value == null) {
			logger.error("Missing translation for: {}", translationKey);
			value = translationKey;
		}
		return value;
	}

	/**
	 * Load the default language.
	 */
	public static void load() {
		// TODO: Make sure prefix "/" works in jar form
		load(Lang.class.getResourceAsStream("/lang/" + DEFAULT_LANGUAGE + ".lang"));
	}

	/**
	 * Load language from {@link InputStream}.
	 *
	 * @param in
	 *        {@link InputStream} to load language from.
	 */
	private static void load(InputStream in) {
		try {
			String string = IOUtils.toString(in, UTF_8);
			String[] lines = string.split("[\n\r]+");
			for (String line : lines) {
				// Skip comment lines
				if (line.startsWith("#")) {
					continue;
				}
				// Add each "key=value"
				if (line.contains("=")) {
					String[] parts = line.split("=", 2);
					String key = parts[0];
					String value = parts[1];
					map.put(key, value);
				}
			}
		} catch (Exception ex) {
			throw new IllegalStateException("Failed to fetch language from input stream", ex);
		}
	}

}
