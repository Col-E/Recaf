package software.coley.recaf.workspace.model.resource;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.behavior.Closing;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.Info;
import software.coley.recaf.info.properties.PropertyContainer;
import software.coley.recaf.util.Streams;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.AndroidClassBundle;
import software.coley.recaf.workspace.model.bundle.Bundle;
import software.coley.recaf.workspace.model.bundle.ClassBundle;
import software.coley.recaf.workspace.model.bundle.FileBundle;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.bundle.VersionedJvmClassBundle;

import java.util.Map;
import java.util.NavigableMap;
import java.util.stream.Stream;

import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.of;

/**
 * Component of a {@link Workspace}. Contain classes and files.
 *
 * @author Matt Coley
 */
public interface WorkspaceResource extends PropertyContainer, Closing {
	/**
	 * Contains the classes within the resource.
	 * <br>
	 * For JAR files, copies of these classes can also be provided for specific JVM versions, which are available
	 * via {@link #getVersionedJvmClassBundles()}.
	 * <br>
	 * For Android files (APK) this will always be empty.
	 * Android APK's classes reside in embedded dex files, which are accessible via {@link #getAndroidClassBundles()}.
	 *
	 * @return Immediate classes within the resource.
	 */
	@Nonnull
	JvmClassBundle getJvmClassBundle();

	/**
	 * Contains additional class bundles for versioned classes.
	 * These exist in multi-release JAR files <i>(New feature to Java 9+)</i>.
	 *
	 * @return Map of versions, to JVM class bundles.
	 */
	@Nonnull
	NavigableMap<Integer, VersionedJvmClassBundle> getVersionedJvmClassBundles();

	/**
	 * Contains Android class bundles.
	 * <br>
	 * Android bundles one or more DEX files into an APK. Each DEX becomes a bundle with its classes
	 * accessible here.
	 *
	 * @return Map of Android class bundles.
	 */
	@Nonnull
	Map<String, AndroidClassBundle> getAndroidClassBundles();

	// TODO: Specific Android resource bundle for 'resources.arsc'?
	//       - Or make custom FileInfo?

	/**
	 * @return Immediate files within the resource.
	 */
	@Nonnull
	FileBundle getFileBundle();

	/**
	 * Suppose you have a Spring boot JAR that has a structure like the following:
	 * <ul>
	 *     <li>WEB-INF/lib-provided/BundledLibrary.jar</li>
	 *     <li>com.example.Launcher.class</li>
	 * </ul>
	 * <p>
	 * If you open the boot JAR in Recaf you would probably want to be able to inspect the contents of
	 * {@code BundledLibrary.jar}. So for that sort of case we extract those containers into their own resources.
	 *
	 * @return Map of container files <i>(JAR/ZIP/WAR/etc)</i> represented as their own workspace resources.
	 */
	@Nonnull
	Map<String, WorkspaceFileResource> getEmbeddedResources();

	/**
	 * @return Containing resource of this one if this represents a <i>"JAR in JAR"</i> kind of situation.
	 * May be {@code null} for a root-resource.
	 */
	@Nullable
	WorkspaceResource getContainingResource();

	/**
	 * Searches within this resource and all embedded resources for containment of the given bundle.
	 * To reconstruct the path to the root from the returned result here use {@link #getContainingResource()}.
	 *
	 * @param bundle
	 * 		Bundle that belongs to this resource, or a {@link #getEmbeddedResources() embedded resource}.
	 *
	 * @return The containing resource.
	 * <ul>
	 *     <li>Can be the current resource.</li>
	 *     <li>Can be any level of nested embedded resource.</li>
	 *     <li>Can be {@code null} if no embedded resource contains the given bundle.</li>
	 * </ul>
	 */
	@Nullable
	default WorkspaceResource resolveBundleContainer(@Nonnull Bundle<?> bundle) {
		// Check for containment in this resource
		if (bundleStream().anyMatch(b -> b == bundle)) return this;

		// Check for containment in any embedded resource
		for (WorkspaceFileResource embedded : getEmbeddedResources().values()) {
			WorkspaceResource resource = embedded.resolveBundleContainer(bundle);
			if (resource != null) return resource;
		}
		return null;
	}

	/**
	 * @param resource
	 * 		Containing resource to assign.
	 */
	void setContainingResource(WorkspaceResource resource);

	/**
	 * @return {@code true} when there is another resource that contains this one.
	 */
	default boolean isEmbeddedResource() {
		return getContainingResource() != null;
	}

	/**
	 * @return Stream of all immediate JVM class bundles in the resource.
	 */
	@Nonnull
	default Stream<JvmClassBundle> jvmClassBundleStream() {
		return of(getJvmClassBundle());
	}

	/**
	 * @return Stream of all JVM class bundles in the resource, and in any embedded resources
	 */
	@Nonnull
	default Stream<JvmClassBundle> jvmClassBundleStreamRecursive() {
		return concat(jvmClassBundleStream(), getEmbeddedResources().values().stream()
				.flatMap(WorkspaceResource::jvmClassBundleStreamRecursive));
	}

	/**
	 * @return Stream of all versioned JVM class bundles in the resource.
	 */
	@Nonnull
	default Stream<VersionedJvmClassBundle> versionedJvmClassBundleStream() {
		return getVersionedJvmClassBundles().values().stream();
	}

