package me.coley.recaf.util;

import javafx.application.Platform;
import me.coley.recaf.util.logging.Logging;
import org.slf4j.Logger;

/**
 * JavaFX utilities.
 *
 * @author xxDark
 * @author Matt Coley
 */
public class JFXUtils {
	private static final String BASE_PLATFORM = "javafx.application.Platform";
	private static final Logger logger = Logging.get(JFXUtils.class);
	private static boolean initialized;

	/**
	 * Initializes JavaFX platform.
	 */
	public static void initializePlatform() {
		// Skip if possible
		if (initialized) {
			return;
		}
		Platform.startup(JFXUtils::onInitializePlatform);
	}

	/**
	 * @return Platform class name.
	 */
	public static String getPlatformClassName() {
		return BASE_PLATFORM;
	}

	/**
	 * Runnable to log information about the initialized environment.
	 */
	private static void onInitializePlatform() {
		logger.debug("JavaFX platform initialized from: {}", getPlatformClassName());
		initialized = true;
	}
}
