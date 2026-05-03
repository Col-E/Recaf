package software.coley.recaf.workspace;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.AndroidClassInfo;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.properties.Property;
import software.coley.recaf.workspace.model.BasicWorkspace;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.AndroidClassBundle;
import software.coley.recaf.workspace.model.bundle.BasicAndroidClassBundle;
import software.coley.recaf.workspace.model.bundle.BasicFileBundle;
import software.coley.recaf.workspace.model.bundle.BasicJvmClassBundle;
import software.coley.recaf.workspace.model.bundle.BasicVersionedJvmClassBundle;
import software.coley.recaf.workspace.model.bundle.FileBundle;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.bundle.VersionedJvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceDirectoryResource;
import software.coley.recaf.workspace.model.resource.WorkspaceDirectoryResourceBuilder;
import software.coley.recaf.workspace.model.resource.WorkspaceFileResource;
import software.coley.recaf.workspace.model.resource.WorkspaceFileResourceBuilder;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;
import software.coley.recaf.workspace.model.resource.WorkspaceResourceBuilder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Utility for promoting an existing supporting {@link WorkspaceResource} into the primary resource of a new workspace.
 *
 * @author Matt Coley
 */
public class WorkspacePromotion {
	private WorkspacePromotion() {}

	/**
	 * @param workspace
	 * 		Source workspace.
	 * @param resource
	 * 		Resource to promote into the new workspace's primary role.
	 *
	 * @return New workspace with a cloned version of the given resource as primary.
	 */
	@Nonnull
	public static Workspace promoteResourceToWorkspace(@Nonnull Workspace workspace, @Nonnull WorkspaceResource resource) {
		// I don't think this sort of use case is really common enough, so all the cloning logic will exist here
		// rather than in the workspace model types. Plus this is kind of an antipattern that I don't want to encourage.
		//
		// Anyways, we just need to deep-copy everything and shift the primary resource over.
		// We don't reuse the same resource objects due to possible property and listener weirdness that could come with that.
		WorkspaceResource primary = cloneResource(resource);
		List<WorkspaceResource> supporting = new ArrayList<>();
		supporting.add(cloneResource(workspace.getPrimaryResource()));
		for (WorkspaceResource supportingResource : workspace.getSupportingResources()) {
			if (supportingResource == resource)
				continue;
			supporting.add(cloneResource(supportingResource));
		}
		return new BasicWorkspace(primary, supporting);
	}

	@Nonnull
	private static WorkspaceResource cloneResource(@Nonnull WorkspaceResource resource) {
		WorkspaceResourceBuilder builder = resourceBuilder(resource)
				.withJvmClassBundle(cloneJvmBundle(resource.getJvmClassBundle()))
				.withVersionedJvmClassBundles(cloneVersionedJvmBundles(resource.getVersionedJvmClassBundles()))
				.withAndroidClassBundles(cloneAndroidBundles(resource.getAndroidClassBundles()))
				.withFileBundle(cloneFileBundle(resource.getFileBundle()))
				.withEmbeddedResources(cloneEmbeddedResources(resource.getEmbeddedResources()));
		WorkspaceResource clone = builder.build();
		copyProperties(resource, clone);
		return clone;
	}

	@Nonnull
	private static WorkspaceResourceBuilder resourceBuilder(@Nonnull WorkspaceResource resource) {
		if (resource instanceof WorkspaceFileResource fileResource) {
			FileInfo fileInfo = cloneFileInfo(fileResource.getFileInfo());
			return new WorkspaceFileResourceBuilder().withFileInfo(fileInfo);
		}
		if (resource instanceof WorkspaceDirectoryResource directoryResource)
			return new WorkspaceDirectoryResourceBuilder().withDirectoryPath(directoryResource.getDirectoryPath());
		return new WorkspaceResourceBuilder();
	}

	@Nonnull
	private static JvmClassBundle cloneJvmBundle(@Nonnull JvmClassBundle bundle) {
		BasicJvmClassBundle clone = new BasicJvmClassBundle();
		for (JvmClassInfo classInfo : bundle.valuesAsCopy())
			clone.put(cloneJvmClassInfo(classInfo));
		return clone;
	}

	@Nonnull
	private static NavigableMap<Integer, VersionedJvmClassBundle> cloneVersionedJvmBundles(@Nonnull NavigableMap<Integer, VersionedJvmClassBundle> bundles) {
		NavigableMap<Integer, VersionedJvmClassBundle> clone = new TreeMap<>();
		for (Map.Entry<Integer, VersionedJvmClassBundle> entry : bundles.entrySet()) {
			BasicVersionedJvmClassBundle versionedClone = new BasicVersionedJvmClassBundle(entry.getKey());
			for (JvmClassInfo classInfo : entry.getValue().valuesAsCopy())
				versionedClone.put(cloneJvmClassInfo(classInfo));
			clone.put(entry.getKey(), versionedClone);
		}
		return clone;
	}

	@Nonnull
	private static Map<String, AndroidClassBundle> cloneAndroidBundles(@Nonnull Map<String, AndroidClassBundle> bundles) {
		Map<String, AndroidClassBundle> clone = new LinkedHashMap<>();
		for (Map.Entry<String, AndroidClassBundle> entry : bundles.entrySet()) {
			AndroidClassBundle bundle = entry.getValue();
			BasicAndroidClassBundle bundleClone = new BasicAndroidClassBundle(bundle.getVersion(), bundle.getLinkData().clone());
			for (AndroidClassInfo classInfo : bundle.valuesAsCopy())
				bundleClone.put(cloneAndroidClassInfo(classInfo));
			clone.put(entry.getKey(), bundleClone);
		}
		return clone;
	}

	@Nonnull
	private static FileBundle cloneFileBundle(@Nonnull FileBundle bundle) {
		BasicFileBundle clone = new BasicFileBundle();
		for (FileInfo fileInfo : bundle.valuesAsCopy())
			clone.put(cloneFileInfo(fileInfo));
		return clone;
	}

	@Nonnull
	private static Map<String, WorkspaceFileResource> cloneEmbeddedResources(@Nonnull Map<String, WorkspaceFileResource> embeddedResources) {
		Map<String, WorkspaceFileResource> clone = new LinkedHashMap<>();
		for (Map.Entry<String, WorkspaceFileResource> entry : embeddedResources.entrySet()) {
			clone.put(entry.getKey(), (WorkspaceFileResource) cloneResource(entry.getValue()));
		}
		return clone;
	}

	@Nonnull
	private static JvmClassInfo cloneJvmClassInfo(@Nonnull JvmClassInfo classInfo) {
		return classInfo.toJvmClassBuilder().build();
	}

	@Nonnull
	private static AndroidClassInfo cloneAndroidClassInfo(@Nonnull AndroidClassInfo classInfo) {
		return classInfo.toAndroidBuilder().build();
	}

	@Nonnull
	private static FileInfo cloneFileInfo(@Nonnull FileInfo fileInfo) {
		return fileInfo.toFileBuilder().build();
	}

	private static void copyProperties(@Nonnull WorkspaceResource source, @Nonnull WorkspaceResource destination) {
		for (Property<?> property : source.getProperties().values())
			if (property.persistent())
				destination.setProperty(property);
	}
}
