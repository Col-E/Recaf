package software.coley.recaf.ui.pane.search;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import org.reactfx.EventStreams;
import software.coley.collections.Lists;
import software.coley.recaf.services.cell.CellConfigurationService;
import software.coley.recaf.services.navigation.Actions;
import software.coley.recaf.services.search.SearchService;
import software.coley.recaf.services.search.match.StringPredicate;
import software.coley.recaf.services.search.match.StringPredicateProvider;
import software.coley.recaf.services.search.query.Query;
import software.coley.recaf.services.search.query.StringQuery;
import software.coley.recaf.services.workspace.WorkspaceManager;
import software.coley.recaf.ui.control.BoundBiDiComboBox;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.util.Lang;
import software.coley.recaf.util.RegexUtil;
import software.coley.recaf.util.ToStringConverter;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

import static software.coley.recaf.services.search.match.StringPredicateProvider.KEY_ANYTHING;
import static software.coley.recaf.services.search.match.StringPredicateProvider.KEY_NOTHING;

/**
 * String search pane.
 *
 * @author Matt Coley
 */
@Dependent
public class StringSearchPane extends AbstractSearchPane {
	private final StringProperty stringPredicateId = new SimpleStringProperty();
	private final StringProperty stringValue;
	private final StringPredicateProvider stringPredicateProvider;

	@Inject
	public StringSearchPane(@Nonnull WorkspaceManager workspaceManager,
							@Nonnull SearchService searchService,
							@Nonnull CellConfigurationService configurationService,
							@Nonnull Actions actions,
							@Nonnull StringPredicateProvider stringPredicateProvider) {
		super(workspaceManager, searchService, configurationService, actions);

		this.stringPredicateProvider = stringPredicateProvider;

		List<String> stringPredicates = stringPredicateProvider.getBiStringMatchers().keySet().stream()
				.filter(s -> !KEY_ANYTHING.equals(s) && !KEY_NOTHING.equals(s))
				.sorted().toList();
		TextField textField = new TextField();
		ComboBox<String> modeCombo = new BoundBiDiComboBox<>(stringPredicateId, stringPredicates,
				ToStringConverter.from(s -> Lang.get(StringPredicate.TRANSLATION_PREFIX + s)));
		modeCombo.getSelectionModel().select(StringPredicateProvider.KEY_CONTAINS);

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

		stringValue = textField.textProperty();
		EventStreams.changesOf(stringValue)
				.or(EventStreams.changesOf(stringPredicateId))
				.reduceSuccessions(Collections::singletonList, Lists::add, Duration.ofMillis(Editor.SHORT_DELAY_MS))
				.addObserver(unused -> search());

		setInputs(input);
	}

	/**
	 * @return Predicate ID property within {@link StringPredicateProvider} used for {@link #stringValueProperty()}.
	 */
	@Nonnull
	public StringProperty stringPredicateIdProperty() {
		return stringPredicateId;
	}

	/**
	 * @return String value property to search with.
	 */
	@Nonnull
	public StringProperty stringValueProperty() {
		return stringValue;
	}

	@Nullable
	@Override
	protected Query buildQuery() {
		String search = stringValue.get();
		String id = stringPredicateId.get();

		// Skip for blank input
		if (search.isBlank())
			return null;

		// Validate regex input
		if (id.contains("regex") && !RegexUtil.validate(search).valid())
			return null;

		// Skip if unrecognized string predicate id
		StringPredicate predicate = stringPredicateProvider.newBiStringPredicate(id, search);
		if (predicate == null)
			return null;

		return new StringQuery(predicate);
	}
}
