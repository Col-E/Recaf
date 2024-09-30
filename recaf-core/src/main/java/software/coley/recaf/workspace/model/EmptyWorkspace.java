package software.coley.recaf.workspace.model;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.properties.Property;
import software.coley.recaf.workspace.model.bundle.*;
import software.coley.recaf.workspace.model.resource.*;

import java.util.Collections;
import java.util.Map;
import java.util.NavigableMap;

/**
 * Empty workspace for testing.
 *
 * @author Matt Coley
 */
public class EmptyWorkspace extends BasicWorkspace {
	private static final EmptyWorkspace INSTANCE = new EmptyWorkspace();

	/**
	 * New empty workspace.
	 */
	private EmptyWorkspace() {
		super(new EmptyWorkspaceResource());
	}

	/**
	 * @return Singleton of empty workspace.
	 */
	@Nonnull
	public static EmptyWorkspace get() {
		return INSTANCE;
	}

	@Override
	public String toString() {
		return EmptyWorkspace.class.getSimpleName();
	}

	/**
	 * No-op / empty resource.
	 */
	private static class EmptyWorkspaceResource implements WorkspaceResource {
		private final JvmClassBundle classes = new BasicJvmClassBundle() {
			@Override
			public JvmClassInfo get(@Nonnull Object key) {
				return null;
			}
		};
		private final FileBundle files = new BasicFileBundle() {
			@Override
			public FileInfo put(@Nonnull String key, @Nonnull FileInfo newValue) {
				return null;
			}
		};

		@Nonnull
		@Override
		public JvmClassBundle getJvmClassBundle() {
			return classes;
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
		public FileBundle getFileBundle() {
			return files;
		}

		@Nonnull
		@Override
		public Map<String, WorkspaceFileResource> getEmbeddedResources() {
			return Collections.emptyMap();
		}

		@Override
		public WorkspaceResource getContainingResource() {
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
		public <V> void setProperty(Property<V> property) {
			// no-op
		}

		@Override
		public void removeProperty(String key) {
			// no-op
		}

		@Nonnull
		@Override
		public Map<String, Property<?>> getProperties() {
			return Collections.emptyMap();
		}
	}
}
