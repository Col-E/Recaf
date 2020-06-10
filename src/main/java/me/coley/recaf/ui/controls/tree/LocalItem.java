package me.coley.recaf.ui.controls.tree;

import me.coley.recaf.search.Context;
import me.coley.recaf.workspace.JavaResource;

/**
 * Item to represent local variables.
 *
 * @author Matt
 */
public class LocalItem extends DirectoryItem {
	private final Context.LocalContext local;

	/**
	 * @param resource
	 * 		The resource associated with the item.
	 * @param local
	 * 		The local variable context.
	 */
	public LocalItem(JavaResource resource, Context.LocalContext local) {
		super(resource, local.getName());
		this.local = local;
	}

	/**
	 * @return Local variable context.
	 */
	public Context.LocalContext getLocal() {
		return local;
	}

	@Override
	public int compareTo(DirectoryItem o) {
		if(o instanceof LocalItem) {
			LocalItem c = (LocalItem) o;
			return local.compareTo(c.getLocal());
		}
		return 1;
	}
}