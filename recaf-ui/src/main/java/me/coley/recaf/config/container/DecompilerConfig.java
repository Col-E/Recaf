package me.coley.recaf.config.container;

import me.coley.recaf.config.ConfigContainer;
import me.coley.recaf.config.ConfigID;
import me.coley.recaf.config.Group;
import me.coley.recaf.config.IntBounds;
import me.coley.recaf.ui.DiffViewMode;
import me.coley.recaf.ui.util.Icons;

/**
 * Config container for decompiler values.
 *
 * @author Matt Coley
 */
public class DecompilerConfig implements ConfigContainer {
	/**
	 * Preferred decompiler implementation.
	 * See {@link me.coley.recaf.decompile.DecompileManager}.
	 */
	@Group("general")
	@ConfigID("impl")
	public String decompiler = "CFR";

	/**
	 * Enable decompiler timeout.
	 */
	@Group("general")
	@ConfigID("enabletimeout")
	public boolean enableDecompilerTimeout = true;

	/**
	 * Time to wait until cancelling a decompile for taking too long.
	 */
	@IntBounds(min = 1000, max = 20000)
	@Group("general")
	@ConfigID("timeout")
	public int decompileTimeout = 10_000;

	/**
	 * Flag to strip variable debug info from classes before decompiling.
	 */
	@Group("filter")
	@ConfigID("vars")
	public boolean filterVars;

	/**
	 * Flag to strip variable debug info from classes before decompiling.
	 */
	@Group("filter")
	@ConfigID("generics")
	public boolean filterGenerics;

	/**
	 * Flag to strip synthetic flags from classes members.
	 */
	@Group("filter")
	@ConfigID("synthetics")
	public boolean filterSynthetics;

	/**
	 * Diff-viewer class representation.
	 */
	@Group("diff")
	@ConfigID("diff-view-mode")
	public DiffViewMode diffViewMode = DiffViewMode.CLASS;

	@Override
	public String iconPath() {
		return Icons.DECOMPILE;
	}

	@Override
	public String internalName() {
		return "conf.decompiler";
	}
}
