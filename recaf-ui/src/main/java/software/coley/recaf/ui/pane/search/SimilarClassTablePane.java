package software.coley.recaf.ui.pane.search;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Separator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.path.PathNodes;
import software.coley.recaf.services.cell.CellConfigurationService;
import software.coley.recaf.services.navigation.Actions;
import software.coley.recaf.services.search.SimilaritySearchService;
import software.coley.recaf.services.search.similarity.MemberOrderMode;
import software.coley.recaf.services.search.similarity.ParameterMatchMode;
import software.coley.recaf.services.search.similarity.ReturnMatchMode;
import software.coley.recaf.services.search.similarity.SimilarClassSearchOptions;
import software.coley.recaf.services.search.similarity.SimilarClassSearchResult;
import software.coley.recaf.services.search.similarity.SimilarClassSearchScope;
import software.coley.recaf.ui.control.BoundBiDiComboBox;
import software.coley.recaf.ui.control.BoundLabel;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.Lang;
import software.coley.recaf.util.ToStringConverter;
import software.coley.recaf.util.threading.ThreadUtil;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Table view showing similar classes for a reference class.
 *
 * @author Matt Coley
 */
@Dependent
public class SimilarClassTablePane extends AbstractSimilarityTablePane<ClassPathNode> {
	private static final Logger logger = Logging.get(SimilarClassTablePane.class);

	private final TableView<SimilarClassRow> table = new TableView<>();
	private final SimpleObjectProperty<ParameterMatchMode> parameterMatchMode =
			new SimpleObjectProperty<>(ParameterMatchMode.EXACT_COUNT_AND_ORDER);
	private final SimpleObjectProperty<ReturnMatchMode> returnMatchMode =
			new SimpleObjectProperty<>(ReturnMatchMode.EXACT_TYPE);
	private final SimpleObjectProperty<MemberOrderMode> methodOrderMode =
			new SimpleObjectProperty<>(MemberOrderMode.IGNORE_ORDER);
	private final SimpleObjectProperty<MemberOrderMode> fieldOrderMode =
			new SimpleObjectProperty<>(MemberOrderMode.IGNORE_ORDER);
	private final SimpleObjectProperty<SimilarClassSearchScope.Mode> scopeMode =
			new SimpleObjectProperty<>(SimilarClassSearchScope.Mode.SELF_RESOURCE);
	private ListView<WorkspaceResource> targetResources;

	@Inject
	public SimilarClassTablePane(@Nonnull SimilaritySearchService searchService,
	                             @Nonnull CellConfigurationService configurationService,
	                             @Nonnull Actions actions) {
		super(searchService, configurationService, actions);
		setupTable();
		initialize(table, table, parameterMatchMode, returnMatchMode, methodOrderMode, fieldOrderMode, scopeMode);
	}

	@Override
	public void onUpdatePath(@Nonnull PathNode<?> path) {
		if (!(path instanceof ClassPathNode classPath))
			return;
		setReferencePath(classPath);
		if (!isDisabled())
			refresh();
	}

