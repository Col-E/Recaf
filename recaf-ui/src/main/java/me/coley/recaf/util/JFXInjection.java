package me.coley.recaf.util;

import me.coley.recaf.util.logging.Logging;
import org.slf4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * JavaFX utility to insert the latest version into the classpath when
 * the current JRE does not have the classes loaded.
 * <br>
 * Once the official releases for 3.X are out, this class should be removed.
 * <ul>
 *     <li>The launcher will handle classpath dependency management.</li>
 *     <li>Agent mode will not need to inject the entire app into the target jvm.</li>
 * </ul>
 *
 * @author xxDark
 * @author Matt Coley
 */
public class JFXInjection {
	private static final String JFX_CLASSIFIER = createClassifier();
	private static final String JFX_VERSION = "18-ea+8";
	private static final List<String> JFX_DEPENDENCY_URLS = Arrays.asList(
			jfxUrlPattern("media"),
			jfxUrlPattern("controls"),
			jfxUrlPattern("graphics"),
			jfxUrlPattern("base")
	);
	private static final Logger logger = Logging.get(JFXInjection.class);

	/**
	 * Ensures that the JavaFX runtime is on the class path.
	 */
	public static void ensureJavafxSupport() {
		// Skip if platform class already exists
		if (ClasspathUtil.classExists(JFXUtils.getPlatformClassName()))
			return;
		// Check if JavaFX independent releases are compatible with current VM
		if (JavaVersion.get() < 11) {
			alertPre11UserMissingJFX();
			return;
		}
		// Ensure dependencies are downloaded
		List<Path> dependencyPaths = getLocalDependencies();
		addToClasspath(dependencyPaths);
	}

	/**
	 * @return List of path elements pointing to the local JavaFX dependencies to add into the classpath.
	 */
	private static List<Path> getLocalDependencies() {
		List<Path> dependencyPaths = new ArrayList<>();
		try {
			Path dependenciesDirectory = Directories.getDependenciesDirectory();
			for (String dependencyPattern : JFX_DEPENDENCY_URLS) {
				// Get appropriate remote URL
				String dependencyUrlPath = String.format(dependencyPattern, JFX_CLASSIFIER);
				Path dependencyFilePath = dependenciesDirectory.resolve(getUrlArtifactFileName(dependencyUrlPath));
				// Add the file to the paths list we will use later to inject
				dependencyPaths.add(dependencyFilePath);
				// Write to local directory if they are not already downloaded
				if (!Files.exists(dependencyFilePath)) {
					URL depURL = new URL(dependencyUrlPath);
					Files.copy(depURL.openStream(), dependencyFilePath, StandardCopyOption.REPLACE_EXISTING);
				}
			}
		} catch (MalformedURLException ex) {
			logger.error("Invalid dependency URL path", ex);
			alertUserFailedInit(ex);
		} catch (IOException ex) {
			logger.error("Failed to write remote dependency to cache", ex);
			alertUserFailedInit(ex);
		}
		return dependencyPaths;
	}

	/**
	 * Inserts the given jars into the classpath.
	 *
	 * @param dependencyPaths
	 * 		List of path elements to JavaFX jars to add to the classpath.
	 */
	private static void addToClasspath(List<Path> dependencyPaths) {
		try {
			// Fetch UCP of application's ClassLoader
			// - ((ClassLoaders.AppClassLoader) ClassLoaders.appClassLoader()).ucp
			Class<?> clsClassLoaders = Class.forName("jdk.internal.loader.ClassLoaders");
			Object appClassLoader = clsClassLoaders.getDeclaredMethod("appClassLoader").invoke(null);
			Class<?> ucpOwner = appClassLoader.getClass();
			// Field removed in 16, but still exists in parent class "BuiltinClassLoader"
			if (JavaVersion.get() >= 16)
				ucpOwner = ucpOwner.getSuperclass();
			Field fieldUCP = ReflectUtil.getDeclaredField(ucpOwner, "ucp");
			Object ucp = fieldUCP.get(appClassLoader);
			Class<?> clsUCP = ucp.getClass();
			Method addURL = clsUCP.getDeclaredMethod("addURL", URL.class);
			addURL.setAccessible(true);
			// Add each jar.
			for (Path path : dependencyPaths) {
				URL url = path.toAbsolutePath().toUri().toURL();
				addURL.invoke(ucp, url);
			}
		} catch (MalformedURLException ex) {
			// This should never occur
			logger.error("Failed to resolve local dependency jar to URL", ex);
			alertUserFailedInit(ex);
		} catch (ReflectiveOperationException ex) {
			// This should only occur if a JRE has some customizations to the way core classloaders are handled.
			// Or if they update something in a newer version of Java.
			logger.error("Failed to add missing JavaFX paths to classpath", ex);
			alertUserFailedInit(ex);
		}
	}

	/**
	 * Create a visible alert that the user cannot install JavaFX automatically due to incompatible Java versions.
	 */
	private static void alertPre11UserMissingJFX() {
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		toolkit.beep();
		// Collect debug information
		StringWriter writer = new StringWriter();
		RuntimeProperties.dump(writer);
		String debugInfo = writer.toString();
		// Show message
		String style = "<style>" +
				"p {font-family: Arial; font-size:14;} " +
				"pre { background: #DDDDDD; padding: 5px; border: 1px solid black; }" +
				"</style>";
		String message = "<p>The required JavaFX classes could not be found locally.<br><br>Your environment:</p>" +
				"<pre>" + debugInfo + "</pre>" +
				"<p>You have two options:<br>" +
				" 1. Use a JDK that bundles JavaFX<br>" +
				" 2. Update to Java 11 or higher <i>(Recaf will automatically download JavaFX)</i></p>";
		JEditorPane pane = new JEditorPane("text/html", style + message);
		pane.setEditable(false);
		pane.setOpaque(false);
		JOptionPane.showMessageDialog(null,
				pane, "JavaFX could not be found",
				JOptionPane.ERROR_MESSAGE);
		System.exit(0);
	}

