package software.coley.recaf.services.phantom;

import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.observables.ObservableBoolean;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.BasicConfigValue;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.services.ServiceConfig;

/**
 * Config for {@link JPhantomGenerator}
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class JPhantomGeneratorConfig extends BasicConfigContainer implements ServiceConfig {
	private final ObservableBoolean generateWorkspacePhantoms = new ObservableBoolean(false);

	@Inject
	public JPhantomGeneratorConfig() {
		super(ConfigGroups.SERVICE_ANALYSIS, JPhantomGenerator.SERVICE_ID + CONFIG_SUFFIX);
		addValue(new BasicConfigValue<>("generate-workspace-phantoms", boolean.class, generateWorkspacePhantoms));
	}

	/**
	 * @return {@code true} to create and register {@link GeneratedPhantomWorkspaceResource} to newly opened workspaces.
	 */
	@Nullable
	public ObservableBoolean getGenerateWorkspacePhantoms() {
		return generateWorkspacePhantoms;
	}
}