	/**
	 * @return Stream of all versioned JVM class bundles in the resource, and in any embedded resources
	 */
	@Nonnull
	default Stream<VersionedJvmClassBundle> versionedJvmClassBundleStreamRecursive() {
		return concat(versionedJvmClassBundleStream(), getEmbeddedResources().values().stream()
				.flatMap(WorkspaceResource::versionedJvmClassBundleStreamRecursive));
	}

	/**
	 * @return Stream of all immediate Android class bundles in the resource.
	 */
	@Nonnull
	default Stream<AndroidClassBundle> androidClassBundleStream() {
		return getAndroidClassBundles().values().stream();
	}

	/**
	 * @return Stream of all Android class bundles in the resource, and in any embedded resources.
	 */
	@Nonnull
	default Stream<AndroidClassBundle> androidClassBundleStreamRecursive() {
		return concat(androidClassBundleStream(), getEmbeddedResources().values().stream()
				.flatMap(WorkspaceResource::androidClassBundleStreamRecursive));
	}

	/**
	 * @return Stream of all immediate class bundles in the resource.
	 */
	@Nonnull
	default Stream<ClassBundle<? extends ClassInfo>> classBundleStream() {
		return concat(jvmClassBundleStream(), concat(versionedJvmClassBundleStream(), androidClassBundleStream()));
	}

	/**
	 * @return Stream of all class bundles in the resource, and in any embedded resources.
	 */
	@Nonnull
	default Stream<ClassBundle<? extends ClassInfo>> classBundleStreamRecursive() {
		return concat(classBundleStream(), getEmbeddedResources().values().stream()
				.flatMap(WorkspaceResource::classBundleStreamRecursive));
	}

	/**
	 * @return Stream of all immediate file bundles in the resource.
	 */
	@Nonnull
	default Stream<FileBundle> fileBundleStream() {
		return of(getFileBundle());
	}

	/**
	 * @return Stream of all file bundles in the resource, and in any embedded resources.
	 */
	@Nonnull
	default Stream<FileBundle> fileBundleStreamRecursive() {
		return concat(fileBundleStream(), getEmbeddedResources().values().stream()
				.flatMap(WorkspaceResource::fileBundleStreamRecursive));
	}

	/**
	 * @return Stream of all immediate bundles in the resource.
	 */
	@Nonnull
	@SuppressWarnings("unchecked")
	default <I extends Info> Stream<Bundle<I>> bundleStream() {
		// Cast to object is a hack to allow generic usage of this method with <Info>.
		// Using <? extends Info> prevents <Info> usage.
		//  noinspection RedundantCast
		return (Stream<Bundle<I>>) (Object)
				Streams.of(
						jvmClassBundleStream(),
						versionedJvmClassBundleStream(),
						androidClassBundleStream(),
						fileBundleStream()
				);
	}

	/**
	 * @return Stream of all bundles in the resource, and in any embedded resources.
	 */
	@Nonnull
	@SuppressWarnings("unchecked")
	default <I extends Info> Stream<Bundle<I>> bundleStreamRecursive() {
		// Cast to object is a hack to allow generic usage of this method with <Info>.
		// Using <? extends Info> prevents <Info> usage.
		//  noinspection RedundantCast
		return (Stream<Bundle<I>>) (Object)
				Streams.of(
						jvmClassBundleStreamRecursive(),
						versionedJvmClassBundleStreamRecursive(),
						androidClassBundleStreamRecursive(),
						fileBundleStreamRecursive()
				);
	}

	/**
	 * @param listener
	 * 		Generic object to add as any supported listener type.
	 */
	default void addListener(Object listener) {
		if (listener instanceof ResourceJvmClassListener)
			addResourceJvmClassListener((ResourceJvmClassListener) listener);
		if (listener instanceof ResourceAndroidClassListener)
			addResourceAndroidClassListener((ResourceAndroidClassListener) listener);
		if (listener instanceof ResourceFileListener)
			addResourceFileListener((ResourceFileListener) listener);
	}

	/**
	 * @param listener
	 * 		Generic object to remove as any supported listener type.
	 */
	default void removeListener(Object listener) {
		if (listener instanceof ResourceJvmClassListener)
			removeResourceJvmClassListener((ResourceJvmClassListener) listener);
		if (listener instanceof ResourceAndroidClassListener)
			removeResourceAndroidClassListener((ResourceAndroidClassListener) listener);
		if (listener instanceof ResourceFileListener)
			removeResourceFileListener((ResourceFileListener) listener);
	}

	/**
	 * @param listener
	 * 		Listener to add.
	 */
	void addResourceJvmClassListener(ResourceJvmClassListener listener);

	/**
	 * @param listener
	 * 		Listener to remove.
	 */
	void removeResourceJvmClassListener(ResourceJvmClassListener listener);

	/**
	 * @param listener
	 * 		Listener to add.
	 */
	void addResourceAndroidClassListener(ResourceAndroidClassListener listener);

	/**
	 * @param listener
	 * 		Listener to remove.
	 */
	void removeResourceAndroidClassListener(ResourceAndroidClassListener listener);

	/**
	 * @param listener
	 * 		Listener to add.
	 */
	void addResourceFileListener(ResourceFileListener listener);

	/**
	 * @param listener
	 * 		Listener to remove.
	 */
	void removeResourceFileListener(ResourceFileListener listener);

	/**
	 * @return {@code true} when this resource represents an internally managed resource within a {@link Workspace}.
	 * These resources are not explicitly created by users and thus should not be visible to them. However, they will
	 * supplement workspace capabilities as any other supporting resource.
	 */
	default boolean isInternal() {
		return false;
	}
}
