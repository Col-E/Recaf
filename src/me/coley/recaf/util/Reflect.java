package me.coley.recaf.util;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Reflection utilities.
 * 
 * @author Matt
 */
public class Reflect {

	/**
	 * Sets the boolean value of the field by the given name in the given object
	 * instance.
	 * 
	 * @param owner
	 *            Object instance.
	 * @param fieldName
	 *            Field name.
	 * @param value
	 *            Value to set. May be a string, value is converted to boolean
	 *            regardless.
	 */
	public static void setBoolean(Object owner, String fieldName, Object value) {
		String vts = value.toString();
		if (Parse.isBoolean(vts)) {
			set(owner, fieldName, Boolean.parseBoolean(vts));
		}
	}

	/**
	 * Sets the integer value of the field by the given name in the given object
	 * instance.
	 * 
	 * @param owner
	 *            Object instance.
	 * @param fieldName
	 *            Field name.
	 * @param value
	 *            Value to set. May be a string, value is converted to int
	 *            regardless.
	 */
	public static void setInt(Object owner, String fieldName, Object value) {
		String vts = value.toString();
		if (Parse.isInt(vts)) {
			set(owner, fieldName, Integer.parseInt(vts));
		}
	}

	/**
	 * Sets the value of the field by the given name in the given object
	 * instance.
	 * 
	 * @param owner
	 *            Object instance.
	 * @param fieldName
	 *            Field name.
	 * @param value
	 *            Value to set.
	 */
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

	/**
	 * Get the value of the field by the given name in the given object
	 * instance.
	 * 
	 * @param owner
	 *            Object instance.
	 * @param fieldName
	 *            Field name.
	 * @return Field value. {@code null} if could not be reached.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T get(Object owner, String fieldName) {
		try {
			Field field = owner.getClass().getDeclaredField(fieldName);
			if (!field.isAccessible()) {
				field.setAccessible(true);
			}
			return (T) field.get(owner);
		} catch (Exception e) {
			return null;
		}

	}

	/**
	 * Adds the contents of the given file <i>(be it a directory or jar, does
	 * not matter)</i> to the system path.
	 * 
	 * @param file
	 * @throws IOException
	 */
	public static void extendClasspath(File file) throws IOException {
		URLClassLoader sysLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
		URL urls[] = sysLoader.getURLs(), udir = file.toURI().toURL();
		String udirs = udir.toString();
		for (int i = 0; i < urls.length; i++)
			if (urls[i].toString().equalsIgnoreCase(udirs)) return;
		Class<URLClassLoader> sysClass = URLClassLoader.class;
		try {
			Method method = sysClass.getDeclaredMethod("addURL", new Class[] { URL.class });
			method.setAccessible(true);
			method.invoke(sysLoader, new Object[] { udir });
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
}
