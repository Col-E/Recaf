package software.coley.recaf.services.plugin;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.util.io.ByteSource;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * Classloader for plugin content.
 *
 * @author xDark
 */
final class PluginClassLoaderImpl extends ClassLoader implements PluginClassLoader {
	private final PluginSource source;
	private final String id;
	private volatile List<PluginClassLoaderImpl> dependencyLoaders = List.of();

	static {
		registerAsParallelCapable();
	}

	PluginClassLoaderImpl(@Nonnull ClassLoader classLoader, @Nonnull PluginSource source, @Nonnull String id) {
		super(classLoader);
		this.source = source;
		this.id = id;
	}

	void setDependencyClassLoaders(@Nonnull Collection<PluginClassLoaderImpl> dependencyLoaders) {
		this.dependencyLoaders = List.copyOf(dependencyLoaders);
	}

	@Override
	protected URL findResource(String name) {
		ByteSource source = this.source.findResource(name);
		if (source == null) {
			return null;
		}
		try {
			String resourcePath = name.startsWith("/") ? name.substring(1) : name;
			URI uri = new URI("recaf", id, "/" + resourcePath, null);
			return URL.of(uri, new URLStreamHandler() {
				@Override
				protected URLConnection openConnection(URL u) {
					return new URLConnection(u) {

						@Override
						public void connect() {
							// no-op
						}

						@Override
						public InputStream getInputStream() throws IOException {
							return source.openStream();
						}
					};
				}
			});
		} catch (MalformedURLException | URISyntaxException ex) {
			throw new IllegalStateException("Failed to create plugin resource URL for: " + name, ex);
		}
	}

	@Override
	protected Enumeration<URL> findResources(String name) {
		URL resource = findResource(name);
		if (resource == null) {
			return Collections.emptyEnumeration();
		}
		return Collections.enumeration(List.of(resource));
	}

	@Nullable
	@Override
	public ByteSource lookupResource(@Nonnull String name) {
		return source.findResource(name);
	}

	@Nonnull
	@Override
	public Class<?> lookupClass(@Nonnull String name) throws ClassNotFoundException {
		synchronized (getClassLoadingLock(name)) {
			Class<?> cls = lookupClassImpl(name);
			if (cls == null) {
				throw new ClassNotFoundException(name);
			}
			return cls;
		}
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		Class<?> cls;
		synchronized (getClassLoadingLock(name)) {
			cls = lookupClassImpl(name);
			if (cls != null) {
				return cls;
			}
		}
		for (PluginClassLoaderImpl dependencyLoader : dependencyLoaders) {
			if ((cls = dependencyLoader.findClass(name)) != null) {
				return cls;
			}
		}
		throw new ClassNotFoundException(name);
	}

	@Nullable
	Class<?> lookupClassImpl(@Nonnull String name) throws ClassNotFoundException {
		Class<?> cls = findLoadedClass(name);
		if (cls != null)
			return cls;
		ByteSource classBytes = source.findResource(name.replace('.', '/') + ".class");
		if (classBytes != null) {
			byte[] bytes;
			try {
				bytes = classBytes.readAll();
			} catch (IOException ex) {
				throw new ClassNotFoundException(name, ex);
			}
			return defineClass(name, bytes, 0, bytes.length);
		}
		return null;
	}
}
