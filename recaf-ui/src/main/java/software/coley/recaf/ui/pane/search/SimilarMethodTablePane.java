package software.coley.recaf.ui.pane.search;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.path.ClassMemberPathNode;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.services.cell.CellConfigurationService;
import software.coley.recaf.services.navigation.Actions;
import software.coley.recaf.services.search.SimilaritySearchService;
import software.coley.recaf.services.search.similarity.ParameterMatchMode;
import software.coley.recaf.services.search.similarity.ReturnMatchMode;
import software.coley.recaf.services.search.similarity.SimilarMethodSearchOptions;
import software.coley.recaf.services.search.similarity.SimilarMethodSearchResult;
import software.coley.recaf.ui.control.BoundBiDiComboBox;
import software.coley.recaf.ui.control.BoundLabel;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.Lang;
import software.coley.recaf.util.ToStringConverter;
import software.coley.recaf.util.threading.ThreadUtil;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Table view showing similar methods for a reference method.
 *
 * @author Matt Coley
 */
@Dependent
public class SimilarMethodTablePane extends AbstractSimilarityTablePane<ClassMemberPathNode> {
	private static final Logger logger = Logging.get(SimilarMethodTablePane.class);

	private final TableView<SimilarMethodRow> table = new TableView<>();
	private final SimpleObjectProperty<ParameterMatchMode> parameterMatchMode =
			new SimpleObjectProperty<>(ParameterMatchMode.EXACT_COUNT_AND_ORDER);
	private final SimpleObjectProperty<ReturnMatchMode> returnMatchMode =
			new SimpleObjectProperty<>(ReturnMatchMode.EXACT_TYPE);

	@Inject
	public SimilarMethodTablePane(@Nonnull SimilaritySearchService searchService,
	                              @Nonnull CellConfigurationService configurationService,
	                              @Nonnull Actions actions) {
		super(searchService, configurationService, actions);
		setupTable();
		initialize(table, table, parameterMatchMode, returnMatchMode);
	}

	@Override
	public void onUpdatePath(@Nonnull PathNode<?> path) {
		if (!(path instanceof ClassMemberPathNode memberPath) || !memberPath.isMethod())
			return;
		setReferencePath(memberPath);
		if (!isDisabled())
			refresh();
	}

