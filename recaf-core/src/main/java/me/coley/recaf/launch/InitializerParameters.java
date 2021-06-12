package me.coley.recaf.launch;

import me.coley.recaf.presentation.PresentationType;

/**
 * Parameters for how Recaf should behave on initialization.
 *
 * @author Matt Coley
 */
public class InitializerParameters {
	private final PresentationType presentationType;

	/**
	 * @param presentationType
	 * 		Presentation type, graphical or headless.
	 */
	public InitializerParameters(PresentationType presentationType) {
		this.presentationType = presentationType;
	}

	/**
	 * @return Default parameters for UI usage.
	 */
	public static InitializerParameters fromDefaultUI() {
		return new InitializerParameters(PresentationType.GUI);
	}

	/**
	 * @return Default parameters for headless usage.
	 */
	public static InitializerParameters fromDefaultHeadless() {
		return new InitializerParameters(PresentationType.HEADLESS);
	}

	/**
	 * @return Default parameters for no presentation layer usage.
	 */
	public static InitializerParameters fromDefaultNoDisplay() {
		return new InitializerParameters(PresentationType.NONE);
	}

	/**
	 * @param args
	 * 		Startup parameters as string array.
	 *
	 * @return Startup parameters.
	 */
	public static InitializerParameters fromArgs(String[] args) {
		InitializerParameters baseline = fromDefaultUI();
		// TODO: Parse arguments and create modified copy
		return baseline;
	}

	/**
	 * @return Presentation UI type.
	 */
	public PresentationType getPresentationType() {
		return presentationType;
	}
}
