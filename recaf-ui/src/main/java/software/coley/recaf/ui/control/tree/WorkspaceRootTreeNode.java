package software.coley.recaf.ui.control.tree;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import net.greypanther.natsort.CaseInsensitiveSimpleNaturalComparator;
import software.coley.recaf.info.AndroidClassInfo;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.Named;
import software.coley.recaf.path.BundlePathNode;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.DirectoryPathNode;
import software.coley.recaf.path.EmbeddedResourceContainerPathNode;
import software.coley.recaf.path.FilePathNode;
import software.coley.recaf.path.ResourcePathNode;
import software.coley.recaf.path.WorkspacePathNode;
import software.coley.recaf.ui.config.WorkspaceExplorerConfig;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.StringUtil;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.WorkspaceModificationListener;
import software.coley.recaf.workspace.model.bundle.AndroidClassBundle;
import software.coley.recaf.workspace.model.bundle.ClassBundle;
import software.coley.recaf.workspace.model.bundle.FileBundle;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.ResourceAndroidClassListener;
import software.coley.recaf.workspace.model.resource.ResourceFileListener;
import software.coley.recaf.workspace.model.resource.ResourceJvmClassListener;
import software.coley.recaf.workspace.model.resource.WorkspaceFileResource;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Workspace tree item subtype representing the root of the tree.
 * <p/>
 * This root offers utilities for {@link #build() automatically building} a full representation of the workspace.
 * To filter what kinds of contents are inserted when building the model, you should override the {@code visitX}
 * methods in a child class.
 *
 * @author Matt Coley
 */
public class WorkspaceRootTreeNode extends WorkspaceTreeNode {
	private final WorkspaceExplorerConfig explorerConfig;
	private final WorkspacePathNode rootPath;
	private final Workspace workspace;
	private final ListenerHost listenerHost = new ListenerHost();

	/**
	 * Create new node with path value.
	 *
	 * @param rootPath
	 * 		Path to workspace root.
	 */
	public WorkspaceRootTreeNode(@Nonnull WorkspaceExplorerConfig explorerConfig, @Nonnull WorkspacePathNode rootPath) {
		super(rootPath);

		this.explorerConfig = explorerConfig;
		this.rootPath = rootPath;
		workspace = rootPath.getValue();
	}

	/**
	 * Build the tree model from the associated {@link Workspace}.
	 */
	public void build() {
		List<WorkspaceResource> resources = workspace.getAllResources(false);
		for (WorkspaceResource resource : resources)
			visitResource(resource);
	}

	/**
	 * Register listeners on the associated {@link Workspace} to facilitate automatic updates to this tree model.
	 */
	public void addWorkspaceListeners() {
		Workspace workspace = rootPath.getValue();

		// Add listeners
		workspace.addWorkspaceModificationListener(listenerHost);
		for (WorkspaceResource resource : workspace.getAllResources(false))
			resource.addListener(listenerHost);
	}

	/**
	 * Unregister listeners on the associated {@link Workspace}.
	 */
	public void removeWorkspaceListeners() {
		Workspace workspace = rootPath.getValue();

		// Remove listeners
		workspace.removeWorkspaceModificationListener(listenerHost);
		for (WorkspaceResource resource : workspace.getAllResources(false))
			resource.removeListener(listenerHost);
	}

	/**
	 * Adds the given resource to the tree.
	 * All paths to items contained by the resource are generated <i>(classes, files, etc)</i>.
	 *
	 * @param resource
	 * 		Resource to add to the tree.
	 */
	protected void visitResource(@Nonnull WorkspaceResource resource) {
		ResourcePathNode resourcePath = rootPath.child(resource);
		resource.classBundleStream().forEach(bundle -> visitClasses(resourcePath, bundle));
		resource.fileBundleStream().forEach(bundle -> visitFiles(resourcePath, bundle));

		// Create sub-trees for embedded resources
		Map<String, WorkspaceFileResource> embeddedResources = resource.getEmbeddedResources();
		if (!embeddedResources.isEmpty()) {
			EmbeddedResourceContainerPathNode containerPath = resourcePath.embeddedChildContainer();
			embeddedResources.entrySet().stream() // Insert in sorted order of path name
					.sorted((o1, o2) -> CaseInsensitiveSimpleNaturalComparator.getInstance().compare(o1.getKey(), o2.getKey()))
					.map(Map.Entry::getValue)
					.forEach(embeddedResource -> {
						ResourcePathNode resourcePathEmbedded = containerPath.child(embeddedResource);
						embeddedResource.classBundleStream().forEach(bundle -> visitClasses(resourcePathEmbedded, bundle));
						embeddedResource.fileBundleStream().forEach(bundle -> visitFiles(resourcePathEmbedded, bundle));
					});
		}
	}

	/**
	 * Adds the given class bundle to the tree.
	 *
	 * @param containingResourcePath
	 * 		Path to resource holding classes.
	 * @param bundle
	 * 		Bundle of classes to insert.
	 */
	protected void visitClasses(@Nonnull ResourcePathNode containingResourcePath,
	                            @Nonnull ClassBundle<?> bundle) {
		Map<String, DirectoryPathNode> directories = new HashMap<>();
		BundlePathNode bundlePath = containingResourcePath.child(bundle);

		// Pre-sort classes to skip tree-building comparisons/synchronizations.
		TreeSet<ClassInfo> sortedClasses = new TreeSet<>(Named.NAME_PATH_COMPARATOR);
		sortedClasses.addAll(bundle.values());

		// Add each class in sorted order.
		for (ClassInfo classInfo : sortedClasses) {
			String packageName = interceptDirectoryName(classInfo.getPackageName());
			DirectoryPathNode packagePath = directories.computeIfAbsent(packageName, bundlePath::child);
			visitClass(packagePath, classInfo);
		}
	}

	/**
	 * Adds the given class to the tree.
	 *
	 * @param packagePath
	 * 		Path of the class's containing package.
	 * @param classInfo
	 * 		Class to insert into the tree.
	 */
	protected void visitClass(@Nonnull DirectoryPathNode packagePath, @Nonnull ClassInfo classInfo) {
		ClassPathNode classPath = packagePath.child(classInfo);
		WorkspaceTreeNode.getOrInsertIntoTree(this, classPath, true);
	}

	/**
	 * Adds the given file bundle to the tree.
	 *
	 * @param containingResourcePath
	 * 		Path to resource holding files.
	 * @param bundle
	 * 		Bundle of files to insert.
	 */
	protected void visitFiles(@Nonnull ResourcePathNode containingResourcePath,
	                          @Nonnull FileBundle bundle) {
		Map<String, DirectoryPathNode> directories = new HashMap<>();
		BundlePathNode bundlePath = containingResourcePath.child(bundle);

		// Pre-sort classes to skip tree-building comparisons/synchronizations.
		TreeSet<FileInfo> sortedFiles = new TreeSet<>(Named.NAME_PATH_COMPARATOR);
		sortedFiles.addAll(bundle.values());

		// Add each file in sorted order.
		for (FileInfo fileInfo : sortedFiles) {
			String directoryName = interceptDirectoryName(fileInfo.getDirectoryName());
			DirectoryPathNode directoryPath = directories.computeIfAbsent(directoryName, bundlePath::child);
			visitFile(directoryPath, fileInfo);
		}
	}

	/**
	 * Adds the given file to the tree.
	 *
	 * @param directoryPath
	 * 		Path of the file's containing directory.
	 * @param fileInfo
	 * 		File to insert into the tree.
	 */
	protected void visitFile(@Nonnull DirectoryPathNode directoryPath, @Nonnull FileInfo fileInfo) {
		FilePathNode filePath = directoryPath.child(fileInfo);
		WorkspaceTreeNode.getOrInsertIntoTree(this, filePath, true);
	}

	/**
	 * @param directory
	 * 		Input package or directory name.
	 *
	 * @return Filtered name to prevent bogus paths with thousands of embedded directories.
	 */
	@Nullable
	private String interceptDirectoryName(@Nullable String directory) {
		if (directory == null)
			return null;
		List<String> split = StringUtil.fastSplit(directory, true, '/');
		int max = explorerConfig.getMaxTreeDirectoryDepth();
		if (split.size() > max)
			return StringUtil.cutOffAtNth(directory, '/', max) + "...";
		return directory;
	}

	/**
	 * @param workspace
	 * 		Workspace to check.
	 *
	 * @return {@code true} when it matches our current {@link #workspace}.
	 */
	public boolean isTargetWorkspace(@Nonnull Workspace workspace) {
		return this.workspace == workspace;
	}

	/**
	 * @param resource
	 * 		Resource to check.
	 *
	 * @return {@code true} when it belongs to the target workspace.
	 */
	private boolean isTargetResource(@Nonnull WorkspaceResource resource) {
		if (workspace.getPrimaryResource() == resource)
			return true;
		for (WorkspaceResource supportingResource : workspace.getSupportingResources()) {
			if (supportingResource == resource)
				return true;
		}
		for (WorkspaceResource internalSupportingResource : workspace.getInternalSupportingResources()) {
			if (internalSupportingResource == resource)
				return true;
		}
		return false;
	}

	private class ListenerHost implements WorkspaceModificationListener, ResourceJvmClassListener, ResourceAndroidClassListener, ResourceFileListener {

		@Override
		public void onAddLibrary(@Nonnull Workspace workspace, @Nonnull WorkspaceResource library) {
			if (isTargetWorkspace(workspace))
				FxThreadUtil.run(() -> visitResource(library));
		}

		@Override
		public void onRemoveLibrary(@Nonnull Workspace workspace, @Nonnull WorkspaceResource library) {
			if (isTargetWorkspace(workspace))
				FxThreadUtil.run(() -> removeNodeByPath(rootPath.child(library)));
		}

		@Override
		public void onNewClass(@Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle, @Nonnull JvmClassInfo cls) {
			newClass(resource, bundle, cls);
		}

		@Override
		public void onUpdateClass(@Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle, @Nonnull JvmClassInfo oldCls, @Nonnull JvmClassInfo newCls) {
			updateClass(resource, bundle, oldCls, newCls);
		}

		@Override
		public void onRemoveClass(@Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle, @Nonnull JvmClassInfo cls) {
			removeClass(resource, bundle, cls);
		}

		@Override
		public void onNewClass(@Nonnull WorkspaceResource resource, @Nonnull AndroidClassBundle bundle, @Nonnull AndroidClassInfo cls) {
			newClass(resource, bundle, cls);
		}

		@Override
		public void onUpdateClass(@Nonnull WorkspaceResource resource, @Nonnull AndroidClassBundle bundle, @Nonnull AndroidClassInfo oldCls, @Nonnull AndroidClassInfo newCls) {
			updateClass(resource, bundle, oldCls, newCls);
		}

		@Override
		public void onRemoveClass(@Nonnull WorkspaceResource resource, @Nonnull AndroidClassBundle bundle, @Nonnull AndroidClassInfo cls) {
			removeClass(resource, bundle, cls);
		}

		@Override
		public void onNewFile(@Nonnull WorkspaceResource resource, @Nonnull FileBundle bundle, @Nonnull FileInfo file) {
			FxThreadUtil.run(() -> {
				if (isTargetResource(resource))
					getOrCreateNodeByPath(rootPath
							.child(resource)
							.child(bundle)
							.child(interceptDirectoryName(file.getDirectoryName()))
							.child(file));
				else {
					WorkspaceResource containingResource = resource.getContainingResource();
					if (containingResource != null && isTargetResource(containingResource)) {
						getOrCreateNodeByPath(rootPath
								.child(containingResource)
								.embeddedChildContainer()
								.child(resource)
								.child(bundle)
								.child(interceptDirectoryName(file.getDirectoryName()))
								.child(file));
					}
				}
			});
		}

		@Override
		public void onUpdateFile(@Nonnull WorkspaceResource resource, @Nonnull FileBundle bundle, @Nonnull FileInfo oldFile, @Nonnull FileInfo newFile) {
			FxThreadUtil.run(() -> {
				if (isTargetResource(resource)) {
					WorkspaceTreeNode node = getOrCreateNodeByPath(rootPath
							.child(resource)
							.child(bundle)
							.child(interceptDirectoryName(oldFile.getDirectoryName()))
							.child(oldFile));
					node.setValue(rootPath
							.child(resource)
							.child(bundle)
							.child(newFile.getDirectoryName())
							.child(newFile));
				} else {
					WorkspaceResource containingResource = resource.getContainingResource();
					if (containingResource != null && isTargetResource(containingResource)) {
						WorkspaceTreeNode node = getOrCreateNodeByPath(rootPath.child(containingResource)
								.embeddedChildContainer()
								.child(resource)
								.child(bundle)
								.child(interceptDirectoryName(oldFile.getDirectoryName()))
								.child(oldFile));
						node.setValue(rootPath.child(containingResource)
								.embeddedChildContainer()
								.child(resource)
								.child(bundle)
								.child(interceptDirectoryName(newFile.getDirectoryName()))
								.child(newFile));
					}
				}
			});
		}

		@Override
		public void onRemoveFile(@Nonnull WorkspaceResource resource, @Nonnull FileBundle bundle, @Nonnull FileInfo file) {
			FxThreadUtil.run(() -> {
				if (isTargetResource(resource))
					removeNodeByPath(rootPath
							.child(resource)
							.child(bundle)
							.child(interceptDirectoryName(file.getDirectoryName()))
							.child(file));
				else {
					WorkspaceResource containingResource = resource.getContainingResource();
					if (containingResource != null && isTargetResource(containingResource)) {
						removeNodeByPath(rootPath
								.child(containingResource)
								.embeddedChildContainer()
								.child(resource)
								.child(bundle)
								.child(interceptDirectoryName(file.getDirectoryName()))
								.child(file));
					}
				}
			});
		}

		private void newClass(@Nonnull WorkspaceResource resource, @Nonnull ClassBundle<?> bundle, @Nonnull ClassInfo cls) {
			FxThreadUtil.run(() -> {
				if (isTargetResource(resource))
					getOrCreateNodeByPath(rootPath
							.child(resource)
							.child(bundle)
							.child(interceptDirectoryName(cls.getPackageName()))
							.child(cls));
				else {
					WorkspaceResource containingResource = resource.getContainingResource();
					if (containingResource != null && isTargetResource(containingResource)) {
						getOrCreateNodeByPath(rootPath
								.child(containingResource)
								.embeddedChildContainer()
								.child(resource)
								.child(bundle)
								.child(interceptDirectoryName(cls.getPackageName()))
								.child(cls));
					}
				}
			});
		}

		private void updateClass(@Nonnull WorkspaceResource resource, @Nonnull ClassBundle<?> bundle, @Nonnull ClassInfo oldCls, @Nonnull ClassInfo newCls) {
			FxThreadUtil.run(() -> {
				if (isTargetResource(resource)) {
					WorkspaceTreeNode node = getOrCreateNodeByPath(rootPath
							.child(resource)
							.child(bundle)
							.child(interceptDirectoryName(oldCls.getPackageName()))
							.child(oldCls));
					node.setValue(rootPath
							.child(resource)
							.child(bundle)
							.child(newCls.getPackageName())
							.child(newCls));
				} else {
					WorkspaceResource containingResource = resource.getContainingResource();
					if (containingResource != null && isTargetResource(containingResource)) {
						WorkspaceTreeNode node = getOrCreateNodeByPath(rootPath.child(containingResource)
								.embeddedChildContainer()
								.child(resource)
								.child(bundle)
								.child(interceptDirectoryName(oldCls.getPackageName()))
								.child(oldCls));
						node.setValue(rootPath.child(containingResource)
								.embeddedChildContainer()
								.child(resource)
								.child(bundle)
								.child(interceptDirectoryName(newCls.getPackageName()))
								.child(newCls));
					}
				}
			});
		}

		private void removeClass(@Nonnull WorkspaceResource resource, @Nonnull ClassBundle<?> bundle, @Nonnull ClassInfo cls) {
			FxThreadUtil.run(() -> {
				if (isTargetResource(resource))
					removeNodeByPath(rootPath
							.child(resource)
							.child(bundle)
							.child(interceptDirectoryName(cls.getPackageName()))
							.child(cls));
				else {
					WorkspaceResource containingResource = resource.getContainingResource();
					if (containingResource != null && isTargetResource(containingResource)) {
						removeNodeByPath(rootPath
								.child(containingResource)
								.embeddedChildContainer()
								.child(resource)
								.child(bundle)
								.child(interceptDirectoryName(cls.getPackageName()))
								.child(cls));
					}
				}
			});
		}
	}
}
