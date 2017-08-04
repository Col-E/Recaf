package me.coley.recaf.util;

import javax.swing.JComponent;
import javax.swing.JInternalFrame;

/**
 * Random utility methods that don't fit in other places go here. Things here
 * should be moved elsewhere as soon as possible.
 */
public class Misc {

	public static boolean isInt(String s) {
		if (s.length() == 0) {
			return false;
		}
		try {
			Integer.parseInt(s);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public static void addAll(JInternalFrame owner, JComponent... components) {
		for (JComponent component : components) {
			owner.add(component);
		}
	}

}
