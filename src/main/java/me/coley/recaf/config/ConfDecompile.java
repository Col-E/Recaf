package me.coley.recaf.config;

import me.coley.recaf.decompile.DecompileImpl;
import me.coley.recaf.ui.controls.view.ClassViewport;
import me.coley.recaf.ui.controls.view.FileViewport;
import me.coley.recaf.util.LangUtil;

/**
 * Decompile configuration.
 *
 * @author Matt
 */
public class ConfDecompile extends Config {
	/**
	 * Decompiler to use.
	 */
	@Conf("decompile.decompiler")
	public DecompileImpl decompiler = DecompileImpl.CFR;

	// ============================ CFR OPTIONS ============================ //

	// TODO: Add cfr options

	// ========================= FERNFLOWER OPTIONS ======================== //

	// TODO: Add ff options

	ConfDecompile() {
		super("decompiler");
	}
}
