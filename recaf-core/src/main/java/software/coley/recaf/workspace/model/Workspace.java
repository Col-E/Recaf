package software.coley.recaf.workspace.model;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.collections.Unchecked;
import software.coley.recaf.behavior.Closing;
import software.coley.recaf.info.AndroidClassInfo;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.path.BundlePathNode;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.DirectoryPathNode;
import software.coley.recaf.path.FilePathNode;
import software.coley.recaf.path.PathNodes;
import software.coley.recaf.workspace.model.bundle.AndroidClassBundle;
import software.coley.recaf.workspace.model.bundle.ClassBundle;
import software.coley.recaf.workspace.model.bundle.FileBundle;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.bundle.VersionedJvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceFileResource;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Queue;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Models a collection of user inputs, represented as {@link WorkspaceResource} instances.
 *
 * @author Matt Coley
 */
public interface Workspace extends Closing {
	/**
	 * @return The primary resource, holding classes and files to modify.
	 */
	@Nonnull
	WorkspaceResource getPrimaryResource();

	/**
	 * @return List of <i>all</i> supporting resources, not including
	 * {@link #getInternalSupportingResources() internal supporting resources}.
	 */
	@Nonnull
	List<WorkspaceResource> getSupportingResources();

	/**
	 * @return List of internal supporting resources. These are added automatically by Recaf to all workspaces.
	 */
	@Nonnull
	List<WorkspaceResource> getInternalSupportingResources();

	/**
	 * @param resource
	 * 		Resource to add to {@link #getSupportingResources()}.
	 */
	void addSupportingResource(@Nonnull WorkspaceResource resource);

	/**
	 * @param resource
	 * 		Resource to remove from {@link #getSupportingResources()}.
	 *
	 * @return {@code true} when the resource was removed.
	 * {@code false} when it was not present.
	 */
	boolean removeSupportingResource(@Nonnull WorkspaceResource resource);

	/**
	 * @param includeInternal
	 * 		Flag to include internal supporting resources.
	 *
	 * @return List of all resources in the workspace. Includes primary, supporting, and internal support resources.
	 */
	@Nonnull
	default Stream<WorkspaceResource> allResourcesStream(boolean includeInternal) {
		List<WorkspaceResource> supportingResources = getSupportingResources();
		if (includeInternal) {
			List<WorkspaceResource> internalSupportingResources = getInternalSupportingResources();
			return Stream.of(
					Stream.of(getPrimaryResource()),
					supportingResources.stream(),
					internalSupportingResources.stream()
			).flatMap(Function.identity());
		}
		return Stream.concat(Stream.of(getPrimaryResource()), supportingResources.stream());
	}

	/**
	 * @param includeInternal
	 * 		Flag to include internal supporting resources.
	 *
	 * @return List of all resources in the workspace. Includes primary, supporting, and internal support resources.
	 */
	@Nonnull
	default List<WorkspaceResource> getAllResources(boolean includeInternal) {
		return allResourcesStream(includeInternal).toList();
	}

	/**
	 * @return Listeners for when the current workspace has its supporting resources updated.
	 */
	@Nonnull
	List<WorkspaceModificationListener> getWorkspaceModificationListeners();

	/**
	 * @param listener
	 * 		Modification listener to add.
	 */
	void addWorkspaceModificationListener(@Nonnull WorkspaceModificationListener listener);

	/**
	 * @param listener
	 * 		Modification listener to remove.
	 */
	void removeWorkspaceModificationListener(@Nonnull WorkspaceModificationListener listener);

	/**
	 * Searches for a class by the given name in the {@link WorkspaceResource#getJvmClassBundle()},
	 * {@link WorkspaceResource#getVersionedJvmClassBundles()}, and {@link WorkspaceResource#getAndroidClassBundles()}
	 * of all resources in the workspace <i>(Including embedded resources in other resources)</i>.
	 *
	 * @param name
	 * 		Class name.
	 *
	 * @return Path to <i>the first</i> class matching the given name.
	 */
	@Nullable
	default ClassPathNode findClass(@Nonnull String name) {
		return findClass(true, name);
	}

