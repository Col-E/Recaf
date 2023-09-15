package software.coley.recaf.util;

import java.io.InputStream;

/**
 * Utility for classpath resources.
 *
 * @author Matt Coley
 */
public class ResourceUtil {
	/**
	 * Check if a resource exists in the current classpath.
	 *
	 * @param path
	 * 		Path to resource.
	 *
	 * @return {@code true} if resource exists. {@code false} otherwise.
	 */
	public static boolean resourceExists(String path) {
		if (!path.startsWith("/"))
			path = "/" + path;
		return ResourceUtil.class.getResource(path) != null;
	}

	/**
	 * Fetch a resource as a stream in the current classpath.
	 *
	 * @param path
	 * 		Path to resource.
	 *
	 * @return Stream of resource.
	 */
	public static InputStream resource(String path) {
		if (!path.startsWith("/"))
			path = "/" + path;
		return ResourceUtil.class.getResourceAsStream(path);
	}
}