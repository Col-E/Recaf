package me.coley.recaf.ui.control.tree.item;

import javafx.beans.value.ObservableValue;

/**
 * Dummy item for displaying arbitrary content.
 *
 * @author Matt Coley
 */
public class DummyItem extends BaseTreeItem {
	private final ObservableValue<String> text;

	/**
	 * Create dummy item.
	 *
	 * @param text
	 * 		Optional text.
	 */
	public DummyItem(ObservableValue<String> text) {
		this.text = text;
		init();
	}

	/**
	 * @return Text to display.
	 */
	public ObservableValue<String> getDummyText() {
		return text;
	}

	@Override
	protected BaseTreeValue createTreeValue() {
		return new BaseTreeValue(this, text.getValue(), false);
	}
}
