package software.coley.recaf.services.navigation;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.observables.ObservableInteger;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.BasicConfigValue;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.services.ServiceConfig;

/**
 * Config for {@link NavigationHistoryService}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class NavigationHistoryConfig extends BasicConfigContainer implements ServiceConfig {
	private final ObservableInteger maxEntries = new ObservableInteger(50);

	@Inject
	public NavigationHistoryConfig() {
		super(ConfigGroups.SERVICE_UI, NavigationHistoryService.ID + CONFIG_SUFFIX);

		addValue(new BasicConfigValue<>("max-entries", int.class, maxEntries));
	}

	/**
	 * @return Maximum number of entries retained in navigation history.
	 */
	@Nonnull
	public ObservableInteger getMaxEntries() {
		return maxEntries;
	}
}
