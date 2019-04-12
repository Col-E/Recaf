package me.coley.recaf;

import javax.swing.JOptionPane;

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
public class DependencyChecks {

	public static boolean check() {
		double version = getVersion();
		if (version == 1.8) {
			// Java 8
			return checkJFX();
		} else if (version > 1.8) {
			// Java 9+
			return true;
		} else {
			// Java 7-
			err("Outdated Java", "Please update java to the latest of Java 8 before using Recaf.");
			return false;
		}
	}

	private static boolean checkJFX() {
		String extra = "";
		// Checking these classes because JavaFX used to be linked to the actual Java release.
		// Some early versions of 1.8 are 'missing' these classes. So verify we have them.
		if (!exists("javafx.application.Application$Parameters") || !exists("javafx.scene.control.MenuBar") || !exists(
				"javafx.scene.control.Menu")) {
			err("Mising JavaFX classes", "Could not load required JavaFX classes.\n" + extra);
			return false;
		}
		try {
			getClass("javafx.scene.control.MenuBar").getConstructor(getClass("[Ljavafx.scene.control.Menu;"));
		} catch (Exception e) {
			err("Mising JavaFX methods", "Could not load required JavaFX methods.\n" + extra);
			return false;
		}
		return true;
	}

	private static void err(String title, String msg) {
		System.err.println(msg);
		// If for some reason I'm still updating this past 2025, Swing is
		// supposed to be EOL March 2025.
		JOptionPane.showMessageDialog(null, msg, title, JOptionPane.ERROR_MESSAGE);
	}

	public static double getVersion() {
		String version = System.getProperty("java.version");
		int pos = version.indexOf('.');
		if (pos == -1)
			return Double.parseDouble(version);
		pos = version.indexOf('.', pos + 1);
		return Double.parseDouble(version.substring(0, pos));
	}

	private static boolean exists(String className) {
		try {
			getClass(className);
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}

	private static Class<?> getClass(String className) throws ClassNotFoundException {
		return Class.forName(className, false, ClassLoader.getSystemClassLoader());
	}
}
