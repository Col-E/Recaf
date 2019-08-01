package me.coley.recaf;

import org.junit.jupiter.api.BeforeAll;
import org.pmw.tinylog.Configurator;
import org.pmw.tinylog.writers.FileWriter;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;

/**
 * Base testing utilities.
 *
 * @author Matt
 */
public class Base {
	@BeforeAll
	public static void setupLogging() {
		// We want our normal logging, but without file-writing.
		Configurator.defaultConfig()
				.formatPattern("{level}-{date}: {message|indent=4}")
				.writingThread(true)
				.activate();
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
	public static File getClasspathFile(String file) throws IOException {
		return new File(URLDecoder.decode(getClasspathUrl(file).getFile(), "UTF-8"));
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
