package me.coley.recaf.config;

/**
 * Assembler configuration.
 *
 * @author Matt
 */
public class ConfAssembler extends Config {
	/**
	 * Verify method bytecode. This also allows better variable type analysis.
	 */
	@Conf("assembler.verify")
	public boolean verify = true;
	/**
	 * Save variable debug information in the assembled method.
	 */
	@Conf("assembler.variables")
	public boolean variables = true;
	/**
	 * Use existing variable debug information instead of recalculating indices and types.
	 */
	@Conf("assembler.useexistingdata")
	public boolean useExistingData = true;
	/**
	 * Strip debug from class before it is sent to the disassembler.
	 * Useful when garbage data is inserted into debug attributes.
	 */
	@Conf("assembler.stripdebug")
	public boolean stripDebug;
	/**
	 * Check to determine if phantom classes should be generated.
	 */
	@Conf("assembler.phantoms")
	public boolean phantoms = true;

	ConfAssembler() {
		super("assembler");
	}
}
