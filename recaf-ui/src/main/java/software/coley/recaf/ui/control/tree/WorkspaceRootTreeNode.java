package software.coley.recaf.ui.control.tree;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Workspace tree item subtype representing the root of the tree.
 * <p>
 * This root offers utilities for {@link #build() automatically building} a full representation of the workspace.
 * To filter what kinds of contents are inserted when building the model, override the {@code shouldIncludeX}
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
		getSourceChildren().clear();

		// Collect resource nodes first so we can sort them before insertion to avoid multiple sorts.
		//  - The 'buildX' methods defined here reduce redundant sorting operations otherwise done by 'visitX' methods.
		//  - The 'visitX' methods are still used for dynamic updates to the tree, but the 'buildX' methods are used for the initial population of the tree.
		List<WorkspaceTreeNode> resourceNodes = new ArrayList<>();
		for (WorkspaceResource resource : workspace.getAllResources(false)) {
			WorkspaceTreeNode resourceNode = buildResourceNode(rootPath.child(resource), resource, true);
			if (resourceNode != null)
				resourceNodes.add(resourceNode);
		}

		// Sort first then add in that exact order.
		resourceNodes.sort(WorkspaceTreeNode::compareTo);
		resourceNodes.forEach(this::addPreSortedChild);
	}

	@Nullable
	private WorkspaceTreeNode buildResourceNode(@Nonnull ResourcePathNode resourcePath,
	                                            @Nonnull WorkspaceResource resource,
	                                            boolean includeEmbedded) {
		// Skip filtered resources.
		if (!shouldIncludeResource(resourcePath, resource))
			return null;

		// Collect tree nodes generated from bundles.
		List<WorkspaceTreeNode> children = new ArrayList<>();
		resource.classBundleStream()
				.map(bundle -> buildClassBundleNode(resourcePath, bundle))
				.forEach(node -> {
					if (node != null)
						children.add(node);
				});
		resource.fileBundleStream()
				.map(bundle -> buildFileBundleNode(resourcePath, bundle))
				.forEach(node -> {
					if (node != null)
						children.add(node);
				});

		// Also collect embedded bundle contents if not filtered out.
		if (includeEmbedded && shouldIncludeEmbeddedResources(resourcePath, resource)) {
			WorkspaceTreeNode embeddedContainerNode = buildEmbeddedResourceContainerNode(resourcePath, resource);
			if (embeddedContainerNode != null)
				children.add(embeddedContainerNode);
		}

		if (children.isEmpty())
			return null;

		// Sort the trees first, then add in that exact order.
		WorkspaceTreeNode resourceNode = new WorkspaceTreeNode(resourcePath);
		children.sort(WorkspaceTreeNode::compareTo);
		children.forEach(resourceNode::addPreSortedChild);
		return resourceNode;
	}

	@Nullable
	private WorkspaceTreeNode buildEmbeddedResourceContainerNode(@Nonnull ResourcePathNode resourcePath,
	                                                             @Nonnull WorkspaceResource resource) {
		Map<String, WorkspaceFileResource> embeddedResources = resource.getEmbeddedResources();
		if (embeddedResources.isEmpty())
			return null;

		// Collect tree nodes for embedded resources in sorted order.
		EmbeddedResourceContainerPathNode containerPath = resourcePath.embeddedChildContainer();
		List<WorkspaceTreeNode> embeddedResourceNodes = new ArrayList<>();
		embeddedResources.entrySet().stream()
				.sorted((o1, o2) -> Named.STRING_PATH_COMPARATOR.compare(o1.getKey(), o2.getKey()))
				.forEach(entry -> {
					WorkspaceFileResource embeddedResource = entry.getValue();
					WorkspaceTreeNode embeddedResourceNode = buildResourceNode(containerPath.child(embeddedResource), embeddedResource, false);
					if (embeddedResourceNode != null)
						embeddedResourceNodes.add(embeddedResourceNode);
				});

		if (embeddedResourceNodes.isEmpty())
			return null;

		// Again, add them in exact order.
		WorkspaceTreeNode containerNode = new WorkspaceTreeNode(containerPath);
		embeddedResourceNodes.forEach(containerNode::addPreSortedChild);
		return containerNode;
	}

	@Nullable
	private WorkspaceTreeNode buildClassBundleNode(@Nonnull ResourcePathNode containingResourcePath,
	                                               @Nonnull ClassBundle<?> bundle) {
		if (bundle.isEmpty() || !shouldIncludeClasses(containingResourcePath, bundle))
			return null;

		// Collect tree nodes for classes in the bundle, grouped by package.
		BundlePathNode bundlePath = containingResourcePath.child(bundle);
		WorkspaceTreeNode bundleNode = new WorkspaceTreeNode(bundlePath);
		Map<String, WorkspaceTreeNode> packages = new HashMap<>();
		for (ClassInfo classInfo : bundle.values()) {
			DirectoryPathNode packagePath = bundlePath.child(interceptDirectoryName(classInfo.getPackageName()));
			if (!shouldIncludeClass(packagePath, classInfo))
				continue;
			WorkspaceTreeNode packageNode = getOrCreateDirectoryNode(bundleNode, packagePath, packages);
			packageNode.addPreSortedChild(new WorkspaceTreeNode(packagePath.child(classInfo)));
		}

		if (bundleNode.isSourceLeaf())
			return null;

		// Sort this subtree and we're done.
		sortTree(bundleNode);
		return bundleNode;
	}

	@Nullable
	private WorkspaceTreeNode buildFileBundleNode(@Nonnull ResourcePathNode containingResourcePath,
	                                              @Nonnull FileBundle bundle) {
		if (bundle.isEmpty() || !shouldIncludeFiles(containingResourcePath, bundle))
			return null;

		// Collect tree nodes for files in the bundle, grouped by directory.
		BundlePathNode bundlePath = containingResourcePath.child(bundle);
		WorkspaceTreeNode bundleNode = new WorkspaceTreeNode(bundlePath);
		Map<String, WorkspaceTreeNode> directories = new HashMap<>();
		for (FileInfo fileInfo : bundle.values()) {
			DirectoryPathNode directoryPath = bundlePath.child(interceptDirectoryName(fileInfo.getDirectoryName()));
			if (!shouldIncludeFile(directoryPath, fileInfo))
				continue;
			WorkspaceTreeNode directoryNode = getOrCreateDirectoryNode(bundleNode, directoryPath, directories);
			directoryNode.addPreSortedChild(new WorkspaceTreeNode(directoryPath.child(fileInfo)));
		}

		if (bundleNode.isSourceLeaf())
			return null;

		// Sort this subtree and we're done.
		sortTree(bundleNode);
		return bundleNode;
	}

	@Nonnull
	private WorkspaceTreeNode getOrCreateDirectoryNode(@Nonnull WorkspaceTreeNode bundleNode,
	                                                   @Nonnull DirectoryPathNode directoryPath,
	                                                   @Nonnull Map<String, WorkspaceTreeNode> directories) {
		// Skip if already exists.
		String fullDirectory = directoryPath.getValue();
		WorkspaceTreeNode existingDirectoryNode = directories.get(fullDirectory);
		if (existingDirectoryNode != null)
			return existingDirectoryNode;

		// Create nodes for each directory in the path if they don't already exist, and return the node for the full path.
		WorkspaceTreeNode node = bundleNode;
		String[] directoryParts = fullDirectory.split("/", -1);
		StringBuilder directoryBuilder = new StringBuilder();
		for (String directoryPart : directoryParts) {
			directoryBuilder.append(directoryPart).append('/');
			String directoryName = directoryBuilder.substring(0, directoryBuilder.length() - 1);

			WorkspaceTreeNode childNode = directories.get(directoryName);
			if (childNode == null) {
				childNode = new WorkspaceTreeNode(directoryPath.withDirectory(directoryName));
				directories.put(directoryName, childNode);
				node.addPreSortedChild(childNode);
			}
			node = childNode;
		}
		return node;
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
		if (!shouldIncludeResource(resourcePath, resource))
			return;

		resource.classBundleStream()
				.filter(bundle -> shouldIncludeClasses(resourcePath, bundle))
				.forEach(bundle -> visitClasses(resourcePath, bundle));
		resource.fileBundleStream()
				.filter(bundle -> shouldIncludeFiles(resourcePath, bundle))
				.forEach(bundle -> visitFiles(resourcePath, bundle));

		// Create subtrees for embedded resources
		Map<String, WorkspaceFileResource> embeddedResources = resource.getEmbeddedResources();
		if (!embeddedResources.isEmpty() && shouldIncludeEmbeddedResources(resourcePath, resource)) {
			EmbeddedResourceContainerPathNode containerPath = resourcePath.embeddedChildContainer();
			embeddedResources.entrySet().stream() // Insert in sorted order of path name
					.sorted((o1, o2) -> Named.STRING_PATH_COMPARATOR.compare(o1.getKey(), o2.getKey()))
					.map(Map.Entry::getValue)
					.forEach(embeddedResource -> {
						ResourcePathNode resourcePathEmbedded = containerPath.child(embeddedResource);
						if (shouldIncludeResource(resourcePathEmbedded, embeddedResource)) {
							embeddedResource.classBundleStream()
									.filter(bundle -> shouldIncludeClasses(resourcePathEmbedded, bundle))
									.forEach(bundle -> visitClasses(resourcePathEmbedded, bundle));
							embeddedResource.fileBundleStream()
									.filter(bundle -> shouldIncludeFiles(resourcePathEmbedded, bundle))
									.forEach(bundle -> visitFiles(resourcePathEmbedded, bundle));
						}
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
		for (ClassInfo classInfo : bundle.values()) {
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
		if (!shouldIncludeClass(packagePath, classInfo))
			return;
		ClassPathNode classPath = packagePath.child(classInfo);
		WorkspaceTreeNode.getOrInsertIntoTree(this, classPath);
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
		for (FileInfo fileInfo : bundle.values()) {
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
		if (!shouldIncludeFile(directoryPath, fileInfo))
			return;
		FilePathNode filePath = directoryPath.child(fileInfo);
		WorkspaceTreeNode.getOrInsertIntoTree(this, filePath);
	}

	/**
	 * @param resourcePath
	 * 		Resource path.
	 * @param resource
	 * 		Resource to include.
	 *
	 * @return {@code true} when the resource should be included in the tree.
	 */
	protected boolean shouldIncludeResource(@Nonnull ResourcePathNode resourcePath, @Nonnull WorkspaceResource resource) {
		return true;
	}

	/**
	 * @param containingResourcePath
	 * 		Path to resource holding classes.
	 * @param bundle
	 * 		Bundle of classes to include.
	 *
	 * @return {@code true} when the class bundle should be included in the tree.
	 */
	protected boolean shouldIncludeClasses(@Nonnull ResourcePathNode containingResourcePath, @Nonnull ClassBundle<?> bundle) {
		return true;
	}

	/**
	 * @param packagePath
	 * 		Path of the class's containing package.
	 * @param classInfo
	 * 		Class to include.
	 *
	 * @return {@code true} when the class should be included in the tree.
	 */
	protected boolean shouldIncludeClass(@Nonnull DirectoryPathNode packagePath, @Nonnull ClassInfo classInfo) {
		return true;
	}

	/**
	 * @param containingResourcePath
	 * 		Path to resource holding files.
	 * @param bundle
	 * 		Bundle of files to include.
	 *
	 * @return {@code true} when the file bundle should be included in the tree.
	 */
	protected boolean shouldIncludeFiles(@Nonnull ResourcePathNode containingResourcePath, @Nonnull FileBundle bundle) {
		return true;
	}

	/**
	 * @param directoryPath
	 * 		Path of the file's containing directory.
	 * @param fileInfo
	 * 		File to include.
	 *
	 * @return {@code true} when the file should be included in the tree.
	 */
	protected boolean shouldIncludeFile(@Nonnull DirectoryPathNode directoryPath, @Nonnull FileInfo fileInfo) {
		return true;
	}

	/**
	 * @param resourcePath
	 * 		Resource path.
	 * @param resource
	 * 		Resource to include embedded resources from.
	 *
	 * @return {@code true} when the resource's embedded resources should be included in the tree.
	 */
	protected boolean shouldIncludeEmbeddedResources(@Nonnull ResourcePathNode resourcePath, @Nonnull WorkspaceResource resource) {
		return true;
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

	/**
	 * Recursively sort the given tree node and all of its children.
	 *
	 * @param node
	 * 		Tree node to sort.
	 */
	private static void sortTree(@Nonnull WorkspaceTreeNode node) {
		node.sortChildren(WorkspaceTreeNode::compareTo);
		for (var child : node.getSourceChildren())
			if (child instanceof WorkspaceTreeNode childNode)
				sortTree(childNode);
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
