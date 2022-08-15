package me.coley.recaf.ui.control.parameterinput.component.control.field;

import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableStringValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.TextField;

public class TextFieldComponentImpl extends TextField implements TextFieldComponent<TextFieldComponentImpl> {
	@Override
	public TextField node() {
		return this;
	}

	@Override
	public TextFieldComponentImpl prompt(String prompt) {
		setPromptText(prompt);
		return this;
	}

	@Override
	public TextFieldComponentImpl prompt(ObservableStringValue prompt) {
		promptTextProperty().bind(prompt);
		return this;
	}

	@Override
	public TextFieldComponentImpl text(String text) {
		setText(text);
		return this;
	}

	@Override
	public TextFieldComponentImpl bindText(StringProperty text) {
		text.bind(textProperty());
		return this;
	}

	@Override
	public TextFieldComponentImpl text(ObservableStringValue text) {
		textProperty().bind(text);
		return this;
	}

	@Override
	public TextFieldComponentImpl onAction(EventHandler<ActionEvent> action) {
		setOnAction(action);
		return this;
	}
}
