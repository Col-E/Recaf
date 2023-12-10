package me.coley.recaf.ui.jfxbuilder.component.control.label;

import javafx.beans.value.ObservableStringValue;
import javafx.scene.control.Label;

public class LabelComponentImpl extends Label implements LabelComponent {
	LabelComponentImpl(String text) {
		super(text);
	}

	LabelComponentImpl(ObservableStringValue text) {
		super(text.get());
		textProperty().bind(text);
	}

	@Override
	public Label node() {
		return this;
	}
}
