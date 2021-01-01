package me.coley.recaf;

import me.coley.recaf.decompile.DecompileManager;

/**
 * Wrapper of multiple services that are provided by a controller.
 * Placing them in here keeps the actual {@link Controller} class minimal.
 *
 * @author Matt Coley
 */
public class Services {
	private final DecompileManager decompileManager;

	/**
	 * Initialize services.
	 *
	 * @param controller
	 * 		Parent controller instance.
	 */
	Services(Controller controller) {
		decompileManager = new DecompileManager();
	}

	/**
	 * @return The decompiler manager.
	 */
	public DecompileManager getDecompileManager() {
		return decompileManager;
	}
}