	/**
	 * Searches for a class by the given name in the {@link WorkspaceResource#getJvmClassBundle()},
	 * {@link WorkspaceResource#getVersionedJvmClassBundles()}, and {@link WorkspaceResource#getAndroidClassBundles()}
	 * of all resources in the workspace <i>(Including embedded resources in other resources)</i>.
	 *
	 * @param includeInternal
	 * 		Flag to include internal supporting resources.
	 * @param name
	 * 		Class name.
	 *
	 * @return Path to <i>the first</i> class matching the given name.
	 */
	@Nullable
	default ClassPathNode findClass(boolean includeInternal, @Nonnull String name) {
		ClassPathNode result = findJvmClass(includeInternal, name);
		if (result == null)
			result = findLatestVersionedJvmClass(name);
		if (result == null)
			result = findAndroidClass(name);
		return result;
	}

	/**
	 * Searches for a class by the given name in the {@link WorkspaceResource#getJvmClassBundle()}
	 * of all resources in the workspace <i>(Including embedded resources in other resources)</i>.
	 *
	 * @param name
	 * 		Class name.
	 *
	 * @return Path to <i>the first</i> JVM class matching the given name.
	 */
	@Nullable
	default ClassPathNode findJvmClass(@Nonnull String name) {
		return findJvmClass(true, name);
	}

	/**
	 * Searches for a class by the given name in the {@link WorkspaceResource#getJvmClassBundle()}
	 * of all resources in the workspace <i>(Including embedded resources in other resources)</i>.
	 *
	 * @param includeInternal
	 * 		Flag to include internal supporting resources.
	 * @param name
	 * 		Class name.
	 *
	 * @return Path to <i>the first</i> JVM class matching the given name.
	 */
	@Nullable
	default ClassPathNode findJvmClass(boolean includeInternal, @Nonnull String name) {
		Queue<WorkspaceResource> resourceQueue = new ArrayDeque<>(getAllResources(includeInternal));
		while (!resourceQueue.isEmpty()) {
			WorkspaceResource resource = resourceQueue.remove();

			// Check JVM bundles for class by the given name
			JvmClassInfo classInfo;
			for (JvmClassBundle bundle : resource.jvmClassBundleStream().toList()) {
				classInfo = bundle.get(name);
				if (classInfo != null)
					return PathNodes.classPath(this, resource, bundle, classInfo);
			}
			for (VersionedJvmClassBundle versionedBundle : resource.versionedJvmClassBundleStream().toList()) {
				classInfo = versionedBundle.get(name);
				if (classInfo != null)
					return PathNodes.classPath(this, resource, versionedBundle, classInfo);
			}

			// Queue up embedded resources
			resourceQueue.addAll(resource.getEmbeddedResources().values());
		}
		return null;
	}

	/**
	 * Searches for a class by the given name in the {@link WorkspaceResource#getVersionedJvmClassBundles()}
	 * of all resources in the workspace <i>(Including embedded resources in other resources)</i>.
	 *
	 * @param name
	 * 		Class name.
	 *
	 * @return @return Path to <i>the first</i> versioned JVM class matching the given name.
	 * If there are multiple versions of the class, the highest is used.
	 */
	@Nullable
	default ClassPathNode findLatestVersionedJvmClass(@Nonnull String name) {
		return findVersionedJvmClass(name, Integer.MAX_VALUE);
	}

