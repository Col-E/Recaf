package me.coley.recaf.ui.jfxbuilder.component.control.button;

import javafx.beans.value.ObservableStringValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import me.coley.recaf.ui.jfxbuilder.component.Component;

public interface ButtonComponent<Self extends ButtonComponent<Self>> extends Component<Button, ButtonComponent<Self>> {

	Self onAction(EventHandler<ActionEvent> action);

	static <R extends ButtonComponent<R>> R button(String text) {
		return (R) new ButtonComponentImpl(text);
	}

	static <R extends ButtonComponent<R>> R button(ObservableStringValue text) {
		return (R) new ButtonComponentImpl(text);
	}
}
