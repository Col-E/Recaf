package me.coley.recaf.util;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.*;
import java.net.URL;
import java.net.URLClassLoader;

import me.coley.recaf.Logging;
import sun.reflect.FieldAccessor;

/**
 * Reflection utilities.
 * 
 * @author Matt
 */
public class Reflect {
	private static Method privateGetDeclaredFields;
	private static Method getFieldAccessor;

	/**
	 * Get all fields belonging to the given class.
	 * 
	 * @param clazz
	 *            Class containing fields.
	 * @return Array of class's fields.
	 */
	public static Field[] fields(Class<?> clazz) {
		// Use underlying method in java/lang/Class so that changes to the
		// fields are permanent since we are accessing the original copy of the
		// field[] instead of the copy that public-facing methods give to us.
		try {
			return (Field[]) privateGetDeclaredFields.invoke(clazz, false);
		} catch (Exception e) {
			Logging.fatal(e);
			return null;
		}
	}

	/**
	 * Get first field matching one of the given names.
	 * 
	 * @param clazz
	 *            Owner with fields.
	 * @param aliases
	 *            Field names.
	 * @return Field matching name in alias set. May be null.
	 */
	public static Field getField(Class<?> clazz, String... aliases) {
		Field field = null;
		for (String alias : aliases) {
			try {
				field = clazz.getDeclaredField(alias);
				if (field != null) break;
			} catch (Exception e) {}
		}
		return field;
	}

	/**
	 * Get the value of the field by its name in the given object instance.
	 * 
	 * @param instance
	 *            Object instance.
	 * @param fieldName
	 *            Field name.
	 * @return Field value. {@code null} if could not be reached.
	 */
	public static <T> T get(Object instance, String fieldName) {
		try {
			Field field = instance.getClass().getDeclaredField(fieldName);
			field.setAccessible(true);
			return get(instance, field);
		} catch (NoSuchFieldException | SecurityException e) {
			Logging.fatal(e);
			return null;
		}
	}

	/**
	 * Get the value of the field in the given object instance.
	 * 
	 * @param owner
	 *            Object instance.
	 * @param field
	 *            Field, assumed to be accessible.
	 * @return Field value. {@code null} if could not be reached.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T get(Object instance, Field field) {
		try {
			FieldAccessor acc = (FieldAccessor) getFieldAccessor.invoke(field, instance);
			return (T) acc.get(instance);
		} catch (Exception e) {
			Logging.fatal(e);
			return null;
		}
	}

	/**
	 * Sets the value of the field in the given object instance.
	 * 
	 * @param owner
	 *            Object instance.
	 * @param field
	 *            Field, assumed to be accessible.
	 * @param value
	 *            Value to set.
	 */
	public static void set(Object instance, Field field, Object value) {
		try {
			FieldAccessor acc = (FieldAccessor) getFieldAccessor.invoke(field, instance);
			acc.set(instance, value);
		} catch (Exception e) {
			Logging.fatal(e);
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

	static {
		try {
			// These are used to access the direct Field instances instead of
			// the copies you normally get through #getDeclaredFields.
			privateGetDeclaredFields = Class.class.getDeclaredMethod("privateGetDeclaredFields", boolean.class);
			privateGetDeclaredFields.setAccessible(true);
			getFieldAccessor = Field.class.getDeclaredMethod("getFieldAccessor", Object.class);
			getFieldAccessor.setAccessible(true);
		} catch (Exception e) {
			Logging.fatal(e);
		}
	}
}
