package software.coley.recaf.plugin;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link ClassLoader} designed specifically for plugins.
 *
 * @author xDark
 */
public final class PluginClassLoader extends URLClassLoader {
	private static final Set<PluginClassLoader> ALL_LOADERS = ConcurrentHashMap.newKeySet();
	private final ClassLoader parent;

	/**
	 * @param urls
	 *        {@link URL[]} array used for classpath.
	 * @param parent
	 * 		Parent {@link ClassLoader}.
	 */
	public PluginClassLoader(URL[] urls, ClassLoader parent) {
		super(urls, null);
		this.parent = parent;
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		try {
			// We prioritize our classes over others.
			return super.findClass(name);
		} catch (ClassNotFoundException ignored) {
		}

		// Try parent class loader first.
		try {
			return parent.loadClass(name);
		} catch (ClassNotFoundException ignored) {
		}

		// Now, try all other loaders.
		for (PluginClassLoader loader : ALL_LOADERS) {
			try {
				return loader.lookupClass(name);
			} catch (ClassNotFoundException ignored) {
			}
		}

		// That's unfortunate.
		throw new ClassNotFoundException(name);
	}

	/**
	 * Registers this loader in a set of
	 * existing loaders.
	 */
	public void register() {
		ALL_LOADERS.add(this);
	}

	@Override
	public void close() throws IOException {
		try {
			super.close();
		} finally {
			ALL_LOADERS.remove(this);
		}
	}

	/**
	 * Helper method to locate a class directly
	 * in this loader.
	 *
	 * @param name
	 * 		the name of the class.
	 *
	 * @return the resulting class.
	 *
	 * @throws ClassNotFoundException
	 * 		if the class could not be found,
	 * 		or if the loader is closed.
	 */
	public Class<?> lookupClass(String name) throws ClassNotFoundException {
		return super.findClass(name);
	}

	static {
		ClassLoader.registerAsParallelCapable();
	}
}
