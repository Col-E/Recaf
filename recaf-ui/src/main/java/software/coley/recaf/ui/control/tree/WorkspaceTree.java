package software.coley.recaf.ui.control.tree;

import atlantafx.base.theme.Styles;
import atlantafx.base.theme.Tweaks;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;
import software.coley.recaf.info.AndroidClassInfo;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.path.*;
import software.coley.recaf.services.cell.CellConfigurationService;
import software.coley.recaf.services.cell.ContextSource;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.workspace.WorkspaceCloseListener;
import software.coley.recaf.workspace.WorkspaceModificationListener;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.AndroidClassBundle;
import software.coley.recaf.workspace.model.bundle.FileBundle;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.ResourceAndroidClassListener;
import software.coley.recaf.workspace.model.resource.ResourceFileListener;
import software.coley.recaf.workspace.model.resource.ResourceJvmClassListener;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.List;

/**
 * Tree view for items within a {@link Workspace}.
 *
 * @author Matt Coley
 */
@Dependent
public class WorkspaceTree extends TreeView<PathNode<?>> implements
		WorkspaceModificationListener, WorkspaceCloseListener,
		ResourceJvmClassListener, ResourceAndroidClassListener, ResourceFileListener {
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
	public WorkspaceTree(@Nonnull CellConfigurationService configurationService) {
		setShowRoot(false);
		setCellFactory(param -> new WorkspaceTreeCell(ContextSource.DECLARATION, configurationService));
		getStyleClass().addAll(Tweaks.EDGE_TO_EDGE, Styles.DENSE);
		setOnKeyPressed(e -> {
			KeyCode code = e.getCode();
			if (code == KeyCode.RIGHT || code == KeyCode.KP_RIGHT) {
				TreeItem<PathNode<?>> selected = getSelectionModel().getSelectedItem();
				if (selected != null)
					TreeItems.recurseOpen(selected);
			}
		});
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
			rootPath = new WorkspacePathNode(workspace);
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
	private void createResourceSubTree(WorkspaceResource resource) {
		ResourcePathNode resourcePath = rootPath.child(resource);
		resource.classBundleStream().forEach(bundle -> {
			BundlePathNode bundlePath = resourcePath.child(bundle);
			for (ClassInfo classInfo : bundle.values()) {
				String packageName = classInfo.getPackageName();
				DirectoryPathNode packagePath = bundlePath.child(packageName);
				ClassPathNode classPath = packagePath.child(classInfo);
				root.getOrCreateNodeByPath(classPath);
			}
		});
		resource.fileBundleStream().forEach(bundle -> {
			BundlePathNode bundlePath = resourcePath.child(bundle);
			for (FileInfo fileInfo : bundle.values()) {
				String directoryName = fileInfo.getDirectoryName();
				DirectoryPathNode directoryPath = bundlePath.child(directoryName);
				FilePathNode filePath = directoryPath.child(fileInfo);
				root.getOrCreateNodeByPath(filePath);
			}
		});
	}

	/**
	 * @param workspace
	 * 		Workspace to check.
	 *
	 * @return {@code true} when it matches our current {@link #workspace}.
	 */
	private boolean isTargetWorkspace(Workspace workspace) {
		return this.workspace == workspace;
	}

	/**
	 * @param resource
	 * 		Resource to check.
	 *
	 * @return {@code true} when it belongs to the target workspace.
	 */
	private boolean isTargetResource(WorkspaceResource resource) {
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
			root.removeNodeByPath(rootPath.child(library));
	}

	@Override
	public void onNewClass(@Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle, @Nonnull JvmClassInfo cls) {
		if (isTargetResource(resource))
			root.getOrCreateNodeByPath(rootPath.child(resource).child(bundle).child(cls.getPackageName()).child(cls));
	}

	@Override
	public void onUpdateClass(@Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle, @Nonnull JvmClassInfo oldCls, @Nonnull JvmClassInfo newCls) {
		if (isTargetResource(resource)) {
			WorkspaceTreeNode node = root.getOrCreateNodeByPath(rootPath.child(resource).child(bundle).child(oldCls.getPackageName()).child(oldCls));
			node.setValue(rootPath.child(resource).child(bundle).child(newCls.getPackageName()).child(newCls));
		}
	}

	@Override
	public void onRemoveClass(@Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle, @Nonnull JvmClassInfo cls) {
		if (isTargetResource(resource))
			root.removeNodeByPath(rootPath.child(resource).child(bundle).child(cls.getPackageName()).child(cls));
	}

	@Override
	public void onNewClass(@Nonnull WorkspaceResource resource, @Nonnull AndroidClassBundle bundle, @Nonnull AndroidClassInfo cls) {
		if (isTargetResource(resource))
			root.getOrCreateNodeByPath(rootPath.child(resource).child(bundle).child(cls.getPackageName()).child(cls));
	}

	@Override
	public void onUpdateClass(@Nonnull WorkspaceResource resource, @Nonnull AndroidClassBundle bundle, @Nonnull AndroidClassInfo oldCls, @Nonnull AndroidClassInfo newCls) {
		if (isTargetResource(resource)) {
			WorkspaceTreeNode node = root.getOrCreateNodeByPath(rootPath.child(resource).child(bundle).child(oldCls.getPackageName()).child(oldCls));
			node.setValue(rootPath.child(resource).child(bundle).child(newCls.getPackageName()).child(newCls));
		}
	}

	@Override
	public void onRemoveClass(@Nonnull WorkspaceResource resource, @Nonnull AndroidClassBundle bundle, @Nonnull AndroidClassInfo cls) {
		if (isTargetResource(resource))
			root.removeNodeByPath(rootPath.child(resource).child(bundle).child(cls.getPackageName()).child(cls));
	}

	@Override
	public void onNewFile(@Nonnull WorkspaceResource resource, @Nonnull FileBundle bundle, @Nonnull FileInfo file) {
		if (isTargetResource(resource))
			root.getOrCreateNodeByPath(rootPath.child(resource).child(bundle).child(file.getDirectoryName()).child(file));
	}

	@Override
	public void onUpdateFile(@Nonnull WorkspaceResource resource, @Nonnull FileBundle bundle, @Nonnull FileInfo oldFile, @Nonnull FileInfo newFile) {
		if (isTargetResource(resource)) {
			WorkspaceTreeNode node = root.getOrCreateNodeByPath(rootPath.child(resource).child(bundle).child(oldFile.getDirectoryName()).child(oldFile));
			node.setValue(rootPath.child(resource).child(bundle).child(newFile.getDirectoryName()).child(newFile));
		}
	}

	@Override
	public void onRemoveFile(@Nonnull WorkspaceResource resource, @Nonnull FileBundle bundle, @Nonnull FileInfo file) {
		if (isTargetResource(resource))
			root.removeNodeByPath(rootPath.child(resource).child(bundle).child(file.getDirectoryName()).child(file));
	}
}
