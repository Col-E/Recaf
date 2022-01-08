package me.coley.recaf.util;

import java.lang.reflect.Constructor;

/**
 * Reflection utils
 *
 * @author Matt
 */
public class ReflectUtil {
	/**
	 * @param type
	 * 		Class to construct.
	 * @param argTypes
	 * 		Argument types.
	 * @param args
	 * 		Argument values.
	 * @param <T>
	 * 		Assumed class type.
	 *
	 * @return New instance of class.
	 */
	public static <T> T quietNew(Class<T> type, Class<?>[] argTypes, Object[] args) {
		try {
			Constructor<T> constructor = type.getDeclaredConstructor(argTypes);
			constructor.setAccessible(true);
			return constructor.newInstance(args);
		} catch (ReflectiveOperationException ex) {
			throw new IllegalStateException("Constructor failure: " + type.getName(), ex);
		}
	}
}
