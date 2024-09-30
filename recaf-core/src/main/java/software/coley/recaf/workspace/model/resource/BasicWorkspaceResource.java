package software.coley.recaf.workspace.model.resource;

import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import software.coley.collections.Unchecked;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.behavior.Closing;
import software.coley.recaf.info.AndroidClassInfo;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.info.Info;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.properties.BasicPropertyContainer;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Basic workspace resource implementation.
 *
 * @author Matt Coley
 * @see WorkspaceResourceBuilder Helper for creating instances.
 */
public class BasicWorkspaceResource extends BasicPropertyContainer implements WorkspaceResource {
	private static final Logger logger = Logging.get(BasicWorkspaceResource.class);
	private final List<ResourceJvmClassListener> jvmClassListeners = new CopyOnWriteArrayList<>();
	private final List<ResourceAndroidClassListener> androidClassListeners = new CopyOnWriteArrayList<>();
	private final List<ResourceFileListener> fileListeners = new CopyOnWriteArrayList<>();
	private final JvmClassBundle jvmClassBundle;
	private final NavigableMap<Integer, VersionedJvmClassBundle> versionedJvmClassBundles;
	private final Map<String, AndroidClassBundle> androidClassBundles;
	private final FileBundle fileBundle;
	private final Map<String, WorkspaceFileResource> embeddedResources;
	private transient WorkspaceResource containingResource;

	/**
	 * @param builder
	 * 		Builder to pull information from.
	 */
	public BasicWorkspaceResource(@Nonnull WorkspaceResourceBuilder builder) {
		this(builder.getJvmClassBundle(),
				builder.getFileBundle(),
				builder.getVersionedJvmClassBundles(),
				builder.getAndroidClassBundles(),
				builder.getEmbeddedResources(),
				builder.getContainingResource());
	}

	/**
	 * @param jvmClassBundle
	 * 		Immediate classes.
	 * @param fileBundle
	 * 		Immediate files.
	 * @param versionedJvmClassBundles
	 * 		Version specific classes.
	 * @param androidClassBundles
	 * 		Android bundles.
	 * @param embeddedResources
	 * 		Embedded resources <i>(like JAR in JAR)</i>
	 * @param containingResource
	 * 		Parent resource <i>(If we are the JAR within a JAR)</i>.
	 */
	public BasicWorkspaceResource(JvmClassBundle jvmClassBundle,
	                              FileBundle fileBundle,
	                              NavigableMap<Integer, VersionedJvmClassBundle> versionedJvmClassBundles,
	                              Map<String, AndroidClassBundle> androidClassBundles,
	                              Map<String, WorkspaceFileResource> embeddedResources,
	                              WorkspaceResource containingResource) {
		this.jvmClassBundle = jvmClassBundle;
		this.fileBundle = fileBundle;
		this.versionedJvmClassBundles = versionedJvmClassBundles;
		this.androidClassBundles = androidClassBundles;
		this.embeddedResources = embeddedResources;
		this.containingResource = containingResource;
		setup();
	}

	/**
	 * Calls other setup methods.
	 */
	protected void setup() {
		setupListenerDelegation();
		linkToEmbedded();
		markInitialBundleStates();
	}

	/**
	 * Add listeners to all bundles contained in the resource,
	 * which forward to the appropriate resource-level listener types.
	 */
	private void setupListenerDelegation() {
		WorkspaceResource resource = this;
		jvmClassBundleStream().forEach(bundle -> delegateJvmClassBundle(resource, bundle));
		androidClassBundleStream().forEach(bundle -> delegateAndroidClassBundle(resource, bundle));
		fileBundleStream().forEach(bundle -> delegateFileBundle(resource, bundle));

		// Embedded resources will notify listeners of their containing resource when they are updated.
		embeddedResources.values().forEach(embeddedResource -> {
			embeddedResource.jvmClassBundleStream().forEach(bundle -> delegateJvmClassBundle(embeddedResource, bundle));
			embeddedResource.androidClassBundleStream().forEach(bundle -> delegateAndroidClassBundle(embeddedResource, bundle));
			embeddedResource.fileBundleStream().forEach(bundle -> delegateFileBundle(embeddedResource, bundle));
		});
	}

