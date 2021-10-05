package me.coley.recaf.config.container;

import me.coley.recaf.config.ConfigContainer;
import me.coley.recaf.config.ConfigID;
import me.coley.recaf.config.Group;
import me.coley.recaf.ui.util.Icons;

/**
 * Config container for compiler values.
 *
 * @author Matt Coley
 */
public class ExportConfig implements ConfigContainer {
	/**
	 * Flag to bundle libraries into the output.
	 */
	@Group("general")
	@ConfigID("shadeLibs")
	public boolean shadeLibs;
	/**
	 * Flag to compress the output <i>(When using archives)</i>.
	 */
	@Group("general")
	@ConfigID("compress")
	public boolean compress = true;

	@Override
	public String iconPath() {
		return Icons.SAVE;
	}

	@Override
	public String internalName() {
		return "conf.export";
	}
}
