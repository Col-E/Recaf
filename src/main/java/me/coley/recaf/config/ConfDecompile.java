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
	 * Strip debug from class before it is sent to the decompiler.
	 * Useful when garbage data is inserted into debug attributes.
	 */
	@Conf("decompile.stripdebug")
	public boolean stripDebug;

	/**
	 * Determine if the decompiler name/version should be output.
	 */
	@Conf("decompile.showname")
	public boolean showName = true;

	/**
	 * Time to wait before aborting the decompile process.
	 */
	@Conf("decompile.timeout")
	public long timeout = 9000;

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
