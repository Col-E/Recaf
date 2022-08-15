package me.coley.recaf.ui.control.parameterinput.component.control.combobox;

import javafx.beans.property.ObjectProperty;
import javafx.scene.control.ComboBox;
import me.coley.recaf.ui.control.EnumComboBox;
import me.coley.recaf.ui.control.parameterinput.component.Component;

public interface ComboBoxComponent<Choice, Self extends ComboBoxComponent<Choice, Self>>
	extends Component<ComboBox<Choice>, ComboBoxComponent<Choice, Self>> {

	Self bind(ObjectProperty<Choice> property);

	static <E extends Enum<E>, CBC extends ComboBoxComponent<E, CBC>> CBC comboBox(Class<E> enumClass, E selected) {
		return (CBC) new EnumComboBox<>(enumClass, selected);
	}
}
