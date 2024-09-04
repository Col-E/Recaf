package software.coley.recaf.services.assembler;

import software.coley.recaf.config.ConfigContainer;

/**
 * Assembler pipeline config outline.
 *
 * @author Justus Garbe
 * @see AndroidAssemblerPipelineConfig
 * @see JvmAssemblerPipelineConfig
 */
public interface AssemblerPipelineConfig extends ConfigContainer {
	/**
	 * @return {@code true} when the assembler's analyzer should use more detailed frame models which include
	 * values for primitives and strings where possible. {@code false} to only track the expected type of values
	 * and nothing else.
	 */
	boolean isValueAnalysisEnabled();

	/**
	 * Requires {@link #isValueAnalysisEnabled()} be {@code true}.
	 *
	 * @return {@code true} to enhance value enabled analysis with the ability to look up values of fields and methods
	 * of known calls. Usually this is for supplying constants like {@link Integer#MAX_VALUE} and such to the analyzer.
	 * {@code false} to disable value content simulation for all field and method references.
	 */
	boolean isSimulatingCommonJvmCalls();
}
