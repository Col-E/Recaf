package software.coley.recaf.services.assembler;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.observables.ObservableBoolean;
import software.coley.observables.ObservableInteger;
import software.coley.observables.ObservableString;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.BasicConfigValue;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.services.ServiceConfig;

/**
 * Common config for all assemblers.
 *
 * @author Justus Garbe
 */
@ApplicationScoped
public class AssemblerPipelineGeneralConfig extends BasicConfigContainer implements ServiceConfig {
	private final ObservableString disassemblyIndent = new ObservableString("    ");
	private final ObservableInteger disassemblyAstParseDelay = new ObservableInteger(100);
	private final ObservableBoolean useWholeFloatingNumbers = new ObservableBoolean(true);

	@Inject
	public AssemblerPipelineGeneralConfig() {
		super(ConfigGroups.SERVICE_ASSEMBLER, AssemblerPipelineManager.SERVICE_ID + ConfigGroups.PACKAGE_SPLIT + "general" + CONFIG_SUFFIX);

		addValue(new BasicConfigValue<>("disassembly-indent", String.class, disassemblyIndent));
		addValue(new BasicConfigValue<>("disassembly-ast-parse-delay", int.class, disassemblyAstParseDelay));
		addValue(new BasicConfigValue<>("disassembly-whole-floating", boolean.class, useWholeFloatingNumbers));
	}

	/**
	 * @return String of a single indentation level.
	 */
	@Nonnull
	public ObservableString getDisassemblyIndent() {
		return disassemblyIndent;
	}

	/**
	 * @return Delay between each parse operation.
	 */
	@Nonnull
	public ObservableInteger getDisassemblyAstParseDelay() {
		return disassemblyAstParseDelay;
	}

	/**
	 * @return {@code true} to prefer {@code 10000000000} over {@link 1e10}.
	 */
	@Nonnull
	public ObservableBoolean getUseWholeFloatingNumbers() {
		return useWholeFloatingNumbers;
	}
}
