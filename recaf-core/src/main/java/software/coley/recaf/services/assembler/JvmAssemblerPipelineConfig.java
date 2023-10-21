package software.coley.recaf.services.assembler;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.services.ServiceConfig;

/**
 * Config for {@link JvmAssemblerPipeline}.
 *
 * @author Justus Garbe
 */
@ApplicationScoped
public class JvmAssemblerPipelineConfig extends BasicConfigContainer implements ServiceConfig, AssemblerPipelineConfig {
	@Inject
	public JvmAssemblerPipelineConfig() {
		super(ConfigGroups.SERVICE_ASSEMBLER, JvmAssemblerPipeline.SERVICE_ID + CONFIG_SUFFIX);
	}
}
