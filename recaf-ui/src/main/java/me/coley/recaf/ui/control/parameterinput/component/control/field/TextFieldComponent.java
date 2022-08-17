package me.coley.recaf.ui.control.parameterinput.component.control.field;

import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableStringValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.TextField;
import me.coley.recaf.ui.control.parameterinput.component.Component;
import me.coley.recaf.ui.control.parameterinput.component.control.RegionComponent;

public interface TextFieldComponent<Self extends TextFieldComponent<Self>>
	extends RegionComponent<Self>, Component<TextField, Self> {

	Self prompt(String prompt);

	Self prompt(ObservableStringValue prompt);

	Self text(String text);

	Self bindText(StringProperty text);

	Self text(ObservableStringValue text);

	Self onAction(EventHandler<ActionEvent> action);

	static <R extends TextFieldComponent<R>> R textField() {
		return (R) new TextFieldComponentImpl();
	}
}
