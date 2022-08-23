package me.coley.recaf.config.container;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.config.ConfigContainer;
import me.coley.recaf.config.ConfigID;
import me.coley.recaf.config.Group;
import me.coley.recaf.config.bounds.IntBounds;
import me.coley.recaf.config.bounds.IntLowerBound;
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
	 * Flag to escape unicode in decompilation results.
	 */
	@Group("filter")
	@ConfigID("escapeunicode")
	public boolean escapeUnicode = true;

	/**
	 * Setting for {@link me.coley.recaf.code.ClassInfo#maxOuterDepth},
	 * which sets the maximal amount of allowed outer classes to circumvent possible obfuscations.
	 * -1 for disabling check.
	 */
	@Group("classfile")
	@ConfigID("maxouterdepth")
	@IntLowerBound(-1)
	public IntegerProperty maxOuterClassDepth = new SimpleIntegerProperty(20);

	@Override
	public void onLoad() {
		maxOuterClassDepth.addListener((observable, oldValue, newValue) -> ClassInfo.maxOuterDepth = newValue.intValue());
		ClassInfo.maxOuterDepth = maxOuterClassDepth.get();
	}

	@Override
	public String iconPath() {
		return Icons.DECOMPILE;
	}

	@Override
	public String internalName() {
		return "conf.decompiler";
	}
}
