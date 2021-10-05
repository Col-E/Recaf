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
 * In most instances this is a non-issue since the launcher will make sure
 * that the launched process contains the proper dependencies.
 * However, when we instrument a remote process we cannot have this level
 * of control. So we must be able to inject in the case of attaching.
 *
 * @author xxDark
 * @author Matt Coley
 */
public class JFXInjection {
	// Do not change to 17 until we figure out compatibility.
	// See: https://mail.openjdk.java.net/pipermail/openjfx-dev/2021-September/031934.html
	private static final String LATEST_JFX_VERSION = "16";
	private static final List<String> JFX_DEPENDENCY_URLS = Arrays.asList(
			jfxUrl("media", LATEST_JFX_VERSION),
			jfxUrl("controls", LATEST_JFX_VERSION),
			jfxUrl("graphics", LATEST_JFX_VERSION),
			jfxUrl("base", LATEST_JFX_VERSION)
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
			OperatingSystem os = OperatingSystem.get();
			Path dependenciesDirectory = Directories.getDependenciesDirectory();
			for (String dependencyPattern : JFX_DEPENDENCY_URLS) {
				// Get appropriate remote URL
				String dependencyUrlPath = String.format(dependencyPattern, os.getMvnName());
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
		writer.append("OS: " + System.getProperty("os.name") + "\n");
		writer.append("Version: " + System.getProperty("java.version") + "\n");
		writer.append("Vendor: " + System.getProperty("java.vm.vendor"));
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
	 * @return Formed URL for the component.
	 */
	private static String jfxUrl(String component, String version) {
		// Add platform specific identifier to the end.
		return String.format("https://repo1.maven.org/maven2/org/openjfx/javafx-%s/%s/javafx-%s-%s",
				component, version, component, version) + "-%s.jar";
	}
}
