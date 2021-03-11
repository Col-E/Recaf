package me.coley.recaf.ui.util;

import me.coley.recaf.util.JavaVersion;
import me.coley.recaf.util.logging.Logging;
import org.slf4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

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
		try {
			Method m = Class.forName(getPlatformClassName()).getDeclaredMethod("startup", Runnable.class);
			m.setAccessible(true);
			m.invoke(null, (Runnable) JFXUtils::onInitializePlatform);
		} catch (NoSuchMethodException ex) {
			throw new IllegalStateException("javafx.application.Platform.startup(Runnable) is missing", ex);
		} catch (IllegalAccessException ex) {
			throw new IllegalStateException("'startup' became inaccessible", ex);
		} catch (InvocationTargetException ex) {
			throw new IllegalStateException("Unable to initialize toolkit", ex.getTargetException());
		} catch (ClassNotFoundException ex) {
			throw new IllegalStateException("Platform class does not contain 'startup' method", ex);
		}
	}

	/**
	 * @return Platform class name.
	 */
	public static String getPlatformClassName() {
		return JavaVersion.get() >= 9 ? BASE_PLATFORM : "com.sun.javafx.application.PlatformImpl";
	}

	/**
	 * Runnable to log information about the initialized environment.
	 */
	private static void onInitializePlatform() {
		logger.debug("JavaFX platform initialized from: " + getPlatformClassName());
		initialized = true;
	}

	/**
	 * Run a given runnable without explicitly depending on the JavaFX platform class.
	 * This prevents direct references from loading the class when it potentially is not yet added to the classpath.
	 *
	 * @param runnable
	 * 		Action to run.
	 */
	public static void runSafe(Runnable runnable) {
		try {
			Class.forName(BASE_PLATFORM).getDeclaredMethod("runLater", Runnable.class).invoke(null, runnable);
		} catch (ReflectiveOperationException ex) {
			logger.error("Failed to execute 'Platform.runLater(Runnable)' via reflection", ex);
		}
	}

	/**
	 * Shutdown the JavaFX Platform.
	 * This prevents direct references from loading the class when it potentially is not yet added to the classpath.
	 */
	public static void shutdownSafe() {
		try {
			Class.forName(BASE_PLATFORM).getDeclaredMethod("exit").invoke(null);
		} catch (ReflectiveOperationException ex) {
			logger.error("Failed to execute 'Platform.exit()' via reflection", ex);
		}
	}
}
