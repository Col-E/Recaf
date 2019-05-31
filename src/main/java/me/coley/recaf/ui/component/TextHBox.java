package me.coley.recaf.ui.component;

import javafx.scene.layout.HBox;

/**
 * HBox that is externally updated to keep track of the text content of child
 * nodes.
 * 
 * @author Matt
 */
public class TextHBox extends HBox {
	private final StringBuilder content = new StringBuilder();

	public TextHBox() {
		getStyleClass().add("text-hbox");
	}

	public void append(String s) {
		content.append(s);
	}

	public String getText() {
		return content.toString();
	}

	public static int compare(TextHBox t1, TextHBox t2) {
		return t1.getText().compareTo(t2.getText());
	}
}
