package me.coley.recaf.util.self;

import me.coley.recaf.Recaf;
import me.coley.recaf.util.ClasspathUtil;
import me.coley.recaf.util.Log;
import me.coley.recaf.util.OSUtil;
import me.coley.recaf.util.VMUtil;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

import static javax.swing.JOptionPane.*;

/**
 * Utility for patching self when missing dependencies.
 *
 * @author Matt
 */
public class SelfDependencyPatcher {
	private static final Path DEPENDENCIES_DIR_PATH = Recaf.getDirectory("dependencies");
	private static final Map<Integer, List<String>> JFX_DEPENDENCIES = new LinkedHashMap<Integer, List<String>>(4, 1F) {
		{
			put(14, Arrays.asList(
					jfxUrl("media", "14.0.2"),
					jfxUrl("controls", "14.0.2"),
					jfxUrl("graphics", "14.0.2"),
					jfxUrl("base", "14.0.2")
			));
			put(13, Arrays.asList(
					jfxUrl("media", "13.0.2"),
					jfxUrl("controls", "13.0.2"),
					jfxUrl("graphics", "13.0.2"),
					jfxUrl("base", "13.0.2")
			));
			put(12, Arrays.asList(
					jfxUrl("media", "12.0.2"),
					jfxUrl("controls", "12.0.2"),
					jfxUrl("graphics", "12.0.2"),
					jfxUrl("base", "12.0.2")
			));
			put(11, Arrays.asList(
					jfxUrl("media", "11.0.2"),
					jfxUrl("controls", "11.0.2"),
					jfxUrl("graphics", "11.0.2"),
					jfxUrl("base", "11.0.2")
			));
		}
	};

	/**
	 * Patch in any missing dependencies, if any.
	 */
	public static void patch() {
		// Do nothing if JavaFX is detected
		try {
			if (ClasspathUtil.classExists("javafx.embed.swing.JFXPanel"))
				return;
		} catch(UnsupportedClassVersionError error) {
			// Loading the JavaFX class was unsupported.
			// We are probably on 8 and its on 11
			showIncompatibleVersion();
			return;
		}
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
		if (VMUtil.getVmVersion() < 11) {
			showIncompatibleVersion();
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
	 * Display a message detailing why self-patching cannot continue.
	 */
	private static void showIncompatibleVersion() {
		String message = "Recaf cannot self-patch below Java 11 on this JVM. " +
				"Please run using JDK 11 or higher or use a JDK that bundles JavaFX.\n" +
				" - Your JDK does not bundle JavaFX\n" +
				" - Downloadable JFX bundles only come with 11 support or higher.";
		if (!Recaf.isHeadless())
			showMessageDialog(null, message, "Error: Cannot self-patch", ERROR_MESSAGE);
		// Log and exit
		Log.error(message);
		System.exit(-1);
	}

	/**
	 * Download dependencies.
	 *
	 * @throws IOException
	 * 		When the files cannot be fetched or saved.
	 */
	private static void fetchDependencies() throws IOException {
		// Get dir to store dependencies in
		Path dependenciesDir = DEPENDENCIES_DIR_PATH;
		if (!Files.isDirectory(dependenciesDir)) {
			Files.createDirectories(dependenciesDir);
		}
		// Download each dependency
		OSUtil os = OSUtil.getOSType();
		List<String> dependencies = getLatestDependencies();
		for(String dependencyPattern : dependencies) {
			String dependencyUrlPath = String.format(dependencyPattern, os.getMvnName());
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
		return files.length >= getLatestDependencies().size();
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
	 * @param component
	 * 		Name of the component.
	 *
	 * @return Formed URL for the component.
	 */
	private static String jfxUrl(String component, String version) {
		// Add platform specific identifier to the end.
		return String.format("https://repo1.maven.org/maven2/org/openjfx/javafx-%s/%s/javafx-%s-%s",
				component, version, component, version) + "-%s.jar";
	}

	/**
	 * @return Latest JavaFX supported version for.
	 */
	private static int getLatestSupportedJfxVersion() {
		int version = VMUtil.getVmVersion();
		while (version >= 11) {
			List<String> dependencies = JFX_DEPENDENCIES.get(version);
			if (dependencies != null)
				return version;
			version--;
		}
		throw new AssertionError("Failed to get latest supported JFX version");
	}

	/**
	 * @return JavaFX dependencies list for the current VM version.
	 */
	private static List<String> getLatestDependencies() {
		int version = getLatestSupportedJfxVersion();
		if (version >= 11) {
			return JFX_DEPENDENCIES.get(version);
		}
		throw new AssertionError("Failed to get latest JFX artifact urls");
	}
}
