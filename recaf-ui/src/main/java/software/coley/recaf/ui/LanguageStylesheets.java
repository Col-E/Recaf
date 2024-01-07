package software.coley.recaf.ui;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.ui.control.richtext.syntax.RegexLanguages;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static software.coley.recaf.util.StringUtil.cutOffAtFirst;
import static software.coley.recaf.util.StringUtil.shortenPath;

/**
 * Languages stylesheets for syntax highlighting.
 *
 * @author Matt Coley
 * @see RegexLanguages For retrieving matchers for the supported languages.
 */
public class LanguageStylesheets {
	private static final Map<String, String> NAME_TO_PATH = new HashMap<>();
	private static final String SHEET_JAVA;
	private static final String SHEET_JASM;
	private static final String SHEET_XML;
	private static final String SHEET_ENIGMA;

	// Prevent construction
	private LanguageStylesheets() {
	}

	static {
		SHEET_JAVA = addLanguage("/syntax/java.css");
		SHEET_JASM = addLanguage("/syntax/jasm.css");
		SHEET_XML = addLanguage("/syntax/xml.css");
		SHEET_ENIGMA = addLanguage("/syntax/enigma.css");
	}

	/**
	 * Adds support for the language outlined by the contents of the given file.
	 *
	 * @param path
	 * 		Full path to stylesheet file. The language name is the file name, without the extension.
	 *
	 * @return Full path to stylesheet file.
	 */
	@Nonnull
	public static String addLanguage(@Nonnull String path) {
		String name = cutOffAtFirst(shortenPath(path), ".");
		return addLanguage(name, path);
	}

	/**
	 * @param name
	 * 		Language name to use as a key. Will be used in {@link #getLanguages()} and {@link #getLanguageStylesheet(String)}.
	 * @param path
	 * 		Full path to stylesheet file.
	 *
	 * @return Full path to stylesheet file.
	 *
	 * @see RegexLanguages#addLanguage(String, InputStream) You should assign a regex language model by the same language name.
	 */
	@Nonnull
	public static String addLanguage(@Nonnull String name, @Nonnull String path) {
		NAME_TO_PATH.put(name, path);
		return path;
	}

	/**
	 * @param name
	 * 		Name of the language, matching the file path inside Recaf's {@code /syntax/} directory,
	 * 		without the {@code .css} extension. To get {@link #getJavaStylesheet()} use {@code java}.
	 *
	 * @return Language stylesheet path, or {@code null} if unknown language.
	 *
	 * @see RegexLanguages#getLanguage(String) Language to match with.
	 */
	@Nullable
	public static String getLanguageStylesheet(String name) {
		return NAME_TO_PATH.get(name);
	}

	/**
	 * @return Map of language names to stylesheet paths.
	 */
	@Nonnull
	public static Map<String, String> getLanguages() {
		return NAME_TO_PATH;
	}

	/**
	 * @return Stylesheet for Java.
	 *
	 * @see RegexLanguages#getJavaLanguage()
	 */
	@Nonnull
	public static String getJavaStylesheet() {
		return SHEET_JAVA;
	}

	/**
	 * @return Stylesheet for JASM.
	 *
	 * @see RegexLanguages#getJasmLanguage()
	 */
	@Nonnull
	public static String getJasmStylesheet() {
		return SHEET_JASM;
	}

	/**
	 * @return Stylesheet for XML.
	 *
	 * @see RegexLanguages#getXmlLanguage()
	 */
	@Nonnull
	public static String getXmlStylesheet() {
		return SHEET_XML;
	}

	/**
	 * @return Stylesheet for Engima.
	 *
	 * @see RegexLanguages#getLangEngimaMap()
	 */
	@Nonnull
	public static String getEnigmaStylesheet() {
		return SHEET_ENIGMA;
	}
}
