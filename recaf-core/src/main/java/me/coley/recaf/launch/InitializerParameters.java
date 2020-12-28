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
	 * @return Default parameters.
	 */
	public static InitializerParameters fromDefault() {
		return new InitializerParameters(PresentationType.GUI);
	}

	/**
	 * @param args
	 * 		Startup parameters as string array.
	 *
	 * @return Startup parameters.
	 */
	public static InitializerParameters fromArgs(String[] args) {
		InitializerParameters baseline = fromDefault();
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
