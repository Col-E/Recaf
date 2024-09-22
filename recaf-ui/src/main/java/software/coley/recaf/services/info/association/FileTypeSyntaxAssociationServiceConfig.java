package software.coley.recaf.services.info.association;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.collections.tuple.Pair;
import software.coley.observables.ObservableMap;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.BasicMapConfigValue;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.services.ServiceConfig;
import software.coley.recaf.ui.LanguageStylesheets;
import software.coley.recaf.ui.control.richtext.syntax.RegexLanguages;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Config for {@link FileTypeSyntaxAssociationService}.
 * <p>
 * Retains mapping of file extensions to language keys used by:
 * <ul>
 *     <li>{@link RegexLanguages#getLanguage(String)}</li>
 *     <li>{@link LanguageStylesheets#getLanguageStylesheet(String)}</li>
 * </ul>
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class FileTypeSyntaxAssociationServiceConfig extends BasicConfigContainer implements ServiceConfig {
	private final ExtensionMapping extensionsToLangKeys;

	@Inject
	public FileTypeSyntaxAssociationServiceConfig() {
		super(ConfigGroups.SERVICE_UI, FileTypeSyntaxAssociationService.SERVICE_ID + CONFIG_SUFFIX);

		extensionsToLangKeys = new ExtensionMapping(List.of(
				new Pair<>("java", "java"),
				new Pair<>("jasm", "jasm"),
				new Pair<>("xml", "xml"),
				new Pair<>("html", "xml"),
				new Pair<>("svg", "xml"),
				new Pair<>("enigma", "enigma")
		));
		addValue(new BasicMapConfigValue<>("extensions-to-langs", Map.class, String.class, String.class, extensionsToLangKeys));
	}

	/**
	 * @return Map of file extensions to {@link RegexLanguages} name keys.
	 *
	 * @see RegexLanguages#getLanguage(String)
	 */
	@Nonnull
	public ExtensionMapping getExtensionsToLangKeys() {
		return extensionsToLangKeys;
	}

	/**
	 * Maps file extensions to {@link RegexLanguages} entries.
	 */
	public static class ExtensionMapping extends ObservableMap<String, String, Map<String, String>> {
		public ExtensionMapping(@Nonnull List<Pair<String, String>> extensions) {
			this(extensions.stream().collect(Collectors.toMap(Pair::getLeft, Pair::getRight)));
		}

		public ExtensionMapping(@Nonnull Map<String, String> extensions) {
			super(extensions, HashMap::new);
		}
	}
}