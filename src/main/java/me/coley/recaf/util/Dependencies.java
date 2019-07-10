package me.coley.recaf.util;

import me.coley.recaf.Recaf;

import javax.swing.*;

/**
 * Check if Recaf can be run on the current JVM.
 * 
 * <ul>
 * <li>Prevent execution on modularized JVM's until support is added</li>
 * <li>Verify compatible JavaFX</li>
 * </ul>
 * 
 * @author Matt
 */
public class Dependencies {

	public static boolean check() {
		double version = getVersion();
		if (version < 1.8) {
			// Java 7-
			err("Outdated Java", "Please update java to the latest of Java 8 before using Recaf.");
			return false;
		} else if (version == 1.8) {
			// Java 8
			if (hasRequiredJFX()) {
				// Using up-to-date java 8 (including associated JavaFx version)
				return true;
			} else {
				err("Outdated JavaFX", "Please update Java 8 to the latest release.");
				return false;
			}
		} else if (version > 1.8 && version < 11) {
			// Java 9/10
			//
			// Unsupported since Java 11 is the first Jigsaw LTS.
			err("Unsupported Java Version", "Java versions 9 & 10 are not supported. Please " +
					"upgrade to Java 11 to run Recaf.");
			return false;
		} else {
			// Java 11+
			//
			// Will have to ensure usage of updated ControlsFx & JavaFx
			if (!hasRequiredJFX()) {
				// Run external script to update the Recaf jar with the proper dependencies
				System.out.println("Detected JDK 11+ without OpenJFX dependencies\n" +
						"Dependencies will be downloaded and Recaf will restart...");
				Updater.updateViaJdk11Patch(Recaf.args);
				return false;
			}
			// All good to go
			return true;
		}
	}

	private static boolean hasRequiredJFX() {
		Class<?> menuBar = findClass("javafx.scene.control.MenuBar");
		Class<?> menu = findClass("javafx.scene.control.Menu");
		if (menuBar == null || menu == null) {
			return false;
		}
		// TODO: Use JavaFX.version() and parse it for more fine-tune
		// Call it via reflection to be super safe
		try {
			Class<?> klass = Class.forName("me.coley.recaf.util.JavaFX");
			String version = (String) klass.getMethod("version").invoke(null);
			// Expected "8.0.BUILDVERSION-Whatever"
			// BUILDVERSION has to be at least somewhat updated to prevent issues like #74
			if (version.startsWith("8.")){
				String buildVersion = version.substring(version.lastIndexOf(".") + 1, version.indexOf("-") );
				int build = Integer.parseInt(buildVersion);
				return build > 100;
			}
			return version != null;
		} catch(Throwable t) {
			return false;
		}
	}

	public static double getVersion() {
		String version = System.getProperty("java.version");
		int pos = version.indexOf('.');
		if (pos == -1)
			return Double.parseDouble(version);
		pos = version.indexOf('.', pos + 1);
		return Double.parseDouble(version.substring(0, pos));
	}

	private static void err(String title, String msg) {
		System.err.println(msg);
		JOptionPane.showMessageDialog(null, msg, title, JOptionPane.ERROR_MESSAGE);
	}

	private static Class<?> findClass(String className) {
		try {
			return Class.forName(className, false, Dependencies.class.getClassLoader());
		} catch (ClassNotFoundException e) {
			return null;
		}
	}
}
