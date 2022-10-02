package me.coley.recaf.ui.jfxbuilder.component.control.field;

import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableStringValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.TextField;

public class TextFieldComponentImpl implements TextFieldComponent<TextFieldComponentImpl> {

	private final TextField textField = new TextField();

	@Override
	public TextField node() {
		return textField;
	}

	@Override
	public TextFieldComponentImpl prompt(String prompt) {
		textField.setPromptText(prompt);
		return this;
	}

	@Override
	public TextFieldComponentImpl prompt(ObservableStringValue prompt) {
		textField.promptTextProperty().bind(prompt);
		return this;
	}

	@Override
	public TextFieldComponentImpl text(String text) {
		textField.setText(text);
		return this;
	}

	@Override
	public TextFieldComponentImpl bindText(StringProperty text) {
		text.bind(textField.textProperty());
		return this;
	}

	@Override
	public TextFieldComponentImpl text(ObservableStringValue text) {
		textField.textProperty().bind(text);
		return this;
	}

	@Override
	public TextFieldComponentImpl onAction(EventHandler<ActionEvent> action) {
		textField.setOnAction(action);
		return this;
	}

	@Override
	public TextFieldComponentImpl minWidth(double minWidth) {
		textField.setMinWidth(minWidth);
		return this;
	}

	@Override
	public TextFieldComponentImpl minHeight(double minHeight) {
		textField.setMinHeight(minHeight);
		return this;
	}

	@Override
	public TextFieldComponentImpl maxWidth(double maxWidth) {
		textField.setMaxWidth(maxWidth);
		return this;
	}

	@Override
	public TextFieldComponentImpl maxHeight(double maxHeight) {
		textField.setMaxHeight(maxHeight);
		return this;
	}

	@Override
	public TextFieldComponentImpl prefWidth(double prefWidth) {
		textField.setPrefWidth(prefWidth);
		return this;
	}

	@Override
	public TextFieldComponentImpl prefHeight(double prefHeight) {
		textField.setPrefHeight(prefHeight);
		return this;
	}

	@Override
	public TextFieldComponentImpl maxSize(double maxWidth, double maxHeight) {
		textField.setMaxSize(maxWidth, maxHeight);
		return this;
	}

	@Override
	public TextFieldComponentImpl minSize(double minWidth, double minHeight) {
		textField.setMinSize(minWidth, minHeight);
		return this;
	}

	@Override
	public TextFieldComponentImpl prefSize(double prefWidth, double prefHeight) {
		textField.setPrefSize(prefWidth, prefHeight);
		return this;
	}

	@Override
	public TextFieldComponentImpl snapToPixel(boolean snapToPixel) {
		textField.setSnapToPixel(snapToPixel);
		return this;
	}

	@Override
	public TextFieldComponentImpl padding(double top, double right, double bottom, double left) {
		textField.setPadding(new Insets(top, right, bottom, left));
		return this;
	}
}
