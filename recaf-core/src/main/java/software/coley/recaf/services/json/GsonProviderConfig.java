package software.coley.recaf.services.json;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.observables.ObservableBoolean;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.BasicConfigValue;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.services.ServiceConfig;

/**
 * Config for {@link GsonProvider}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class GsonProviderConfig extends BasicConfigContainer implements ServiceConfig {
	private final ObservableBoolean prettyPrint = new ObservableBoolean(true);

	@Inject
	public GsonProviderConfig() {
		super(ConfigGroups.SERVICE_IO, GsonProvider.SERVICE_ID + CONFIG_SUFFIX);
		addValue(new BasicConfigValue<>("pretty-print", boolean.class, prettyPrint));
	}

	/**
	 * @return {@code true} to enable pretty printing with {@link GsonProvider}.
	 */
	@Nonnull
	public ObservableBoolean getPrettyPrint() {
		return prettyPrint;
	}
}
