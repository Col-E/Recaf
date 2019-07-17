package me.coley.recaf;

import org.junit.jupiter.api.BeforeAll;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;

/**
 * Base testing utilities.
 *
 * @author Matt
 */
public class Base {
	@BeforeAll
	public static void setupLogging() {
		Recaf.setupLogging();
	}

	/**
	 * @param file
	 * 		Path to file in classpath.
	 *
	 * @return File reference.
	 *
	 * @throws IOException
	 * 		Thrown if the URL to the file could not be created.
	 */
	public File getClasspathFile(String file) throws IOException {
		ClassLoader classLoader = Base.class.getClassLoader();
		return new File(URLDecoder.decode(classLoader.getResource(file).getFile(), "UTF-8"));
	}
}
