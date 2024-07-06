package software.coley.recaf.ui.pane.search;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import org.reactfx.EventStreams;
import software.coley.collections.Lists;
import software.coley.recaf.services.cell.CellConfigurationService;
import software.coley.recaf.services.navigation.Actions;
import software.coley.recaf.services.search.SearchService;
import software.coley.recaf.services.search.match.NumberPredicate;
import software.coley.recaf.services.search.match.NumberPredicateProvider;
import software.coley.recaf.services.search.query.NumberQuery;
import software.coley.recaf.services.search.query.Query;
import software.coley.recaf.services.workspace.WorkspaceManager;
import software.coley.recaf.ui.control.BoundBiDiComboBox;
import software.coley.recaf.ui.control.DynamicNumericTextField;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.util.Lang;
import software.coley.recaf.util.ToStringConverter;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

/**
 * Number search pane.
 *
 * @author Matt Coley
 */
@Dependent
public class NumberSearchPane extends AbstractSearchPane {
	private final StringProperty numericPredicateId = new SimpleStringProperty();
	private final ObjectProperty<Number> numericValueProperty = new SimpleObjectProperty<>(0);
	private final ObjectProperty<Class<? extends Number>> numericTypeProperty = new SimpleObjectProperty<>(Integer.class);
	private final ObservableValue<Boolean> isBlank;
	private final NumberPredicateProvider numberPredicateProvider;

	@Inject
	public NumberSearchPane(@Nonnull WorkspaceManager workspaceManager,
							@Nonnull SearchService searchService,
							@Nonnull CellConfigurationService configurationService,
							@Nonnull Actions actions,
							@Nonnull NumberPredicateProvider numberPredicateProvider) {
		super(workspaceManager, searchService, configurationService, actions);

		this.numberPredicateProvider = numberPredicateProvider;

		List<String> biPredicates = numberPredicateProvider.getBiNumberMatchers().keySet().stream().sorted().toList();
		DynamicNumericTextField textField = new DynamicNumericTextField(numericValueProperty, numericTypeProperty);
		ComboBox<String> modeCombo = new BoundBiDiComboBox<>(numericPredicateId, biPredicates,
				ToStringConverter.from(s -> Lang.get(NumberPredicate.TRANSLATION_PREFIX + s)));
		modeCombo.getSelectionModel().select(NumberPredicateProvider.KEY_EQUAL);
		isBlank = textField.textProperty().isEmpty();

		GridPane input = new GridPane();
		ColumnConstraints colTexts = new ColumnConstraints();
		ColumnConstraints colCombos = new ColumnConstraints();
		colTexts.setFillWidth(true);
		input.setAlignment(Pos.CENTER);
		input.getColumnConstraints().addAll(colTexts, colCombos);
		input.setHgap(10);
		input.setPadding(new Insets(10));
		input.addRow(0, textField, modeCombo);
		GridPane.setHgrow(textField, Priority.ALWAYS);

		EventStreams.changesOf(numericValueProperty)
				.or(EventStreams.changesOf(numericPredicateId))
				.reduceSuccessions(Collections::singletonList, Lists::add, Duration.ofMillis(Editor.SHORT_DELAY_MS))
				.addObserver(unused -> search());

		setInputs(input);
	}

	/**
	 * @return Predicate ID property within {@link NumberPredicateProvider} used for {@link #numericValuePropertyProperty()}.
	 */
	@Nonnull
	public StringProperty numericPredicateIdProperty() {
		return numericPredicateId;
	}

	/**
	 * @return Number value property to search with.
	 */
	@Nonnull
	public ObjectProperty<Number> numericValuePropertyProperty() {
		return numericValueProperty;
	}

	/**
	 * @return Type of {@link #numericValuePropertyProperty()} value.
	 */
	@Nonnull
	public ObjectProperty<Class<? extends Number>> numericTypePropertyProperty() {
		return numericTypeProperty;
	}

	@Nullable
	@Override
	protected Query buildQuery() {
		// Skip if blank
		if (isBlank.getValue())
			return null;

		Number search = numericValueProperty.get();
		String id = numericPredicateId.get();

		// Skip if unrecognized number predicate id
		NumberPredicate predicate = numberPredicateProvider.newBiNumberPredicate(id, search);
		if (predicate == null)
			return null;

		return new NumberQuery(predicate);
	}
}
