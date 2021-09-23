package me.coley.recaf.config.container;

import me.coley.recaf.config.ConfigContainer;
import me.coley.recaf.config.ConfigID;
import me.coley.recaf.config.Group;

/**
 * Config container for dialog values, like recent file locations.
 *
 * @author Matt Coley
 */
public class DialogConfig implements ConfigContainer {
	/**
	 * Last location of a loaded file.
	 */
	@Group("fileprompt")
	@ConfigID("load")
	public String appLoadLocation = System.getProperty("user.dir");
	/**
	 * Last location of an exported file.
	 */
	@Group("fileprompt")
	@ConfigID("export")
	public String appExportLocation = System.getProperty("user.dir");
	/**
	 * Last location of a loaded mappings file.
	 */
	@Group("fileprompt")
	@ConfigID("loadmap")
	public String mapLoadLocation = System.getProperty("user.dir");
	/**
	 * Last location of an exported mappings file.
	 */
	@Group("fileprompt")
	@ConfigID("exportmap")
	public String mapExportLocation = System.getProperty("user.dir");

	@Override
	public String iconPath() {
		return null;
	}

	@Override
	public String internalName() {
		return "conf.dialog";
	}

	@Override
	public boolean isInternal() {
		return true;
	}
}