	@SuppressWarnings("unchecked")
	private void setupTable() {
		TableColumn<SimilarClassRow, SimilarClassRow> classColumn = new TableColumn<>();
		TableColumn<SimilarClassRow, String> packageColumn = new TableColumn<>();
		TableColumn<SimilarClassRow, Number> similarityColumn = new TableColumn<>();

		classColumn.textProperty().bind(Lang.getBinding("menu.search.class-similar.column.class"));
		packageColumn.textProperty().bind(Lang.getBinding("misc.accessflag.visibility.package"));
		similarityColumn.textProperty().bind(Lang.getBinding("menu.search.class-similar.column.similarity"));

		classColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue()));
		packageColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().packageName()));
		similarityColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().result().similarity() * 100D));

		classColumn.setCellFactory(param -> new PathCell());
		similarityColumn.setCellFactory(param -> new TableCell<>() {
			@Override
			protected void updateItem(Number item, boolean empty) {
				super.updateItem(item, empty);
				setText(empty || item == null ? null : String.format("%.2f%%", item.doubleValue()));
			}
		});

		classColumn.setPrefWidth(360);
		packageColumn.setPrefWidth(260);
		similarityColumn.setPrefWidth(120);
		similarityColumn.setSortType(TableColumn.SortType.DESCENDING);

		table.getColumns().addAll(classColumn, packageColumn, similarityColumn);
		table.getSortOrder().setAll(similarityColumn);
		table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
		table.setPlaceholder(new BoundLabel(Lang.getBinding("menu.search.class-similar.placeholder")));
		table.setRowFactory(param -> {
			TableRow<SimilarClassRow> row = new TableRow<>();
			row.setOnMousePressed(e -> {
				if (!row.isEmpty() && e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2)
					openResult(row.getItem());
			});
			return row;
		});
		table.setOnKeyPressed(e -> {
			if (e.getCode() == KeyCode.ENTER) {
				SimilarClassRow selected = table.getSelectionModel().getSelectedItem();
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
		TableSearchFeedback feedback = createTableSearchFeedback();
		replaceSearchFeedback(feedback);
		table.setPlaceholder(searchingPlaceholder());

		SimilarClassSearchOptions options = new SimilarClassSearchOptions(
				threshold.get(),
				parameterMatchMode.get(),
				returnMatchMode.get(),
				methodOrderMode.get(),
				fieldOrderMode.get(),
				SimilarClassSearchOptions.DEFAULT_METHOD_WEIGHT,
				SimilarClassSearchOptions.DEFAULT_FIELD_WEIGHT,
				buildSearchScope()
		);

		CompletableFuture.supplyAsync(() -> searchService.searchClasses(referencePath, options, feedback), ThreadUtil.executor())
				.whenCompleteAsync((results, error) -> {
					if (generation != searchGeneration || feedback.hasRequestedCancellation())
						return;

					clearSearchFeedback(feedback);
					if (error != null) {
						logger.error("Similar class search failed", error);
						table.getItems().clear();
						table.setPlaceholder(new BoundLabel(Lang.getBinding("menu.search.class-similar.failed")));
						return;
					}

					populate(results);
				}, FxThreadUtil.executor());
	}

	private void populate(@Nonnull List<SimilarClassSearchResult> results) {
		table.getItems().setAll(results.stream().map(SimilarClassRow::new).toList());
		table.sort();
		table.setPlaceholder(new BoundLabel(Lang.getBinding("menu.search.class-similar.empty")));
	}

	@Nonnull
	@Override
	protected String referenceText() {
		if (referencePath == null)
			return "";
		return referencePath.getValue().getName();
	}

	@Nonnull
	@Override
	protected String referenceLabelKey() {
		return "menu.search.class-similar.reference";
	}

	@Nonnull
	@Override
	protected String thresholdLabelKey() {
		return "menu.search.class-similar.threshold";
	}

	@Nonnull
	@Override
	protected String advancedOptionsTooltipKey() {
		return "menu.search.class-similar.advanced";
	}

	@Nonnull
	@Override
	protected Node createAdvancedOptionsContent() {
		GridPane content = createBaseAdvancedOptionsContent();
		List<ParameterMatchMode> parameterModes = List.of(ParameterMatchMode.values());
		List<ReturnMatchMode> returnModes = List.of(ReturnMatchMode.values());
		List<MemberOrderMode> orderModes = List.of(MemberOrderMode.values());
		List<SimilarClassSearchScope.Mode> scopeModes = List.of(SimilarClassSearchScope.Mode.values());

		ComboBox<ParameterMatchMode> parameterModeCombo = new BoundBiDiComboBox<>(parameterMatchMode, parameterModes,
				ToStringConverter.from(mode -> Lang.get("menu.search.method-similar.parameter." + mode.name().toLowerCase())));
		ComboBox<ReturnMatchMode> returnModeCombo = new BoundBiDiComboBox<>(returnMatchMode, returnModes,
				ToStringConverter.from(mode -> Lang.get("menu.search.method-similar.return." + mode.name().toLowerCase())));
		ComboBox<MemberOrderMode> methodOrderCombo = new BoundBiDiComboBox<>(methodOrderMode, orderModes,
				ToStringConverter.from(mode -> Lang.get("menu.search.class-similar.order." + mode.name().toLowerCase())));
		ComboBox<MemberOrderMode> fieldOrderCombo = new BoundBiDiComboBox<>(fieldOrderMode, orderModes,
				ToStringConverter.from(mode -> Lang.get("menu.search.class-similar.order." + mode.name().toLowerCase())));
		ComboBox<SimilarClassSearchScope.Mode> scopeCombo = new BoundBiDiComboBox<>(scopeMode, scopeModes,
				ToStringConverter.from(mode -> Lang.get("menu.search.class-similar.scope." + mode.name().toLowerCase())));

		Workspace workspace = referencePath.getValueOfType(Workspace.class);
		List<WorkspaceResource> selectableResources = workspace == null ? List.of() : workspace.getAllResources(false).stream()
				.filter(resource -> !resource.isInternal())
				.toList();
		targetResources = new ListView<>(FXCollections.observableArrayList(selectableResources));
		targetResources.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		targetResources.setPrefHeight(Math.min(140, Math.max(60, selectableResources.size() * 28)));
		targetResources.setCellFactory(param -> new ListCell<>() {
			@Override
			protected void updateItem(WorkspaceResource item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || item == null || workspace == null) {
					setText(null);
					setGraphic(null);
					return;
				}

				PathNode<?> resourcePath = PathNodes.resourcePath(workspace, item);
				setText(configurationService.textOf(resourcePath));
				setGraphic(configurationService.graphicOf(resourcePath));
			}
		});
		targetResources.getSelectionModel().getSelectedItems().addListener((ListChangeListener<WorkspaceResource>) change -> refresh());

		BooleanBinding showTargets = scopeMode.isEqualTo(SimilarClassSearchScope.Mode.TARGET_RESOURCES);
		targetResources.visibleProperty().bind(showTargets);
		targetResources.managedProperty().bind(showTargets);

		int row = 0;
		content.add(new BoundLabel(Lang.getBinding("menu.search.class-similar.advanced.parameters")), 0, row);
		content.add(AbstractSearchPane.fixed(parameterModeCombo), 1, row++);
		content.add(new BoundLabel(Lang.getBinding("menu.search.class-similar.advanced.return")), 0, row);
		content.add(AbstractSearchPane.fixed(returnModeCombo), 1, row++);
		content.add(new Separator(), 0, row++, 2, 1);
		content.add(new BoundLabel(Lang.getBinding("menu.search.class-similar.advanced.method-order")), 0, row);
		content.add(AbstractSearchPane.fixed(methodOrderCombo), 1, row++);
		content.add(new BoundLabel(Lang.getBinding("menu.search.class-similar.advanced.field-order")), 0, row);
		content.add(AbstractSearchPane.fixed(fieldOrderCombo), 1, row++);
		content.add(new BoundLabel(Lang.getBinding("menu.search.class-similar.advanced.scope")), 0, row);
		content.add(AbstractSearchPane.fixed(scopeCombo), 1, row++);
		content.add(new Separator(), 0, row++, 2, 1);
		content.add(new BoundLabel(Lang.getBinding("menu.search.class-similar.advanced.targets")), 0, row);
		content.add(targetResources, 1, row++);
		row = AbstractSearchPane.addTextOption(content, row, "dialog.search.options.include-packages",
				"dialog.search.options.package-prefixes.tooltip", includedPackages);
		AbstractSearchPane.addTextOption(content, row, "dialog.search.options.exclude-packages",
				"dialog.search.options.package-prefixes.tooltip", excludedPackages);
		return content;
	}

	@Nonnull
	private SimilarClassSearchScope buildSearchScope() {
		return switch (scopeMode.get()) {
			case SELF_RESOURCE -> SimilarClassSearchScope.selfResource();
			case ALL_NON_INTERNAL -> SimilarClassSearchScope.allNonInternal();
			case TARGET_RESOURCES -> SimilarClassSearchScope.targetResources(
					targetResources == null ? List.of() : List.copyOf(targetResources.getSelectionModel().getSelectedItems()));
		};
	}

	private void openResult(@Nonnull SimilarClassRow row) {
		try {
			actions.gotoDeclaration(row.result().path());
		} catch (Exception ex) {
			logger.error("Failed to open similar class result", ex);
		}
	}

	private class PathCell extends TableCell<SimilarClassRow, SimilarClassRow> {
		@Override
		protected void updateItem(SimilarClassRow item, boolean empty) {
			super.updateItem(item, empty);
			if (empty || item == null) {
				setText(null);
				setGraphic(null);
				return;
			}

			setText(null);
			setGraphic(createConfiguredPathDisplay(item.result().path()));
		}
	}

	private record SimilarClassRow(@Nonnull SimilarClassSearchResult result) {
		@Nonnull
		private String packageName() {
			String packageName = result.path().getValue().getPackageName();
			return packageName == null ? Lang.get("tree.defaultpackage") : packageName;
		}
	}
}
