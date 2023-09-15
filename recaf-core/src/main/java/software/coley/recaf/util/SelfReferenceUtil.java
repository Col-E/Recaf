package software.coley.recaf.util;

import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Utility for self-referencing the Recaf application as a file.
 *
 * @author Matt Coley
 */
public class SelfReferenceUtil {
	private static final Logger logger = Logging.get(SelfReferenceUtil.class);
	private static SelfReferenceUtil instance = null;
	private final File file;
	private final boolean isJar;

	private SelfReferenceUtil(File file) {
		this.file = file;
		this.isJar = file.getName().toLowerCase().endsWith(".jar");
	}

	/**
	 * @return File reference to self.
	 */
	public File getFile() {
		return file;
	}

	/**
	 * @return File path to self.
	 */
	public String getPath() {
		return file.getAbsolutePath();
	}

	/**
	 * @return Is the current executable context a jar file.
	 */
	public boolean isJar() {
		return isJar;
	}

	/**
	 * Used in the UI module for syntax highlighting based on language patterns.
	 *
	 * @return List of language files recognized.
	 */
	public List<InternalPath> getLanguages() {
		return getFiles("languages/", ".json");
	}

	/**
	 * Used in the UI module for translating the UI.
	 *
	 * @return List of translation files recognized.
	 */
	public List<InternalPath> getTranslations() {
		return getFiles("translations/", ".lang");
	}

	/**
	 * @param prefix
	 * 		File prefix to match.
	 * @param suffix
	 * 		File suffix to match <i>(such as a file extension)</i>.
	 *
	 * @return List of matching files.
	 */
	private List<InternalPath> getFiles(String prefix, String suffix) {
		List<InternalPath> list = new ArrayList<>();
		if (isJar()) {
			// Read self as jar
			try (ZipFile file = new ZipFile(getFile())) {
				Enumeration<? extends ZipEntry> entries = file.entries();
				while (entries.hasMoreElements()) {
					ZipEntry entry = entries.nextElement();
					// skip directories
					if (entry.isDirectory()) continue;
					String name = entry.getName();
					if (prefix != null && !name.startsWith(prefix))
						continue;
					if (suffix != null && !name.endsWith(suffix))
						continue;
					list.add(InternalPath.internal(name));
				}
			} catch (Exception ex) {
				logger.error("Failed internal file (archive) lookup: {}", getFile(), ex);
			}
		} else {
			try {
				// You may be asking yourself "wtf is this?"
				// Well, if this is run via gradle the current path is: 'recaf-ui/build/classes/java/main'
				//  - Needs to become: 'recaf-ui/build/resources/main'
				//  - So cd '../../resources/main'
				// But if you want to use IntelliJ's optimized run (waaaaay faster startup than Gradle) this changes.
				// However, for both you can use the system classloader's internal state to check the loaded directories.
				// Using this we scan for '/resources/' since this is a common path element in each case.
				// Plus, this supports finding files from all modules, not just the 'recaf-ui' module.
				List<URL> resourceDirUrls = ClassLoaderInternals.getUcpPathList(ClassLoaderInternals.getUcp()).stream()
						.filter(url -> url.toString().contains("/resources/"))
						.toList();
				for (URL url : resourceDirUrls) {
					Path dir = Paths.get(url.toURI());
					Files.walk(dir).forEach(p -> {
						File file = dir.relativize(p).toFile();
						String path = file.getPath().replace('\\', '/');
						if (prefix != null && !path.startsWith(prefix))
							return;
						if (suffix != null && !path.endsWith(suffix))
							return;
						list.add(InternalPath.internal(path));
					});
				}
			} catch (IOException ex) {
				logger.error("Failed internal file (directory) lookup: {}", getFile(), ex);
			} catch (ReflectiveOperationException ex) {
				logger.error("Failed to lookup resources path from UCP", ex);
			} catch (URISyntaxException ex) {
				throw new IllegalStateException("Malformed URI path from UCP", ex);
			}
		}
		return list;
	}

	/**
	 * Initializes the self-reference util based on the {@link CodeSource} of the given class.
	 *
	 * @param context
	 * 		Class to initialize from.
	 *
	 * @see #getInstance()
	 */
	public static void initializeFromContext(Class<?> context) {
		if (instance != null) {
			return;
		}

		try {
			CodeSource codeSource = context.getProtectionDomain().getCodeSource();
			File selfFile = new File(codeSource.getLocation().toURI().getPath());
			instance = new SelfReferenceUtil(selfFile);
		} catch (URISyntaxException e) {
			logger.error("Failed to resolve self reference", e);
		}
	}

	public static SelfReferenceUtil getInstance() {
		return instance;
	}
}