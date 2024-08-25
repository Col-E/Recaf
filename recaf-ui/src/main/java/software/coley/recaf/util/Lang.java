package software.coley.recaf.util;

import jakarta.annotation.Nonnull;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Simple translation utility, tracking a bundle instance in the future may be a better choice.
 *
 * @author Matt Coley
 */
public class Lang {
	private static final String DEFAULT_TRANSLATIONS = "en_US";
	private static String SYSTEM_LANGUAGE;
	private static final List<String> translationKeys = new ArrayList<>();
	private static final Logger logger = Logging.get(Lang.class);
	private static final Map<String, Map<String, String>> translations = new ConcurrentHashMap<>();
	private static final Map<String, StringBinding> translationBindings = new ConcurrentHashMap<>();
	private static Map<String, String> currentTranslationMap;
	private static final StringProperty currentTranslation = new SynchronizedSimpleStringProperty(DEFAULT_TRANSLATIONS);

	/**
	 * @return Provided translations, also keys for {@link #getTranslations()}.
	 */
	@Nonnull
	public static List<String> getTranslationKeys() {
		return translationKeys;
	}

	/**
	 * @return Default translations, English, also key for {@link #getTranslations()}.
	 */
	@Nonnull
	public static String getDefaultTranslations() {
		return DEFAULT_TRANSLATIONS;
	}

	/**
	 * @return Current translations, used as key in {@link #getTranslations()}.
	 */
	public static String getCurrentTranslations() {
		return currentTranslation.get();
	}

	/**
	 * Sets the current translations. Should be called before UI is shown for text components to use new values.
	 *
	 * @param translationsKey
	 * 		New translations, used as key in {@link #getTranslations()}.
	 */
	public static void setCurrentTranslations(String translationsKey) {
		if (translations.containsKey(translationsKey)) {
			currentTranslationMap = translations.getOrDefault(translationsKey, Collections.emptyMap());
			currentTranslation.set(translationsKey);
		} else {
			logger.warn("Tried to set translations to '{}', but no entries for the translations were found!", translationsKey);
			// For case it fails to load, use default.
			// If for some reason the default translations are not loading, we got a problem...
			if (!DEFAULT_TRANSLATIONS.equals(translationsKey)) {
				setCurrentTranslations(DEFAULT_TRANSLATIONS);
			} else {
				logger.error("Could not load default translations: {}", DEFAULT_TRANSLATIONS);
			}
		}
	}

	/**
	 * Sets the system language.
	 *
	 * @param translations
	 * 		System language.
	 */
	public static void setSystemLanguage(String translations) {
		SYSTEM_LANGUAGE = translations;
	}

	/**
	 * @return System language, or {@link #getDefaultTranslations()} if not set.
	 */
	@Nonnull
	public static String getSystemLanguage() {
		return SYSTEM_LANGUAGE == null ? getDefaultTranslations() : SYSTEM_LANGUAGE;
	}

	/**
	 * @return Map of supported translations and their key entries.
	 */
	@Nonnull
	public static Map<String, Map<String, String>> getTranslations() {
		return translations;
	}

	/**
	 * @param translationKey
	 * 		Key name.
	 *
	 * @return JavaFX string binding for specific translation key.
	 */
	@Nonnull
	public static synchronized StringBinding getBinding(@Nonnull String translationKey) {
		return translationBindings.computeIfAbsent(translationKey, k -> {
			StringProperty currentTranslation = Lang.currentTranslation;
			return new SynchronizedStringBinding() {
				{
					bind(currentTranslation);
				}

				@Override
				protected synchronized String computeValue() {
					String translated = Lang.get(currentTranslation.get(), translationKey);
					translated = translated.replace("\\n", "\n");
					return translated;
				}
			};
		});
	}

	/**
	 * @param format
	 * 		String format.
	 * @param args
	 * 		Format arguments.
	 *
	 * @return JavaFX string binding for specific translation key with arguments.
	 */
	@Nonnull
	public static StringBinding formatBy(@Nonnull String format, ObservableValue<?>... args) {
		return new SynchronizedStringBinding() {
			{
				bind(args);
			}

			@Override
			protected synchronized String computeValue() {
				return String.format(format, Arrays.stream(args)
						.map(ObservableValue::getValue).toArray());
			}
		};
	}

	/**
	 * @param translationKey
	 * 		Key name.
	 * @param args
	 * 		Format arguments.
	 *
	 * @return JavaFX string binding for specific translation key with arguments.
	 */
	@Nonnull
	public static StringBinding format(@Nonnull String translationKey, ObservableValue<?>... args) {
		StringBinding root = getBinding(translationKey);
		return new SynchronizedStringBinding() {
			{
				bind(root);
				bind(args);
			}

			@Override
			protected synchronized String computeValue() {
				return String.format(root.getValue(), Arrays.stream(args)
						.map(ObservableValue::getValue).toArray());
			}
		};
	}

	/**
	 * @param translationKey
	 * 		Key name.
	 * @param args
	 * 		Format arguments.
	 *
	 * @return JavaFX string binding for specific translation key with arguments.
	 */
	@Nonnull
	public static StringBinding formatLiterals(@Nonnull String translationKey, Object... args) {
		StringBinding root = getBinding(translationKey);
		return new SynchronizedStringBinding() {
			{
				bind(root);
			}

			@Override
			protected synchronized String computeValue() {
				return String.format(root.getValue(), args);
			}
		};
	}