	/**
	 * Adds listeners to the given bundle to forward to the appropriate resource-level listener types.
	 *
	 * @param resource
	 * 		Resource to delegate to.
	 * @param bundle
	 * 		Bundle to delegate from.
	 */
	protected void delegateJvmClassBundle(@Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle) {
		bundle.addBundleListener(new BundleListener<>() {
			@Override
			public void onNewItem(@Nonnull String key, @Nonnull JvmClassInfo cls) {
				Unchecked.checkedForEach(jvmClassListeners, listener -> listener.onNewClass(resource, bundle, cls),
						(listener, t) -> logger.error("Exception thrown when adding class", t));
			}

			@Override
			public void onUpdateItem(@Nonnull String key, @Nonnull JvmClassInfo oldCls, @Nonnull JvmClassInfo newCls) {
				Unchecked.checkedForEach(jvmClassListeners, listener -> listener.onUpdateClass(resource, bundle, oldCls, newCls),
						(listener, t) -> logger.error("Exception thrown when updating class", t));
			}

			@Override
			public void onRemoveItem(@Nonnull String key, @Nonnull JvmClassInfo cls) {
				Unchecked.checkedForEach(jvmClassListeners, listener -> listener.onRemoveClass(resource, bundle, cls),
						(listener, t) -> logger.error("Exception thrown when removing class", t));
			}
		});
	}

	/**
	 * Adds listeners to the given bundle to forward to the appropriate resource-level listener types.
	 *
	 * @param resource
	 * 		Resource to delegate to.
	 * @param bundle
	 * 		Bundle to delegate from.
	 */
	protected void delegateAndroidClassBundle(@Nonnull WorkspaceResource resource, @Nonnull AndroidClassBundle bundle) {
		bundle.addBundleListener(new BundleListener<>() {
			@Override
			public void onNewItem(@Nonnull String key, @Nonnull AndroidClassInfo cls) {
				Unchecked.checkedForEach(androidClassListeners, listener -> listener.onNewClass(resource, bundle, cls),
						(listener, t) -> logger.error("Exception thrown when adding class", t));
			}

			@Override
			public void onUpdateItem(@Nonnull String key, @Nonnull AndroidClassInfo oldCls, @Nonnull AndroidClassInfo newCls) {
				Unchecked.checkedForEach(androidClassListeners, listener -> listener.onUpdateClass(resource, bundle, oldCls, newCls),
						(listener, t) -> logger.error("Exception thrown when updating class", t));
			}

			@Override
			public void onRemoveItem(@Nonnull String key, @Nonnull AndroidClassInfo cls) {
				Unchecked.checkedForEach(androidClassListeners, listener -> listener.onRemoveClass(resource, bundle, cls),
						(listener, t) -> logger.error("Exception thrown when removing class", t));
			}
		});
	}

	/**
	 * Adds listeners to the given bundle to forward to the appropriate resource-level listener types.
	 *
	 * @param resource
	 * 		Resource to delegate to.
	 * @param bundle
	 * 		Bundle to delegate from.
	 */
	protected void delegateFileBundle(@Nonnull WorkspaceResource resource, @Nonnull FileBundle bundle) {
		bundle.addBundleListener(new BundleListener<>() {
			@Override
			public void onNewItem(@Nonnull String key, @Nonnull FileInfo file) {
				Unchecked.checkedForEach(fileListeners, listener -> listener.onNewFile(resource, bundle, file),
						(listener, t) -> logger.error("Exception thrown when adding file", t));
			}

			@Override
			public void onUpdateItem(@Nonnull String key, @Nonnull FileInfo oldFile, @Nonnull FileInfo newFile) {
				Unchecked.checkedForEach(fileListeners, listener -> listener.onUpdateFile(resource, bundle, oldFile, newFile),
						(listener, t) -> logger.error("Exception thrown when updating file", t));
			}

			@Override
			public void onRemoveItem(@Nonnull String key, @Nonnull FileInfo file) {
				Unchecked.checkedForEach(fileListeners, listener -> listener.onRemoveFile(resource, bundle, file),
						(listener, t) -> logger.error("Exception thrown when removing file", t));
			}
		});
	}

