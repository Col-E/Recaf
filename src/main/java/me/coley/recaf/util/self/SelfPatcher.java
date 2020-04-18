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
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Utility for patching self when missing dependencies.
 *
 * @author Matt
 */
public class SelfPatcher {
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
		if (!hasCachedDependencies())
		{
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
	@SuppressWarnings({"unchecked", "rawtypes"})
	private static void loadFromCache() throws IOException, ReflectiveOperationException {
		Log.info(" - Loading dependencies...");
		Log.info(" - Disabling access system");
		// Bypass the access system
		Permit.godMode();
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
		// Fetch UCP fields/methods to update.call:
		// - List<URL> path
		// - Deque<URL> unopenedUrls
		// - List<Loader> loaders
		// - Map<String, Loader> lmap
		// - Loader getLoader(URL)
		Field fieldListPathUrls = clsUCP.getDeclaredField("path");
		Field fieldUnopenedUrls = clsUCP.getDeclaredField("unopenedUrls");
		Field fieldListLoaders = clsUCP.getDeclaredField("loaders");
		Field fieldMapUrlToLoader = clsUCP.getDeclaredField("lmap");
		Method methodGetLoader = clsUCP.getDeclaredMethod("getLoader", URL.class);
		fieldListPathUrls.setAccessible(true);
		fieldUnopenedUrls.setAccessible(true);
		fieldListLoaders.setAccessible(true);
		fieldMapUrlToLoader.setAccessible(true);
		methodGetLoader.setAccessible(true);
		List listPathUrls = (List) fieldListPathUrls.get(ucp);
		Deque listUnopenedPathUrls = (Deque) fieldUnopenedUrls.get(ucp);
		List listLoaders = (List) fieldListPathUrls.get(ucp);
		Map mapLoaders = (Map) fieldMapUrlToLoader.get(ucp);
		// Add each jar
		for(URL url : jarUrls) {
			String urlPath = jarUrls.toString();
			Object loader = methodGetLoader.invoke(ucp, url);
			// Update fields
			listPathUrls.add(url);
			listUnopenedPathUrls.add(url);
			listLoaders.add(loader);
			mapLoaders.put(urlPath, loader);
		}
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
		for (String dependencyPattern : DEPENDENCIES) {
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
