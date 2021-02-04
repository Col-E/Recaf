package me.coley.recaf.ui.control.tree.item;

/**
 * Dummy item for displaying arbitrary content.
 *
 * @author Matt Coley
 */
public class DummyItem extends BaseTreeItem {
	private final String text;

	/**
	 * Create dummy item.
	 *
	 * @param text
	 * 		Optional text.
	 */
	public DummyItem(String text) {
		this.text = text;
		init();
	}

	/**
	 * @return Text to display. May be {@code null}.
	 */
	public String getDummyText() {
		return text;
	}

	@Override
	protected BaseTreeValue createTreeValue() {
		return new BaseTreeValue(this, text, false);
	}
}
