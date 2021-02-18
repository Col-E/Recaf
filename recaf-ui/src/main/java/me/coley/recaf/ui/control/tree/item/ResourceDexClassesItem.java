package me.coley.recaf.ui.control.tree.item;

/**
 * Item to wrap classes of a {@link me.coley.recaf.workspace.resource.DexClassMap}.
 *
 * @author Matt Coley
 */
public class ResourceDexClassesItem extends BaseTreeItem {
	private final String dexName;

	/**
	 * Create the item.
	 *
	 * @param dexName
	 * 		Dex file name.
	 */
	public ResourceDexClassesItem(String dexName) {
		this.dexName = dexName;
		init();
	}

	/**
	 * @return Dex file name.
	 */
	public String getDexName() {
		return dexName;
	}

	@Override
	protected BaseTreeValue createTreeValue() {
		return new BaseTreeValue(this, null, true);
	}

	@Override
	public int compareTo(BaseTreeItem other) {
		if (other instanceof ResourceDexClassesItem)
			return dexName.compareTo(((ResourceDexClassesItem) other).getDexName());
		return super.compareTo(other);
	}
}