	@SuppressWarnings("unchecked")
	private void setupTable() {
		TableColumn<SimilarMethodRow, SimilarMethodRow> methodColumn = new TableColumn<>();
		TableColumn<SimilarMethodRow, SimilarMethodRow> classColumn = new TableColumn<>();
		TableColumn<SimilarMethodRow, Number> similarityColumn = new TableColumn<>();

		methodColumn.textProperty().bind(Lang.getBinding("menu.search.method-similar.column.method"));
		classColumn.textProperty().bind(Lang.getBinding("menu.search.method-similar.column.class"));
		similarityColumn.textProperty().bind(Lang.getBinding("menu.search.method-similar.column.similarity"));

		methodColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue()));
		classColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue()));
		similarityColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().result().similarity() * 100D));

		methodColumn.setCellFactory(param -> new PathCell(true));
		classColumn.setCellFactory(param -> new PathCell(false));
		similarityColumn.setCellFactory(param -> new TableCell<>() {
			@Override
			protected void updateItem(Number item, boolean empty) {
				super.updateItem(item, empty);
				setText(empty || item == null ? null : String.format("%.2f%%", item.doubleValue()));
			}
		});

		methodColumn.setPrefWidth(280);
		classColumn.setPrefWidth(360);
		similarityColumn.setPrefWidth(120);
		similarityColumn.setSortType(TableColumn.SortType.DESCENDING);

		table.getColumns().addAll(methodColumn, classColumn, similarityColumn);
		table.getSortOrder().setAll(similarityColumn);
		table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
		table.setPlaceholder(new BoundLabel(Lang.getBinding("menu.search.method-similar.placeholder")));
		table.setRowFactory(param -> {
			TableRow<SimilarMethodRow> row = new TableRow<>();
			row.setOnMousePressed(e -> {
				if (!row.isEmpty() && e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2)
					openResult(row.getItem());
			});
			return row;
		});
		table.setOnKeyPressed(e -> {
			if (e.getCode() == KeyCode.ENTER) {
				SimilarMethodRow selected = table.getSelectionModel().getSelectedItem();
				if (selected != null)
					openResult(selected);
			}
		});
	}

	@Override
	protected void refresh() {
		if (isDisabled() || referencePath == null)
			return;

		int generation = ++searchGeneration;
		SimilarMethodSearchOptions options = new SimilarMethodSearchOptions(
				threshold.get(),
				parameterMatchMode.get(),
				returnMatchMode.get(),
				true
		);
		TableSearchFeedback feedback = createTableSearchFeedback();
		replaceSearchFeedback(feedback);
		table.setPlaceholder(searchingPlaceholder());

		CompletableFuture.supplyAsync(() -> searchService.searchMethods(referencePath, options, feedback), ThreadUtil.executor())
				.whenCompleteAsync((results, error) -> {
					if (generation != searchGeneration || feedback.hasRequestedCancellation())
						return;

					clearSearchFeedback(feedback);
					if (error != null) {
						logger.error("Similar method search failed", error);
						table.getItems().clear();
						table.setPlaceholder(new BoundLabel(Lang.getBinding("menu.search.method-similar.failed")));
						return;
					}

					populate(results);
				}, FxThreadUtil.executor());
	}

	private void populate(@Nonnull List<SimilarMethodSearchResult> results) {
		table.getItems().setAll(results.stream().map(SimilarMethodRow::new).toList());
		table.sort();
		table.setPlaceholder(new BoundLabel(Lang.getBinding("menu.search.method-similar.empty")));
	}

	@Nonnull
	@Override
	protected String referenceText() {
		if (referencePath == null)
			return "";
		ClassInfo declaringClass = referencePath.getValueOfType(ClassInfo.class);
		MethodMember method = (MethodMember) referencePath.getValue();
		String owner = declaringClass == null ? "?" : declaringClass.getName();
		return owner + "#" + method.getName() + method.getDescriptor();
	}

	@Nonnull
	@Override
	protected String referenceLabelKey() {
		return "menu.search.method-similar.reference";
	}

	@Nonnull
	@Override
	protected String thresholdLabelKey() {
		return "menu.search.method-similar.threshold";
	}

	@Nonnull
	@Override
	protected String advancedOptionsTooltipKey() {
		return "menu.search.method-similar.advanced";
	}

	@Nonnull
	@Override
	protected Node createAdvancedOptionsContent() {
		GridPane content = createBaseAdvancedOptionsContent();

		List<ParameterMatchMode> parameterModes = List.of(ParameterMatchMode.values());
		List<ReturnMatchMode> returnModes = List.of(ReturnMatchMode.values());
		ComboBox<ParameterMatchMode> parameterModeCombo = new BoundBiDiComboBox<>(parameterMatchMode, parameterModes,
				ToStringConverter.from(mode -> Lang.get("menu.search.method-similar.parameter." + mode.name().toLowerCase())));
		ComboBox<ReturnMatchMode> returnModeCombo = new BoundBiDiComboBox<>(returnMatchMode, returnModes,
				ToStringConverter.from(mode -> Lang.get("menu.search.method-similar.return." + mode.name().toLowerCase())));

		int row = 0;
		content.add(new software.coley.recaf.ui.control.BoundLabel(Lang.getBinding("menu.search.method-similar.advanced.parameters")), 0, row);
		content.add(AbstractSearchPane.fixed(parameterModeCombo), 1, row++);
		content.add(new software.coley.recaf.ui.control.BoundLabel(Lang.getBinding("menu.search.method-similar.advanced.return")), 0, row);
		content.add(AbstractSearchPane.fixed(returnModeCombo), 1, row++);
		row = AbstractSearchPane.addTextOption(content, row, "dialog.search.options.include-packages",
				"dialog.search.options.package-prefixes.tooltip", includedPackages);
		AbstractSearchPane.addTextOption(content, row, "dialog.search.options.exclude-packages",
				"dialog.search.options.package-prefixes.tooltip", excludedPackages);
		return content;
	}

	private void openResult(@Nonnull SimilarMethodRow row) {
		try {
			actions.gotoDeclaration(row.result().path());
		} catch (Exception ex) {
			logger.error("Failed to open similar method result", ex);
		}
	}

	private class PathCell extends TableCell<SimilarMethodRow, SimilarMethodRow> {
		private final boolean methodCell;

		private PathCell(boolean methodCell) {
			this.methodCell = methodCell;
		}

		@Override
		protected void updateItem(SimilarMethodRow item, boolean empty) {
			super.updateItem(item, empty);
			if (empty || item == null) {
				setText(null);
				setGraphic(null);
				return;
			}

			PathNode<?> path = methodCell ? item.result().path() : item.result().path().getParent();
			setText(null);
			setGraphic(path == null ? null : createConfiguredPathDisplay(path));
		}
	}

	private record SimilarMethodRow(@Nonnull SimilarMethodSearchResult result) {}
}
