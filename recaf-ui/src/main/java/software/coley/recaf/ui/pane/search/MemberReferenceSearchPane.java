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
import software.coley.recaf.services.search.query.ReferenceQuery;
import software.coley.recaf.services.workspace.WorkspaceManager;
import software.coley.recaf.ui.control.BoundBiDiComboBox;
import software.coley.recaf.ui.control.BoundLabel;
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
 * Member reference search pane.
 *
 * @author Matt Coley
 */
@Dependent
public class MemberReferenceSearchPane extends AbstractSearchPane {
	private final StringProperty ownerPredicateId = new SimpleStringProperty();
	private final StringProperty namePredicateId = new SimpleStringProperty();
	private final StringProperty descPredicateId = new SimpleStringProperty();
	private final StringProperty ownerValue;
	private final StringProperty nameValue;
	private final StringProperty descValue;
	private final StringPredicateProvider stringPredicateProvider;

	@Inject
	public MemberReferenceSearchPane(@Nonnull WorkspaceManager workspaceManager,
									 @Nonnull SearchService searchService,
									 @Nonnull CellConfigurationService configurationService,
									 @Nonnull Actions actions,
									 @Nonnull StringPredicateProvider stringPredicateProvider) {
		super(workspaceManager, searchService, configurationService, actions);

		this.stringPredicateProvider = stringPredicateProvider;

		List<String> stringPredicates = stringPredicateProvider.getBiStringMatchers().keySet().stream()
				.filter(s -> !KEY_ANYTHING.equals(s) && !KEY_NOTHING.equals(s))
				.sorted().toList();
		TextField textOwner = new TextField();
		TextField textName = new TextField();
		TextField textDesc = new TextField();
		ComboBox<String> modeComboOwner = new BoundBiDiComboBox<>(ownerPredicateId, stringPredicates,
				ToStringConverter.from(s -> Lang.get(StringPredicate.TRANSLATION_PREFIX + s)));
		modeComboOwner.getSelectionModel().select(StringPredicateProvider.KEY_CONTAINS);
		ComboBox<String> modeComboName = new BoundBiDiComboBox<>(namePredicateId, stringPredicates,
				ToStringConverter.from(s -> Lang.get(StringPredicate.TRANSLATION_PREFIX + s)));
		modeComboName.getSelectionModel().select(StringPredicateProvider.KEY_CONTAINS);
		ComboBox<String> modeComboDesc = new BoundBiDiComboBox<>(descPredicateId, stringPredicates,
				ToStringConverter.from(s -> Lang.get(StringPredicate.TRANSLATION_PREFIX + s)));
		modeComboDesc.getSelectionModel().select(StringPredicateProvider.KEY_CONTAINS);

		GridPane input = new GridPane();
		ColumnConstraints colTexts = new ColumnConstraints();
		ColumnConstraints colCombos = new ColumnConstraints();
		colTexts.setFillWidth(true);
		input.setAlignment(Pos.CENTER);
		input.getColumnConstraints().addAll(colTexts, colCombos);
		input.setHgap(10);
		input.setVgap(10);
		input.setPadding(new Insets(10));
		input.addRow(0, new BoundLabel(Lang.getBinding("dialog.search.member-owner")), textOwner, modeComboOwner);
		input.addRow(1, new BoundLabel(Lang.getBinding("dialog.search.member-name")), textName, modeComboName);
		input.addRow(2, new BoundLabel(Lang.getBinding("dialog.search.member-descriptor")), textDesc, modeComboDesc);
		GridPane.setHgrow(textOwner, Priority.ALWAYS);
		GridPane.setHgrow(textName, Priority.ALWAYS);
		GridPane.setHgrow(textDesc, Priority.ALWAYS);

		ownerValue = textOwner.textProperty();
		nameValue = textName.textProperty();
		descValue = textDesc.textProperty();

		EventStreams.changesOf(ownerValue)
				.or(EventStreams.changesOf(ownerPredicateId))
				.or(EventStreams.changesOf(nameValue))
				.or(EventStreams.changesOf(namePredicateId))
				.or(EventStreams.changesOf(descValue))
				.or(EventStreams.changesOf(descPredicateId))
				.reduceSuccessions(Collections::singletonList, Lists::add, Duration.ofMillis(Editor.SHORT_DELAY_MS))
				.addObserver(unused -> search());

		setInputs(input);
	}

	/**
	 * @return Predicate ID property within {@link StringPredicateProvider} used for {@link #ownerValueProperty()}.
	 */
	@Nonnull
	public StringProperty ownerPredicateIdProperty() {
		return ownerPredicateId;
	}

	/**
	 * @return Predicate ID property within {@link StringPredicateProvider} used for {@link #nameValueProperty()}.
	 */
	@Nonnull
	public StringProperty namePredicateIdProperty() {
		return namePredicateId;
	}

	/**
	 * @return Predicate ID property within {@link StringPredicateProvider} used for {@link #descValueProperty()}.
	 */
	@Nonnull
	public StringProperty descPredicateIdProperty() {
		return descPredicateId;
	}

	/**
	 * @return Reference owner name value property to search with.
	 */
	@Nonnull
	public StringProperty ownerValueProperty() {
		return ownerValue;
	}

	/**
	 * @return Reference member name value property to search with.
	 */
	@Nonnull
	public StringProperty nameValueProperty() {
		return nameValue;
	}

	/**
	 * @return Reference member descriptor value property to search with.
	 */
	@Nonnull
	public StringProperty descValueProperty() {
		return descValue;
	}

	@Nullable
	@Override
	protected Query buildQuery() {
		String ownerSearch = ownerValue.get();
		String ownerId = ownerPredicateId.get();

		String nameSearch = nameValue.get();
		String nameId = namePredicateId.get();

		String descSearch = descValue.get();
		String descId = descPredicateId.get();

		StringPredicate ownerPredicate = buildPredicate(ownerId, ownerSearch);
		StringPredicate namePredicate = buildPredicate(nameId, nameSearch);
		StringPredicate descPredicate = buildPredicate(descId, descSearch);

		// Skip if no predicates are provided
		if (ownerPredicate == null && namePredicate == null && descPredicate == null)
			return null;

		return new ReferenceQuery(ownerPredicate, namePredicate, descPredicate);
	}

	@Nullable
	private StringPredicate buildPredicate(@Nonnull String id, @Nonnull String text) {
		StringPredicate namePredicate;
		if (text.isBlank())
			return null;

		// Validate regex input
		if (id.contains("regex") && !RegexUtil.validate(text).valid())
			return null;

		return stringPredicateProvider.newBiStringPredicate(id, text);
	}
}
