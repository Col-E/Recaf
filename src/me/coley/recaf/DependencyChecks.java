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
		if (version < 1.8) {
			err("Outdated Java", "Please update java to the latest of Java 8 before using Recaf.");
			return false;
		} else if (version == 1.8) {
			return checkJFX();
		} else {
			err("Incompatibility - Java 9+", "Recaf does not currently support Java 9+ due to the modularization of the JDK.\n"
					+ "Java 9+ support will be added at a future date.\n" + "Please run under Java 8.");
			return false;
		}
	}

	private static boolean checkJFX() {
		String extra = "";
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

	private static double getVersion() {
		String version = System.getProperty("java.version");
		int pos = version.indexOf('.');
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
