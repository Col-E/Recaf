package software.coley.recaf.ui.pane.search;

import atlantafx.base.controls.Popover;
import atlantafx.base.theme.Styles;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.animation.AnimationTimer;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import org.reactfx.EventStreams;
import org.slf4j.Logger;
import software.coley.bentofx.dockable.Dockable;
import software.coley.collections.Lists;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.path.IncompletePathException;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.path.PathNodes;
import software.coley.recaf.path.WorkspacePathNode;
import software.coley.recaf.services.cell.CellConfigurationService;
import software.coley.recaf.services.navigation.Actions;
import software.coley.recaf.services.navigation.Navigable;
import software.coley.recaf.services.search.CancellableSearchFeedback;
import software.coley.recaf.services.search.SearchService;
import software.coley.recaf.services.search.query.Query;
import software.coley.recaf.services.search.result.Result;
import software.coley.recaf.services.search.result.Results;
import software.coley.recaf.services.workspace.WorkspaceManager;
import software.coley.recaf.ui.control.ActionButton;
import software.coley.recaf.ui.control.BoundCheckBox;
import software.coley.recaf.ui.control.BoundLabel;
import software.coley.recaf.ui.control.BoundTextField;
import software.coley.recaf.ui.control.PathNodeTree;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.ui.control.tree.TreeItems;
import software.coley.recaf.ui.control.tree.WorkspaceTreeNode;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.Lang;
import software.coley.recaf.util.threading.Batch;
import software.coley.recaf.util.threading.ThreadUtil;
import software.coley.recaf.workspace.model.Workspace;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Common base capabilities for search panels.
 *
 * @author Matt Coley
 */
public abstract class AbstractSearchPane extends BorderPane implements Navigable {
	private static final Logger logger = Logging.get(AbstractSearchPane.class);
	private final WorkspaceManager workspaceManager;
	private final SearchService searchService;
	private final CellConfigurationService configurationService;
	private final Actions actions;
	private final WorkspacePathNode workspacePath;
	private final SearchOptions searchOptions = new SearchOptions();
	protected final PathNodeTree liveResultsTree;
	protected final BooleanProperty liveResults = new SimpleBooleanProperty(true);
	private final AtomicInteger searchGeneration = new AtomicInteger();
	private final List<Dockable> searchResultDockables = new ArrayList<>();
	private int searchResultSequence;
	private boolean focusNextSearchResults;
	private ActionButton searchOptionsButton;
	private Popover searchOptionsPopover;
	private CancellableSearchFeedback lastSearchFeedback;

	/**
	 * Create the base outline of a search panel capabilities.
	 *
	 * @param workspaceManager
	 * 		Manager to pull current workspace from.
	 * @param searchService
	 * 		Search service to initiate searches with.
	 * @param configurationService
	 * 		Cell configuration service to stylize the output tree model.
	 * @param actions
	 * 		Action service to assist stylizing the output tree model.
	 */
	public AbstractSearchPane(@Nonnull WorkspaceManager workspaceManager,
	                          @Nonnull SearchService searchService,
	                          @Nonnull CellConfigurationService configurationService,
	                          @Nonnull Actions actions) {
		this.workspaceManager = workspaceManager;
		this.searchService = searchService;
		this.configurationService = configurationService;
		this.actions = actions;

		liveResultsTree = newTree();

		workspacePath = PathNodes.workspacePath(Objects.requireNonNull(workspaceManager.getCurrent(),
				"Cannot open search if no workspace is open"));
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
		searchGeneration.incrementAndGet();
		cancelLastSearch();
		liveResultsTree.setRoot(null);
		for (Dockable dockable : new ArrayList<>(searchResultDockables))
			dockable.inContainer(container -> container.closeDockable(dockable));
		searchResultDockables.clear();
		getChildren().clear();
		setDisable(true);
	}

