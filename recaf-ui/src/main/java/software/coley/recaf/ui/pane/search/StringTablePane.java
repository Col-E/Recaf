package software.coley.recaf.ui.pane.search;

import atlantafx.base.controls.Popover;
import atlantafx.base.theme.Styles;
import atlantafx.base.theme.Tweaks;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.info.member.ClassMember;
import software.coley.recaf.path.ClassMemberPathNode;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.IncompletePathException;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.path.PathNodes;
import software.coley.recaf.path.WorkspacePathNode;
import software.coley.recaf.services.cell.CellConfigurationService;
import software.coley.recaf.services.navigation.Actions;
import software.coley.recaf.services.navigation.Navigable;
import software.coley.recaf.services.search.CancellableSearchFeedback;
import software.coley.recaf.services.search.SearchService;
import software.coley.recaf.services.search.match.StringPredicate;
import software.coley.recaf.services.search.match.StringPredicateProvider;
import software.coley.recaf.services.search.query.Query;
import software.coley.recaf.services.search.query.StringQuery;
import software.coley.recaf.services.search.result.Result;
import software.coley.recaf.services.search.result.Results;
import software.coley.recaf.services.search.result.StringResult;
import software.coley.recaf.services.workspace.WorkspaceManager;
import software.coley.recaf.ui.control.ActionButton;
import software.coley.recaf.ui.control.BoundBiDiComboBox;
import software.coley.recaf.ui.control.BoundCheckBox;
import software.coley.recaf.ui.control.BoundIntSpinner;
import software.coley.recaf.ui.control.BoundLabel;
import software.coley.recaf.ui.control.BoundTextField;
import software.coley.recaf.ui.control.PathNodeTree;
import software.coley.recaf.ui.control.tree.TreeItems;
import software.coley.recaf.ui.control.tree.WorkspaceTreeNode;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.Lang;
import software.coley.recaf.util.RegexUtil;
import software.coley.recaf.util.TextDisplayUtil;
import software.coley.recaf.util.ToStringConverter;
import software.coley.recaf.util.threading.ThreadUtil;
import software.coley.recaf.workspace.model.Workspace;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;

import static software.coley.recaf.services.search.match.StringPredicateProvider.KEY_ANYTHING;
import static software.coley.recaf.services.search.match.StringPredicateProvider.KEY_NOTHING;

/**
 * An alternative to {@link StringSearchPane} that shows all strings in a table view instead of a tree.
 * This can be useful for quick skimming of all strings in a workspace, and also will make IDA users
 * feel more at home.
 *
 * @author Matt Coley
 */
@Dependent
public class StringTablePane extends BorderPane implements Navigable {
	private static final Logger logger = Logging.get(StringTablePane.class);
	private static final int LOCATION_POPOVER_WIDTH = 450;
	private static final int LOCATION_POPOVER_HEIGHT = 375;
	private final WorkspaceManager workspaceManager;
	private final SearchService searchService;
	private final CellConfigurationService configurationService;
	private final Actions actions;
	private final StringPredicateProvider stringPredicateProvider;
	private final WorkspacePathNode workspacePath;
	private final SearchOptions searchOptions = new SearchOptions();
	private final TableView<StringTableItem> table = new TableView<>();
	private final IntegerProperty minLength = new SimpleIntegerProperty(0);
	private final IntegerProperty maxLength = new SimpleIntegerProperty(Integer.MAX_VALUE);
	private final StringProperty stringPredicateId = new SimpleStringProperty(KEY_ANYTHING);
	private final StringProperty stringValue = new SimpleStringProperty("");
	private ActionButton searchOptionsButton;
	private Popover searchOptionsPopover;
	private CancellableSearchFeedback lastSearchFeedback;
	private int searchGeneration;

	@Inject
	public StringTablePane(@Nonnull WorkspaceManager workspaceManager,
	                        @Nonnull SearchService searchService,
	                        @Nonnull CellConfigurationService configurationService,
	                        @Nonnull Actions actions,
	                        @Nonnull StringPredicateProvider stringPredicateProvider) {
		this.workspaceManager = workspaceManager;
		this.searchService = searchService;
		this.configurationService = configurationService;
		this.actions = actions;
		this.stringPredicateProvider = stringPredicateProvider;
		workspacePath = PathNodes.workspacePath(Objects.requireNonNull(workspaceManager.getCurrent(), "No workspace open"));

		setTop(createInputs());
		setCenter(table);
		setupTable();
	}