	/**
	 * Link all the embedded resources to the current instance as their container.
	 */
	private void linkToEmbedded() {
		embeddedResources.values().forEach(resource -> resource.setContainingResource(this));
	}

	/**
	 * Mark the bundle as being in its initial state.
	 */
	private void markInitialBundleStates() {
		// Since the bundles have been passed to our resource, we're going to assume that its fully constructed.
		// Any changes after this point will be tracked as deviations from the state at this point.
		bundleStream().forEach(bundle -> {
			if (bundle instanceof BasicBundle<Info> basicBundle) {
				basicBundle.markInitialState();
			}
		});
	}

	@Nonnull
	@Override
	public JvmClassBundle getJvmClassBundle() {
		return jvmClassBundle;
	}

	@Nonnull
	@Override
	public NavigableMap<Integer, VersionedJvmClassBundle> getVersionedJvmClassBundles() {
		return versionedJvmClassBundles;
	}

	@Nonnull
	@Override
	public Map<String, AndroidClassBundle> getAndroidClassBundles() {
		if (androidClassBundles == null) return Collections.emptyMap();
		return androidClassBundles;
	}

	@Nonnull
	@Override
	public FileBundle getFileBundle() {
		return fileBundle;
	}

	@Nonnull
	@Override
	public Map<String, WorkspaceFileResource> getEmbeddedResources() {
		return embeddedResources;
	}

	@Override
	public WorkspaceResource getContainingResource() {
		return containingResource;
	}

	@Override
	public void setContainingResource(WorkspaceResource containingResource) {
		this.containingResource = containingResource;
	}

	@Override
	public void addResourceJvmClassListener(ResourceJvmClassListener listener) {
		jvmClassListeners.add(listener);
	}

	@Override
	public void removeResourceJvmClassListener(ResourceJvmClassListener listener) {
		jvmClassListeners.remove(listener);
	}

	@Override
	public void addResourceAndroidClassListener(ResourceAndroidClassListener listener) {
		androidClassListeners.add(listener);
	}

	@Override
	public void removeResourceAndroidClassListener(ResourceAndroidClassListener listener) {
		androidClassListeners.remove(listener);
	}

	@Override
	public void addResourceFileListener(ResourceFileListener listener) {
		fileListeners.add(listener);
	}

	@Override
	public void removeResourceFileListener(ResourceFileListener listener) {
		fileListeners.remove(listener);
	}

	/**
	 * Called by containing {@link Workspace#close()}.
	 */
	@Override
	public void close() {
		// Clear all listeners
		jvmClassListeners.clear();
		androidClassListeners.clear();
		fileListeners.clear();
		// Close embedded resources
		embeddedResources.values().forEach(WorkspaceResource::close);
		// Close all bundles
		bundleStream().forEach(Closing::close);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		// NOTE: Do NOT check the containing resource (we want to prevent cycles)
		WorkspaceResource other = (WorkspaceResource) o;
		if (!jvmClassBundle.equals(other.getJvmClassBundle())) return false;
		if (!versionedJvmClassBundles.equals(other.getVersionedJvmClassBundles())) return false;
		if (!androidClassBundles.equals(other.getAndroidClassBundles())) return false;
		if (!fileBundle.equals(other.getFileBundle())) return false;
		if (!embeddedResources.equals(other.getEmbeddedResources())) return false;
		return getProperties().equals(other.getProperties());
	}

	@Override
	public int hashCode() {
		// NOTE: Do NOT consider the containing resource (we want to prevent cycles)
		int result = jvmClassBundle.hashCode();
		result = 31 * result + versionedJvmClassBundles.hashCode();
		result = 31 * result + androidClassBundles.hashCode();
		result = 31 * result + fileBundle.hashCode();
		result = 31 * result + embeddedResources.hashCode();
		result = 31 * result + getProperties().hashCode();
		return result;
	}
}
