package me.coley.recaf.util;

import java.lang.reflect.Field;

import javax.swing.JComponent;
import javax.swing.JInternalFrame;

import org.objectweb.asm.Handle;

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

	public static boolean isBoolean(String s) {
		if (s.length() == 0) {
			return false;
		}
		try {
			Boolean.parseBoolean(s);
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

	public static void setInt(Object owner, String fieldName, Object value) {
		String vts = value.toString();
		if (Misc.isBoolean(vts)) {
			set(owner, fieldName, Boolean.parseBoolean(vts));
		}
	}

	public static void setBoolean(Object owner, String fieldName, Object value) {
		String vts = value.toString();
		if (Misc.isInt(vts)) {
			set(owner, fieldName, Integer.parseInt(vts));
		}
	}

	public static void set(Object owner, String fieldName, Object value) {
		// Ok, so this is mostly used in lambdas, which can't handle
		// exceptions....
		// so just try-catch it. Ugly, but hey it'll have to do.
		try {
			Field field = owner.getClass().getDeclaredField(fieldName);
			if (!field.isAccessible()) {
				field.setAccessible(true);
			}
			field.set(owner, value);
		} catch (Exception e) {}
	}

}
