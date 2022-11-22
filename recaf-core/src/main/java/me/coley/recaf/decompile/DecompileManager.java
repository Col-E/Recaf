package me.coley.recaf.decompile;

import jakarta.enterprise.context.ApplicationScoped;
import me.coley.recaf.decompile.cfr.CfrDecompiler;
import me.coley.recaf.decompile.fallback.FallbackDecompiler;
import me.coley.recaf.decompile.jadx.JadxDecompiler;
import me.coley.recaf.decompile.procyon.ProcyonDecompiler;
import me.coley.recaf.decompile.quilt.QuiltFlowerDecompiler;
import me.coley.recaf.plugin.tools.ToolManager;

/**
 * Manager of decompilers.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class DecompileManager extends ToolManager<Decompiler> {
	/**
	 * Initialize the decompiler manager with local decompiler implementations.
	 */
	public DecompileManager() {
		register(new CfrDecompiler());
		register(new JadxDecompiler());
		register(new QuiltFlowerDecompiler());
		register(new ProcyonDecompiler());
		register(new FallbackDecompiler());
	}
}
