package me.coley.recaf;

import me.coley.recaf.launch.InitializerParameters;
import me.coley.recaf.presentation.Presentation;
import me.coley.recaf.presentation.PresentationType;

/**
 * Entry point.
 *
 * @author Matt Coley
 */
public final class Recaf {
	private Controller controller;

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
		try {
			presentation = presentationType.createInstance();
		} catch (ReflectiveOperationException ex) {
			throw new IllegalStateException("Failed to initialize presentation layer: " + presentationType.name());
		}
		// Setup controller with presentation implementation.
		controller = new Controller(presentation);
		// TODO: Additional controller setup
		// Initialize the presentation layer
		presentation.initialize(controller);
	}
}
