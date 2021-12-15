package me.coley.recaf.util.self;

import me.coley.recaf.Recaf;
import me.coley.recaf.util.*;

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
 * Utility for patching self when missing dependencies.
 *
 * @author Matt
 * @author xxDark
 */
public class SelfDependencyPatcher {
	private static final String JFX_CLASSIFIER = createClassifier();
	private static final String JFX_VERSION = "18-ea+8";
	private static final List<String> JFX_DEPENDENCY_URLS = Arrays.asList(
		jfxUrlPattern("media"),
		jfxUrlPattern("controls"),
		jfxUrlPattern("graphics"),
		jfxUrlPattern("base")
	);

	/**
	 * Ensures that the JavaFX runtime is on the class path.
	 */
	public static void ensureJavafxSupport() {
		// Skip if platform class already exists
		if (ClasspathUtil.classExists("javafx.application.Platform"))
			return;
		// Check if JavaFX independent releases are compatible with current VM
		if (VMUtil.getVmVersion() < 11) {
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
			Path dependenciesDirectory = Recaf.getDirectory("dependencies");
			if (!Files.exists(dependenciesDirectory)) {
				Files.createDirectory(dependenciesDirectory);
			}
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
			Log.error("Invalid dependency URL path", ex);
			alertUserFailedInit(ex);
		} catch (IOException ex) {
			Log.error("Failed to write remote dependency to cache", ex);
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
			if (VMUtil.getVmVersion() >= 16)
				ucpOwner = ucpOwner.getSuperclass();
			Field fieldUCP = ucpOwner.getDeclaredField("ucp");
			fieldUCP.setAccessible(true);
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
			Log.error("Failed to resolve local dependency jar to URL", ex);
			alertUserFailedInit(ex);
		} catch (ReflectiveOperationException ex) {
			// This should only occur if a JRE has some customizations to the way core classloaders are handled.
			// Or if they update something in a newer version of Java.
			Log.error("Failed to add missing JavaFX paths to classpath", ex);
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
		String[] properties = {
			"os.name", "os.version", "os.arch",
			"java.version", "java.vm.name", "java.vm.vendor", "java.home"
		};
		StringWriter writer = new StringWriter();
		for (String prop : properties) {
			writer.append(String.format("%s = %s", prop, System.getProperty(prop)));
		}
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
		OSUtil os = OSUtil.getOSType();
		if (os == OSUtil.MAC)
			return "mac";
		if (os == OSUtil.WINDOWS)
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
		String arch = normalize(System.getProperty("os.arch"));
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
