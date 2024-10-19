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
import software.coley.recaf.path.ResourcePathNode;
import software.coley.recaf.path.WorkspacePathNode;
import software.coley.recaf.workspace.model.bundle.AndroidClassBundle;
import software.coley.recaf.workspace.model.bundle.ClassBundle;
import software.coley.recaf.workspace.model.bundle.FileBundle;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.bundle.VersionedJvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.Comparator;
import java.util.List;
import java.util.NavigableMap;
import java.util.Optional;
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
		ClassPathNode result = findJvmClass(name);
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
		for (WorkspaceResource resource : getAllResources(true)) {
			Optional<ClassPathNode> path = resource.jvmClassBundleStreamRecursive()
					.filter(bundle -> bundle.get(name) != null)
					.findFirst()
					.map(bundle -> {
						JvmClassInfo classInfo = bundle.get(name);
						return PathNodes.classPath(this, resource, bundle, classInfo);
					});
			if (path.isPresent()) return path.get();
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
	 * {@link WorkspaceResource#getVersionedJvmClassBundles()}of all resources in the workspace
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
		for (WorkspaceResource resource : getAllResources(false)) {
			Optional<ClassPathNode> path = resource.versionedJvmClassBundleStreamRecursive()
					.filter(bundle -> bundle.version() <= version && bundle.get(name) != null)
					.sorted(Comparator.comparingInt(VersionedJvmClassBundle::version).thenComparing(VersionedJvmClassBundle::hashCode))
					.map(bundle -> {
						JvmClassInfo classInfo = bundle.get(name);
						return PathNodes.classPath(this, resource, bundle, classInfo);
					}).findFirst();
			if (path.isPresent()) return path.get();
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
		for (WorkspaceResource resource : getAllResources(false)) {
			Optional<ClassPathNode> path = resource.androidClassBundleStreamRecursive()
					.filter(bundle -> bundle.get(name) != null)
					.map(bundle -> {
						AndroidClassInfo classInfo = bundle.get(name);
						return PathNodes.classPath(this, resource, bundle, classInfo);
					}).findFirst();
			if (path.isPresent()) return path.get();
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
		for (WorkspaceResource resource : getAllResources(false)) {
			for (ClassBundle<? extends ClassInfo> bundle : resource.classBundleStream().toList()) {
				for (String key : bundle.keySet()) {
					if (key.startsWith(cmp))
						return PathNodes.directoryPath(this, resource, bundle, name);
				}
			}
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
		SortedSet<ClassPathNode> result = new TreeSet<>();
		result.addAll(findJvmClasses(Unchecked.cast(filter)));
		result.addAll(findVersionedJvmClasses(Unchecked.cast(filter)));
		result.addAll(findAndroidClasses(Unchecked.cast(filter)));
		return result;
	}

	/**
	 * @return Stream of all classes.
	 */
	@Nonnull
	default Stream<ClassPathNode> classesStream() {
		return Stream.concat(jvmClassesStream(), androidClassesStream());
	}

	/**
	 * @return Stream of JVM classes.
	 */
	@Nonnull
	default Stream<ClassPathNode> jvmClassesStream() {
		WorkspacePathNode workspacePath = PathNodes.workspacePath(this);
		return allResourcesStream(true)
				.flatMap(resource -> {
					Stream<ClassPathNode> stream = null;
					List<JvmClassBundle> bundles = Stream.concat(resource.jvmClassBundleStreamRecursive(), resource.versionedJvmClassBundleStreamRecursive()).toList();
					for (JvmClassBundle bundle : bundles) {
						BundlePathNode bundlePath = workspacePath.child(resource).child(bundle);
						Stream<ClassPathNode> localStream = bundle.values()
								.stream()
								.map(cls -> bundlePath.child(cls.getPackageName()).child(cls));
						if (stream == null) stream = localStream;
						else stream = Stream.concat(stream, localStream);
					}
					return stream == null ? Stream.empty() : stream;
				});
	}

	/**
	 * @return Stream of Android classes.
	 */
	@Nonnull
	default Stream<ClassPathNode> androidClassesStream() {
		WorkspacePathNode workspacePath = PathNodes.workspacePath(this);
		return allResourcesStream(true)
				.flatMap(resource -> {
					Stream<ClassPathNode> stream = null;
					for (AndroidClassBundle bundle : resource.androidClassBundleStreamRecursive().toList()) {
						BundlePathNode bundlePath = workspacePath.child(resource).child(bundle);
						Stream<ClassPathNode> localStream = bundle.values()
								.stream()
								.map(cls -> bundlePath.child(cls.getPackageName()).child(cls));
						stream = stream == null ? localStream : Stream.concat(stream, localStream);
					}
					return stream == null ? Stream.empty() : stream;
				});
	}

	/**
	 * @return Stream of all files.
	 */
	@Nonnull
	default Stream<FilePathNode> filesStream() {
		WorkspacePathNode workspacePath = PathNodes.workspacePath(this);

		// Internal resources don't have files, so we won't iterate over those.
		return allResourcesStream(false)
				.flatMap(resource -> {
					Stream<FilePathNode> stream = null;
					for (FileBundle bundle : resource.fileBundleStreamRecursive().toList()) {
						BundlePathNode bundlePath = workspacePath.child(resource).child(bundle);
						Stream<FilePathNode> localStream = bundle.values()
								.stream()
								.map(cls -> bundlePath.child(cls.getDirectoryName()).child(cls));
						stream = stream == null ? localStream : Stream.concat(stream, localStream);
					}
					return stream == null ? Stream.empty() : stream;
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
		return jvmClassesStream().filter(node -> filter.test((JvmClassInfo) node.getValue()))
				.collect(Collectors.toCollection(TreeSet::new));
	}

	/**
	 * @param filter
	 * 		JVM class filter.
	 *
	 * @return Classes matching the given filter.
	 */
	@Nonnull
	default SortedSet<ClassPathNode> findVersionedJvmClasses(@Nonnull Predicate<JvmClassInfo> filter) {
		SortedSet<ClassPathNode> results = new TreeSet<>();
		WorkspacePathNode workspacePath = PathNodes.workspacePath(this);
		for (WorkspaceResource resource : getAllResources(true)) {
			ResourcePathNode resourcePath = workspacePath.child(resource);
			for (JvmClassBundle bundle : resource.getVersionedJvmClassBundles().values()) {
				BundlePathNode bundlePath = resourcePath.child(bundle);
				for (JvmClassInfo classInfo : bundle.values()) {
					if (filter.test(classInfo)) {
						results.add(bundlePath
								.child(classInfo.getPackageName())
								.child(classInfo));
					}
				}
			}
		}
		return results;
	}

	/**
	 * @param filter
	 * 		Android class filter.
	 *
	 * @return Classes matching the given filter.
	 */
	@Nonnull
	default SortedSet<ClassPathNode> findAndroidClasses(@Nonnull Predicate<AndroidClassInfo> filter) {
		SortedSet<ClassPathNode> results = new TreeSet<>();
		WorkspacePathNode workspacePath = PathNodes.workspacePath(this);
		for (WorkspaceResource resource : getAllResources(true)) {
			ResourcePathNode resourcePath = workspacePath.child(resource);
			for (AndroidClassBundle bundle : resource.getAndroidClassBundles().values()) {
				BundlePathNode bundlePath = resourcePath.child(bundle);
				for (AndroidClassInfo classInfo : bundle.values()) {
					if (filter.test(classInfo)) {
						results.add(bundlePath
								.child(classInfo.getPackageName())
								.child(classInfo));
					}
				}
			}
		}
		return results;
	}

	/**
	 * @param name
	 * 		File name.
	 *
	 * @return Path to <i>the first</i> file matching the given name.
	 */
	@Nullable
	default FilePathNode findFile(@Nonnull String name) {
		for (WorkspaceResource resource : getAllResources(false)) {
			Optional<FilePathNode> path = resource.fileBundleStreamRecursive()
					.filter(bundle -> bundle.get(name) != null)
					.findFirst()
					.map(bundle -> {
						FileInfo fileInfo = bundle.get(name);
						return PathNodes.filePath(this, resource, bundle, fileInfo);
					});
			if (path.isPresent()) return path.get();
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
		return filesStream().filter(node -> filter.test(node.getValue()))
				.collect(Collectors.toCollection(TreeSet::new));
	}
}
