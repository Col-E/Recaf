package software.coley.recaf.services.plugin;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import software.coley.recaf.RecafConstants;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.plugin.*;
import software.coley.recaf.util.ByteHeaderUtil;
import software.coley.recaf.util.CancelSignal;
import software.coley.recaf.util.IOUtil;
import software.coley.recaf.util.io.ByteSource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.file.NoSuchFileException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Plugin loader that is capable of loading plugins from ZIP archives.
 * Note that JAR is technically a ZIP archive.
 *
 * @author xDark
 */
public final class ZipPluginLoader implements PluginLoader {
	private static final String INFORMATION_DESCRIPTOR = Type.getDescriptor(PluginInformation.class);
	private static final Logger logger = Logging.get(ZipPluginLoader.class);
	private final ClassLoader primaryClassLoader;

	/**
	 * @param primaryClassLoader
	 *        {@link ClassLoader} that is used to locate system classes.
	 */
	public ZipPluginLoader(ClassLoader primaryClassLoader) {
		this.primaryClassLoader = primaryClassLoader;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends Plugin> PluginContainer<T> load(ClassAllocator allocator, ByteSource source) throws IOException, PluginLoadException, UnsupportedSourceException {
		// Read whole zip archive into memory.
		Map<String, byte[]> content = new HashMap<>();
		try (ZipInputStream zis = new ZipInputStream(source.openStream())) {
			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null) {
				// We don't care about directories.
				if (entry.isDirectory())
					continue;

				byte[] bytes = IOUtil.toByteArray(zis);
				content.put(entry.getName(), bytes);
			}
		}

		// Locate plugin entrypoint before processing.
		String pluginClass = getPluginClass(content);
		if (pluginClass == null)
			throw new UnsupportedSourceException("ZIP archive does not contain plugin entrypoint.");

		// Create in-memory implementation of URL, so it can be used for PluginClassLoader
		URL url = new URL("recaf", "", -1, "/", new URLStreamHandler() {
			@Override
			protected URLConnection openConnection(URL u) throws IOException {
				String file = u.getFile();
				file = file.startsWith("/") ? file.substring(1) : file;
				byte[] bytes = content.get(file);
				if (bytes == null)
					throw new NoSuchFileException(file);
				return new URLConnection(u) {
					private InputStream in;

					@Override
					public void connect() {
						// no-op
					}

					@Override
					public InputStream getInputStream() {
						InputStream in = this.in;
						if (in == null) {
							in = new ByteArrayInputStream(bytes);
							this.in = in;
						}
						return in;
					}
				};
			}
		});

		PluginClassLoader classLoader = new PluginClassLoader(new URL[]{url}, primaryClassLoader);
		try {
			// Load plugin entrypoint.
			Class<?> entrypoint;
			try {
				entrypoint = classLoader.lookupClass(pluginClass);
			} catch (ClassNotFoundException | NoClassDefFoundError ex) {
				throw new PluginLoadException("Plugin entrypoint was not found: " + pluginClass, ex);
			}
			if (!Plugin.class.isAssignableFrom(entrypoint))
				throw new PluginLoadException("Plugin entrypoint is not assignable to base Plugin interface");

			// Read plugin information.
			PluginInfo information;
			try {
				information = readPluginInformation(entrypoint);
			} catch (IllegalStateException ex) {
				throw new PluginLoadException("Could not read plugin information", ex);
			}

			// Actually create plugin instance.
			T plugin;
			try {
				plugin = (T) allocator.instance(entrypoint);
			} catch (AllocationException ex) {
				throw new PluginLoadException("Could not create plugin instance", ex);
			}

			// Don't forget to register a loader.
			classLoader.register();
			classLoader = null;
			logger.info("Loaded plugin {} v{} by {}", information.getName(),
					information.getVersion(), information.getAuthor());
			return new PluginContainer<>(plugin, information, this);
		} finally {
			if (classLoader != null) {
				// Something went wrong during plugin loading, so don't leak a loader.
				IOUtil.closeQuietly(classLoader);
			}
		}
	}

	@Override
	public boolean isSupported(ByteSource source) throws IOException {
		byte[] header = source.peek(4);
		return header.length == 4 && ByteHeaderUtil.match(header, ByteHeaderUtil.ZIP);
	}

	@Override
	public void enablePlugin(PluginContainer<?> container) {
		Plugin plugin = container.getPlugin();
		ClassLoader classLoader = plugin.getClass().getClassLoader();
		if (!(classLoader instanceof PluginClassLoader))
			throw new IllegalStateException("Plugin does not belong to ZipPluginLoader");

		logger.info("Enabling plugin {}", container.getInformation().getName());
		container.getPlugin().onEnable();
	}

	@Override
	public void disablePlugin(PluginContainer<?> container) {
		Plugin plugin = container.getPlugin();
		ClassLoader classLoader = plugin.getClass().getClassLoader();
		if (!(classLoader instanceof PluginClassLoader))
			throw new IllegalStateException("Plugin does not belong to ZipPluginLoader");

		logger.info("Disabling plugin {}", container.getInformation().getName());
		try {
			plugin.onDisable();
		} finally {
			try {
				((PluginClassLoader) classLoader).close();
			} catch (IOException ex) {
				logger.warn("Could not close plugin class loader", ex);
			}
		}
	}

	/**
	 * Locates plugin entrypoint class.
	 *
	 * @param content
	 *        {@link Map} containing archive data.
	 *
	 * @return located plugin entrypoint or {@code null},
	 * if not found.
	 */
	private static String getPluginClass(Map<String, byte[]> content) {
		PluginAnnotationVisitor visitor = new PluginAnnotationVisitor();
		for (Map.Entry<String, byte[]> entry : content.entrySet()) {
			String name = entry.getKey();
			if (!name.endsWith(".class")) {
				continue; //  We are only looking for classes.
			}

			ClassReader cr = new ClassReader(entry.getValue());
			try {
				cr.accept(visitor, ClassReader.SKIP_FRAMES | ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG);
			} catch (CancelSignal signal) {
				// Class found.
				return name.substring(0, name.length() - 6).replace('/', '.');
			}
		}
		return null;
	}

	/**
	 * Reads plugin information from the class.
	 *
	 * @param type
	 * 		Plugin class.
	 *
	 * @return Information about plugin.
	 */
	private static PluginInfo readPluginInformation(Class<?> type) {
		PluginInformation annotation = type.getAnnotation(PluginInformation.class);
		if (annotation == null)
			throw new IllegalStateException("@PluginInformation is missing on " + type);
		return new PluginInfo(annotation.name(),
				annotation.version(), annotation.author(), annotation.description());
	}

	private static final class PluginAnnotationVisitor extends ClassVisitor {
		PluginAnnotationVisitor() {
			super(RecafConstants.getAsmVersion());
		}

		@Override
		public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
			if (INFORMATION_DESCRIPTOR.equals(descriptor))
				throw CancelSignal.get();
			return null;
		}
	}
}
