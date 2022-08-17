package me.coley.recaf.ui.control.parameterinput.component.control.choice;

import javafx.beans.property.Property;
import javafx.beans.property.SetProperty;
import me.coley.recaf.ui.control.parameterinput.component.Component;

public interface ChoiceComponent<Choice, Self extends ChoiceComponent<Choice, Self>>
	extends Component<MultipleChoiceSelector<Choice>, Self> {
	Self bind(SetProperty<Choice> choice);

	Self bind(Property<Choice> choice);
}
