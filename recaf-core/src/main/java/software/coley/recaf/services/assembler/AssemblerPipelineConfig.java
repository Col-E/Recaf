package software.coley.recaf.services.assembler;

import software.coley.recaf.config.ConfigContainer;

/**
 * Assembler pipeline config outline.
 *
 * @author Justus Garbe
 */
public interface AssemblerPipelineConfig extends ConfigContainer {
	boolean isValueAnalysisEnabled();
}
