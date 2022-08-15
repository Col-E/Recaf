package me.coley.recaf.ui.control;

import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableStringValue;
import javafx.beans.value.ObservableValue;
import javafx.scene.layout.VBox;
import org.stringtemplate.v4.ST;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class ParameterInputPane<S> extends PanelPane {
	private final VBox vbox = new VBox();

	public ParameterInputPane(ParameterInputBuilder<S> builder) {
		/*




		ParameterInputBuilder.build(State.class)
			.addLabel("New Project")
			.addTextField("Name", (state, text) -> new State().setName(text))
			.addTextField("Path", (state, text) -> state.setPath(text))
			.addCheckBox("Create", (state, checked) -> state.setCreate(checked))
			.addChoice("Language", Language.values(), lang -> lang.name(), (state, choice) -> state.setLanguage(choice))
			.addChoiceTree("Build System", BuildSystem.values(), (state, choice) -> state.setBuildSystem(choice))
				.forChoice(GRADLE)
					.addChoice("Gradle DSL", List.of("Groovy", "Kotlin"), (state, dsl) -> state.setDSL(dsl))
					.finish()
				.onClear(state -> state.setBuildSystem(null))
				.finish()
			.addCheckBox("Sample Code", (state, checked) -> state.setSampleCode(checked))
			.build()



		 */
	}

	interface PIB<S> {
		ParameterInputPane<S> build();
	}

	interface PIBWithLabel<S> {
		PIB<S> addLabel(String label);
	}

	interface PIBWithTextField<S> {
		PIB<S> addTextField(String label, BiConsumer<S, ObservableStringValue> applier);
	}

	interface PIBWithCheckBox<S> {
		PIB<S> addCheckBox(String label, BiConsumer<S, ObservableBooleanValue> applier);
	}

	interface C2String<C> extends Function<C, String> {}
	interface C2ObservableStringValue<C> extends Function<C, String> {}

	interface PIBWithChoice<S, C> {
		PIB<S> addChoice(String label, C[] choices, C2String<C> transformer, BiConsumer<S, ObservableValue<C>> applier);
		PIB<S> addChoice(String label, C[] choices, C2ObservableStringValue<C> transformer, BiConsumer<S, ObservableValue<C>> applier);
	}

	interface PIBWithStringChoice<S>  {
		PIB<S> addChoice(String label, String[] choices, BiConsumer<S, ObservableStringValue> applier);
	}

	interface ToPIBReturnable<S> {
		PIB<S> finish();
	}
























	public static class ParameterInputBuilder<S> {
		private final S state;

		private ParameterInputBuilder(S state) {
			this.state = state;
		}

		public final ParameterInputPane<S> build() {
			return new ParameterInputPane<>(this);
		}

		public static class PIBRoot extends ParameterInputBuilder<Void> {
			private PIBRoot() {
				super(null);
			}
		}

		public static class PIBRootWithState<S> extends ParameterInputBuilder<S> {

			private PIBRootWithState(S state) {
				super(state);
			}
		}
	}
}