	/**
	 * Searches for a class by the given name in the target version bundle within the
	 * {@link WorkspaceResource#getVersionedJvmClassBundles()} of all resources in the workspace
	 * <i>(Including embedded resources in other resources)</i>.
	 *
	 * @param name
	 * 		Class name.
	 * @param version
	 * 		Version to look for.
	 * 		This value is dropped down to the first available version bundle via {@link NavigableMap#floorEntry(Object)}.
	 *
	 * @return Path to <i>the highest</i> versioned JVM class matching the given name, supporting the given version.
	 */
	@Nullable
	default ClassPathNode findVersionedJvmClass(@Nonnull String name, int version) {
		// Internal resources don't have versioned classes, so we won't iterate over those.
		Queue<WorkspaceResource> resourceQueue = new ArrayDeque<>(getAllResources(false));
		while (!resourceQueue.isEmpty()) {
			WorkspaceResource resource = resourceQueue.remove();

			// Check versioned bundles for class by the given name, in descending order from the given version.
			NavigableMap<Integer, VersionedJvmClassBundle> versionedBundleMap = resource.getVersionedJvmClassBundles();
			Map.Entry<Integer, VersionedJvmClassBundle> entry = versionedBundleMap.floorEntry(version);
			while (entry != null) {
				VersionedJvmClassBundle versionedBundle = entry.getValue();
				JvmClassInfo classInfo = versionedBundle.get(name);
				if (classInfo != null)
					return PathNodes.classPath(this, resource, versionedBundle, classInfo);
				entry = versionedBundleMap.floorEntry(entry.getKey() - 1);
			}

			// Queue up embedded resources.
			resourceQueue.addAll(resource.getEmbeddedResources().values());
		}
		return null;
	}

	/**
	 * Searches for a class by the given name in the {@link WorkspaceResource#getAndroidClassBundles()}
	 * of all resources in the workspace <i>(Including embedded resources in other resources)</i>.
	 *
	 * @param name
	 * 		Class name.
	 *
	 * @return Path to <i>the first</i> Android class matching the given name.
	 */
	@Nullable
	default ClassPathNode findAndroidClass(@Nonnull String name) {
		// Internal resources don't have android classes, so we won't iterate over those.
		Queue<WorkspaceResource> resourceQueue = new ArrayDeque<>(getAllResources(false));
		while (!resourceQueue.isEmpty()) {
			WorkspaceResource resource = resourceQueue.remove();

			// Check all android bundles (dex files).
			for (AndroidClassBundle bundle : resource.getAndroidClassBundles().values()) {
				AndroidClassInfo classInfo = bundle.get(name);
				if (classInfo != null)
					return PathNodes.classPath(this, resource, bundle, classInfo);
			}

			// Queue up embedded resources.
			resourceQueue.addAll(resource.getEmbeddedResources().values());
		}
		return null;
	}

	/**
	 * @param name
	 * 		Package name.
	 *
	 * @return Path to <i>the first</i> package matching the given name.
	 */
	@Nullable
	default DirectoryPathNode findPackage(@Nonnull String name) {
		// Map '.' to '/' in case users pass in the common dot format instead.
		name = name.replace('.', '/');

		// Modify input such that the package name we compare against ends with '/'.
		// This prevents confusing matches like "com/example/a" from matching against contents in "com/example/abc".
		String cmp;
		if (name.endsWith("/")) {
			cmp = name;
			name = name.substring(0, name.length() - 1);
		} else {
			cmp = name + "/";
		}

		// We *may* want to allow specifying 'internal=true' some time later, but for now I haven't had
		// a particular use case for that.
		Queue<WorkspaceResource> resourceQueue = new ArrayDeque<>(getAllResources(false));
		while (!resourceQueue.isEmpty()) {
			WorkspaceResource resource = resourceQueue.remove();

			// Check all class bundles for entries that contain the package name.
			for (ClassBundle<? extends ClassInfo> bundle : resource.classBundleStream().toList()) {
				for (String key : bundle.keySet()) {
					if (key.startsWith(cmp))
						return PathNodes.directoryPath(this, resource, bundle, name);
				}
			}

			// Queue up embedded resources.
			resourceQueue.addAll(resource.getEmbeddedResources().values());
		}
		return null;
	}

	/**
	 * @param filter
	 * 		Class filter.
	 *
	 * @return Classes matching the given filter.
	 */
	@Nonnull
	default SortedSet<ClassPathNode> findClasses(@Nonnull Predicate<ClassInfo> filter) {
		return findClasses(true, filter);
	}