	/**
	 * @param translationKey
	 * 		Key name.
	 * @param args
	 * 		Format arguments.
	 *
	 * @return JavaFX string binding for specific translation key with arguments.
	 */
	@Nonnull
	public static StringBinding format(@Nonnull String translationKey, Object... args) {
		StringBinding root = getBinding(translationKey);
		return new SynchronizedStringBinding() {
			{
				bind(root);
			}

			@Override
			protected synchronized String computeValue() {
				return String.format(root.getValue(), args);
			}
		};
	}

	/**
	 * @param translation
	 * 		Translation value.
	 * @param args
	 * 		Format arguments.
	 *
	 * @return JavaFX string binding for specific translation key with arguments.
	 */
	@Nonnull
	public static StringBinding concat(@Nonnull ObservableValue<String> translation, String... args) {
		return new SynchronizedStringBinding() {
			{
				bind(translation);
			}

			@Override
			protected synchronized String computeValue() {
				return translation.getValue() + String.join(" ", args);
			}
		};
	}

	/**
	 * @param translationKey
	 * 		Key name.
	 * @param args
	 * 		Format arguments.
	 *
	 * @return JavaFX string binding for specific translation key with arguments.
	 */
	@Nonnull
	public static StringBinding concat(@Nonnull String translationKey, String... args) {
		StringBinding root = getBinding(translationKey);
		return new SynchronizedStringBinding() {
			{
				bind(root);
			}

			@Override
			protected synchronized String computeValue() {
				return root.getValue() + String.join(" ", args);
			}
		};
	}

	/**
	 * @return Translations property.
	 */
	@Nonnull
	public static StringProperty translationsProperty() {
		return currentTranslation;
	}

	/**
	 * @param translationKey
	 * 		Key name.
	 *
	 * @return Translated value, based on {@link #getCurrentTranslations() current loaded mappings}.
	 */
	public static String get(String translationKey) {
		return get(getCurrentTranslations(), translationKey);
	}

	/**
	 * @param translations
	 * 		Language translations group to load from.
	 * @param translationKey
	 * 		Key name.
	 *
	 * @return Translated value, based on {@link #getCurrentTranslations() current loaded mappings}.
	 */
	@Nonnull
	public static String get(@Nonnull String translations, @Nonnull String translationKey) {
		Map<String, String> map = Lang.translations.getOrDefault(translations, currentTranslationMap);
		String value = map.get(translationKey);
		if (value == null) {
			// Fallback to English if possible.
			if (translations.equals(DEFAULT_TRANSLATIONS)) {
				logger.error("Missing translation for '{}' in language '{}'", translationKey, currentTranslation.get());
				value = translationKey;
			} else {
				value = get(DEFAULT_TRANSLATIONS, translationKey);
			}
		}
		return value;
	}

	/**
	 * @param translations
	 * 		Language translations group to load from.
	 * @param translationKey
	 * 		Key name.
	 *
	 * @return {@code true} when the translation is present in the given translations.
	 */
	public static boolean has(String translations, String translationKey) {
		return Lang.translations.getOrDefault(translations, currentTranslationMap).containsKey(translationKey);
	}

	/**
	 * @param translationKey
	 * 		Key name.
	 *
	 * @return {@code true} when the translation is present in the current translations.
	 */
	public static boolean has(String translationKey) {
		return has(getCurrentTranslations(), translationKey);
	}

	/**
	 * Load the translations and initialize the default one.
	 */
	public static void initialize() {
		// Get the actual locale for translations
		String userCountry = Locale.getDefault().getCountry();
		String userLanguage = Locale.getDefault().getLanguage();
		String userLanguageKey = userLanguage + "_" + userCountry;
		setSystemLanguage(userLanguageKey);
		// Then set the jvm to use to avoid the locale bug
		//  - https://mattryall.net/blog/the-infamous-turkish-locale-bug
		Locale.setDefault(Locale.US);
		// Load provided translations
		SelfReferenceUtil.initializeFromContext(Lang.class);
		SelfReferenceUtil selfReferenceUtil = SelfReferenceUtil.getInstance();
		List<InternalPath> translations = selfReferenceUtil.getTranslations();
		if (!translations.isEmpty())
			logger.debug("Found {} translations", translations.size());
		else
			logger.error("Translations could not be loaded! CodeSource: {}",
					Lang.class.getProtectionDomain().getCodeSource().getLocation());
		for (InternalPath translationPath : translations) {
			String translationName = StringUtil.cutOffAtFirst(translationPath.getFileName(), ".");
			try {
				load(translationName, translationPath.getURL().openStream());
				translationKeys.add(translationName);
				logger.info("Loaded translations '{}'", translationName);
			} catch (IOException e) {
				logger.info("Failed to load translations '{}'", translationName, e);
			}
		}

		// Set default translations
		setCurrentTranslations(DEFAULT_TRANSLATIONS);
	}

	/**
	 * Load translations from {@link InputStream}.
	 *
	 * @param translations
	 * 		Target translations identifier. The key for {@link #getTranslations()}.
	 * @param in
	 *        {@link InputStream} to load translations from.
	 */
	public static void load(String translations, InputStream in) {
		try {
			Map<String, String> translationsMap = Lang.translations.computeIfAbsent(translations, l -> new HashMap<>());
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
					translationsMap.put(key, value);
				}
			}
		} catch (Exception ex) {
			throw new IllegalStateException("Failed to fetch language from input stream", ex);
		}
	}
}

