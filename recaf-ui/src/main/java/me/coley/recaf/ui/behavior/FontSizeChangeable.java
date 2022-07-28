package me.coley.recaf.ui.behavior;

import javafx.scene.Node;

import java.util.function.Consumer;

public interface FontSizeChangeable {

	/**
	 * @param fontSize
	 * 		New font size.
	 */
	void setFontSize(int fontSize);


	void applyScrollEvent(Consumer<Node> consumer);

}
