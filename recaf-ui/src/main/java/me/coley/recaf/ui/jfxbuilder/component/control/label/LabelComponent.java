package me.coley.recaf.ui.jfxbuilder.component.control.label;

import javafx.beans.value.ObservableStringValue;
import javafx.scene.control.Label;
import me.coley.recaf.ui.jfxbuilder.component.Component;

public interface LabelComponent extends Component<Label, LabelComponent> {

	static LabelComponent label(String text) {
		return new LabelComponentImpl(text);
	}

	static LabelComponent label(ObservableStringValue text) {
		return new LabelComponentImpl(text);
	}
}
