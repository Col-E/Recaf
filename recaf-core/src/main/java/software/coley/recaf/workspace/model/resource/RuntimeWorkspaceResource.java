package software.coley.recaf.workspace.model.resource;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.ClassReader;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.builder.JvmClassInfoBuilder;
import software.coley.recaf.info.properties.BasicPropertyContainer;
import software.coley.recaf.util.ClasspathUtil;
import software.coley.recaf.util.IOUtil;
import software.coley.recaf.util.threading.ThreadPoolFactory;
import software.coley.recaf.workspace.model.bundle.AndroidClassBundle;
import software.coley.recaf.workspace.model.bundle.BasicFileBundle;
import software.coley.recaf.workspace.model.bundle.BasicJvmClassBundle;
import software.coley.recaf.workspace.model.bundle.FileBundle;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.bundle.VersionedJvmClassBundle;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * Implementation of a workspace resource sourced from runtime classes.
 * This is a special case of resource which is automatically added to all workspaces.
 * Listeners and such are not implemented and are ignored by design.
 *
 * @author Matt Coley
 */
public class RuntimeWorkspaceResource extends BasicPropertyContainer implements WorkspaceResource {
	private static final Logger logger = Logging.get(RuntimeWorkspaceResource.class);
	private static final Map<String, JvmClassInfo> cache = new ConcurrentHashMap<>();
	private static final Set<String> stubClasses = ConcurrentHashMap.newKeySet();
	private final JvmClassBundle classes;
	private final FileBundle files;

	/**
	 * @return Instance of runtime workspace resource.
	 */
	public static RuntimeWorkspaceResource getInstance() {
		return InstanceHolder.INSTANCE;
	}

	/**
	 * @param cls
	 * 		Class to get bytecode of.
	 *
	 * @return Bytecode of class if found.
	 */
	@Nullable
	public static byte[] getRuntimeClass(@Nonnull Class<?> cls) {
		String name = cls.getName().replace('.', '/');
		return getRuntimeClass(name);
	}

	/**
	 * @param className
	 * 		Name of class to get bytecode of.
	 *
	 * @return Bytecode of class if found.
	 */
	@Nullable
	public static byte[] getRuntimeClass(@Nonnull String className) {
		JvmClassInfo info = getInstance().getJvmClassBundle().get(className);
		if (info == null)
			return null;
		return info.getBytecode();
	}

	private RuntimeWorkspaceResource() {
		classes = new BasicJvmClassBundle() {
			private final byte[] loadBuffer = IOUtil.newByteBuffer();

			@Override
			public JvmClassInfo get(@Nonnull Object name) {
				String key = name.toString();
				if (key.indexOf('.') >= 0)
					key = key.replace('.', '/');

				// Check if we have a cached value.
				JvmClassInfo present = cache.get(key);
				if (present != null)
					return present;

				// Check if the class is a known failure case.
				if (stubClasses.contains(key))
					return null;

				// Get the class bytes.
				byte[] classBytes = getClassBytes(key);
				if (classBytes == null) {
					stubClasses.add(key);
					return null;
				}

				// Try and parse the class and yield the result.
				try {
					JvmClassInfo info = new JvmClassInfoBuilder(classBytes, ClassReader.SKIP_CODE).build();
					cache.put(key, info);
					return info;
				} catch (Throwable t) {
					// There are some weird auto-generated classes in the VM like 'accessibility_ja'
					// which have invalid constant pools and kill our class parser. Ignore those.
					stubClasses.add(key);
					return null;
				}
			}

			@Nullable
			private byte[] getClassBytes(@Nonnull String key) {
				// First try doing a system lookup (for types like 'java/lang/String')
				byte[] value = null;
				try (InputStream in = ClassLoader.getSystemResourceAsStream(key + ".class")) {
					if (in != null)
						synchronized (loadBuffer) {
							value = IOUtil.toByteArray(in, loadBuffer);
						}
				} catch (IOException ex) {
					logger.error("Failed to fetch runtime (system-resource) bytecode of class: " + key, ex);
				}

				// Then try doing a classpath lookup (for types bundled into Recaf)
				if (value == null) {
					try (InputStream in = RuntimeWorkspaceResource.class.getResourceAsStream("/" + key + ".class")) {
						if (in != null)
							synchronized (loadBuffer) {
								value = IOUtil.toByteArray(in, loadBuffer);
							}
					} catch (IOException ex) {
						logger.error("Failed to fetch runtime (recaf-resource) bytecode of class: " + key, ex);
					}
				}

				return value;
			}

			@Override
			public Set<String> keySet() {
				return cache.keySet();
			}

			@Override
			public Collection<JvmClassInfo> values() {
				return cache.values();
			}

			@Override
			public Set<Entry<String, JvmClassInfo>> entrySet() {
				return cache.entrySet();
			}

			@Override
			public void incrementHistory(@Nonnull JvmClassInfo info) {
				// no-op
			}

			@Override
			public void decrementHistory(@Nonnull String key) {
				// no-op
			}

			@Override
			public JvmClassInfo remove(@Nonnull Object key) {
				return get(key);
			}

			@Override
			public boolean containsKey(@Nonnull Object key) {
				return get(key) != null;
			}

			@Override
			public boolean isEmpty() {
				return false;
			}

			@Override
			public int size() {
				return cache.size();
			}

			@Override
			public void clear() {
				// no-op
			}
		};
		files = new BasicFileBundle();

		// Populate the system classes in the background.
		try (ExecutorService ex = ThreadPoolFactory.newSingleThreadExecutor("runtime-class-population")) {
			CompletableFuture.runAsync(() -> {
				for (String name : ClasspathUtil.getSystemClassSet())
					classes.get(name);
			}, ex);
		}
	}

	@Nonnull
	@Override
	public JvmClassBundle getJvmClassBundle() {
		return classes;
	}

	@Nonnull
	@Override
	public FileBundle getFileBundle() {
		return files;
	}

	@Nonnull
	@Override
	public NavigableMap<Integer, VersionedJvmClassBundle> getVersionedJvmClassBundles() {
		return Collections.emptyNavigableMap();
	}

	@Nonnull
	@Override
	public Map<String, AndroidClassBundle> getAndroidClassBundles() {
		return Collections.emptyMap();
	}

	@Nonnull
	@Override
	public Map<String, WorkspaceFileResource> getEmbeddedResources() {
		return Collections.emptyMap();
	}

	@Override
	public WorkspaceResource getContainingResource() {
		// Not applicable
		return null;
	}

	@Override
	public void setContainingResource(WorkspaceResource resource) {
		// no-op
	}

	@Override
	public void addResourceJvmClassListener(ResourceJvmClassListener listener) {
		// no-op
	}

	@Override
	public void removeResourceJvmClassListener(ResourceJvmClassListener listener) {
		// no-op
	}

	@Override
	public void addResourceAndroidClassListener(ResourceAndroidClassListener listener) {
		// no-op
	}

	@Override
	public void removeResourceAndroidClassListener(ResourceAndroidClassListener listener) {
		// no-op
	}

	@Override
	public void addResourceFileListener(ResourceFileListener listener) {
		// no-op
	}

	@Override
	public void removeResourceFileListener(ResourceFileListener listener) {
		// no-op
	}

	@Override
	public void close() {
		// no-op
	}

	@Override
	public boolean isInternal() {
		return true;
	}

	/** Statically initialized inner class used as a lightweight alternative to double-sync block. */
	private static final class InstanceHolder {
		private static final RuntimeWorkspaceResource INSTANCE = new RuntimeWorkspaceResource();
	}
}
