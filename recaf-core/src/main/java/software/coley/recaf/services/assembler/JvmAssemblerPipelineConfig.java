package software.coley.recaf.services.assembler;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import me.darknet.assembler.compile.JvmVariableMode;
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
	private final ObservableBoolean recycleConstPool = new ObservableBoolean(false);
	private final ObservableObject<JvmVariableMode> variableEmissionMode = new ObservableObject<>(JvmVariableMode.ALWAYS_WRITE);


	@Inject
	public JvmAssemblerPipelineConfig() {
		super(ConfigGroups.SERVICE_ASSEMBLER, JvmAssemblerPipeline.SERVICE_ID + CONFIG_SUFFIX);
		addValue(new BasicConfigValue<>("value-analysis", boolean.class, valueAnalysis));
		addValue(new BasicConfigValue<>("simulate-jvm-calls", boolean.class, simulateJvmCalls));
		addValue(new BasicConfigValue<>("try-range-comments", boolean.class, tryRangeComments));
		addValue(new BasicConfigValue<>("recycle-const-pool", boolean.class, recycleConstPool));
		addValue(new BasicConfigValue<>("variable-emission-mode", JvmVariableMode.class, variableEmissionMode));
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
	 * @return {@code true} to recycle the constant pool, {@code false} to not recycle and emit a new constant pool with each class.
	 */
	public boolean isRecycleConstPool() {
		return recycleConstPool.getValue();
	}

	/**
	 * @return Variable table emission mode.
	 */
	@Nonnull
	public JvmVariableMode getVariableTableMode() {
		return variableEmissionMode.getValue();
	}
}
