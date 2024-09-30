package software.coley.recaf.workspace.model.resource;

import jakarta.annotation.Nonnull;
import org.objectweb.asm.ClassReader;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.builder.JvmClassInfoBuilder;
import software.coley.recaf.info.properties.BasicPropertyContainer;
import software.coley.recaf.util.IOUtil;
import software.coley.recaf.workspace.model.bundle.AndroidClassBundle;
import software.coley.recaf.workspace.model.bundle.BasicFileBundle;
import software.coley.recaf.workspace.model.bundle.BasicJvmClassBundle;
import software.coley.recaf.workspace.model.bundle.FileBundle;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.bundle.VersionedJvmClassBundle;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;

/**
 * Implementation of a workspace resource sourced from runtime classes.
 * This is a special case of resource which is automatically added to all workspaces.
 * Listeners and such are not implemented and are ignored by design.
 *
 * @author Matt Coley
 */
public class RuntimeWorkspaceResource extends BasicPropertyContainer implements WorkspaceResource {
	private static final Object STUB = new Object();
	private static final Logger logger = Logging.get(RuntimeWorkspaceResource.class);
	private static final Map<String, Object> cache = new HashMap<>();
	private static RuntimeWorkspaceResource instance;
	private final JvmClassBundle classes;
	private final FileBundle files;

	/**
	 * @return Instance of runtime workspace resource.
	 */
	public static RuntimeWorkspaceResource getInstance() {
		if (instance == null)
			instance = new RuntimeWorkspaceResource();
		return instance;
	}

	private RuntimeWorkspaceResource() {
		classes = new BasicJvmClassBundle() {
			@Override
			public JvmClassInfo get(@Nonnull Object name) {
				String key = name.toString();
				if (key.indexOf('.') >= 0)
					key = key.replace('.', '/');
				Object present = cache.get(key);
				if (present == STUB)
					return null;
				if (present instanceof JvmClassInfo)
					return (JvmClassInfo) present;
				// Can't do "computeIfAbsent" since we also want to store null values.
				byte[] value = null;
				try (InputStream in = ClassLoader.getSystemResourceAsStream(key + ".class")) {
					if (in != null) {
						value = IOUtil.toByteArray(in);
					}
				} catch (IOException ex) {
					logger.error("Failed to fetch runtime bytecode of class: " + key, ex);
				}
				if (value == null) {
					cache.put(key, STUB);
					return null;
				}
				JvmClassInfo info = new JvmClassInfoBuilder()
						.adaptFrom(value, ClassReader.SKIP_CODE)
						.build();
				cache.put(key, info);
				return info;
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
			public void clear() {
				// no-op
			}
		};
		files = new BasicFileBundle();
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
}
