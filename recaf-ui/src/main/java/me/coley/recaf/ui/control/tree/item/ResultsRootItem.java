package me.coley.recaf.ui.control.tree.item;

import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.DexClassInfo;
import me.coley.recaf.search.Search;
import me.coley.recaf.search.result.Result;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.resource.Resource;
import me.coley.recaf.workspace.resource.ResourceClassListener;
import me.coley.recaf.workspace.resource.ResourceDexClassListener;

import java.util.Collection;

/**
 * Root item for {@link me.coley.recaf.ui.panel.ResultsPane}.
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
			BaseTreeItem ownerItem = addPath(this, ownerInfo.getName(), ClassItem::new, PackageItem::new);
			// TODO: Populate fields/methods/annotations
			if (result.getContainingField() != null) {
				// Result is in a field
				if (result.getContainingAnnotation() != null) {
					// Result is on an annotation applied to the field
				}
			} else if (result.getContainingMethod() != null) {
				// Result is in a method
				if (result.getOpcode() > -1) {
					// Result is on an instruction
				} else if (result.getContainingAnnotation() != null) {
					// Result is on an annotation applied to the method
				}
			} else if (result.getContainingAnnotation() != null) {
				// Result is on an annotation applied to the class
			}
		}
	}

	public Search getSearch() {
		return search;
	}

	public Collection<Result> getResults() {
		return results;
	}

	@Override
	protected BaseTreeValue createTreeValue() {
		return new ResultsRootValue(this);
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