	/**
	 * @param includeInternal
	 * 		Flag to include internal supporting resources.
	 * @param filter
	 * 		Class filter.
	 *
	 * @return Classes matching the given filter.
	 */
	@Nonnull
	default SortedSet<ClassPathNode> findClasses(boolean includeInternal, @Nonnull Predicate<ClassInfo> filter) {
		SortedSet<ClassPathNode> result = new TreeSet<>();
		result.addAll(findJvmClasses(includeInternal, Unchecked.cast(filter)));
		result.addAll(findAndroidClasses(Unchecked.cast(filter)));
		return result;
	}

	/**
	 * @return Stream of all classes.
	 */
	@Nonnull
	default Stream<ClassPathNode> classesStream() {
		return classesStream(true);
	}

	/**
	 * @param includeInternal
	 * 		Flag to include internal supporting resources.
	 *
	 * @return Stream of all classes.
	 */
	@Nonnull
	default Stream<ClassPathNode> classesStream(boolean includeInternal) {
		return Stream.concat(jvmClassesStream(includeInternal), androidClassesStream());
	}

	/**
	 * @return Stream of JVM classes.
	 */
	@Nonnull
	default Stream<ClassPathNode> jvmClassesStream() {
		return jvmClassesStream(true);
	}

	/**
	 * @param includeInternal
	 * 		Flag to include internal supporting resources.
	 *
	 * @return Stream of JVM classes.
	 */
	@Nonnull
	default Stream<ClassPathNode> jvmClassesStream(boolean includeInternal) {
		return allResourcesStream(includeInternal)
				.flatMap(resource -> {
					Function<WorkspaceResource, Stream<ClassPathNode>> streamBuilder = res -> {
						Stream<ClassPathNode> stream = null;
						List<JvmClassBundle> bundles = new ArrayList<>();
						bundles.addAll(res.jvmClassBundleStream().toList());
						bundles.addAll(res.versionedJvmClassBundleStream().toList());
						for (JvmClassBundle bundle : bundles) {
							BundlePathNode bundlePath = PathNodes.bundlePath(this, res, bundle);
							Stream<ClassPathNode> localStream = bundle.values()
									.stream()
									.map(cls -> bundlePath.child(cls.getPackageName()).child(cls));
							if (stream == null) stream = localStream;
							else stream = Stream.concat(stream, localStream);
						}
						return stream;
					};
					Stream<ClassPathNode> stream = streamBuilder.apply(resource);

					// Visit embedded resources
					Queue<WorkspaceFileResource> embeddedResources = new ArrayDeque<>(resource.getEmbeddedResources().values());
					while (!embeddedResources.isEmpty()) {
						WorkspaceFileResource embeddedResource = embeddedResources.remove();
						stream = Stream.concat(stream, streamBuilder.apply(embeddedResource));
						embeddedResources.addAll(embeddedResource.getEmbeddedResources().values());
					}

					return stream;
				});
	}

	/**
	 * @return Stream of Android classes.
	 */
	@Nonnull
	default Stream<ClassPathNode> androidClassesStream() {
		// Internal resources don't have android classes, so we won't iterate over those.
		return allResourcesStream(false)
				.flatMap(resource -> {
					Function<WorkspaceResource, Stream<ClassPathNode>> streamBuilder = res -> {
						Stream<ClassPathNode> stream = null;
						for (AndroidClassBundle bundle : res.getAndroidClassBundles().values()) {
							BundlePathNode bundlePath = PathNodes.bundlePath(this, res, bundle);
							Stream<ClassPathNode> localStream = bundle.values()
									.stream()
									.map(cls -> bundlePath.child(cls.getPackageName()).child(cls));
							if (stream == null) stream = localStream;
							else stream = Stream.concat(stream, localStream);
						}
						return stream == null ? Stream.empty() : stream;
					};
					Stream<ClassPathNode> stream = streamBuilder.apply(resource);

					// Visit embedded resources
					Queue<WorkspaceFileResource> embeddedResources = new ArrayDeque<>(resource.getEmbeddedResources().values());
					while (!embeddedResources.isEmpty()) {
						WorkspaceFileResource embeddedResource = embeddedResources.remove();
						stream = Stream.concat(stream, streamBuilder.apply(embeddedResource));
						embeddedResources.addAll(embeddedResource.getEmbeddedResources().values());
					}

					return stream;
				});
	}

