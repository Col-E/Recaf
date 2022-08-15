package me.coley.recaf.ui.control.parameterinput.component.control.button;

import javafx.beans.value.ObservableStringValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;

public class ButtonComponentImpl extends Button implements ButtonComponent<ButtonComponentImpl> {

	ButtonComponentImpl(String text) {
		super(text);
	}

	ButtonComponentImpl(ObservableStringValue text) {
		super(text.get());
		textProperty().bind(text);
	}

	@Override
	public Button node() {
		return this;
	}

	@Override
	public ButtonComponentImpl onAction(EventHandler<ActionEvent> action) {
		setOnAction(action);
		return this;
	}
}
