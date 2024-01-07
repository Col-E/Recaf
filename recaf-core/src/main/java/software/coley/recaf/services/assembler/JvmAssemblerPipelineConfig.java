package software.coley.recaf.services.assembler;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.observables.ObservableBoolean;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.BasicConfigValue;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.services.ServiceConfig;

/**
 * Config for {@link JvmAssemblerPipeline}.
 *
 * @author Justus Garbe
 */
@ApplicationScoped
public class JvmAssemblerPipelineConfig extends BasicConfigContainer implements ServiceConfig, AssemblerPipelineConfig {
	private final ObservableBoolean valueAnalysis = new ObservableBoolean(true);

	@Inject
	public JvmAssemblerPipelineConfig() {
		super(ConfigGroups.SERVICE_ASSEMBLER, JvmAssemblerPipeline.SERVICE_ID + CONFIG_SUFFIX);
		addValue(new BasicConfigValue<>("value-analysis", boolean.class, valueAnalysis));
	}

	@Override
	public boolean isValueAnalysisEnabled() {
		return valueAnalysis.getValue();
	}
}
