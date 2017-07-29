package me.coley.recaf.util;

import javax.swing.JComponent;
import javax.swing.JInternalFrame;

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
