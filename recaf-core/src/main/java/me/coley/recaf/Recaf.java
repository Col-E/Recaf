package me.coley.recaf;

import me.coley.recaf.launch.InitializerParameters;
import me.coley.recaf.presentation.Presentation;
import me.coley.recaf.presentation.PresentationType;
import me.coley.recaf.util.RuntimeProperties;
import me.coley.recaf.util.logging.Logging;
import org.slf4j.Logger;

/**
 * Entry point.
 *
 * @author Matt Coley
 */
public final class Recaf {
	private static final Logger logger = Logging.get(Recaf.class);
	private Controller controller;

	/**
	 * Start Recaf.
	 *
	 * @param parameters
	 * 		Initialization parameters.
	 */
	public void initialize(InitializerParameters parameters) {
		logger.debug("Initialize: Recaf-{} ({})", BuildConfig.VERSION, BuildConfig.GIT_SHA);
		RuntimeProperties.dump(logger);
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
		// Initialize the presentation layer
		presentation.initialize(controller);
	}

	/**
	 * @return Controller instance;
	 */
	public Controller getController() {
		return controller;
	}
}