	/**
	 * @return Stream of all files.
	 */
	@Nonnull
	default Stream<FilePathNode> filesStream() {
		// Internal resources don't have files, so we won't iterate over those.
		return allResourcesStream(false)
				.flatMap(resource -> {
					Function<WorkspaceResource, Stream<FilePathNode>> streamBuilder = res -> {
						FileBundle bundle = res.getFileBundle();
						BundlePathNode bundlePath = PathNodes.bundlePath(this, res, bundle);
						return bundle.values()
								.stream()
								.map(cls -> bundlePath.child(cls.getDirectoryName()).child(cls));
					};
					Stream<FilePathNode> stream = streamBuilder.apply(resource);

					// Visit embedded resources
					Queue<WorkspaceFileResource> embeddedResources = new ArrayDeque<>(resource.getEmbeddedResources().values());
					while (!embeddedResources.isEmpty()) {
						WorkspaceFileResource embeddedResource = embeddedResources.remove();
						stream = Stream.concat(stream, streamBuilder.apply(embeddedResource));
						embeddedResources.addAll(embeddedResource.getEmbeddedResources().values());
					}

					return stream;
				});
	}

	/**
	 * @param filter
	 * 		JVM class filter.
	 *
	 * @return Classes matching the given filter.
	 */
	@Nonnull
	default SortedSet<ClassPathNode> findJvmClasses(@Nonnull Predicate<JvmClassInfo> filter) {
		return findJvmClasses(true, filter);
	}

	/**
	 * @param includeInternal
	 * 		Flag to include internal supporting resources.
	 * @param filter
	 * 		JVM class filter.
	 *
	 * @return Classes matching the given filter.
	 */
	@Nonnull
	default SortedSet<ClassPathNode> findJvmClasses(boolean includeInternal, @Nonnull Predicate<JvmClassInfo> filter) {
		return jvmClassesStream(includeInternal)
				.filter(node -> filter.test((JvmClassInfo) node.getValue()))
				.collect(Collectors.toCollection(TreeSet::new));
	}

	/**
	 * @param filter
	 * 		Android class filter.
	 *
	 * @return Classes matching the given filter.
	 */
	@Nonnull
	default SortedSet<ClassPathNode> findAndroidClasses(@Nonnull Predicate<AndroidClassInfo> filter) {
		return androidClassesStream()
				.filter(node -> filter.test((AndroidClassInfo) node.getValue()))
				.collect(Collectors.toCollection(TreeSet::new));
	}

	/**
	 * @param name
	 * 		File name.
	 *
	 * @return Path to <i>the first</i> file matching the given name.
	 */
	@Nullable
	default FilePathNode findFile(@Nonnull String name) {
		// Internal resources don't have files, so we won't iterate over those.
		Queue<WorkspaceResource> resourceQueue = new ArrayDeque<>(getAllResources(false));
		while (!resourceQueue.isEmpty()) {
			WorkspaceResource resource = resourceQueue.remove();

			FileBundle bundle = resource.getFileBundle();
			FileInfo fileInfo = bundle.get(name);
			if (fileInfo != null)
				return PathNodes.filePath(this, resource, bundle, fileInfo);

			// Queue up embedded resources.
			resourceQueue.addAll(resource.getEmbeddedResources().values());
		}

		return null;
	}

	/**
	 * @param filter
	 * 		File filter.
	 *
	 * @return Files matching the given filter.
	 */
	@Nonnull
	default SortedSet<FilePathNode> findFiles(@Nonnull Predicate<FileInfo> filter) {
		return filesStream()
				.filter(node -> filter.test(node.getValue()))
				.collect(Collectors.toCollection(TreeSet::new));
	}
}
