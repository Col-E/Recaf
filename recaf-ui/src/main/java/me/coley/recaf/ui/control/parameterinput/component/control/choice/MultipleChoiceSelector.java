package me.coley.recaf.ui.control.parameterinput.component.control.choice;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import me.coley.recaf.ui.control.parameterinput.util.ToObservableString;
import me.coley.recaf.ui.control.parameterinput.util.ToString;

import java.util.Arrays;

public class MultipleChoiceSelector<Choice> extends HBox
	implements ChoiceComponent<Choice, MultipleChoiceSelector<Choice>> {
	private final SimpleObjectProperty<Choice> selected = new SimpleObjectProperty<>();

	private final ToggleGroup group = new ToggleGroup();

	private MultipleChoiceSelector(Choice[] choices, ToObservableString<Choice> transformer, Choice selected) {
		if (choices.length <= 1) throw new IllegalArgumentException("MultipleChoiceSelector requires at least 2 choices");
		if (selected == null) throw new IllegalArgumentException("Selected choice cannot be null");
		if (Arrays.stream(choices).noneMatch(selected::equals))
			throw new IllegalArgumentException("Selected choice must be in choices");
		this.selected.set(selected);

		getStylesheets().add("style/ide-features.css");
		getStyleClass().add("selection");
		setSpacing(0);


		ToggleButton left = buttonFromChoice(choices[0], transformer);
		left.getStyleClass().add("left");
		getChildren().add(left);
		for (int i = 1; i < choices.length - 1; i++) {
			getChildren().add(buttonFromChoice(choices[i], transformer));
		}
		ToggleButton right = buttonFromChoice(choices[choices.length - 1], transformer);
		right.getStyleClass().add("right");
		getChildren().add(right);
		ObservableList<Node> children = getChildren();
		for (Node child : children) {
			if (child.getUserData() == selected) child.getStyleClass().add("selected");
			child.setOnMouseClicked(e -> {
				ToggleButton button = (ToggleButton) e.getSource();
				Choice c = (Choice) button.getUserData();
				button.getStyleClass().add("selected");
				for (Node node : getChildren()) if (node != button) node.getStyleClass().remove("selected");
				MultipleChoiceSelector.this.selected.set(c);
			});
		}
		setPrefSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);
	}

	private MultipleChoiceSelector(Choice[] choices, ToString<Choice> transformer, Choice selected) {
		this(choices, (ToObservableString<Choice>) c -> new SimpleStringProperty(transformer.apply(c)), selected);
	}

	private ToggleButton buttonFromChoice(Choice choice, ToObservableString<Choice> transformer) {
		ToggleButton button = new ToggleButton();
		button.textProperty().bind(transformer.apply(choice));
		button.getStyleClass().add("selection-button");
		group.getToggles().add(button);
		button.setUserData(choice);
		return button;
	}

	public static <C> MultipleChoiceSelector<C> choice(C[] choices, ToObservableString<C> transformer, C selected) {
		return new MultipleChoiceSelector<>(choices, transformer, selected);
	}

	public static <C> MultipleChoiceSelector<C> choice(C[] choices, ToString<C> transformer, C selected) {
		return new MultipleChoiceSelector<>(choices, transformer, selected);
	}

	public ObservableValue<Choice> selectedProperty() {
		return selected;
	}

	@Override
	public MultipleChoiceSelector<Choice> node() {
		return this;
	}

	@Override
	public MultipleChoiceSelector<Choice> bind(Property<Choice> choice) {
		choice.bind(selected);
		return this;
	}
}
