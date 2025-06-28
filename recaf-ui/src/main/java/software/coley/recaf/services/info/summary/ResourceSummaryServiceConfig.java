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
	private final ObservableBoolean summarizeOnOpen = new ObservableBoolean(true);

	@Inject
	public ResourceSummaryServiceConfig() {
		super(ConfigGroups.SERVICE_ANALYSIS, ResourceSummaryService.SERVICE_ID + CONFIG_SUFFIX);
		// Add values
		addValue(new BasicConfigValue<>("summarize-on-open", boolean.class, summarizeOnOpen));
	}

	/**
	 * @return {@code true} when workspace contents should be summarized on opening a new workspace.
	 */
	@Nonnull
	public ObservableBoolean getSummarizeOnOpen() {
		return summarizeOnOpen;
	}
}