	@Override
	public boolean isTrackable() {
		// We want this type to be navigable to benefit from automatic close support.
		return false;
	}

	@Nonnull
	@Override
	public PathNode<?> getPath() {
		return workspacePath;
	}

	@Nonnull
	@Override
	public Collection<Navigable> getNavigableChildren() {
		return Collections.emptyList();
	}

	@Override
	public void disable() {
		searchGeneration++;
		cancelLastSearch();
		table.getItems().clear();
		getChildren().clear();
		setDisable(true);
	}

	@Override
	public void requestFocus() {
		table.requestFocus();
	}

	/**
	 * @return String predicate id property within {@link StringPredicateProvider}.
	 */
	@Nonnull
	public StringProperty stringPredicateIdProperty() {
		return stringPredicateId;
	}

	/**
	 * @return String value property to filter table contents with.
	 */
	@Nonnull
	public StringProperty stringValueProperty() {
		return stringValue;
	}

	/**
	 * @return Minimum string length property.
	 */
	@Nonnull
	public IntegerProperty minLengthProperty() {
		return minLength;
	}

	/**
	 * @return Maximum string length property.
	 */
	@Nonnull
	public IntegerProperty maxLengthProperty() {
		return maxLength;
	}

	@Nonnull
	private Node createInputs() {
		List<String> stringPredicates = stringPredicateProvider.getBiStringMatchers().keySet().stream()
				.sorted().toList();

		BoundIntSpinner minLengthSpinner = new BoundIntSpinner(minLength, 0, Integer.MAX_VALUE);
		BoundIntSpinner maxLengthSpinner = new BoundIntSpinner(maxLength, 0, Integer.MAX_VALUE);
		minLengthSpinner.setPrefWidth(100);
		maxLengthSpinner.setPrefWidth(120);

		TextField textField = new TextField();
		textField.setMaxWidth(Double.MAX_VALUE);
		textField.textProperty().bindBidirectional(stringValue);
		textField.disableProperty().bind(stringPredicateId.isEqualTo(KEY_ANYTHING)
				.or(stringPredicateId.isEqualTo(KEY_NOTHING)));
		textField.setOnAction(e -> refresh()); // Enter to refresh search

		ComboBox<String> modeCombo = new BoundBiDiComboBox<>(stringPredicateId, stringPredicates,
				ToStringConverter.from(s -> Lang.get(StringPredicate.TRANSLATION_PREFIX + s)));
		modeCombo.getSelectionModel().select(KEY_ANYTHING);

		BooleanBinding invalidInputs = new BooleanBinding() {
			{
				bind(minLength, maxLength, stringPredicateId, stringValue);
			}

			@Override
			protected boolean computeValue() {
				if (minLength.get() > maxLength.get())
					return true;

				String id = stringPredicateId.get();
				String search = stringValue.get();
				return id != null && id.contains("regex") && !RegexUtil.validate(search).valid();
			}
		};

		ActionButton refresh = new ActionButton(CarbonIcons.SEARCH, this::refresh);
		refresh.withTooltip("menu.search");
		refresh.disableProperty().bind(invalidInputs);
		refresh.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.ACCENT);

