package me.coley.recaf.util;

import java.lang.reflect.Constructor;

/**
 * Utility for defining classes at runtime.
 * 
 * @author Matt
 */
public class DefineUtil {
	/**
	 * Define a class of the give name via its bytecode.
	 * 
	 * @param name
	 *            Name of the class to define.
	 * @param bytecode
	 *            Bytecode of the class.
	 * @return Instance of the class <i>(Default constructor)</i>
	 * @throws ClassNotFoundException
	 *             Not thrown, only there to satisfy compiler enforced
	 *             exception.
	 * @throws NoSuchMethodException
	 *             Thrown if there is no default constructor in the class.
	 * @throws ReflectiveOperationException
	 *             Thrown if the constructor could not be called.
	 */
	public static Object create(String name, byte[] bytecode) throws ClassNotFoundException, NoSuchMethodException,
			ReflectiveOperationException {
		Class<?> c = new ClassDefiner(name, bytecode).findClass(name);
		Constructor<?> con = c.getDeclaredConstructor();
		return con.newInstance();
	}

	/**
	 * Define a class of the give name via its bytecode.
	 * 
	 * @param name
	 *            Name of the class to define.
	 * @param bytecode
	 *            Bytecode of the class.
	 * @param argTypes
	 *            Constructor type arguments.
	 * @param argValues
	 *            Constructor argument values.
	 * @return Instance of the class.
	 * @throws ClassNotFoundException
	 *             Not thrown, only there to satisfy compiler enforced
	 *             exception.
	 * @throws NoSuchMethodException
	 *             Thrown if a constructor defined by the given type array does
	 *             not exist.
	 * @throws ReflectiveOperationException
	 *             Thrown if the constructor could not be called.
	 */
	public static Object create(String name, byte[] bytecode, Class<?>[] argTypes, Object[] argValues)
			throws ClassNotFoundException, NoSuchMethodException, ReflectiveOperationException {
		Class<?> c = new ClassDefiner(name, bytecode).findClass(name);
		Constructor<?> con = c.getDeclaredConstructor(argTypes);
		return con.newInstance(argValues);
	}

	/**
	 * Simple loader that exposes defineClass. Will only load the supplied
	 * class.
	 * 
	 * @author Matt
	 */
	static class ClassDefiner extends ClassLoader {
		private final byte[] bytecode;
		private final String name;

		public ClassDefiner(String name, byte[] bytecode) {
			super(ClasspathUtil.scl);
			this.name = name;
			this.bytecode = bytecode;
		}

		@Override
		public final Class<?> findClass(String name) throws ClassNotFoundException {
			if (this.name.equals(name)) {
				return defineClass(name, bytecode, 0, bytecode.length, null);
			}
			return super.findClass(name);

		}
	}

}