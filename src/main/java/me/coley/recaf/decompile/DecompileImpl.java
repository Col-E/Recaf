package me.coley.recaf.decompile;

import com.strobel.Procyon;
import me.coley.recaf.control.Controller;
import me.coley.recaf.decompile.cfr.CfrDecompiler;
import me.coley.recaf.decompile.fernflower.FernFlowerDecompiler;
import me.coley.recaf.decompile.procyon.ProcyonDecompiler;
import me.coley.recaf.util.StringUtil;
import org.benf.cfr.reader.util.CfrVersionInfo;

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

	@Override
	public String toString() {
		switch(this) {
			case CFR:
				return "CFR";
			case FERNFLOWER:
				return "FernFlower";
			case PROCYON:
				return "Procyon";
			default:
				return StringUtil.toString(this);
		}
	}

	/**
	 * @return String representation of the decompiler with version number included.
	 */
	public String getNameAndVersion() {
		switch(this) {
			case CFR:
				return "CFR " + CfrVersionInfo.VERSION;
			case FERNFLOWER:
				return "FernFlower"; // Fernflower does not have an easily accessible version number...
			case PROCYON:
				return "Procyon " + Procyon.version();
			default:
				return StringUtil.toString(this);
		}
	}
}
