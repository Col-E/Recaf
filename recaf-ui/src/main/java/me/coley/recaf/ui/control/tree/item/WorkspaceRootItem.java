package me.coley.recaf.ui.control.tree.item;

import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.code.DexClassInfo;
import me.coley.recaf.code.FileInfo;
import me.coley.recaf.ui.control.tree.WorkspaceTreeWrapper;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.WorkspaceListener;
import me.coley.recaf.workspace.resource.Resource;
import me.coley.recaf.workspace.resource.ResourceClassListener;
import me.coley.recaf.workspace.resource.ResourceDexClassListener;
import me.coley.recaf.workspace.resource.ResourceFileListener;

import java.util.HashMap;
import java.util.Map;

/**
 * Root item for {@link WorkspaceTreeWrapper}.
 *
 * @author Matt Coley
 */
public class WorkspaceRootItem extends BaseTreeItem implements WorkspaceListener,
		ResourceClassListener, ResourceDexClassListener, ResourceFileListener {
	private final Map<Resource, ResourceItem> resourceToItem = new HashMap<>();
	private final Workspace workspace;

	/**
	 * Create root item.
	 *
	 * @param workspace
	 * 		Workspace to represent.
	 */
	public WorkspaceRootItem(Workspace workspace) {
		this.workspace = workspace;
		init();
	}

	/**
	 * Initialize sub-items to match the workspace layout.
	 */
	public void setup() {
		// Skip if already setup
		if (!getChildren().isEmpty()) {
			return;
		}
		// create the tree hierarchy
		if (workspace == null) {
			setupNoWorkspace();
		} else {
			setupWorkspace();
		}
	}

	private void setupNoWorkspace() {
		addChild(new DummyItem(Lang.getBinding("tree.prompt")));
	}

	private void setupWorkspace() {
		try {
			addResource(workspace.getResources().getPrimary());
			workspace.getResources().getLibraries().forEach(this::addResource);
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	/**
	 * @param resource
	 * 		Resource to remove.
	 *
	 * @return {@code true} when an item was removed. {@code false} if no updates occurred.
	 */
	public boolean removeResource(Resource resource) {
		ResourceItem resourceItem = resourceToItem.remove(resource);
		if (resourceItem != null) {
			removeChild(resourceItem);
			return true;
		}
		return false;
	}

	/**
	 * @param resource
	 * 		Resource to add.
	 */
	public void addResource(Resource resource) {
		// Add resource
		ResourceItem resourceRoot = new ResourceItem(resource);
		resourceToItem.put(resource, resourceRoot);
		// Add classes
		resourceRoot.addResourceChildren();
		addChild(resourceRoot);
	}

	@Override
	protected BaseTreeValue createTreeValue() {
		return new WorkspaceRootValue(this);
	}

	@Override
	public boolean forceVisible() {
		return true;
	}

	@Override
	public void onAddLibrary(Workspace workspace, Resource library) {
		addResource(library);
	}

	@Override
	public void onRemoveLibrary(Workspace workspace, Resource library) {
		ResourceItem item = resourceToItem.get(library);
		if (item != null) {
			removeChild(item);
		}
	}

	@Override
	public void onNewClass(Resource resource, ClassInfo newValue) {
		ResourceItem item = resourceToItem.get(resource);
		if (item != null) {
			item.addClass(newValue.getName());
		}
	}

	@Override
	public void onRemoveClass(Resource resource, ClassInfo oldValue) {
		ResourceItem item = resourceToItem.get(resource);
		if (item != null) {
			item.removeClass(oldValue.getName());
		}
	}

	@Override
	public void onNewDexClass(Resource resource, String dexName, DexClassInfo newValue) {
		ResourceItem item = resourceToItem.get(resource);
		if (item != null) {
			item.addDexClass(dexName, newValue.getName());
		}
	}

	@Override
	public void onRemoveDexClass(Resource resource, String dexName, DexClassInfo oldValue) {
		ResourceItem item = resourceToItem.get(resource);
		if (item != null) {
			item.removeDexClass(dexName, oldValue.getName());
		}
	}

	@Override
	public void onNewFile(Resource resource, FileInfo newValue) {
		ResourceItem item = resourceToItem.get(resource);
		if (item != null) {
			item.addFile(newValue.getName());
		}
	}

	@Override
	public void onRemoveFile(Resource resource, FileInfo oldValue) {
		ResourceItem item = resourceToItem.get(resource);
		if (item != null) {
			item.removeFile(oldValue.getName());
		}
	}

	@Override
	public void onUpdateClass(Resource resource, ClassInfo oldValue, ClassInfo newValue) {
		// TODO: Force Redraw?
	}

	@Override
	public void onUpdateDexClass(Resource resource, String dexName, DexClassInfo oldValue, DexClassInfo newValue) {
		// TODO: Force Redraw?
	}

	@Override
	public void onUpdateFile(Resource resource, FileInfo oldValue, FileInfo newValue) {
		// TODO: Force Redraw?
	}
}
