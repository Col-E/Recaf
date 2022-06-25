package me.coley.recaf.config.container;

import me.coley.recaf.assemble.ast.PrintContext;
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
	@Group("format")
	@ConfigID("prefix")
	public boolean usePrefix = true;
	@Group("validation")
	@ConfigID("ast")
	public boolean astValidation = true;
	@Group("validation")
	@ConfigID("bytecode")
	public boolean bytecodeValidation = true;
	@Group("debug")
	@ConfigID("ast-debug")
	public boolean astDebug = false;

	@Override
	public String iconPath() {
		return Icons.COMPILE;
	}

	@Override
	public String internalName() {
		return "conf.assembler";
	}

	/**
	 * @return Print context, prefix set if {@link #usePrefix} is active.
	 */
	public PrintContext createContext() {
		return new PrintContext(usePrefix ? "." : "");
	}
}
