package me.coley.recaf.config.container;

import me.coley.recaf.config.ConfigContainer;
import me.coley.recaf.config.ConfigID;
import me.coley.recaf.config.Group;
import me.coley.recaf.ui.util.Icons;

/**
 * Config container for assembler values.
 *
 * @author Matt Coley
 */
public class AssemblerConfig implements ConfigContainer {
	@Group("validation")
	@ConfigID("ast")
	public boolean astValidation = true;
	@Group("validation")
	@ConfigID("bytecode")
	public boolean bytecodeValidation = true;

	@Override
	public String iconPath() {
		return Icons.COMPILE;
	}

	@Override
	public String internalName() {
		return "conf.assembler";
	}
}
