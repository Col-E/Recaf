package me.coley.recaf;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Base testing utilities.
 *
 * @author Matt
 */
public class Base {
	/**
	 * @param file
	 * 		Path to file in classpath.
	 *
	 * @return File reference.
	 *
	 * @throws IOException
	 * 		Thrown if the URL to the file could not be created.
	 */
	public static Path getClasspathFile(String file) throws IOException {
		return new File(URLDecoder.decode(getClasspathUrl(file).getFile(), "UTF-8")).toPath();
	}

	/**
	 * @param file
	 * 		Path to file in classpath.
	 *
	 * @return URL reference.
	 */
	public static URL getClasspathUrl(String file) {
		ClassLoader classLoader = Base.class.getClassLoader();
		return classLoader.getResource(file);
	}
}
