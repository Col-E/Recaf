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

	// ============================ COMMON OPTIONS ============================ //

	/**
	 * Show synthetic members. Most decompilers will change formatting semantics as well with
	 * this, yielding less legible output.
	 */
	@Conf("decompile.showsynthetics")
	public boolean showSynthetic;

	/**
	 * Time to wait before aborting the decompile process.
	 */
	@Conf("decompile.timeout")
	public long timeout = 5000;

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
