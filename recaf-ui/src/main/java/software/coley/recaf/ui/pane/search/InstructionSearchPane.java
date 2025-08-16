package software.coley.recaf.ui.pane.search;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import org.reactfx.EventStreams;
import software.coley.collections.Lists;
import software.coley.recaf.services.cell.CellConfigurationService;
import software.coley.recaf.services.navigation.Actions;
import software.coley.recaf.services.search.SearchService;
import software.coley.recaf.services.search.match.StringPredicate;
import software.coley.recaf.services.search.match.StringPredicateProvider;
import software.coley.recaf.services.search.query.InstructionQuery;
import software.coley.recaf.services.search.query.Query;
import software.coley.recaf.services.workspace.WorkspaceManager;
import software.coley.recaf.ui.control.ActionButton;
import software.coley.recaf.ui.control.BoundBiDiComboBox;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.util.Lang;
import software.coley.recaf.util.RegexUtil;
import software.coley.recaf.util.ToStringConverter;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static software.coley.recaf.services.search.match.StringPredicateProvider.KEY_ANYTHING;
import static software.coley.recaf.services.search.match.StringPredicateProvider.KEY_NOTHING;

/**
 * Instruction disassembly search pane.
 *
 * @author Matt Coley
 */
@Dependent
public class InstructionSearchPane extends AbstractSearchPane {
	private final StringPredicateProvider stringPredicateProvider;
	private final ObservableList<Line> lines = FXCollections.observableArrayList();

	@Inject
	public InstructionSearchPane(@Nonnull WorkspaceManager workspaceManager,
	                             @Nonnull SearchService searchService,
	                             @Nonnull CellConfigurationService configurationService,
	                             @Nonnull Actions actions,
	                             @Nonnull StringPredicateProvider stringPredicateProvider) {
		super(workspaceManager, searchService, configurationService, actions);

		this.stringPredicateProvider = stringPredicateProvider;

		GridPane input = new GridPane();
		ColumnConstraints colTexts = new ColumnConstraints();
		ColumnConstraints colCombos = new ColumnConstraints();
		colTexts.setFillWidth(true);
		input.setAlignment(Pos.CENTER);
		input.getColumnConstraints().addAll(colTexts, colCombos);
		input.setHgap(10);
		input.setPadding(new Insets(10));

		List<String> stringPredicates = stringPredicateProvider.getBiStringMatchers().keySet().stream()
				.filter(s -> !KEY_ANYTHING.equals(s) && !KEY_NOTHING.equals(s))
				.sorted().toList();
		lines.addListener((ListChangeListener<Line>) change -> {
			while (change.next()) {
				for (Line line : change.getAddedSubList()) {
					StringProperty stringValue = line.text();
					StringProperty stringPredicateId = line.predicateId();
					TextField textField = new TextField();
					textField.setId(line.uuid().toString());
					stringValue.bind(textField.textProperty());
					ComboBox<String> modeCombo = new BoundBiDiComboBox<>(stringPredicateId, stringPredicates,
							ToStringConverter.from(s -> Lang.get(StringPredicate.TRANSLATION_PREFIX + s)));
					modeCombo.getSelectionModel().select(StringPredicateProvider.KEY_CONTAINS);
					EventStreams.changesOf(stringValue)
							.or(EventStreams.changesOf(stringPredicateId))
							.reduceSuccessions(Collections::singletonList, Lists::add, Duration.ofMillis(Editor.MEDIUM_DELAY_MS))
							.addObserver(unused -> search());
					GridPane.setHgrow(textField, Priority.ALWAYS);
					input.addRow(input.getRowCount(), textField, modeCombo, new ActionButton(CarbonIcons.TRASH_CAN, () -> lines.remove(line)));
				}
				for (Line line : change.getRemoved()) {
					for (Node child : input.getChildrenUnmodifiable()) {
						if (line.uuid().toString().equals(child.getId())) {
							Integer nodeRow = GridPane.getRowIndex(child);
							if (nodeRow != null) {
								input.getRowConstraints().remove(nodeRow.intValue());
								break;
							}
						}
					}
				}
			}
		});

		// Add an initial line, and a button to facilitate adding additional lines
		lines.add(new Line(UUID.randomUUID(), new SimpleStringProperty(), new SimpleStringProperty()));
		Button addLine = new ActionButton(CarbonIcons.ADD_ALT, Lang.getBinding("dialog.search.add-instruction-line"), () -> {
			lines.add(new Line(UUID.randomUUID(), new SimpleStringProperty(), new SimpleStringProperty()));
		});
		input.addRow(0, addLine);
		setInputs(input);
	}

	/**
	 * @return List of models to match against lines of disassembled instructions.
	 */
	@Nonnull
	public ObservableList<Line> getLinePredicates() {
		return lines;
	}

	@Nullable
	@Override
	protected Query buildQuery() {
		List<StringPredicate> predicates = lines.stream().map(line -> {
			String search = line.text().get();
			String id = line.predicateId().get();

			// Skip for blank input
			if (search.isBlank())
				return null;

			// Validate regex input
			if (id.contains("regex") && !RegexUtil.validate(search).valid())
				return null;

			// May be null if no such id exists as a predicate, but we filter out nulls anyways.
			return stringPredicateProvider.newBiStringPredicate(id, search);
		}).filter(Objects::nonNull).toList();
		return new InstructionQuery(predicates);
	}

	/**
	 * Model of a single line of text to search against disassembled code.
	 *
	 * @param uuid
	 * 		Unique identifier for this input.
	 * @param text
	 * 		Text to match on the line.
	 * @param predicateId
	 * 		Predicate identifier for {@link StringPredicateProvider} lookups.
	 */
	public record Line(@Nonnull UUID uuid, @Nonnull StringProperty text, @Nonnull StringProperty predicateId) {}
}
