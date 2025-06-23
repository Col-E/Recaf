package software.coley.recaf.services.info.summary;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.observables.ObservableBoolean;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.BasicConfigValue;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.services.ServiceConfig;

/**
 * Config for {@link ResourceSummaryService}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class ResourceSummaryServiceConfig extends BasicConfigContainer implements ServiceConfig {
	private final ObservableBoolean summerizeInOpen = new ObservableBoolean(true);

	@Inject
	public ResourceSummaryServiceConfig() {
		super(ConfigGroups.SERVICE_ANALYSIS, ResourceSummaryService.SERVICE_ID + CONFIG_SUFFIX);
		// Add values
		addValue(new BasicConfigValue<>("summerize-in-open",boolean.class, summerizeInOpen));
	}

	/**
	 * @return {@code true} when comments should be summarized in the open dialog.
	 */
	@Nonnull
	public ObservableBoolean getSummerizeInOpen() {
		return summerizeInOpen;
	}
}
