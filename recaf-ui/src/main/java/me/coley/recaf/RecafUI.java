package me.coley.recaf;

import me.coley.recaf.launch.InitializerParameters;
import me.coley.recaf.presentation.PresentationType;

/**
 * Main entry point for running Recaf with a UI.
 *
 * @author Matt Coley
 */
public class RecafUI {
	/**
	 * Main entry point.
	 *
	 * @param args
	 * 		Program arguments.
	 */
	public static void main(String[] args) {
		//InitializerParameters parameters = InitializerParameters.fromArgs(args);
		InitializerParameters parameters = new InitializerParameters(PresentationType.NONE);
		new Recaf().initialize(parameters);
	}
}
