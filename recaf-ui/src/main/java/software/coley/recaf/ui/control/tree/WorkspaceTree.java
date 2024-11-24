package software.coley.recaf.ui.control.tree;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.scene.control.TreeItem;
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
import software.coley.recaf.path.PathNode;
import software.coley.recaf.path.PathNodes;
import software.coley.recaf.path.ResourcePathNode;
import software.coley.recaf.path.WorkspacePathNode;
import software.coley.recaf.services.cell.CellConfigurationService;
import software.coley.recaf.services.navigation.Actions;
import software.coley.recaf.services.workspace.WorkspaceCloseListener;
import software.coley.recaf.ui.config.KeybindingConfig;
import software.coley.recaf.ui.config.WorkspaceExplorerConfig;
import software.coley.recaf.ui.control.PathNodeTree;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.NodeEvents;
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
 * Tree view for navigating a {@link Workspace}.
 *
 * @author Matt Coley
 */
@Dependent
public class WorkspaceTree extends PathNodeTree implements
		WorkspaceModificationListener, WorkspaceCloseListener,
		ResourceJvmClassListener, ResourceAndroidClassListener, ResourceFileListener {
	private final WorkspaceExplorerConfig explorerConfig;
	private WorkspaceTreeNode root;
	private WorkspacePathNode rootPath;
	private Workspace workspace;

	/**
	 * Initialize empty tree.
	 *
	 * @param configurationService
	 * 		Service to configure cell content.
	 */
	@Inject
	public WorkspaceTree(@Nonnull CellConfigurationService configurationService, @Nonnull Actions actions,
	                     @Nonnull KeybindingConfig keys, @Nonnull WorkspaceExplorerConfig explorerConfig) {
		super(configurationService, actions);

		// Additional workspace-explorer specific bind handling
		NodeEvents.addKeyPressHandler(this, e -> {
			if (keys.getRename().match(e)) {
				TreeItem<PathNode<?>> selectedItem = getSelectionModel().getSelectedItem();
				if (selectedItem != null)
					actions.rename(selectedItem.getValue());
			}
		});

		this.explorerConfig = explorerConfig;
	}

	/**
	 * Sets the workspace, and creates a complete model for it.
	 *
	 * @param workspace
	 * 		Workspace to represent.
	 */
	public void createWorkspaceRoot(@Nullable Workspace workspace) {
		Workspace oldWorkspace = this.workspace;
		if (oldWorkspace != null) {
			// Remove listeners on old workspace
			oldWorkspace.removeWorkspaceModificationListener(this);
			for (WorkspaceResource resource : oldWorkspace.getAllResources(false))
				resource.removeListener(this);
		}

		// Update workspace reference & populate root.
		this.workspace = workspace;
		if (workspace == null) {
			root = null;
		} else {
			// Create root
			rootPath = PathNodes.workspacePath(workspace);
			root = new WorkspaceTreeNode(rootPath);
			List<WorkspaceResource> resources = workspace.getAllResources(false);
			for (WorkspaceResource resource : resources)
				createResourceSubTree(resource);

			// Add listeners
			workspace.addWorkspaceModificationListener(this);
			for (WorkspaceResource resource : resources)
				resource.addListener(this);
		}
		FxThreadUtil.run(() -> setRoot(root));
	}

	/**
	 * Adds the given resource to the tree.
	 * All paths to items contained by the resource are generated <i>(classes, files, etc)</i>.
	 *
	 * @param resource
	 * 		Resource to add to the tree.
	 */
	private void createResourceSubTree(@Nonnull WorkspaceResource resource) {
		ResourcePathNode resourcePath = rootPath.child(resource);
		resource.classBundleStream().forEach(bundle -> insertClasses(resourcePath, bundle));
		resource.fileBundleStream().forEach(bundle -> insertFiles(resourcePath, bundle));

		// Create sub-trees for embedded resources
		Map<String, WorkspaceFileResource> embeddedResources = resource.getEmbeddedResources();
		if (!embeddedResources.isEmpty()) {
			EmbeddedResourceContainerPathNode containerPath = resourcePath.embeddedChildContainer();
			embeddedResources.entrySet().stream() // Insert in sorted order of path name
					.sorted((o1, o2) -> CaseInsensitiveSimpleNaturalComparator.getInstance().compare(o1.getKey(), o2.getKey()))
					.map(Map.Entry::getValue)
					.forEach(embeddedResource -> {
						ResourcePathNode resourcePathEmbedded = containerPath.child(embeddedResource);
						embeddedResource.classBundleStream().forEach(bundle -> insertClasses(resourcePathEmbedded, bundle));
						embeddedResource.fileBundleStream().forEach(bundle -> insertFiles(resourcePathEmbedded, bundle));
					});
		}
	}

	/**
	 * @param containingResourcePath
	 * 		Path to resource holding classes.
	 * @param bundle
	 * 		Bundle of classes to insert.
	 */
	private void insertClasses(@Nonnull ResourcePathNode containingResourcePath,
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
			ClassPathNode classPath = packagePath.child(classInfo);
			WorkspaceTreeNode.getOrInsertIntoTree(root, classPath, true);
		}
	}

	/**
	 * @param containingResourcePath
	 * 		Path to resource holding files.
	 * @param bundle
	 * 		Bundle of files to insert.
	 */
	private void insertFiles(@Nonnull ResourcePathNode containingResourcePath,
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
			FilePathNode filePath = directoryPath.child(fileInfo);
			WorkspaceTreeNode.getOrInsertIntoTree(root, filePath, true);
		}
	}

	/**
	 * @param workspace
	 * 		Workspace to check.
	 *
	 * @return {@code true} when it matches our current {@link #workspace}.
	 */
	private boolean isTargetWorkspace(@Nonnull Workspace workspace) {
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

	@Override
	public void onWorkspaceClosed(@Nonnull Workspace workspace) {
		// Workspace closed, disable tree.
		if (isTargetWorkspace(workspace))
			setDisable(true);
	}

	@Override
	public void onAddLibrary(@Nonnull Workspace workspace, @Nonnull WorkspaceResource library) {
		if (isTargetWorkspace(workspace))
			createResourceSubTree(library);
	}

	@Override
	public void onRemoveLibrary(@Nonnull Workspace workspace, @Nonnull WorkspaceResource library) {
		if (isTargetWorkspace(workspace))
			FxThreadUtil.run(() -> root.removeNodeByPath(rootPath.child(library)));
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
				root.getOrCreateNodeByPath(rootPath
						.child(resource)
						.child(bundle)
						.child(interceptDirectoryName(file.getDirectoryName()))
						.child(file));
			else {
				WorkspaceResource containingResource = resource.getContainingResource();
				if (containingResource != null && isTargetResource(containingResource)) {
					root.getOrCreateNodeByPath(rootPath
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
				WorkspaceTreeNode node = root.getOrCreateNodeByPath(rootPath
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
					WorkspaceTreeNode node = root.getOrCreateNodeByPath(rootPath.child(containingResource)
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
				root.removeNodeByPath(rootPath
						.child(resource)
						.child(bundle)
						.child(interceptDirectoryName(file.getDirectoryName()))
						.child(file));
			else {
				WorkspaceResource containingResource = resource.getContainingResource();
				if (containingResource != null && isTargetResource(containingResource)) {
					root.removeNodeByPath(rootPath
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
				root.getOrCreateNodeByPath(rootPath
						.child(resource)
						.child(bundle)
						.child(interceptDirectoryName(cls.getPackageName()))
						.child(cls));
			else {
				WorkspaceResource containingResource = resource.getContainingResource();
				if (containingResource != null && isTargetResource(containingResource)) {
					root.getOrCreateNodeByPath(rootPath
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
				WorkspaceTreeNode node = root.getOrCreateNodeByPath(rootPath
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
					WorkspaceTreeNode node = root.getOrCreateNodeByPath(rootPath.child(containingResource)
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
				root.removeNodeByPath(rootPath
						.child(resource)
						.child(bundle)
						.child(interceptDirectoryName(cls.getPackageName()))
						.child(cls));
			else {
				WorkspaceResource containingResource = resource.getContainingResource();
				if (containingResource != null && isTargetResource(containingResource)) {
					root.removeNodeByPath(rootPath
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

	/**
	 * @param directory
	 * 		Input package or directory name.
	 *
	 * @return Filtered name to prevent bogus paths with thousands of embedded directories.
	 */
	@Nullable
	private String interceptDirectoryName(@Nullable String directory) {
		if (directory == null) return null;

		List<String> split = StringUtil.fastSplit(directory, true, '/');
		int max = explorerConfig.getMaxTreeDirectoryDepth();
		if (split.size() > max) {
			return StringUtil.cutOffAtNth(directory, '/', max) + "...";
		}
		return directory;
	}
}
