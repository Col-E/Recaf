package software.coley.recaf.services.assembler;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import me.darknet.assembler.compile.WriteLocalVariableFilter;
import software.coley.observables.ObservableBoolean;
import software.coley.observables.ObservableObject;
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
	private final ObservableBoolean simulateJvmCalls = new ObservableBoolean(true);
	private final ObservableBoolean tryRangeComments = new ObservableBoolean(true);
	private final ObservableObject<VariableEmissionMode> variableEmissionMode = new ObservableObject<>(VariableEmissionMode.EMIT_ALWAYS);


	@Inject
	public JvmAssemblerPipelineConfig() {
		super(ConfigGroups.SERVICE_ASSEMBLER, JvmAssemblerPipeline.SERVICE_ID + CONFIG_SUFFIX);
		addValue(new BasicConfigValue<>("value-analysis", boolean.class, valueAnalysis));
		addValue(new BasicConfigValue<>("simulate-jvm-calls", boolean.class, simulateJvmCalls));
		addValue(new BasicConfigValue<>("try-range-comments", boolean.class, tryRangeComments));
		addValue(new BasicConfigValue<>("variable-emission-mode", VariableEmissionMode.class, variableEmissionMode));
	}

	@Override
	public boolean isValueAnalysisEnabled() {
		return valueAnalysis.getValue();
	}

	@Override
	public boolean isSimulatingCommonJvmCalls() {
		return simulateJvmCalls.getValue();
	}

	/**
	 * @return {@code true} to emit comments for where try-catch ranges start/end/catch.
	 */
	public boolean emitTryRangeComments() {
		return tryRangeComments.getValue();
	}

	/**
	 * @return {@code true} to emit variable on assembling, {@code false} to not emit any variable information.
	 */
	public boolean doEmitVariableInfo() {
		return variableEmissionMode.getValue() == VariableEmissionMode.EMIT_ALWAYS;
	}

	/**
	 * Different strategies for when to emit variable information.
	 */
	public enum VariableEmissionMode {
		EMIT_ALWAYS,
		EMIT_NEVER
	}
}
