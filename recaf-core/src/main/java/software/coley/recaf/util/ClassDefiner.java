package software.coley.recaf.util;

import jakarta.annotation.Nonnull;
import software.coley.collections.Maps;

import java.util.Map;

/**
 * Classloader implementation for loading a class from raw {@code byte[]}.
 *
 * @author Matt Coley
 */
public class ClassDefiner extends ClassLoader {
	private final Map<String, byte[]> classes;

	/**
	 * @param name
	 * 		Name of class.
	 * @param bytecode
	 * 		Bytecode of class.
	 */
	public ClassDefiner(@Nonnull String name, @Nonnull byte[] bytecode) {
		this(Maps.of(name, bytecode));
	}

	/**
	 * @param classes
	 * 		Map of classes.
	 */
	public ClassDefiner(@Nonnull Map<String, byte[]> classes) {
		super(ClassDefiner.class.getClassLoader());
		this.classes = classes;
	}

	@Override
	public final Class<?> findClass(String name) throws ClassNotFoundException {
		byte[] bytecode = classes.get(name);
		if (bytecode != null)
			return defineClass(name, bytecode, 0, bytecode.length, null);
		return super.findClass(name);
	}
}