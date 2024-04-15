package software.coley.recaf.services.translation;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.observables.ObservableObject;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.BasicConfigValue;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.util.Lang;

/**
 * Config to specify which language to use.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class LangConfig extends BasicConfigContainer {
	private final ObservableObject<SupportedLanguage> currentLanguage = new ObservableObject<>(SupportedLanguage.en_US);

	@Inject
	public LangConfig() {
		super(ConfigGroups.SERVICE_UI, "language" + CONFIG_SUFFIX);
		addValue(new BasicConfigValue<>("current", SupportedLanguage.class, currentLanguage));
		currentLanguage.addChangeListener((ob, old, cur) -> Lang.setCurrentTranslations(cur.name()));
	}

	/**
	 * @return Current translation language file name.
	 */
	@Nonnull
	public ObservableObject<SupportedLanguage> getCurrentLanguage() {
		return currentLanguage;
	}

	/**
	 * Supported languages.
	 */
	public enum SupportedLanguage {
		cs_CZ,
		de_DE,
		en_US,
		sv_SE,
		zh_CN
	}
}