	/**
	 * Create a visible alert that the user cannot install JavaFX automatically due to some error that occurred.
	 */
	private static void alertUserFailedInit(Exception ex) {
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		toolkit.beep();
		// Collect some debug info
		StringWriter writer = new StringWriter();
		writer.append("OS: " + System.getProperty("os.name") + "\n");
		writer.append("Version: " + System.getProperty("java.version") + "\n");
		writer.append("Vendor: " + System.getProperty("java.vm.vendor") + "\n\n");
		writer.append("Exception: ");
		// Append exception to string
		ex.printStackTrace(new PrintWriter(writer));
		String errorString = writer.toString();
		// Copy to clipboard
		StringSelection selection = new StringSelection(errorString);
		Clipboard clipboard = toolkit.getSystemClipboard();
		clipboard.setContents(selection, selection);
		// Show message
		String bugReportURL = "https://github.com/Col-E/Recaf/issues/new/choose";
		String style = "<style>" +
				"p {font-family: Arial; font-size:14;} " +
				"pre { background: #DDDDDD; padding: 5px; border: 1px solid black; }" +
				"</style>";
		String message = "<p>Something went wrong when trying to load JavaFX.<br>" +
				"<b>The following information about the problem has been copied to your clipboard:</b></p><br>" +
				"<pre>" + errorString + "</pre>" +
				"<p>Please make sure that you meet one of the following requirements:<br>" +
				" 1. Use a JDK that bundles JavaFX<br>" +
				" 2. Update to Java 11 or higher <i>(Recaf will automatically download JavaFX)</i><br><br>" +
				"If you believe this is a bug, please " +
				"<a href=\"" + bugReportURL + "\">open an issue on GitHub</a></p>";
		JEditorPane pane = new JEditorPane("text/html", style + message);
		pane.setEditable(false);
		pane.setOpaque(false);
		int height = 250 + StringUtil.count("\n", errorString) * 22;
		if (height > toolkit.getScreenSize().height - 100) {
			height = toolkit.getScreenSize().height - 100;
		}
		JScrollPane scroll = new JScrollPane(pane);
		scroll.setPreferredSize(new Dimension(800, height));
		scroll.setBorder(BorderFactory.createEmptyBorder());
		JOptionPane.showMessageDialog(null,
				scroll, "Error initializing JavaFX",
				JOptionPane.ERROR_MESSAGE);
		System.exit(0);
	}

	/**
	 * @param url
	 * 		Full url path.
	 *
	 * @return Name of file at url.
	 */
	private static String getUrlArtifactFileName(String url) {
		return url.substring(url.lastIndexOf('/') + 1);
	}

	/**
	 * @param component
	 * 		Name of the component.
	 *
	 * @return Formed pattern <i>(Arg for classifier)</i> for the URL to the component.
	 */
	private static String jfxUrlPattern(String component) {
		// Add platform specific identifier to the end.
		return String.format("https://repo1.maven.org/maven2/org/openjfx/javafx-%s/%s/javafx-%s-%s",
				component, JFX_VERSION, component, JFX_VERSION) + "-%s.jar";
	}


	/**
	 * @return JavaFX Maven classifier based on the current OS/platform.
	 */
	private static String createClassifier() {
		// Possible targets:
		// - linux-aarch64
		// - linux-arm32
		// - linux
		// - mac-aarch64
		// - mac
		// - win
		String os = normalizeOs();
		// JavaFX does not differentiate against windows
		if (os.equals("win"))
			return os;
		// Check for arch-specific releases
		String arch = normalizeArch();
		if (os.equals("mac") && arch.equals("aarch64"))
			return os + "-" + arch;
		if (os.equals("linux") && arch.equals("aarch64"))
			return os + "-" + arch;
		if (os.equals("linux") && arch.equals("arm32"))
			return os + "-" + arch;
		// Fallback to default
		return os;
	}

	/**
	 * @return Operating system name pattern matching the maven classifier format.
	 * This portion supplies the prefix in {@code OS-ARCH} classifiers.
	 */
	private static String normalizeOs() {
		String os = normalize(RuntimeProperties.OS_NAME);
		if (os.startsWith("macosx") || os.startsWith("osx"))
			return "mac";
		if (os.startsWith("win"))
			return "win";
		// It's probably a linux system
		return "linux";
	}

	/**
	 * @return Architecture name pattern matching the maven classifier format.
	 * This portion supplies the suffix in {@code OS-ARCH} classifiers.
	 */
	private static String normalizeArch() {
		// JavaFX only targets certain architectures, so we only care about normalizing a few.
		String arch = normalize(RuntimeProperties.OS_ARCH);
		if ("aarch64".equals(arch))
			return "aarch64";
		if (arch.matches("^(arm|arm32)$"))
			return "arm32";
		return arch;
	}

	/**
	 * @param value
	 * 		Some text value.
	 *
	 * @return Value lower-cased with non-letters and non-numbers stripped from the name.
	 */
	private static String normalize(String value) {
		return value.toLowerCase().replaceAll("[^a-z0-9]+", "");
	}
}
