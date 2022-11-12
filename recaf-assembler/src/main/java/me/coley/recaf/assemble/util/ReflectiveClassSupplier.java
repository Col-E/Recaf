package me.coley.recaf.assemble.util;

import me.coley.recaf.util.IOUtil;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Class supplier that pulls info from runtime classes.
 *
 * @author Matt Coley
 */
public class ReflectiveClassSupplier implements ClassSupplier {
	private static final ReflectiveClassSupplier INSTANCE = new ReflectiveClassSupplier();
	private final Map<String, byte[]> cache = new HashMap<>();

	private ReflectiveClassSupplier() {
		// disallow creation
	}

	@Override
	public byte[] getClass(String name) throws ClassNotFoundException {
		byte[] data = cache.get(name);
		if (data == null) {
			try {
				InputStream classStream = ClassLoader.getSystemResourceAsStream(name + ".class");
				if (classStream == null) {
					throw new ClassNotFoundException(name);
				}
				data = IOUtil.toByteArray(classStream);
				cache.put(name, data);
			} catch (ClassNotFoundException ex) {
				// Pass to callee
				throw ex;
			} catch (Exception ex) {
				// return ClassPool.getDefault().makeClass(name.replace('/', '.')).toBytecode();
				throw new IllegalStateException(ex);
			}
		}
		return data;
	}

	/**
	 * @return Singleton instance.
	 */
	public static ReflectiveClassSupplier getInstance() {
		return INSTANCE;
	}
}
