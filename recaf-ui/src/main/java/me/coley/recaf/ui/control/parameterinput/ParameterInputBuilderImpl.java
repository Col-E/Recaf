package me.coley.recaf.ui.control.parameterinput;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableStringValue;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import me.coley.recaf.ui.control.MultipleChoiceSelector;

import java.util.function.BiConsumer;

interface IParameterInputBuilderImpl<S> extends ParameterInputBuilderWithAll<S, IParameterInputBuilderImpl<S>> {}

public class ParameterInputBuilderImpl<State> implements IParameterInputBuilderImpl<State> {
	private final State state;
	private final GridPane grid = new GridPane();
	private int row = 0;

	ParameterInputBuilderImpl(State state) {
		this.state = state;
		ColumnConstraints c1 = new ColumnConstraints();
		c1.setFillWidth(false);
		c1.setHgrow(Priority.NEVER);
		ColumnConstraints c2 = new ColumnConstraints();
		c2.setFillWidth(true);
		c2.setHgrow(Priority.ALWAYS);
		grid.getColumnConstraints().addAll(c1, c2);
		grid.setVgap(10);
		grid.setHgap(10);
	}

	public final Node build() {
		return grid;
	}

	@Override
	public ParameterInputBuilderImpl<State> addLabel(String label) {
		grid.add(new Label(label), 0, row++);
		return this;
	}

	@Override
	public ParameterInputBuilderImpl<State> addLabel(ObservableStringValue label) {
		Label l = new Label();
		l.textProperty().bind(label);
		grid.add(l, 0, row++);
		return this;
	}

	@Override
	public ParameterInputBuilderImpl<State> addTextField(String label, BiConsumer<State, ObservableStringValue> applier) {
		grid.add(new Label(label), 0, row);
		TextField textField = new TextField();
		applier.accept(state, textField.textProperty());
		grid.add(textField, 1, row++);
		return this;
	}

	@Override
	public ParameterInputBuilderImpl<State> addCheckBox(String label, BiConsumer<State, ObservableBooleanValue> applier) {
		grid.add(new Label(label), 0, row);
		CheckBox checkBox = new CheckBox();
		applier.accept(state, checkBox.selectedProperty());
		grid.add(checkBox, 1, row++);
		return this;
	}

	@Override
	public <C> ParameterInputBuilderImpl<State> addChoice(String label, C[] choices, C2String<C> transformer, BiConsumer<State, ObservableValue<C>> applier) {
		return addChoice(label, choices, (C2ObservableStringValue<C>) c -> new SimpleStringProperty(transformer.apply(c)), applier);
	}

	@Override
	public <C> ParameterInputBuilderImpl<State> addChoice(String label, C[] choices, C2ObservableStringValue<C> transformer, BiConsumer<State, ObservableValue<C>> applier) {
		grid.add(new Label(label), 0, row);
		MultipleChoiceSelector<C> selector = MultipleChoiceSelector.create(choices, transformer, -1);
		applier.accept(state, selector.selectedProperty());
		grid.add(selector, 1, row++);
		return this;
	}

	@Override
	public ParameterInputBuilderChoiceTreeBuilder addChoiceTree(String label, Object[] objects, C2String transformer, BiConsumer applier) {
		return null;
	}

	@Override
	public ParameterInputBuilderChoiceTreeBuilder addChoiceTree(String label, Object[] choices, C2ObservableStringValue transformer, BiConsumer applier) {
		return null;
	}
}
