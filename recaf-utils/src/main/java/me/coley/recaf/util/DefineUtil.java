package me.coley.recaf.util;

import org.objectweb.asm.ClassReader;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility for defining classes at runtime.
 *
 * @author Matt Coley
 */
public class DefineUtil {
	/**
	 * Define a class with the given bytecode.
	 *
	 * @param bytecode
	 * 		Bytecode of the class.
	 *
	 * @return Class of type.
	 *
	 * @throws ClassNotFoundException
	 * 		Thrown if the class bytecode is invalid.
	 */
	public static Class<?> create(byte[] bytecode) throws ClassNotFoundException {
		return create(new ClassReader(bytecode).getClassName(), bytecode);
	}

	/**
	 * Define a class of the given name via its bytecode.
	 *
	 * @param name
	 * 		Name of the class to define.
	 * @param bytecode
	 * 		Bytecode of the class.
	 *
	 * @return Class of type.
	 *
	 * @throws ClassNotFoundException
	 * 		Thrown if the class bytecode is invalid.
	 */
	public static Class<?> create(String name, byte[] bytecode) throws ClassNotFoundException {
		return new ClassDefiner(name, bytecode)
				.findClass(name.replace('/', '.'));
	}

	/**
	 * Define classes of the give names and return the class of the given name.
	 *
	 * @param name
	 * 		Name of the class to return.
	 * @param definitions
	 * 		Bytecode of multiple classes.
	 *
	 * @return Class of type defined by the parameter.
	 *
	 * @throws ClassNotFoundException
	 * 		Thrown if the class bytecode is invalid.
	 */
	public static Class<?> create(Map<String, byte[]> definitions, String name) throws ClassNotFoundException {
		return new ClassDefiner(definitions)
				.findClass(name.replace('/', '.'));
	}

	/**
	 * Simple loader that exposes defineClass. Will only load the supplied class.
	 */
	public static class ClassDefiner extends ClassLoader {
		private final Map<String, byte[]> definitions;

		/**
		 * @param name
		 * 		Name of class from the bytecode.
		 * @param bytecode
		 * 		Bytecode of the class.
		 */
		public ClassDefiner(String name, byte[] bytecode) {
			this(Map.of(name, bytecode));
		}

		/**
		 * @param definitions
		 * 		Map of class definitions.
		 */
		public ClassDefiner(Map<String, byte[]> definitions) {
			super(ClasspathUtil.scl);
			this.definitions = definitions.entrySet().stream()
					.collect(Collectors.toMap(e -> e.getKey().replace('/', '.'), Map.Entry::getValue));
		}

		@Override
		public final Class<?> findClass(String name) throws ClassNotFoundException {
			try {
				byte[] bytecode = definitions.get(name);
				if (bytecode != null)
					return defineClass(name, bytecode, 0, bytecode.length, null);
			} catch (ClassFormatError error) {
				throw new ClassNotFoundException(name, error);
			}
			return super.findClass(name);
		}
	}
}