package me.coley.recaf.util;

import javafx.application.Platform;
import me.coley.recaf.util.logging.Logging;
import org.slf4j.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * JavaFX utilities.
 *
 * @author xxDark
 * @author Matt Coley
 */
public class JFXUtils {
	private static final String BASE_PLATFORM = "javafx.application.Platform";
	private static final Logger logger = Logging.get(JFXUtils.class);
	private static final AtomicBoolean initialized = new AtomicBoolean();

	/**
	 * Initializes JavaFX platform.
	 *
	 * @param init
	 *      Startup runnable.
	 *
	 * @throws IllegalStateException
	 *      If platform is already initialized.
	 */
	public static void initializePlatform(Runnable init) {
		if (!initialized.compareAndSet(false, true)) {
			throw new IllegalStateException("Already initialized!");
		}
		Platform.startup(() -> {
			try {
				init.run();
			} finally {
				onInitializePlatform();
			}
		});
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
	}
}
