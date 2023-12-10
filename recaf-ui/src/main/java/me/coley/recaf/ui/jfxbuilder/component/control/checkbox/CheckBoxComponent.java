package me.coley.recaf.ui.jfxbuilder.component.control.checkbox;


import javafx.beans.property.BooleanProperty;
import javafx.scene.control.CheckBox;
import me.coley.recaf.ui.jfxbuilder.component.Component;

public interface CheckBoxComponent extends Component<CheckBox, CheckBoxComponent> {
	CheckBoxComponent bind(BooleanProperty property);

	static CheckBoxComponent checkbox() {
		return new CheckBoxComponentImpl();
	}
}