		searchOptionsButton = new ActionButton(CarbonIcons.SETTINGS, this::showSearchOptionsPopover);
		searchOptionsButton.withTooltip("dialog.search.options");
		searchOptionsButton.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT);

		Region spacer = new Region();
		HBox input = new HBox(10,
				new BoundLabel(Lang.getBinding("menu.search.string-table.min")), minLengthSpinner,
				new BoundLabel(Lang.getBinding("menu.search.string-table.max")), maxLengthSpinner,
				textField, modeCombo,
				spacer, searchOptionsButton, refresh);
		input.setAlignment(Pos.CENTER_LEFT);
		input.setPadding(new Insets(10));
		HBox.setHgrow(textField, Priority.ALWAYS);
		return input;
	}

	@SuppressWarnings("unchecked")
	private void setupTable() {
		TableColumn<StringTableItem, String> columnValue = new TableColumn<>();
		TableColumn<StringTableItem, Number> columnLength = new TableColumn<>();
		TableColumn<StringTableItem, StringTableItem> columnLocations = new TableColumn<>();

		columnValue.textProperty().bind(Lang.getBinding("menu.search.string-table.column.string"));
		columnLength.textProperty().bind(Lang.getBinding("menu.search.string-table.column.length"));
		columnLocations.textProperty().bind(Lang.getBinding("menu.search.string-table.column.locations"));

		columnValue.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().escapedValue()));
		columnLength.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().length()));
		columnLocations.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue()));

		columnValue.setCellFactory(param -> new TableCell<>() {
			@Override
			protected void updateItem(String item, boolean empty) {
				super.updateItem(item, empty);
				setText(empty ? null : item);
			}
		});
		columnLocations.setCellFactory(param -> new LocationCell());

		columnLength.setPrefWidth(80);
		columnLength.setMinWidth(65);
		columnLength.setMaxWidth(120);
		columnLocations.setPrefWidth(380);

		table.getColumns().addAll(columnValue, columnLength, columnLocations);
		table.getStyleClass().addAll(Styles.STRIPED, Tweaks.EDGE_TO_EDGE);
		table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
		table.setPlaceholder(new BoundLabel(Lang.getBinding("menu.search.string-table.placeholder")));
		table.setRowFactory(param -> {
			TableRow<StringTableItem> row = new TableRow<>();
			row.setOnMousePressed(e -> {
				if (!row.isEmpty() && e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2)
					openLocation(row.getItem().viableLocation());
			});
			return row;
		});
		table.setOnKeyPressed(e -> {
			if (e.getCode() == KeyCode.ENTER) {
				StringTableItem selectedItem = table.getSelectionModel().getSelectedItem();
				if (selectedItem != null)
					openLocation(selectedItem.viableLocation());
			}
		});
	}

	/**
	 * Runs a search with the current filters.
	 */
	private void refresh() {
		// Skip if the panel has been disabled.
		if (isDisabled())
			return;

		// Skip if length is invalid.
		if (minLength.get() > maxLength.get())
			return;

		// Skip if no workspace is open.
		if (!workspaceManager.hasCurrentWorkspace()) {
			table.getItems().clear();
			return;
		}

		Query query = buildQuery();
		if (query == null)
			return;

		int generation = ++searchGeneration;
		Workspace workspace = workspaceManager.getCurrent();
		TableSearchFeedback feedback = new TableSearchFeedback(searchOptions.snapshot(), minLength.get(), maxLength.get());
		cancelLastSearch();
		lastSearchFeedback = feedback;
		table.setPlaceholder(new ProgressIndicator());

		CompletableFuture.supplyAsync(() -> searchService.search(workspace, query, feedback), ThreadUtil.executor())
				.whenCompleteAsync((results, error) -> {
					if (generation != searchGeneration || feedback.hasRequestedCancellation())
						return;

					if (lastSearchFeedback == feedback)
						lastSearchFeedback = null;

					if (error != null) {
						logger.error("String table search failed", error);
						table.setPlaceholder(new BoundLabel(Lang.getBinding("menu.search.string-table.failed")));
						return;
					}

					populate(results);
				}, FxThreadUtil.executor());
	}

	@Nullable
	private Query buildQuery() {
		String search = stringValue.get();
		String id = stringPredicateId.get();

		if (id == null)
			return null;
		if (KEY_ANYTHING.equals(id))
			return new StringQuery(stringPredicateProvider.newAnythingPredicate());
		if (KEY_NOTHING.equals(id))
			return new StringQuery(stringPredicateProvider.newNothingPredicate());

		if (id.contains("regex") && !RegexUtil.validate(search).valid())
			return null;

		try {
			return new StringQuery(stringPredicateProvider.newBiStringPredicate(id, search));
		} catch (NoSuchElementException ex) {
			logger.warn("Unknown string predicate for string table: {}", id);
			return null;
		}
	}

	/**
	 * @param results Results to show in the table.
	 */
	private void populate(@Nonnull Results results) {
		// We want to group results by string value, so we can show all locations for a given string in one row.
		Map<String, List<PathNode<?>>> grouped = new TreeMap<>();
		for (Result<?> result : results)
			if (result instanceof StringResult stringResult)
				grouped.computeIfAbsent(stringResult.getValue(), unused -> new ArrayList<>()).add(stringResult.getPath());

		// Convert to table items and show.
		List<StringTableItem> items = grouped.entrySet().stream()
				.map(entry -> new StringTableItem(entry.getKey(), List.copyOf(entry.getValue())))
				.toList();
		table.getItems().setAll(items);
		table.setPlaceholder(new BoundLabel(Lang.getBinding("menu.search.string-table.empty")));
	}

	/**
	 * @param path Path to open.
	 */
	private void openLocation(@Nonnull PathNode<?> path) {
		try {
			actions.gotoDeclaration(path);
		} catch (IncompletePathException ex) {
			logger.error("Failed to open string location", ex);
		}
	}

	/**
	 * Shows the advanced search options popover.
	 */
	private void showSearchOptionsPopover() {
		if (searchOptionsPopover == null) {
			searchOptionsPopover = new Popover(createSearchOptionsContent());
			searchOptionsPopover.setArrowLocation(Popover.ArrowLocation.TOP_RIGHT);
		}
		searchOptionsPopover.show(searchOptionsButton);
	}

	/**
	 * @return Content for the advanced search options popover.
	 */
	@Nonnull
	private GridPane createSearchOptionsContent() {
		GridPane content = new GridPane();
		ColumnConstraints labelColumn = new ColumnConstraints();
		ColumnConstraints controlColumn = new ColumnConstraints();
		controlColumn.setFillWidth(true);
		controlColumn.setHgrow(Priority.ALWAYS);
		controlColumn.setHalignment(HPos.RIGHT);
		content.getColumnConstraints().addAll(labelColumn, controlColumn);
		content.setHgap(10);
		content.setVgap(5);

		int row = 0;
		content.add(new BoundCheckBox(Lang.getBinding("dialog.search.options.search-classes"),
				searchOptions.searchClassesProperty()), 0, row++, 2, 1);
		row = addTextOption(content, row, "dialog.search.options.include-packages",
				"dialog.search.options.package-prefixes.tooltip", searchOptions.includedPackagesProperty());
		row = addTextOption(content, row, "dialog.search.options.exclude-packages",
				"dialog.search.options.package-prefixes.tooltip", searchOptions.excludedPackagesProperty());
		content.add(new BoundCheckBox(Lang.getBinding("dialog.search.options.search-files"),
				searchOptions.searchFilesProperty()), 0, row++, 2, 1);
		row = addTextOption(content, row, "dialog.search.options.include-directories",
				"dialog.search.options.directory-prefixes.tooltip", searchOptions.includedDirectoriesProperty());
		addTextOption(content, row, "dialog.search.options.exclude-directories",
				"dialog.search.options.directory-prefixes.tooltip", searchOptions.excludedDirectoriesProperty());

		return content;
	}

	/**
	 * @param content
	 * 		Grid to add the option to.
	 * @param row
	 * 		Grid row to add the option to.
	 * @param labelKey
	 * 		Translation key for the option label.
	 * @param tooltipKey
	 * 		Translation key for the option tooltip.
	 * @param property
	 * 		Property to bind the option value to.
	 *
	 * @return Next row index after the added option.
	 */
	private static int addTextOption(@Nonnull GridPane content,
	                                 int row,
	                                 @Nonnull String labelKey,
	                                 @Nonnull String tooltipKey,
	                                 @Nonnull StringProperty property) {
		BoundTextField field = new BoundTextField(property).withTooltip(tooltipKey);
		content.add(new BoundLabel(Lang.getBinding(labelKey)), 0, row);
		content.add(AbstractSearchPane.fixed(field), 1, row);
		return row + 1;
	}

	/**
	 * Stops the prior search.
	 */
	private void cancelLastSearch() {
		if (lastSearchFeedback != null) {
			lastSearchFeedback.cancel();
			lastSearchFeedback = null;
		}
	}

	private class LocationCell extends TableCell<StringTableItem, StringTableItem> {
		@Override
		protected void updateItem(StringTableItem item, boolean empty) {
			super.updateItem(item, empty);
			if (empty || item == null) {
				setText(null);
				setGraphic(null);
				return;
			}

			PathNode<?> location = item.viableLocation();
			Label locationLabel = new Label(configurationService.textOf(location));
			locationLabel.setGraphic(configurationService.graphicOf(location));
			locationLabel.setMaxWidth(Double.MAX_VALUE);
			HBox.setHgrow(locationLabel, Priority.ALWAYS);
			configurationService.configureStyle(locationLabel, location);
			locationLabel.setContextMenu(configurationService.contextMenuOf(SearchContextSource.SEARCH_INSTANCE, location));

			HBox container = new HBox(6, locationLabel);
			container.setAlignment(Pos.CENTER_LEFT);
			if (item.locationCount() > 1) {
				ActionButton locations = new ActionButton("+" + (item.locationCount() - 1), () -> showLocations(this, item));
				locations.addEventFilter(MouseEvent.MOUSE_PRESSED, MouseEvent::consume);
				locations.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT);
				container.getChildren().add(locations);
			}

			setText(null);
			setGraphic(container);
		}

		private void showLocations(@Nonnull Node owner, @Nonnull StringTableItem item) {
			Workspace workspace = item.viableLocation().getValueOfType(Workspace.class);
			if (workspace == null)
				return;

			PathNodeTree tree = new PathNodeTree(configurationService, actions);
			tree.contextSourceObjectPropertyProperty().set(SearchContextSource.SEARCH_INSTANCE);
			tree.setPrefSize(LOCATION_POPOVER_WIDTH, LOCATION_POPOVER_HEIGHT);

			WorkspaceTreeNode root = new WorkspaceTreeNode(PathNodes.workspacePath(workspace));
			root.setExpanded(true);
			tree.setRoot(root);
			for (PathNode<?> location : item.locations()) {
				WorkspaceTreeNode node = WorkspaceTreeNode.getOrInsertIntoTree(root, location);
				TreeItems.expandParents(node);
			}

			Popover popover = new Popover(tree);
			popover.setArrowLocation(Popover.ArrowLocation.TOP_RIGHT);
			popover.show(owner);
		}
	}

	private static class TableSearchFeedback extends CancellableSearchFeedback {
		private final SearchOptions.Snapshot optionsSnapshot;
		private final int minLength;
		private final int maxLength;

		private TableSearchFeedback(@Nonnull SearchOptions.Snapshot optionsSnapshot, int minLength, int maxLength) {
			this.optionsSnapshot = optionsSnapshot;
			this.minLength = minLength;
			this.maxLength = maxLength;
		}

		@Override
		public boolean doVisitClass(@Nonnull software.coley.recaf.info.ClassInfo cls) {
			return optionsSnapshot.shouldVisitClass(cls);
		}

		@Override
		public boolean doVisitFile(@Nonnull software.coley.recaf.info.FileInfo file) {
			return optionsSnapshot.shouldVisitFile(file);
		}

		@Override
		public boolean doAcceptResult(@Nonnull Result<?> result) {
			if (result instanceof StringResult stringResult) {
				int length = stringResult.getValue().length();
				return length >= minLength && length <= maxLength;
			}
			return false;
		}
	}

	private record StringTableItem(@Nonnull String value, @Nonnull List<PathNode<?>> locations) {
		private StringTableItem {
			if (locations.isEmpty())
				throw new IllegalArgumentException("String table item requires at least one location");
		}

		private int length() {
			return value.length();
		}

		@Nonnull
		private String escapedValue() {
			return TextDisplayUtil.escapeLimit(value, Integer.MAX_VALUE);
		}

		@Nonnull
		private PathNode<?> viableLocation() {
			PathNode<?> path = locations.getFirst();

			// Most of these results are going to be in instruction literals.
			// Since this is a flat table and not a tree we can only show one location, and the method/class is
			// more useful than the instruction itself.
			PathNode<? extends ClassMember> member = path.getPathOfType(ClassMember.class);
			if (member != null)
				return member;
			PathNode<? extends ClassInfo> clazz = path.getPathOfType(ClassInfo.class);
			if (clazz != null)
				return clazz;
			PathNode<? extends FileInfo> file = path.getPathOfType(FileInfo.class);
			if (file != null)
				return file;

			return path;
		}

		private int locationCount() {
			return locations.size();
		}
	}
}
