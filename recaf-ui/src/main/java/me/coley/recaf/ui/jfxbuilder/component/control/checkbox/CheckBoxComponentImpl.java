package me.coley.recaf.ui.jfxbuilder.component.control.checkbox;

import javafx.beans.property.BooleanProperty;
import javafx.scene.control.CheckBox;

public class CheckBoxComponentImpl extends CheckBox implements CheckBoxComponent {
	@Override
	public CheckBox node() {
		return this;
	}

	@Override
	public CheckBoxComponent bind(BooleanProperty property) {
		property.bind(this.selectedProperty());
		return this;
	}
}
