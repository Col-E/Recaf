package me.coley.recaf.config.impl;

import me.coley.recaf.Recaf;
import me.coley.recaf.config.Config;

public class ConfUI extends Config {
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
	 * Show member windows even if class has none.
	 */
	public boolean showEmptyMemberWindows = true;
	/**
	 * Show attributes that should not be messed with or are uncommonly used
	 * such as:
	 * <ul>
	 * <li>Enclosing method</li>
	 * <li>InnerClasses</li>
	 * <li>SourceDebug</li>
	 * </ul>
	 */
	public boolean showUncommonAttributes = true;
	/**
	 * Display variable's signature in the opcode edit window for variable
	 * opcodes. Allows editing of signatures <i>(Generic types)</i> and
	 * significantly increases the edit window size.
	 */
	public boolean showVariableSignatureInTable;
	/**
	 * Max length for text in ldc opcodes to be displayed.
	 */
	public int ldcMaxLength = 125;
	/**
	 * Language to show GUI in.
	 */
	public String language = "en";
	/**
	 * The look and feel to apply to Recaf on launch.
	 */
	private String lookAndFeel = "javax.swing.plaf.nimbus.NimbusLookAndFeel";

	public ConfUI() {
		super("rcinterface");
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
		if (Recaf.INSTANCE.ui != null) {
			Recaf.INSTANCE.ui.refreshLAF(lookAndFeel);
		}
	}

}