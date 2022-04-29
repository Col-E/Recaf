package me.coley.recaf.ui.control.tree.item;

import javafx.beans.value.ObservableValue;

/**
 * Dummy item for displaying arbitrary content.
 *
 * @author Matt Coley
 */
public class DummyItem extends BaseTreeItem {
	private final ObservableValue<String> text;
	private final String pathElementValue;

	/**
	 * Create dummy item.
	 *
	 * @param text
	 * 		Optional text.
	 */
	public DummyItem(ObservableValue<String> text) {
		this(text.getValue(), text);
	}

	/**
	 * Create dummy item.
	 *
	 * @param pathElementValue
	 * 		Path name to use, as {@link BaseTreeValue#getPathElementValue()}.
	 * 		Used in tree traversal but not shown in the UI.
	 * @param text
	 * 		Optional text.
	 */
	public DummyItem(String pathElementValue, ObservableValue<String> text) {
		this.pathElementValue = pathElementValue;
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
		return new BaseTreeValue(this, pathElementValue, false);
	}
}
