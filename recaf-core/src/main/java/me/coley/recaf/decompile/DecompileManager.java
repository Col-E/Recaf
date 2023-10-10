package me.coley.recaf.decompile;

import me.coley.recaf.decompile.cfr.CfrDecompiler;
import me.coley.recaf.decompile.fallback.FallbackDecompiler;
import me.coley.recaf.decompile.jadx.JadxDecompiler;
import me.coley.recaf.decompile.procyon.ProcyonDecompiler;
import me.coley.recaf.decompile.vine.VineFlowerDecompiler;
import me.coley.recaf.plugin.tools.ToolManager;

/**
 * Manager of decompilers.
 *
 * @author Matt Coley
 */
public class DecompileManager extends ToolManager<Decompiler> {
	/**
	 * Initialize the decompiler manager with local decompiler implementations.
	 */
	public DecompileManager() {
		register(new CfrDecompiler());
		register(new JadxDecompiler());
		register(new VineFlowerDecompiler());
		register(new ProcyonDecompiler());
		register(new FallbackDecompiler());
	}
}
