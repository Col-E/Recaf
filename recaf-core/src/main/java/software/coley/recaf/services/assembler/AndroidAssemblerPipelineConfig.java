package software.coley.recaf.services.assembler;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.observables.ObservableBoolean;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.BasicConfigValue;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.services.ServiceConfig;

/**
 * Config for {@link AndroidAssemblerPipeline}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class AndroidAssemblerPipelineConfig extends BasicConfigContainer implements ServiceConfig, AssemblerPipelineConfig {
	private final ObservableBoolean valueAnalysis = new ObservableBoolean(true);
	private final ObservableBoolean simulateJvmCalls = new ObservableBoolean(true);

	@Inject
	public AndroidAssemblerPipelineConfig() {
		super(ConfigGroups.SERVICE_ASSEMBLER, AndroidAssemblerPipeline.SERVICE_ID + CONFIG_SUFFIX);
		addValue(new BasicConfigValue<>("value-analysis", boolean.class, valueAnalysis));
		addValue(new BasicConfigValue<>("simulate-jvm-calls", boolean.class, simulateJvmCalls));
	}

	@Override
	public boolean isValueAnalysisEnabled() {
		return valueAnalysis.getValue();
	}

	@Override
	public boolean isSimulatingCommonJvmCalls() {
		return simulateJvmCalls.hasValue();
	}
}
