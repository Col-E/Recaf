package software.coley.recaf.services.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.observables.ObservableString;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.BasicConfigValue;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.services.ServiceConfig;

/**
 * Config for {@link ConfigManager}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class ConfigManagerConfig extends BasicConfigContainer implements ServiceConfig {
	private final ObservableString currentProfile = new ObservableString(ConfigManager.DEFAULT_PROFILE_NAME);

	@Inject
	public ConfigManagerConfig() {
		super(ConfigGroups.SERVICE, ConfigManager.SERVICE_ID + CONFIG_SUFFIX);
		addValue(new BasicConfigValue<>("current-profile", String.class, currentProfile, true));
	}

	/**
	 * @return Current managed config profile name.
	 */
	public ObservableString getCurrentProfile() {
		return currentProfile;
	}
}
