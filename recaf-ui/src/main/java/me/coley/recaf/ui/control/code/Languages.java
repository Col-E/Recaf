package me.coley.recaf.ui.control.code;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.coley.recaf.config.Configs;
import me.coley.recaf.ui.util.LanguageAssociationListener;
import me.coley.recaf.util.ClasspathUtil;
import me.coley.recaf.util.IOUtil;
import me.coley.recaf.util.InternalPath;
import me.coley.recaf.util.SelfReferenceUtil;
import me.coley.recaf.util.logging.Logging;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.InputStream;
import java.util.*;

/**
 * Utility for loading language style rule-sets.
 *
 * @author Matt Coley
 */
public class Languages {
	private static final Logger logger = Logging.get(Languages.class);
	private static final Map<String, Language> CACHE = new HashMap<>();
	private static final Map<String, String> EXTENSION_REDIRECTS = new HashMap<>();
	private static final Gson gson = new GsonBuilder().create();
	private static final List<LanguageAssociationListener> associationListeners = new ArrayList<>();
	/**
	 * Java language.
	 */
	public static final Language JAVA = Languages.get("java");
	/**
	 * Java bytecode language.
	 */
	public static final Language JAVA_BYTECODE = Languages.get("bytecode");
	/**
	 * Android bytecode language.
	 */
	public static final Language DALVIK_BYTECODE = Languages.get("dalvik");
	/**
	 * Extensible markup language.
	 */
	public static final Language XML = Languages.get("xml");
	/**
	 * Java manifest language.
	 */
	public static final Language MANIFEST = Languages.get("mf");
	/**
	 * Dummy default language used as a fallback.
	 */
	public static final Language NONE = new Language("_default", "None", Collections.emptyList(), true);

	/**
	 * Add support for a language's syntax.
	 *
	 * @param key
	 * 		Key to associate with language. Should be lower case and match the standard file extension of the language.
	 * @param language
	 * 		Language definition with rules.
	 */
	public static void register(String key, Language language) {
		logger.debug("Registering language syntax for '{}'", language.getName());
		CACHE.put(key, language);
	}

	/**
	 * @return All active languages.
	 */
	public static Collection<Language> allLanguages() {
		return CACHE.values();
	}

	/**
	 * @param key
	 * 		Language key.
	 * @param fallback
	 * 		Fallback value to return.
	 *
	 * @return Language rule-set for styling.
	 */
	public static Language getOrDefault(String key, Language fallback) {
		Language lang = get(key);
		if (lang == null)
			lang = fallback;
		return lang;
	}

	/**
	 * @param key
	 * 		Language key.
	 *
	 * @return Language rule-set for styling.
	 */
	public static Language get(String key) {
		key = key.toLowerCase();
		key = EXTENSION_REDIRECTS.getOrDefault(key, key);
		// Check if already fetched
		Language language = CACHE.get(key);
		if (language != null)
			return language;

		// Try to find an associated language
		language = loadBundled(Configs.editor().fileExtensionAssociations.getOrDefault(key, key));
		if (language != null)
			return language;

		// Attempt to read language file
		language = loadBundled(key);
		return language;
	}

	/**
	 * Adds an association between a file extension and a {@link Language language}.
	 *
	 * @param extension
	 * 		The file extension to associate with the new language.
	 * @param language
	 * 		The language to associate with the extension.
	 */
	public static void setExtensionAssociation(String extension, Language language) {
		String languageKey = language.getKey();
		EXTENSION_REDIRECTS.put(extension, languageKey);
		Configs.editor().fileExtensionAssociations.put(extension, languageKey);

		associationListeners.forEach(listener -> listener.onAssociationChanged(extension, language));
	}

	/**
	 * Removes an association between a file extension and a {@link Language language}.
	 *
	 * @param extension
	 * 		The extension to clear associations for.
	 */
	public static void removeExtensionAssociation(String extension) {
		EXTENSION_REDIRECTS.remove(extension);
		Configs.editor().fileExtensionAssociations.remove(extension);

		// Just try to find a default language for this extension
		Language language = get(extension);
		associationListeners.forEach(listener -> listener.onAssociationChanged(extension, language));
	}

	/**
	 * Adds a new language association listener.
	 *
	 * @param listener
	 * 		Listener to add.
	 */
	public static void addAssociationListener(LanguageAssociationListener listener) {
		associationListeners.add(listener);
	}

	/**
	 * Removes an existing language association listener.
	 *
	 * @param listener
	 * 		Listener to remove.
	 */
	public static void removeAssociationListener(LanguageAssociationListener listener) {
		associationListeners.remove(listener);
	}

	private static Language loadBundled(String key) {
		Language language = CACHE.get(key);
		if (language != null)
			return language;
		String file = "languages/" + key + ".json";
		InputStream res = ClasspathUtil.resource(file);
		if (res == null)
			return NONE;
		try (BufferedReader reader = IOUtil.toBufferedReader(res)) {
			language = gson.fromJson(reader, Language.class);
		} catch (Exception ex) {
			logger.error("Failed parsing language json for type: " + key, ex);
			return NONE;
		}
		language.setKey(key);
		register(key, language);
		return language;
	}

	static {
		// Load all internal languages
		SelfReferenceUtil.initializeFromContext(Languages.class);
		for (InternalPath path : SelfReferenceUtil.getInstance().getLanguages()) {
			String name = path.getFileName();
			name = name.substring(0, name.indexOf('.'));
			loadBundled(name);
		}
		// Setup redirects for extensions that match similar rules
		EXTENSION_REDIRECTS.put("kt", "java");
		EXTENSION_REDIRECTS.put("html", "xml");
		EXTENSION_REDIRECTS.put("htm", "xml");
	}
}
