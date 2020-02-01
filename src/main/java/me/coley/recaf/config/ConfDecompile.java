package me.coley.recaf.config;

import me.coley.recaf.decompile.DecompileImpl;

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

	// ========================= PROCYON OPTIONS ======================== //

	// TODO: Add procyon options

	ConfDecompile() {
		super("decompiler");
	}
}
