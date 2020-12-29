package me.coley.recaf;

import me.coley.recaf.launch.InitializerParameters;
import me.coley.recaf.presentation.PresentationType;

public class RecafUI {
	/**
	 * Main entry point.
	 *
	 * @param args
	 * 		Program arguments.
	 */
	public static void main(String[] args) {
		//InitializerParameters parameters = InitializerParameters.fromArgs(args);
		InitializerParameters parameters = new InitializerParameters(PresentationType.HEADLESS);
		new Recaf().initialize(parameters);
	}
}
