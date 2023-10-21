package software.coley.recaf.services.assembler;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.services.ServiceConfig;

/**
 * Config for {@link AndroidAssemblerPipeline}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class AndroidAssemblerPipelineConfig extends BasicConfigContainer implements ServiceConfig, AssemblerPipelineConfig {
	@Inject
	public AndroidAssemblerPipelineConfig() {
		super(ConfigGroups.SERVICE_ASSEMBLER, AndroidAssemblerPipeline.SERVICE_ID + CONFIG_SUFFIX);
	}
}
