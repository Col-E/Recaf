package me.coley.recaf.ui.control.tree.item;

import me.coley.recaf.code.*;
import me.coley.recaf.search.Search;
import me.coley.recaf.search.result.Result;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.resource.Resource;
import me.coley.recaf.workspace.resource.ResourceClassListener;
import me.coley.recaf.workspace.resource.ResourceDexClassListener;

import java.util.Collection;

/**
 * Root item for {@link me.coley.recaf.ui.pane.ResultsPane}.
 *
 * @author Matt Coley
 */
public class ResultsRootItem extends BaseTreeItem implements ResourceClassListener, ResourceDexClassListener {
	private final Workspace workspace;
	private final Search search;
	private final Collection<Result> results;

	/**
	 * Create root item.
	 *
	 * @param workspace
	 * 		Workspace to represent.
	 * @param search
	 * 		The search ran.
	 * @param results
	 * 		Search results to show as child items.
	 */
	public ResultsRootItem(Workspace workspace, Search search, Collection<Result> results) {
		this.workspace = workspace;
		this.search = search;
		this.results = results;
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
		for (Result result : results) {
			CommonClassInfo ownerInfo = result.getContainingClass();
			BaseTreeItem ownerItem = getClass(ownerInfo);
			if (result.getContainingField() != null) {
				FieldItem fieldItem = getField(ownerItem, result.getContainingField());
				// Result is in a field
				if (result.getContainingAnnotation() != null) {
					// Result is on an annotation applied to the field
					fieldItem.setAnnotationType(result.getContainingAnnotation());
				}
			} else if (result.getContainingMethod() != null) {
				boolean hasInsn = result.getInstruction() != null;
				MethodItem methodItem = getMethod(ownerItem, result.getContainingMethod(), hasInsn);
				// Result is in a method
				if (hasInsn) {
					// Result is on an instruction
					InsnItem insnItem = new InsnItem(result.getInstruction());
					methodItem.addChild(insnItem);
				} else if (result.getContainingAnnotation() != null) {
					// Result is on an annotation applied to the method
					methodItem.setAnnotationType(result.getContainingAnnotation());
				}
			} else if (result.getContainingAnnotation() != null) {
				// Result is on an annotation applied to the class
				((ClassItem) ownerItem).setAnnotationType(result.getContainingAnnotation());
			}
		}
	}

	private BaseTreeItem getClass(CommonClassInfo ownerInfo) {
		BaseTreeItem item = this;
		String[] pieces = ownerInfo.getName().split("/");
		for (String piece : pieces) {
			if (item == null) break;
			item = item.getChildDirectory(piece);
		}
		if (!(item instanceof ClassItem)) {
			item = addPath(this, ownerInfo.getName(), n -> new ClassItem(n, true), PackageItem::new);
		}
		return item;
	}

	private MethodItem getMethod(BaseTreeItem ownerItem, MethodInfo info, boolean hasInsn) {
		String key = info.getName() + info.getDescriptor();
		MethodItem item = (MethodItem) ownerItem.getChildFile(key);
		if (item == null) {
			item = (MethodItem) ownerItem.getChildDirectory(key);
		}
		if (item == null) {
			item = new MethodItem(info, hasInsn);
			ownerItem.addChild(item);
		}
		return item;
	}

	private FieldItem getField(BaseTreeItem ownerItem, FieldInfo info) {
		String key = info.getDescriptor() + " " + info.getName();
		FieldItem item = (FieldItem) ownerItem.getChildFile(key);
		if (item == null) {
			item = new FieldItem(info);
			ownerItem.addChild(item);
		}
		return item;
	}

	/**
	 * @return The search ran.
	 */
	public Search getSearch() {
		return search;
	}

	/**
	 * @return Search results of the {@link #getSearch() ran search}.
	 */
	public Collection<Result> getResults() {
		return results;
	}

	@Override
	protected BaseTreeValue createTreeValue() {
		return new ResultsRootValue(this);
	}

	@Override
	public boolean forceVisible() {
		return true;
	}

	@Override
	public void onNewClass(Resource resource, ClassInfo newValue) {
		// no-op
	}

	@Override
	public void onRemoveClass(Resource resource, ClassInfo oldValue) {
		remove(this, oldValue.getName());
	}

	@Override
	public void onNewDexClass(Resource resource, String dexName, DexClassInfo newValue) {
		// no-op
	}

	@Override
	public void onRemoveDexClass(Resource resource, String dexName, DexClassInfo oldValue) {
		remove(this, oldValue.getName());
	}

	@Override
	public void onUpdateClass(Resource resource, ClassInfo oldValue, ClassInfo newValue) {
		// TODO: Force Redraw?
	}

	@Override
	public void onUpdateDexClass(Resource resource, String dexName, DexClassInfo oldValue, DexClassInfo newValue) {
		// TODO: Force Redraw?
	}
}
