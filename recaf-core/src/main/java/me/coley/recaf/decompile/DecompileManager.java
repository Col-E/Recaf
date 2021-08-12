package me.coley.recaf.decompile;

import me.coley.recaf.decompile.cfr.CfrDecompiler;
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
	}
}
