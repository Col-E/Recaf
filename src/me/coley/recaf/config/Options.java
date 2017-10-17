package me.coley.recaf.config;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import me.coley.recaf.Recaf;

public class Options extends Config {
	/**
	 * Show confirmation prompt on doing potentially dangerous things.
	 */
	public boolean confirmDeletions = true;
	/**
	 * Show extra jump information.
	 */
	public boolean opcodeShowJumpHelp = true;
	/**
	 * Simplify descriptor displays on the opcode list.
	 */
	public boolean opcodeSimplifyDescriptors = true;
	/**
	 * Display variable's signature in the opcode edit window for variable
	 * opcodes. Allows editing of signatures <i>(Generic types)</i> and
	 * significantly increases the edit window size.
	 */
	public boolean showVariableSignatureInTable;
	/**
	 * Flags for reading in classes.
	 */
	public int classFlagsInput = ClassReader.EXPAND_FRAMES;
	/**
	 * Flags for writing classes.
	 */
	public int classFlagsOutput = ClassWriter.COMPUTE_FRAMES;
	/**
	 * Max length for text in ldc opcodes to be displayed.
	 */
	public int ldcMaxLength = 125;
	/**
	 * The look and feel to apply to Recaf on launch.
	 */
	private String lookAndFeel = "javax.swing.plaf.nimbus.NimbusLookAndFeel";

	public Options() {
		super("rcoptions");
	}

	/**
	 * @return The current look and feel.
	 */
	public String getLookAndFeel() {
		return lookAndFeel;
	}

	/**
	 * Set the current look and feel.
	 * 
	 * @param lookAndFeel
	 *            Look and feel to set.
	 */
	public void setLookAndFeel(String lookAndFeel) {
		this.lookAndFeel = lookAndFeel;
		// Refresh the UI if already loaded
		if (Recaf.INSTANCE.gui != null) {
			Recaf.INSTANCE.gui.refreshLAF(lookAndFeel);
		}
	}

}
