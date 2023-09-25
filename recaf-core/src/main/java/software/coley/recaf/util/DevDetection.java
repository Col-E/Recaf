package software.coley.recaf.util;

import java.io.File;

/**
 * Basic util to detect if Recaf is running in a developer environment.
 *
 * @author Matt Coley
 */
public class DevDetection {
	/**
	 * @return {@code true} when a developer environment is detected.
	 */
	public static boolean isDevEnv() {
		// Should only be true when building Recaf from source/build-system.
		String path = System.getProperty("java.class.path");
		return path.contains("recaf-core" + File.separator + "build") ||
				path.contains("recaf-core" + File.separator + "out") ||
				path.contains("recaf-ui" + File.separator + "build") ||
				path.contains("recaf-ui" + File.separator + "out");
	}
}
