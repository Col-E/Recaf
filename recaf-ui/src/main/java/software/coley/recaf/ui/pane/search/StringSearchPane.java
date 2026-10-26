package software.coley.recaf.ui.pane.search;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
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
import software.coley.recaf.ui.control.BoundCheckBox;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.util.EscapeUtil;
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
	private final BooleanProperty escapedInput = new SimpleBooleanProperty();
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
		BoundCheckBox escaped = new BoundCheckBox(Lang.getBinding("menu.search.string.escaped-input"), escapedInput);
		escaped.visibleProperty().bind(stringPredicateId.map(s -> !(s.contains("regex") || s.contains("wildcard"))));
		escaped.managedProperty().bind(escaped.visibleProperty());

		GridPane input = new GridPane();
		ColumnConstraints colTexts = new ColumnConstraints();
		ColumnConstraints colCombos = new ColumnConstraints();
		colTexts.setFillWidth(true);
		input.setAlignment(Pos.CENTER);
		input.getColumnConstraints().addAll(colTexts, colCombos);
		input.setHgap(10);
		input.setPadding(new Insets(10));
		input.addRow(0, textField, modeCombo, escaped);
		GridPane.setHgrow(textField, Priority.ALWAYS);

		stringValue = textField.textProperty();
		EventStreams.changesOf(stringValue).map(unused -> new Object())
				.or(EventStreams.changesOf(stringPredicateId).map(unused -> new Object()))
				.or(EventStreams.changesOf(escapedInput).map(unused -> new Object()))
				.reduceSuccessions(Collections::singletonList, Lists::add, Duration.ofMillis(Editor.MEDIUM_DELAY_MS))
				.addObserver(unused -> searchLive());

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

	/**
	 * @return {@code true} if the input is escaped, {@code false} if the input is raw.
	 */
	@Nonnull
	public BooleanProperty escapedInputProperty() {
		return escapedInput;
	}

	/**
	 * @param value
	 * 		Value to search for
	 */
	public void setSearchValue(@Nonnull String value) {
		String displayValue = EscapeUtil.escapeStandardAndUnicodeWhitespace(value);
		escapedInput.set(!displayValue.equals(value)); // Auto-detect if we need to escape the input for display purposes.
		stringValue.set(displayValue);
	}

	@Override
	protected boolean supportsFileSearchOptions() {
		return true;
	}

	@Nullable
	@Override
	protected Query buildQuery() {
		String input = stringValue.get();
		String id = stringPredicateId.get();
		if (input.isBlank())
			return null;

		// Validate regex input
		boolean isRegex = id.contains("regex");
		String search = isRegex ? input : resolveSearchText(input, escapedInput.get());
		if (isRegex && !RegexUtil.validate(search).valid())
			return null;

		// Skip if unrecognized string predicate id
		StringPredicate predicate = stringPredicateProvider.newBiStringPredicate(id, search);
		if (predicate == null)
			return null;

		return new StringQuery(predicate);
	}

	@Nonnull
	private static String resolveSearchText(@Nonnull String input, boolean escapedInput) {
		if (!escapedInput)
			return input;

		// Preserve escaped backslashes before decoding other sequences.
		// We wouldn't want "\\n" to be decoded to a newline, but "\\\\n" should be decoded to "\n".
		StringBuilder result = null;
		int segmentStart = 0;
		for (int i = 0; i + 1 < input.length(); i++) {
			if (input.charAt(i) == '\\' && input.charAt(i + 1) == '\\') {
				if (result == null)
					result = new StringBuilder(input.length());
				result.append(EscapeUtil.unescapeStandardAndUnicodeWhitespace(input.substring(segmentStart, i)));
				result.append('\\');
				i++;
				segmentStart = i + 1;
			}
		}

		if (result == null)
			return EscapeUtil.unescapeStandardAndUnicodeWhitespace(input);
		return result.append(EscapeUtil.unescapeStandardAndUnicodeWhitespace(input.substring(segmentStart))).toString();
	}
}
