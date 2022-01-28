package me.coley.recaf.ui.util;

import me.coley.recaf.util.IOUtil;
import me.coley.recaf.util.InternalPath;
import me.coley.recaf.util.SelfReferenceUtil;
import me.coley.recaf.util.logging.Logging;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Simple translation utility, tracking a bundle instance in the future may be a better choice.
 *
 * @author Matt Coley
 */
public class Lang {
	private static final String DEFAULT_LANGUAGE = "en";
	private static final List<String> languageKeys = new ArrayList<>();
	private static final Logger logger = Logging.get(Lang.class);
	private static final Map<String, Map<String, String>> languages = new HashMap<>();
	private static Map<String, String> currentLanguageMap;
	private static String currentLanguage = DEFAULT_LANGUAGE;

	/**
	 * @return Provided languages, also keys for {@link #getLanguages()}.
	 */
	public static List<String> getLanguageKeys() {
		return languageKeys;
	}

	/**
	 * @return Default language, English, also key for {@link #getLanguages()}.
	 */
	public static String getDefaultLanguage() {
		return DEFAULT_LANGUAGE;
	}

	/**
	 * @return Current language, used as key in {@link #getLanguages()}.
	 */
	public static String getCurrentLanguage() {
		return currentLanguage;
	}

	/**
	 * Sets the current language. Should be called before UI is shown for text components to use new values.
	 *
	 * @param language
	 * 		New language, used as key in {@link #getLanguages()}.
	 */
	public static void setCurrentLanguage(String language) {
		if (languages.containsKey(language)) {
			currentLanguage = language;
			currentLanguageMap = languages.get(language);
		} else {
			logger.warn("Tried to set language to '{}', but no entries for the language were found!", language);
		}
	}

	/**
	 * @return Map of supported languages and their translation key entries.
	 */
	public static Map<String, Map<String, String>> getLanguages() {
		return languages;
	}

	/**
	 * @param translationKey
	 * 		Key name.
	 *
	 * @return Translated value, based on {@link #getCurrentLanguage() current loaded mappings}.
	 */
	public static String get(String translationKey) {
		return get(getCurrentLanguage(), translationKey);
	}

	/**
	 * @param languageKey
	 * 		Language name.
	 * @param translationKey
	 * 		Key name.
	 *
	 * @return Translated value, based on {@link #getCurrentLanguage() current loaded mappings}.
	 */
	public static String get(String languageKey, String translationKey) {
		Map<String, String> languageMap = languages.getOrDefault(languageKey, currentLanguageMap);
		String value = languageMap.get(translationKey);
		if (value == null) {
			// Fallback to English if possible
			if (languageKey.equals(DEFAULT_LANGUAGE)) {
				logger.error("Missing translation for '{}' in language '{}'", translationKey, currentLanguage);
				value = translationKey;
			} else {
				value = get(DEFAULT_LANGUAGE, translationKey);
			}
		}
		return value;
	}

	/**
	 * Load the languages and initialize the default one.
	 */
	public static void initialize() {
		// Load provided languages
		// TODO: Make sure prefix "/" works in jar form

		SelfReferenceUtil.createInstance(Lang.class);
		SelfReferenceUtil selfReferenceUtil = SelfReferenceUtil.getInstance();
		for (InternalPath lang : selfReferenceUtil.getLangs()) {
			String language = FilenameUtils.removeExtension(lang.getFileName());

			try {
				load(language, lang.getURL().openStream());
				languageKeys.add(language);
				logger.info("Loaded language {}", language);
			} catch (IOException e) {
				logger.info("Failed to load language {}", language, e);
			}
		}

		// Set default
		setCurrentLanguage(DEFAULT_LANGUAGE);
	}

	/**
	 * Load language from {@link InputStream}.
	 *
	 * @param language
	 * 		Target language identifier. The key for {@link #getLanguages()}.
	 * @param in
	 *        {@link InputStream} to load language from.
	 */
	public static void load(String language, InputStream in) {
		try {
			Map<String, String> languageMap = languages.computeIfAbsent(language, l -> new HashMap<>());
			String string = IOUtil.toString(in, UTF_8);
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
					languageMap.put(key, value);
				}
			}
		} catch (Exception ex) {
			throw new IllegalStateException("Failed to fetch language from input stream", ex);
		}
	}
}

