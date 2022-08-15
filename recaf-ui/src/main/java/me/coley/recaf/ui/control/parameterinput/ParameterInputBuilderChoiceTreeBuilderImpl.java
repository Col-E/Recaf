package me.coley.recaf.ui.control.parameterinput;


import javafx.beans.value.ObservableValue;

import java.util.function.Consumer;
import java.util.function.Function;

interface IParameterInputBuilderChoiceTreeBuilderImpl<State, Choice, RPIB extends ParameterInputBuilder<State>, Self extends IParameterInputBuilderChoiceTreeBuilderImpl<State, Choice, RPIB, Self>>
	extends ParameterInputBuilderChoiceTreeBuilder<State, Choice, RPIB, Self> {
}

public class ParameterInputBuilderChoiceTreeBuilderImpl<State, Choice, RPIB extends ParameterInputBuilder<State> >
implements IParameterInputBuilderChoiceTreeBuilderImpl<State, Choice, RPIB, ParameterInputBuilderChoiceTreeBuilderImpl<State, Choice, RPIB>> {

	@Override
	public RPIB finish() {
		return null;
	}

	@Override
	public <NestedPIB extends ParameterInputBuilder.Returnable<ParameterInputBuilderChoiceTreeBuilderImpl<State, Choice, RPIB>> & ParameterInputBuilderWithAll<State, NestedPIB>> NestedPIB forChoice(Choice choice) {
		return null;
	}

	@Override
	public <NestedPIB extends ParameterInputBuilder.Returnable<ParameterInputBuilderChoiceTreeBuilderImpl<State, Choice, RPIB>> & ParameterInputBuilderWithAll<State, NestedPIB>> NestedPIB forChoice(ObservableValue<Choice> choice) {
		return null;
	}

	@Override
	public <NestedPIB extends ParameterInputBuilder.Returnable<ParameterInputBuilderChoiceTreeBuilderImpl<State, Choice, RPIB>> & ParameterInputBuilderWithAll<State, NestedPIB>> NestedPIB forChoices(Choice... choice) {
		return null;
	}

	@Override
	public <NestedPIB extends ParameterInputBuilder.Returnable<ParameterInputBuilderChoiceTreeBuilderImpl<State, Choice, RPIB>> & ParameterInputBuilderWithAll<State, NestedPIB>> NestedPIB forChoices(ObservableValue<Choice>... choice) {
		return null;
	}

	@Override
	public ParameterInputBuilderChoiceTreeBuilderImpl<State, Choice, RPIB> forChoice(Choice choice, Function<State, RPIB> builder) {
		return null;
	}

	@Override
	public ParameterInputBuilderChoiceTreeBuilderImpl<State, Choice, RPIB> forChoice(ObservableValue<Choice> choice, Function<State, RPIB> builder) {
		return null;
	}

	@Override
	public ParameterInputBuilderChoiceTreeBuilderImpl<State, Choice, RPIB> forChoices(Choice[] choice, Function<State, RPIB> builder) {
		return null;
	}

	@Override
	public ParameterInputBuilderChoiceTreeBuilderImpl<State, Choice, RPIB> forChoices(ObservableValue<Choice>[] choice, Function<State, RPIB> builder) {
		return null;
	}

	@Override
	public ParameterInputBuilderChoiceTreeBuilderImpl<State, Choice, RPIB> onClear(Consumer<State> applier) {
		return null;
	}
}
