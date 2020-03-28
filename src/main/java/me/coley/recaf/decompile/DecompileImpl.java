package me.coley.recaf.decompile;

import me.coley.recaf.control.Controller;
import me.coley.recaf.decompile.cfr.CfrDecompiler;
import me.coley.recaf.decompile.fernflower.FernFlowerDecompiler;
import me.coley.recaf.decompile.procyon.ProcyonDecompiler;

import java.util.function.Function;

/**
 * Enumeration of implemented decompilers.
 *
 * @author Matt
 */
public enum DecompileImpl {
	CFR(CfrDecompiler::new),
	FERNFLOWER(FernFlowerDecompiler::new),
	PROCYON(ProcyonDecompiler::new);

	private final Function<Controller, Decompiler> supplier;

	DecompileImpl(Function<Controller, Decompiler> supplier) {
		this.supplier = supplier;
	}

	/**
	 * @param controller
	 * 		Controller to use.
	 *
	 * @return New decompiler instance of the type.
	 */
	public Decompiler create(Controller controller) {
		return supplier.apply(controller);
	}
}
