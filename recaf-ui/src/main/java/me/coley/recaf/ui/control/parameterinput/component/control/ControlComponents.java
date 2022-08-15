package me.coley.recaf.ui.control.parameterinput.component.control;

import javafx.beans.value.ObservableStringValue;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import me.coley.recaf.ui.control.parameterinput.component.control.button.ButtonComponent;
import me.coley.recaf.ui.control.parameterinput.component.control.checkbox.CheckBoxComponent;
import me.coley.recaf.ui.control.parameterinput.component.control.checkbox.CheckBoxComponentImpl;
import me.coley.recaf.ui.control.parameterinput.component.control.choice.MultipleChoiceSelector;
import me.coley.recaf.ui.control.parameterinput.component.control.field.TextFieldComponent;
import me.coley.recaf.ui.control.parameterinput.component.control.label.LabelComponent;
import me.coley.recaf.ui.control.parameterinput.component.control.nodeswitch.NodeSwitchComponent;
import me.coley.recaf.ui.control.parameterinput.util.ToObservableString;
import me.coley.recaf.ui.control.parameterinput.util.ToString;

public class ControlComponents {
	private ControlComponents() {}

	public static Node nodeWithData(Node node, Object data) {
		node.setUserData(data);
		return node;
	}

	public static <C> MultipleChoiceSelector<C> choice(C[] choices, ToObservableString<C> transformer, C selected) {
		return MultipleChoiceSelector.choice(choices, transformer, selected);
	}

	public static MultipleChoiceSelector<String> choice(String[] choices, String selected) {
		return MultipleChoiceSelector.choice(choices, (ToString<String>) s->s, selected);
	}

	public static <C> MultipleChoiceSelector<C> choice(C[] choices, ToString<C> transformer, C selected) {
		return MultipleChoiceSelector.choice(choices, transformer, selected);
	}

	public static <Choice, R extends NodeSwitchComponent<Choice, R>> R nodeSwitch(Choice[] choices, ObservableValue<Choice> observable) {
		return NodeSwitchComponent.nodeSwitch(choices, observable);
	}

	public static LabelComponent label(String text) {
		return LabelComponent.label(text);
	}

	public static LabelComponent label(ObservableStringValue text) {
		return LabelComponent.label(text);
	}

	public static <R extends TextFieldComponent<R>> R textField() {
		return TextFieldComponent.textField();
	}

	public static <R extends ButtonComponent<R>> R button(String text) {
		return ButtonComponent.button(text);
	}

	public static <R extends ButtonComponent<R>> R button(ObservableStringValue text) {
		return  ButtonComponent.button(text);
	}

	public static CheckBoxComponent checkbox() {
		return new CheckBoxComponentImpl();
	}
}
