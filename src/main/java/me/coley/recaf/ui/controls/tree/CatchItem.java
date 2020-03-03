package me.coley.recaf.ui.controls.tree;

import me.coley.recaf.search.Context;
import me.coley.recaf.workspace.JavaResource;

/**
 * Item to represent catch blocks.
 *
 * @author Matt
 */
public class CatchItem extends DirectoryItem {
	private final Context.CatchContext catchContext;

	/**
	 * @param resource
	 * 		The resource associated with the item.
	 * @param catchContext
	 * 		The catch block context.
	 */
	public CatchItem(JavaResource resource, Context.CatchContext catchContext) {
		super(resource, catchContext.getType());
		this.catchContext = catchContext;
	}

	/**
	 * @return Catch block context.
	 */
	public Context.CatchContext getCatchContext() {
		return catchContext;
	}

	/**
	 * @return Catch block type.
	 */
	public String getCatchType() {
		return getCatchContext().getType();
	}

	@Override
	public int compareTo(DirectoryItem o) {
		if(o instanceof CatchItem) {
			CatchItem c = (CatchItem) o;
			return getCatchContext().compareTo(c.getCatchContext());
		}
		return 1;
	}
}