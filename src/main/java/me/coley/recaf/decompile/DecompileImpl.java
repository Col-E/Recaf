package me.coley.recaf.decompile;

import me.coley.recaf.decompile.cfr.CfrDecompiler;
import me.coley.recaf.decompile.fernflower.FernFlowerDecompiler;

import java.util.function.Supplier;

/**
 * Enumeration of implemented decompilers.
 *
 * @author Matt
 */
public enum DecompileImpl {
	CFR(CfrDecompiler::new),
	FF(FernFlowerDecompiler::new);

	private final Supplier<Decompiler> supplier;

	DecompileImpl(Supplier<Decompiler> supplier) {
		this.supplier = supplier;
	}

	/**
	 * @return New decompiler instance of the type.
	 */
	public Decompiler create() {
		return supplier.get();
	}
}
