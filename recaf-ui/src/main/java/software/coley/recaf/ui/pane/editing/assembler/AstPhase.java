package software.coley.recaf.ui.pane.editing.assembler;

import software.coley.recaf.services.assembler.AssemblerPipeline;

import java.util.List;

/**
 * Phases of assembly going through a {@link AssemblerPipeline}.
 *
 * @author Matt Coley
 */
public enum AstPhase {
	/**
	 * @see AssemblerPipeline#concreteParse(List)
	 */
	ROUGH_PARTIAL,
	/**
	 * @see AssemblerPipeline#roughParse(List)
	 */
	ROUGH,
	/**
	 * @see AssemblerPipeline#concreteParse(List)
	 */
	CONCRETE_PARTIAL,
	/**
	 * @see AssemblerPipeline#concreteParse(List)
	 */
	CONCRETE
}