	/**
	 * Set up the UI with the given inputs.
	 *
	 * @param input
	 * 		Node to handle user input.
	 */
	protected void setInputs(@Nonnull Node input) {
		Node liveResultsDisplay = createLiveResultsDisplay();
		ActionButton searchButton = new ActionButton(CarbonIcons.SEARCH, this::searchManual);
		searchButton.withTooltip("menu.search");
		searchButton.setFocusTraversable(false);
		searchButton.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.ACCENT);
		searchButton.visibleProperty().bind(liveResults.not());
		searchButton.managedProperty().bind(liveResults.not());
		HBox inputWrapper = new HBox(input, searchButton);
		inputWrapper.setAlignment(Pos.CENTER_LEFT);
		inputWrapper.setSpacing(0);
		inputWrapper.setPadding(new Insets(0, 10, 0, 0));
		HBox.setHgrow(input, Priority.ALWAYS);
		input.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
			if (event.getCode() == KeyCode.ENTER) {
				searchManual();
				event.consume();
			}
		});

		setTop(inputWrapper);
		setCenter(liveResultsDisplay);

		liveResults.addListener((ob, old, cur) -> {
			if (!cur)
				liveResultsTree.setRoot(null);
			setCenter(liveResultsDisplay);
			if (cur)
				search();
		});
		setupSearchOptionsListener();
	}

	/**
	 * @return {@code true} when this search type can visit files.
	 */
	protected boolean supportsFileSearchOptions() {
		return false;
	}

	/**
	 * @return Node wrapping the live results tree with overlay controls.
	 */
	@Nonnull
	private Node createLiveResultsDisplay() {
		if (!supportsFileSearchOptions())
			searchOptions.searchFilesProperty().set(false);

		searchOptionsButton = new ActionButton(CarbonIcons.SETTINGS, this::showSearchOptionsPopover);
		searchOptionsButton.withTooltip("dialog.search.options");
		searchOptionsButton.setFocusTraversable(false);
		searchOptionsButton.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.ACCENT, Styles.FLAT);
		StackPane.setAlignment(searchOptionsButton, Pos.BOTTOM_RIGHT);
		StackPane.setMargin(searchOptionsButton, new Insets(7));

		return new StackPane(liveResultsTree, searchOptionsButton);
	}

	/**
	 * Refreshes search results when advanced options change.
	 */
	private void setupSearchOptionsListener() {
		EventStreams.changesOf(searchOptions.searchClassesProperty()).map(unused -> new Object())
				.or(EventStreams.changesOf(searchOptions.searchFilesProperty()).map(unused -> new Object()))
				.or(EventStreams.changesOf(searchOptions.includedPackagesProperty()).map(unused -> new Object()))
				.or(EventStreams.changesOf(searchOptions.excludedPackagesProperty()).map(unused -> new Object()))
				.or(EventStreams.changesOf(searchOptions.includedDirectoriesProperty()).map(unused -> new Object()))
				.or(EventStreams.changesOf(searchOptions.excludedDirectoriesProperty()).map(unused -> new Object()))
				.reduceSuccessions(Collections::singletonList, Lists::add, Duration.ofMillis(Editor.SHORT_DELAY_MS))
				.addObserver(unused -> searchLive());
	}

	/**
	 * Shows the advanced search options popover.
	 */
	private void showSearchOptionsPopover() {
		if (searchOptionsPopover == null) {
			searchOptionsPopover = new Popover(createSearchOptionsContent());
			searchOptionsPopover.setArrowLocation(Popover.ArrowLocation.BOTTOM_RIGHT);
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
		content.add(new BoundCheckBox(Lang.getBinding("search.live"), liveResults), 0, row++, 2, 1);
		row = addCustomSearchOptions(content, row);
		if (supportsFileSearchOptions()) {
			// Can only disable class searching if file searching is enabled.
			content.add(new BoundCheckBox(Lang.getBinding("dialog.search.options.search-classes"),
					searchOptions.searchClassesProperty()), 0, row++, 2, 1);
		}
		row = addTextOption(content, row, "dialog.search.options.include-packages",
				"dialog.search.options.package-prefixes.tooltip", searchOptions.includedPackagesProperty());
		row = addTextOption(content, row, "dialog.search.options.exclude-packages",
				"dialog.search.options.package-prefixes.tooltip", searchOptions.excludedPackagesProperty());

		if (supportsFileSearchOptions()) {
			content.add(new BoundCheckBox(Lang.getBinding("dialog.search.options.search-files"),
					searchOptions.searchFilesProperty()), 0, row++, 2, 1);
			row = addTextOption(content, row, "dialog.search.options.include-directories",
					"dialog.search.options.directory-prefixes.tooltip", searchOptions.includedDirectoriesProperty());
			row = addTextOption(content, row, "dialog.search.options.exclude-directories",
					"dialog.search.options.directory-prefixes.tooltip", searchOptions.excludedDirectoriesProperty());
		}

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
	protected static int addTextOption(@Nonnull GridPane content,
	                                   int row,
	                                   @Nonnull String labelKey,
	                                   @Nonnull String tooltipKey,
	                                   @Nonnull StringProperty property) {
		BoundTextField field = new BoundTextField(property).withTooltip(tooltipKey);
		content.add(new BoundLabel(Lang.getBinding(labelKey)), 0, row);
		content.add(fixed(field), 1, row);
		return row + 1;
	}

	/**
	 * Hook for search pane children to add custom search controls to the options popover.
	 *
	 * @param content
	 * 		Grid to add options to.
	 * @param row
	 * 		Grid row to begin at.
	 *
	 * @return Next row index after the added options.
	 */
	protected int addCustomSearchOptions(@Nonnull GridPane content, int row) {
		return row;
	}

	/**
	 * @param control
	 * 		Control to set to fill horizontal space.
	 *
	 * @return Given control with max width set to fill horizontal space.
	 */
	@Nonnull
	protected static Control fixed(@Nonnull Control control) {
		control.setMaxWidth(Double.MAX_VALUE);
		GridPane.setFillWidth(control, true);
		return control;
	}

	/**
	 * @return New path-node tree.
	 */
	@Nonnull
	protected PathNodeTree newTree() {
		PathNodeTree tree = new PathNodeTree(configurationService, actions);
		tree.contextSourceObjectPropertyProperty().set(SearchContextSource.SEARCH_INSTANCE);
		tree.setOnMousePressed(e -> {
			if (e.getClickCount() == 2 && e.isPrimaryButtonDown()) {
				var item = tree.getSelectionModel().getSelectedItem();
				if (item != null && item.isLeaf()) {
					try {
						actions.gotoDeclaration(item.getValue());
					} catch (IncompletePathException ignored) {
						// ignored
					}
				}
			}
		});
		return tree;
	}

	/**
	 * @return The built query from current search inputs,
	 * or {@code null} if the inputs were invalid for any reason.
	 */
	@Nullable
	protected abstract Query buildQuery();

	/**
	 * @return Predicate that determines which search results are accepted for the current search run.
	 */
	@Nonnull
	protected Predicate<Result<?>> createResultFilter() {
		return this::doAcceptResult;
	}

	/**
	 * @param result
	 * 		Result to consider.
	 *
	 * @return {@code true} when the result should be accepted.
	 */
	protected boolean doAcceptResult(@Nonnull Result<?> result) {
		return true;
	}

	/**
	 * Search and show results inline with this panel if the live results option is enabled.
	 *
	 * @see #searchManual()
	 */
	protected final void searchLive() {
		if (liveResults.get())
			search();
	}

	/**
	 * Search and show results in a separate dockable once the search completes.
	 *
	 * @see #handleSearchResults(Results, boolean)
	 * @see #searchLive()
	 */
	private void searchManual() {
		focusNextSearchResults = true;
		search();
	}

	/**
	 * Initiates the search with current search inputs. Updates the output display.
	 */
	protected final void search() {
		// Skip if the panel has been disabled (occurs when closing it).
		// Sometimes the delay between searching and the user closing will initiate a search after closing.
		if (isDisabled()) return;

		// Every search attempt invalidates completions from prior generations, including invalid queries.
		int generation = searchGeneration.incrementAndGet();
		boolean focusResults = focusNextSearchResults;
		focusNextSearchResults = false;
		cancelLastSearch();

		// Must have a current workspace to search in.
		if (!workspaceManager.hasCurrentWorkspace())
			return;

		// Create a new root for live searches.
		Workspace workspace = workspaceManager.getCurrent();
		WorkspaceTreeNode root = null;
		if (liveResults.get()) {
			root = new WorkspaceTreeNode(PathNodes.workspacePath(workspace));
			root.setExpanded(true);
			liveResultsTree.setRoot(root);
		}
		WorkspaceTreeNode liveRoot = root;

		// Skip if the query couldn't be built (invalid inputs most likely)
		Query query = buildQuery();
		if (query == null)
			return;

		// Run new search.
		SearchOptions.Snapshot optionsSnapshot = searchOptions.snapshot();
		Predicate<Result<?>> resultFilter = createResultFilter();
		CancellableSearchFeedback feedback;
		if (liveResults.get()) {
			feedback = new LiveOnlySearchFeedback(optionsSnapshot, resultFilter, result -> {
				if (generation != searchGeneration.get() || isDisabled())
					return;
				WorkspaceTreeNode node = WorkspaceTreeNode.getOrInsertIntoTree(liveRoot, result.getPath());
				TreeItems.expandParents(node);
			});
			lastSearchFeedback = feedback;
			CompletableFuture.runAsync(() -> searchService.search(workspace, query, feedback), ThreadUtil.executor())
					.whenCompleteAsync((ignored, error) -> {
						if (lastSearchFeedback == feedback)
							lastSearchFeedback = null;
						if (error != null && generation == searchGeneration.get() && !feedback.hasRequestedCancellation())
							logger.error("Search failed", error);
					}, FxThreadUtil.executor());
		} else {
			feedback = new FilteringSearchFeedback(optionsSnapshot, resultFilter);
			lastSearchFeedback = feedback;
			CompletableFuture.supplyAsync(() -> searchService.search(workspace, query, feedback), ThreadUtil.executor())
					.whenCompleteAsync((results, error) -> {
						if (generation != searchGeneration.get() || feedback.hasRequestedCancellation() || isDisabled()
								|| !workspaceManager.hasCurrentWorkspace() || workspaceManager.getCurrent() != workspace)
							return;
						if (lastSearchFeedback == feedback)
							lastSearchFeedback = null;
						if (error != null) {
							logger.error("Search failed", error);
							return;
						}
						handleSearchResults(results, focusResults);
					}, FxThreadUtil.executor());
		}
	}

	/**
	 * Displays a completed manual search result set.
	 *
	 * @param results
	 * 		Results of the search.
	 * @param focusResults
	 * 		Whether the result dockable should be selected because the user explicitly pressed Search.
	 */
	private void handleSearchResults(@Nonnull Results results, boolean focusResults) {
		if (!workspaceManager.hasCurrentWorkspace())
			return;

		SearchResultsModel model = new SearchResultsModel();
		SearchResultsPane resultsPane = new SearchResultsPane(workspaceManager.getCurrent(), configurationService,
				actions, model);
		resultsPane.setResults(results);
		int resultSequence = ++searchResultSequence;
		Dockable dockable = actions.openSearchResults(this, resultsPane,
				Lang.format("search.results.title", resultSequence, model.getMatchCount()),
				searchResultDockables::remove);
		searchResultDockables.add(dockable);
		if (focusResults)
			dockable.inContainer(container -> container.selectDockable(dockable));
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

	/**
	 * Feedback that filters which workspace items are visited.
	 */
	private static class FilteringSearchFeedback extends CancellableSearchFeedback {
		private final SearchOptions.Snapshot optionsSnapshot;
		private final Predicate<Result<?>> resultFilter;

		private FilteringSearchFeedback(@Nonnull SearchOptions.Snapshot optionsSnapshot,
		                                @Nonnull Predicate<Result<?>> resultFilter) {
			this.optionsSnapshot = optionsSnapshot;
			this.resultFilter = resultFilter;
		}

		@Override
		public boolean doVisitClass(@Nonnull ClassInfo cls) {
			return optionsSnapshot.shouldVisitClass(cls);
		}

		@Override
		public boolean doVisitFile(@Nonnull FileInfo file) {
			return optionsSnapshot.shouldVisitFile(file);
		}

		@Override
		public boolean doAcceptResult(@Nonnull Result<?> result) {
			return resultFilter.test(result);
		}
	}

	/**
	 * Feedback that incrementally updates the search results tree.
	 * <br>
	 * Disables the collection of results into a single wrapper at the end of a search.
	 * Since this is for live-only feedback, we won't use the resulting collection anyways, so we don't need to do
	 * the extra work.
	 */
	private class LiveOnlySearchFeedback extends FilteringSearchFeedback {
		private final Batch batch = FxThreadUtil.batch();
		private final AnimationTimer batchTimer = new AnimationTimer() {
			private static final long BATCH_INTERVAL_MS = 1000 / 4;
			private long last;

			@Override
			public void handle(long now) {
				if (now - last > BATCH_INTERVAL_MS) {
					publishResults();
					last = now;
				}
			}
		};
		private final Consumer<Result<?>> resultConsumer;

		private LiveOnlySearchFeedback(@Nonnull SearchOptions.Snapshot optionsSnapshot,
		                               @Nonnull Predicate<Result<?>> resultFilter,
		                               @Nonnull Consumer<Result<?>> resultConsumer) {
			super(optionsSnapshot, resultFilter);
			this.resultConsumer = resultConsumer;
			batchTimer.start();
		}

		@Override
		public boolean doAcceptResult(@Nonnull Result<?> result) {
			if (!super.doAcceptResult(result))
				return false;
			batch.add(() -> resultConsumer.accept(result));
			return false;
		}

		@Override
		public void onCompletion() {
			batchTimer.stop();
			publishResults();
		}

		private void publishResults() {
			batch.execute();
		}
	}
}
