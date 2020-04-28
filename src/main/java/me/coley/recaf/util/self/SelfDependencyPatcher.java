package me.coley.recaf.util.self;

import com.nqzero.permit.Permit;
import me.coley.recaf.Recaf;
import me.coley.recaf.util.ClasspathUtil;
import me.coley.recaf.util.Log;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static javax.swing.JOptionPane.*;

/**
 * Utility for patching self when missing dependencies.
 *
 * @author Matt
 */
public class SelfDependencyPatcher {
	private static final Path DEPENDENCIES_DIR_PATH = Recaf.getDirectory("dependencies");
	private static final String[] DEPENDENCIES = new String[] {
		"https://repo1.maven.org/maven2/org/openjfx/javafx-media/11.0.2/javafx-media-11.0.2-%s.jar",
		"https://repo1.maven.org/maven2/org/openjfx/javafx-controls/11.0.2/javafx-controls-11.0.2-%s.jar",
		"https://repo1.maven.org/maven2/org/openjfx/javafx-graphics/11.0.2/javafx-graphics-11.0.2-%s.jar",
		"https://repo1.maven.org/maven2/org/openjfx/javafx-base/11.0.2/javafx-base-11.0.2-%s.jar"
	};

	/**
	 * Patch in any missing dependencies, if any.
	 */
	public static void patch() {
		// Do nothing if JavaFX is detected
		if (ClasspathUtil.classExists("javafx.application.Platform"))
			return;
		Log.info("Missing JavaFX dependencies, attempting to patch in missing classes");
		// Check if dependencies need to be downloaded
		if (!hasCachedDependencies()) {
			Log.info(" - No local cache, downloading dependencies...");
			try {
				fetchDependencies();
			} catch(IOException ex) {
				Log.error(ex, "Failed to download dependencies!");
				System.exit(-1);
			}
		} else {
			Log.info(" - Local cache found!");
		}
		// Add the dependencies
		try {
			loadFromCache();
		} catch(IOException ex) {
			Log.error(ex, ex.getMessage());
			System.exit(-1);
		} catch(ReflectiveOperationException ex) {
			Log.error(ex, "Failed to add dependencies to classpath!");
			System.exit(-1);
		}
		Log.info(" - Done!");
	}


	/**
	 * Inject them into the current classpath.
	 *
	 * @throws IOException
	 * 		When the locally cached dependency urls cannot be resolved.
	 * @throws ReflectiveOperationException
	 * 		When the call to add these urls to the system classpath failed.
	 */
	private static void loadFromCache() throws IOException, ReflectiveOperationException {
		// So the problem with Java 8 is that some distributions DO NOT BUNDLE JAVAFX
		// Why is this a problem? OpenJFX does not come in public bundles prior to Java 11
		// So you're out of luck unless you change your JDK or update Java.
		if (getVersion() < 11) {
			String message = "Recaf cannot self-patch below Java 11. Please run using JDK 11 or higher.\n" +
					" - Your JDK does not bundle JavaFX\n" +
					" - Downloadable JFX bundles only come with 11 support or higher.";
			if (!Recaf.isHeadless())
				showMessageDialog(null, message, "Error: Cannot self-patch", ERROR_MESSAGE);
			// Log and exit
			Log.error(message);
			System.exit(-1);
			return;
		}
		// Otherwise we're free to download in Java 11+
		Log.info(" - Loading dependencies...");
		// Get Jar URLs
		List<URL> jarUrls = new ArrayList<>();
		Files.walk(DEPENDENCIES_DIR_PATH).forEach(path -> {
			try {
				jarUrls.add(path.toUri().toURL());
			} catch(MalformedURLException ex) {
				Log.error(ex, "Failed to convert '%s' to URL", path.toFile().getAbsolutePath());
			}
		});
		// Bypass the access system
		Permit.godMode();
		// Fetch UCP of application's ClassLoader
		// - ((ClassLoaders.AppClassLoader) ClassLoaders.appClassLoader()).ucp
		Class<?> clsClassLoaders = Class.forName("jdk.internal.loader.ClassLoaders");
		Object appClassLoader = clsClassLoaders.getDeclaredMethod("appClassLoader").invoke(null);
		Field fieldUCP = appClassLoader.getClass().getDeclaredField("ucp");
		fieldUCP.setAccessible(true);
		Object ucp = fieldUCP.get(appClassLoader);
		Class<?> clsUCP = ucp.getClass();
		Method addURL = clsUCP.getDeclaredMethod("addURL", URL.class);
		addURL.setAccessible(true);
		// Add each jar.
		for(URL url : jarUrls)
			addURL.invoke(ucp, url);
	}

	/**
	 * Download dependencies.
	 *
	 * @throws IOException
	 * 		When the files cannot be fetched or saved.
	 */
	private static void fetchDependencies() throws IOException {
		// Get dir to store dependencies in
		File dependenciesDir = DEPENDENCIES_DIR_PATH.toFile();
		if (!dependenciesDir.exists()) {
			dependenciesDir.mkdirs();
		}
		// Download each dependency
		String os = getOSType();
		for(String dependencyPattern : DEPENDENCIES) {
			String dependencyUrlPath = String.format(dependencyPattern, os);
			URL depURL = new URL(dependencyUrlPath);
			Path dependencyFilePath = DEPENDENCIES_DIR_PATH.resolve(getFileName(dependencyUrlPath));
			Files.copy(depURL.openStream(), dependencyFilePath, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	/**
	 * @return {@code true} when the dependencies directory has files in it.
	 */
	private static boolean hasCachedDependencies() {
		String[] files = DEPENDENCIES_DIR_PATH.toFile().list();
		if (files == null)
			return false;
		return files.length >= DEPENDENCIES.length;
	}

	/**
	 * @return Operating system short-hand name.
	 */
	private static String getOSType() {
		String s = System.getProperty("os.name").toLowerCase(Locale.ROOT);
		if (s.contains("win")) {
			return "win";
		}
		if (s.contains("mac") || s.contains("osx")) {
			return "mac";
		}
		return "linux";
	}

	/**
	 * @param url
	 * 		Full url path.
	 *
	 * @return Name of file at url.
	 */
	private static String getFileName(String url) {
		return url.substring(url.lastIndexOf('/') + 1);
	}

	/**
	 * @return Java version version.
	 */
	private static float getVersion() {
		return Float.parseFloat(System.getProperty("java.class.version")) - 44;
	}
}
