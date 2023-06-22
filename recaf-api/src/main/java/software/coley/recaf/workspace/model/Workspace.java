package software.coley.recaf.workspace.model;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.behavior.Closing;
import software.coley.recaf.info.AndroidClassInfo;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.path.*;
import software.coley.recaf.util.Unchecked;
import software.coley.recaf.workspace.WorkspaceModificationListener;
import software.coley.recaf.workspace.model.bundle.AndroidClassBundle;
import software.coley.recaf.workspace.model.bundle.ClassBundle;
import software.coley.recaf.workspace.model.bundle.FileBundle;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceFileResource;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.*;
import java.util.function.Predicate;

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
	default List<WorkspaceResource> getAllResources(boolean includeInternal) {
		List<WorkspaceResource> supportingResources = getSupportingResources();
		int supportingSize = supportingResources.size();
		if (includeInternal) {
			List<WorkspaceResource> internalSupportingResources = getInternalSupportingResources();
			List<WorkspaceResource> list = new ArrayList<>(1 + supportingSize + internalSupportingResources.size());
			list.add(getPrimaryResource());
			list.addAll(supportingResources);
			list.addAll(internalSupportingResources);
			return list;
		}
		List<WorkspaceResource> list = new ArrayList<>(1 + supportingSize);
		list.add(getPrimaryResource());
		list.addAll(supportingResources);
		return list;
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
	void addWorkspaceModificationListener(WorkspaceModificationListener listener);

	/**
	 * @param listener
	 * 		Modification listener to remove.
	 */
	void removeWorkspaceModificationListener(WorkspaceModificationListener listener);

	/**
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
	 * @param name
	 * 		Class name.
	 *
	 * @return Path to <i>the first</i> JVM class matching the given name.
	 */
	@Nullable
	default ClassPathNode findJvmClass(@Nonnull String name) {
		for (WorkspaceResource resource : getAllResources(true)) {
			JvmClassBundle bundle = resource.getJvmClassBundle();
			JvmClassInfo classInfo = bundle.get(name);
			if (classInfo != null)
				return PathNodes.classPath(this, resource, bundle, classInfo);
		}
		return null;
	}

	/**
	 * @param name
	 * 		Class name.
	 *
	 * @return @return Path to <i>the first</i> versioned JVM class matching the given name.
	 * If there are multiple versions of the class, the latest is used.
	 */
	@Nullable
	default ClassPathNode findLatestVersionedJvmClass(@Nonnull String name) {
		return findVersionedJvmClass(name, Integer.MAX_VALUE);
	}

	/**
	 * @param name
	 * 		Class name.
	 * @param version
	 * 		Version to look for.
	 * 		This value is dropped down to the first available version bundle via {@link NavigableMap#floorEntry(Object)}.
	 *
	 * @return Path to <i>the first</i> versioned JVM class matching the given name, supporting the given version.
	 */
	@Nullable
	default ClassPathNode findVersionedJvmClass(@Nonnull String name, int version) {
		for (WorkspaceResource resource : getAllResources(false)) {
			// Get floor version target.
			Map.Entry<Integer, JvmClassBundle> entry = resource.getVersionedJvmClassBundles().floorEntry(version);
			if (entry == null) continue;

			// Bundle exists, check if the class exists in the path.
			JvmClassBundle bundle = entry.getValue();
			JvmClassInfo classInfo = bundle.get(name);
			if (classInfo != null)
				return PathNodes.classPath(this, resource, bundle, classInfo);
		}
		return null;
	}

	/**
	 * @param name
	 * 		Class name.
	 *
	 * @return Path to <i>the first</i> Android class matching the given name.
	 */
	@Nullable
	default ClassPathNode findAndroidClass(@Nonnull String name) {
		for (WorkspaceResource resource : getAllResources(false)) {
			for (AndroidClassBundle bundle : resource.getAndroidClassBundles().values()) {
				AndroidClassInfo classInfo = bundle.get(name);
				if (classInfo != null)
					return PathNodes.classPath(this, resource, bundle, classInfo);
			}
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
	 * @param filter
	 * 		JVM class filter.
	 *
	 * @return Classes matching the given filter.
	 */
	@Nonnull
	default SortedSet<ClassPathNode> findJvmClasses(@Nonnull Predicate<JvmClassInfo> filter) {
		SortedSet<ClassPathNode> results = new TreeSet<>();
		WorkspacePathNode workspacePath = new WorkspacePathNode(this);
		for (WorkspaceResource resource : getAllResources(true)) {
			JvmClassBundle bundle = resource.getJvmClassBundle();
			BundlePathNode bundlePath = workspacePath.child(resource).child(bundle);
			for (JvmClassInfo classInfo : bundle.values()) {
				if (filter.test(classInfo)) {
					results.add(bundlePath
							.child(classInfo.getPackageName())
							.child(classInfo));
				}
			}
		}
		return results;
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
		WorkspacePathNode workspacePath = new WorkspacePathNode(this);
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
		WorkspacePathNode workspacePath = new WorkspacePathNode(this);
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
			FileBundle bundle = resource.getFileBundle();
			FileInfo fileInfo = bundle.get(name);
			if (fileInfo != null)
				return PathNodes.filePath(this, resource, bundle, fileInfo);
			for (WorkspaceFileResource embedded : resource.getEmbeddedResources().values()) {
				FileInfo embeddedFileInfo = embedded.getFileInfo();
				if (embeddedFileInfo.getName().equals(name))
					return PathNodes.filePath(this, resource, bundle, embeddedFileInfo);
			}
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
		SortedSet<FilePathNode> results = new TreeSet<>();
		WorkspacePathNode workspacePath = new WorkspacePathNode(this);
		// Internal resources don't have files, so we won't iterate over those.
		for (WorkspaceResource resource : getAllResources(false)) {
			ResourcePathNode resourcePath = workspacePath.child(resource);
			FileBundle bundle = resource.getFileBundle();
			BundlePathNode bundlePath = resourcePath.child(bundle);
			for (FileInfo fileInfo : bundle.values()) {
				if (filter.test(fileInfo)) {
					results.add(bundlePath
							.child(fileInfo.getDirectoryName())
							.child(fileInfo));
				}
			}

			// TODO: Match 'resource.getEmbeddedResources()'
			//  - Path model does not have existing logic to support embedded resources yet.
			//    - They do not exist as entries in the file-bundle
			//    - Perhaps we should change the API for that, having them as entries, but tracked specially
			//      to allow easy access. Having them in the file-bundle would make logic simpler as there would be
			//      less edge-case work.
		}
		return results;
	}
}
