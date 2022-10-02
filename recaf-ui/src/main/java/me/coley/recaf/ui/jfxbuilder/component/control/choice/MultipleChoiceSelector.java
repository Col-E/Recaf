package me.coley.recaf.ui.jfxbuilder.component.control.choice;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import me.coley.recaf.ui.jfxbuilder.util.function.ToObservableString;
import me.coley.recaf.ui.jfxbuilder.util.function.ToString;
import me.coley.recaf.util.threading.FxThreadUtil;

import java.util.Arrays;

public class MultipleChoiceSelector<Choice> extends HBox
	implements ChoiceComponent<Choice, MultipleChoiceSelector<Choice>> {

	public enum SelectionMode {
		SINGLE, SINGLE_OR_NONE,
		MULTIPLE, MULTIPLE_OR_NONE;

		public boolean acceptsNone() {return this == SINGLE_OR_NONE || this == MULTIPLE_OR_NONE;}

		public boolean acceptsMultiple() {return this == MULTIPLE || this == MULTIPLE_OR_NONE;}
	}

	private final SetProperty<Choice> selectedChoices = new SimpleSetProperty<>(FXCollections.observableSet());
	private final SelectionMode mode;

	private final ObjectProperty<Choice> selectedChoice = new SimpleObjectProperty<>();

	private MultipleChoiceSelector(Choice[] choices, ToObservableString<Choice> transformer, Choice selected, SelectionMode mode) {
		this.mode = mode;
		if (choices.length <= 1) throw new IllegalArgumentException("MultipleChoiceSelector requires at least 2 choices");
		if (selected == null) {
			if (!mode.acceptsNone()) throw new IllegalArgumentException("Selected choice cannot be null");
		} else {
			if (Arrays.stream(choices).noneMatch(selected::equals))
				throw new IllegalArgumentException("Selected choice must be in choices");
			selectedChoices.add(selected);
			selectedChoice.set(selected);
		}
		getStylesheets().add("style/ide-features.css");
		getStyleClass().add("selection");
		setSpacing(0);
		getChildren().addAll(Arrays.stream(choices).map(choice -> buttonFromChoice(choice, transformer)).toArray(ToggleButton[]::new));
		setPrefSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);
	}

	private MultipleChoiceSelector(Choice[] choices, ToString<Choice> transformer, Choice selected, SelectionMode mode) {
		this(choices, (ToObservableString<Choice>) c -> new SimpleStringProperty(transformer.apply(c)), selected, mode);
	}

	private ToggleButton buttonFromChoice(Choice choice, ToObservableString<Choice> transformer) {
		var button = new ToggleButton();
		if (selectedChoices.contains(choice)) button.setSelected(true);
		button.textProperty().bind(transformer.apply(choice));
		button.selectedProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue) {
				selectedChoices.add(choice);
				selectedChoice.set(choice);
				if (!mode.acceptsMultiple()) for (Node b : getChildren()) {
					if (b != button && ((ToggleButton) b).isSelected()) {
						((ToggleButton) b).setSelected(false);
					}
				}
			} else if (!mode.acceptsNone()) {
				if (getChildren().stream().noneMatch(b -> b != button && ((ToggleButton) b).isSelected())) {
					FxThreadUtil.run(() -> button.setSelected(true));
				}
			}
		});
		button.setUserData(choice);
		return button;
	}

	public static <C> MultipleChoiceSelector<C> choice(C[] choices, ToObservableString<C> transformer, C selected, SelectionMode mode) {
		return new MultipleChoiceSelector<>(choices, transformer, selected, mode);
	}

	public static <C> MultipleChoiceSelector<C> choice(C[] choices, ToString<C> transformer, C selected, SelectionMode mode) {
		return new MultipleChoiceSelector<>(choices, transformer, selected, mode);
	}

	@Override
	public MultipleChoiceSelector<Choice> node() {
		return this;
	}

	@Override
	public MultipleChoiceSelector<Choice> bind(SetProperty<Choice> choices) {
		choices.bind(this.selectedChoices);
		return this;
	}

	@Override
	public MultipleChoiceSelector<Choice> bind(Property<Choice> choice) {
		if (mode.acceptsMultiple())
			throw new IllegalArgumentException("MultipleChoiceSelector with mode MULTIPLE cannot be bound to a single choice");
		choice.bind(selectedChoice);
		return this;
	}
}
