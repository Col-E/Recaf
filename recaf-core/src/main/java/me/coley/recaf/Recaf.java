package me.coley.recaf;


import me.coley.recaf.launch.InitializerParameters;
import me.coley.recaf.presentation.EmptyPresentation;
import me.coley.recaf.presentation.HeadlessPresentation;
import me.coley.recaf.presentation.JavaFXPresentation;
import me.coley.recaf.presentation.Presentation;
import me.coley.recaf.presentation.PresentationType;

/**
 * Entry point &amp; version constant.
 *
 * @author Matt Coley
 */
public final class Recaf {

	public static final String VERSION = Recaf.class.getPackage().getSpecificationVersion();
	private Controller controller;

	/**
	 * Main entry point.
	 *
	 * @param args
	 * 		Program arguments.
	 */
	public static void main(String[] args) {
		new Recaf().initialize(InitializerParameters.fromArgs(args));
	}

	/**
	 * Start Recaf.
	 *
	 * @param parameters
	 * 		Initialization parameters.
	 */
	public void initialize(InitializerParameters parameters) {
		// Create presentation layer
		PresentationType presentationType = parameters.getPresentationType();
		Presentation presentation;
		switch (presentationType) {
			case HEADLESS:
				presentation = new HeadlessPresentation();
				break;
			case GUI:
				presentation = new JavaFXPresentation();
				break;
			case NONE:
				presentation = new EmptyPresentation();
				break;
			default:
				throw new IllegalArgumentException("Illegal presentation type specified: " + presentationType.name());
		}
		// Setup controller with presentation implementation.
		controller = new Controller(presentation);
		// TODO: Additional controller setup
		// Initialize the presentation layer
		presentation.initialize(controller);
	}
}
