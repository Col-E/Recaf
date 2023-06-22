package software.coley.recaf.workspace.io;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.services.ServiceConfig;

/**
 * Config for {@link InfoImporter}
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class InfoImporterConfig extends BasicConfigContainer implements ServiceConfig {
	@Inject
	public InfoImporterConfig() {
		super(ConfigGroups.SERVICE_IO, InfoImporter.SERVICE_ID + CONFIG_SUFFIX);
	}
}