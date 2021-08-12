package me.coley.recaf.config.container;

import me.coley.recaf.config.ConfigContainer;
import me.coley.recaf.config.ConfigID;
import me.coley.recaf.config.Group;
import me.coley.recaf.ui.util.Lang;

/**
 * Config container for editor values.
 *
 * @author Matt Coley
 */
public class EditorConfig implements ConfigContainer {
	/**
	 * Show types of fields and methods in the {@link me.coley.recaf.ui.panel.OutlinePanel}.
	 */
	@Group("outline")
	@ConfigID("showoutlinedtypes")
	public boolean showOutlinedTypes = false;

	/**
	 * Show synthetic fields and methods in the {@link me.coley.recaf.ui.panel.OutlinePanel}.
	 */
	@Group("outline")
	@ConfigID("showoutlinedsynths")
	public boolean showOutlinedSynthetics = false;

	/**
	 * Preferred decompiler implementation.
	 * See {@link me.coley.recaf.decompile.DecompileManager}.
	 */
	@Group("decompile")
	@ConfigID("implementation")
	public String decompiler = "CFR";

	@Override
	public String displayName() {
		return Lang.get(internalName());
	}

	@Override
	public String internalName() {
		return "conf.editor";
	}
}
