package software.coley.recaf.services.plugin;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.util.io.ByteSource;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;

/**
 * Classloader for plugin content.
 *
 * @author xDark
 */
final class PluginClassLoaderImpl extends ClassLoader implements PluginClassLoader {
	private final PluginGraph graph;
	private final PluginSource source;
	private final String id;

	PluginClassLoaderImpl(@Nonnull ClassLoader classLoader, @Nonnull PluginGraph graph, @Nonnull PluginSource source, @Nonnull String id) {
		super(classLoader);
		this.graph = graph;
		this.source = source;
		this.id = id;
	}

	@Override
	protected URL findResource(String name) {
		ByteSource source = this.source.findResource(name);
		if (source == null) {
			return null;
		}
		try {
			URI uri = new URI("recaf", "/", name);
			return URL.of(uri, new URLStreamHandler() {
				@Override
				protected URLConnection openConnection(URL u) {
					return new URLConnection(u) {
						InputStream in;

						@Override
						public void connect() {
							// no-op
						}

						@Override
						public InputStream getInputStream() throws IOException {
							InputStream in = this.in;
							if (in == null) {
								in = source.openStream();
								this.in = in;
							}
							return in;
						}
					};
				}
			});
		} catch (MalformedURLException | URISyntaxException ex) {
			throw new IllegalStateException(ex);
		}
	}

	@Nullable
	@Override
	public ByteSource lookupResource(@Nonnull String name) {
		return source.findResource(name);
	}

	@Nonnull
	@Override
	public Class<?> lookupClass(@Nonnull String name) throws ClassNotFoundException {
		Class<?> cls = lookupClassImpl(name);
		if (cls == null) {
			throw new ClassNotFoundException(name);
		}
		return cls;
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		Class<?> cls = lookupClassImpl(name);
		if (cls != null)
			return cls;
		var dependencyLoaders = graph.getDependencyClassloaders(id);
		while (dependencyLoaders.hasNext()) {
			if ((cls = dependencyLoaders.next().findClass(name)) != null)
				return cls;
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
