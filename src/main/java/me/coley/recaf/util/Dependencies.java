package me.coley.recaf.util;

import me.coley.recaf.Recaf;

import javax.swing.*;

import static me.coley.recaf.util.Classpath.getSystemClass;

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
		if(version < 1.8) {
			// Java 7-
			err("Outdated Java", "Please update java to the latest of Java 8 before using Recaf.");
			return false;
		} else if(version == 1.8) {
			// Java 8
			if(checkJFX()) {
				// Using up-to-date java 8 (including associated JavaFx version)
				return true;
			} else {
				err("Outdated JavaFX", "Please update Java 8 to the latest release.");
				return false;
			}
		} else if(version > 1.8 && version < 11) {
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
			if(!checkJFX()) {
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

	private static boolean checkJFX() {
		// Checking these classes because JavaFX used to be linked to the actual Java release.
		// Some early versions of 1.8 are 'missing' these classes. So verify we have them.
		if (!exists("javafx.scene.control.MenuBar") || !exists("javafx.scene.control.Menu")) {
			return false;
		}
		try {
			getSystemClass("javafx.scene.control.MenuBar").getConstructor(getSystemClass("[Ljavafx.scene.control.Menu;"));
		} catch (Exception e) {
			return false;
		}
		return true;
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

	private static boolean exists(String className) {
		return Classpath.getSystemClassIfExists(className).isPresent();
	}
}
