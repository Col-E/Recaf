package software.coley.recaf.ui.pane.analysis;

import atlantafx.base.controls.CustomTextField;
import atlantafx.base.controls.RingProgressIndicator;
import atlantafx.base.controls.Spacer;
import atlantafx.base.theme.Styles;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.TextAlignment;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import software.coley.bentofx.Bento;
import software.coley.bentofx.building.DockBuilding;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.layout.container.DockContainerLeaf;
import software.coley.bentofx.layout.container.DockContainerRootBranch;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.IncompletePathException;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.path.PathNodes;
import software.coley.recaf.path.WorkspacePathNode;
import software.coley.recaf.services.analysis.structure.AreaAnalysisResult;
import software.coley.recaf.services.analysis.structure.AreaAnalysisService;
import software.coley.recaf.services.analysis.structure.AreaGroup;
import software.coley.recaf.services.analysis.structure.AreaLink;
import software.coley.recaf.services.cell.CellConfigurationService;
import software.coley.recaf.services.cell.icon.IconProviderService;
import software.coley.recaf.services.cell.text.TextProviderService;
import software.coley.recaf.services.info.summary.builtin.AreaAnalysisSummarizer;
import software.coley.recaf.services.navigation.Actions;
import software.coley.recaf.services.navigation.Navigable;
import software.coley.recaf.services.search.similarity.PackagePurpose;
import software.coley.recaf.services.workspace.WorkspaceManager;
import software.coley.recaf.ui.control.BoundLabel;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.ui.docking.DockingManager;
import software.coley.recaf.ui.docking.EmbeddedBento;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.Lang;
import software.coley.recaf.util.threading.ThreadPoolFactory;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;
import software.coley.spatialcanvas.SpatialCanvas;
import software.coley.spatialcanvas.content.EdgeArrowVisibility;
import software.coley.spatialcanvas.content.EdgeShape;
import software.coley.spatialcanvas.content.GraphContentModel;
import software.coley.spatialcanvas.content.GraphEdgeItem;
import software.coley.spatialcanvas.content.GraphNodeItem;
import software.coley.spatialcanvas.graph.Graph;
import software.coley.spatialcanvas.graph.GraphEdgeSpec;
import software.coley.spatialcanvas.graph.GraphNodeSpec;
import software.coley.spatialcanvas.graph.layout.BackEdgeAwareLayeredGraphLayoutEngine;
import software.coley.spatialcanvas.render.ContentRenderer;
import software.coley.spatialcanvas.render.EdgeRenderer;
import software.coley.spatialcanvas.render.RenderContext;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Architecture explorer for {@link AreaAnalysisResult}.
 * <p>
 * Initially shows a summary pane with some overview statistics.
 * Clicking on a group in the summary pane or side explorer will
 * show a focused graph of the selected group and its immediate relationships.
 *
 * @author Matt Coley
 */
@Dependent
public class AreaAnalysisPane extends StackPane implements Navigable {
	// Fixed node sizing keeps the layered layout readable.
	private static final double GRAPH_NODE_WIDTH = 196;
	private static final double GRAPH_NODE_HEIGHT = 106;
	private static final double GRAPH_PADDING = 48;
	private static final Object GRAPH_NODE_RENDERER_KEY = "area-graph-node";
	private static final Object GRAPH_EDGE_RENDERER_KEY = "area-graph-edge";
	private static final BackEdgeAwareLayeredGraphLayoutEngine GRAPH_LAYOUT_ENGINE = new BackEdgeAwareLayeredGraphLayoutEngine();

	private final Workspace workspace;
	private final WorkspacePathNode workspacePath;
	private final AreaAnalysisService areaAnalysisService;
	private final TextProviderService textProviderService;
	private final IconProviderService iconProviderService;
	private final CellConfigurationService cellConfigurationService;
	private final Actions actions;
	private final ExecutorService analysisPool = ThreadPoolFactory.newSingleThreadExecutor("area-analysis");
	private final AtomicInteger runCounter = new AtomicInteger();
	private final AreaAnalysisPaneModel model = new AreaAnalysisPaneModel();
	private final Bento bento = new EmbeddedBento();
	private final RingProgressIndicator progressIndicator = new RingProgressIndicator(ProgressIndicator.INDETERMINATE_PROGRESS);
	private final Label progressLabel = new BoundLabel(Lang.getBinding("service.analysis.areas.loading"));
	private final VBox progressBox = new VBox();
	private final StackPane loadingOverlay = new StackPane();
	private final Label statusLabel = new Label();
	private final Label placeholderLabel = new Label();
	private final VBox placeholderBox = new VBox();
	private final ComboBox<WorkspaceResource> resourceSelector = new ComboBox<>();
	private final CustomTextField searchField = new CustomTextField();
	private final ComboBox<PurposeOption> purposeSelector = new ComboBox<>();
	private final ToggleButton entryPointsFilter = createFilterToggle("service.analysis.areas.filter.entry-points");
	private final ToggleButton largeGroupsFilter = createFilterToggle("service.analysis.areas.filter.large");
	private final ToggleButton highFanInFilter = createFilterToggle("service.analysis.areas.filter.high-fan-in");
	private final ToggleButton highFanOutFilter = createFilterToggle("service.analysis.areas.filter.high-fan-out");
	private final Button clearSelectionButton = new Button();
	private final ListView<AreaGroup> explorerList = new ListView<>();
	private final SpatialCanvas relationshipGraph = new SpatialCanvas();
	private final Label graphEmptyLabel = new Label(Lang.get("service.analysis.areas.graph.empty.select"));
	private final StackPane graphSurface = new StackPane();
	private final HBox graphModeOverlay = new HBox(0);
	private final Group graphModeOverlayGroup = new Group();
	private final Group graphLegendOverlayGroup = new Group();
	private final ScrollPane overviewScroll = new ScrollPane();
	private final VBox overviewSections = new VBox(18);
	private final BorderPane centerWrapper = new BorderPane();
	private final ToggleGroup focusModeGroup = new ToggleGroup();
	private final ToggleButton focusBothToggle = createModeToggle("service.analysis.areas.mode.both", CarbonIcons.ARROWS_HORIZONTAL, AreaAnalysisPaneModel.AreaViewMode.GROUP_FOCUS_BOTH);
	private final ToggleButton focusInboundToggle = createModeToggle("service.analysis.areas.mode.inbound", CarbonIcons.ARROW_RIGHT, AreaAnalysisPaneModel.AreaViewMode.GROUP_FOCUS_INBOUND);
	private final ToggleButton focusOutboundToggle = createModeToggle("service.analysis.areas.mode.outbound", CarbonIcons.ARROW_RIGHT, AreaAnalysisPaneModel.AreaViewMode.GROUP_FOCUS_OUTBOUND);
	private final Label focusedGraphTitle = new Label(Lang.get("service.analysis.areas.graph.focused"));
	private final Label detailTitle = new Label(Lang.get("service.analysis.areas.selection.none"));
	private final Label detailSubtitle = new Label(Lang.get("service.analysis.areas.details.select-group"));
	private final Label detailKindValue = new Label();
	private final Label detailPurposeValue = new Label();
	private final Label detailConfidenceValue = new Label();
	private final Label detailClassCountValue = new Label();
	private final Label detailEntryValue = new Label();
	private final Label detailInboundValue = new Label();
	private final Label detailOutboundValue = new Label();
	private final Label detailVisibleInboundValue = new Label();
	private final Label detailVisibleOutboundValue = new Label();
	private final Label detailHiddenInboundValue = new Label();
	private final Label detailHiddenOutboundValue = new Label();
	private final Label detailHiddenEdgesValue = new Label();
	private final Label spaghettiWarning = new BoundLabel(Lang.getBinding("service.analysis.areas.warning.spaghetti"));
	private final Label edgeSourceValue = new Label();
	private final Label edgeTargetValue = new Label();
	private final Label edgeWeightValue = new Label();
	private final Label edgeCountValue = new Label();
	private final VBox edgeDetailsBox = new VBox();
	private final ListView<ClassPathNode> classList = new ListView<>();
	private final ObjectProperty<WorkspaceResource> selectedResource = new SimpleObjectProperty<>();
	private boolean suppressSelectionAnalyze;

	@Inject
	public AreaAnalysisPane(@Nonnull WorkspaceManager workspaceManager,
	                        @Nonnull AreaAnalysisService areaAnalysisService,
	                        @Nonnull TextProviderService textProviderService,
	                        @Nonnull IconProviderService iconProviderService,
	                        @Nonnull CellConfigurationService cellConfigurationService,
	                        @Nonnull Actions actions) {
		this.workspace = Objects.requireNonNull(workspaceManager.getCurrent(), "Cannot open area analysis without a workspace");
		this.workspacePath = PathNodes.workspacePath(workspace);
		this.areaAnalysisService = areaAnalysisService;
		this.textProviderService = textProviderService;
		this.iconProviderService = iconProviderService;
		this.cellConfigurationService = cellConfigurationService;
		this.actions = actions;

		statusLabel.getStyleClass().add(Styles.TEXT_SUBTLE);
		spaghettiWarning.getStyleClass().addAll(Styles.TEXT_BOLD, Styles.WARNING);
		spaghettiWarning.setWrapText(true);
		focusedGraphTitle.getStyleClass().add(Styles.TITLE_4);
		detailTitle.getStyleClass().addAll(Styles.TITLE_4);
		detailSubtitle.getStyleClass().add(Styles.TEXT_SUBTLE);
		graphEmptyLabel.getStyleClass().add(Styles.TEXT_SUBTLE);
		graphEmptyLabel.setWrapText(true);
		graphEmptyLabel.setTextAlignment(TextAlignment.CENTER);

		configureOverlays();
		configureToolbarControls();
		configureExplorer();
		configureClassList();
		configureGraphSurface();
		configureDetailsPanel();
		configureModelListeners();

		BorderPane content = new BorderPane();
		content.setTop(createToolbar());
		content.setCenter(new StackPane(createShell(), placeholderBox));

		getChildren().addAll(content, loadingOverlay);
		getStyleClass().add("background");
	}

	/**
	 * Selects a resource to analyze. Selecting the already active resource will re-trigger analysis.
	 *
	 * @param resource
	 * 		Resource to analyze.
	 */
	public void selectResource(@Nonnull WorkspaceResource resource) {
		if (!resourceSelector.getItems().contains(resource))
			resourceSelector.getItems().add(resource);
		resourceSelector.getSelectionModel().select(resource);
		if (selectedResource.get() == resource)
			analyze(resource);
	}

	/**
	 * Assigns a pre-computed analysis result to the pane, bypassing the normal analysis flow.
	 * Used for the {@link AreaAnalysisSummarizer} to <i>"immediately"</i> show the UI with a pre-computed result.
	 *
	 * @param resource
	 * 		Resource the result belongs to.
	 * @param result
	 * 		Result to display.
	 */
	public void setResult(@Nonnull WorkspaceResource resource, @Nonnull AreaAnalysisResult result) {
		runCounter.incrementAndGet();
		showLoading(false);
		hidePlaceholder();
		selectResourceWithoutAnalysis(resource);
		model.setResult(result);
		if (result.groups().isEmpty())
			showPlaceholder(Lang.get("service.analysis.areas.empty"), false);
	}

	/**
	 * Re-runs analysis for the currently selected resource.
	 */
	public void refreshAnalysis() {
		WorkspaceResource resource = selectedResource.get();
		if (resource != null)
			analyze(resource);
	}

	@Override
	public boolean isTrackable() {
		return false;
	}

	@Nullable
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
		analysisPool.shutdownNow();
		explorerList.getItems().clear();
		classList.getItems().clear();
		relationshipGraph.setContentModel(null);
	}

	@PreDestroy
	private void destroy() {
		analysisPool.shutdownNow();
	}

	/**
	 * Executes area analysis asynchronously for the given resource.
	 *
	 * @param resource
	 * 		Resource to analyze.
	 */
	private void analyze(@Nonnull WorkspaceResource resource) {
		int runId = runCounter.incrementAndGet();
		showLoading(true);
		hidePlaceholder();

		CompletableFuture.supplyAsync(() -> areaAnalysisService.analyze(workspace, resource), analysisPool)
				.whenCompleteAsync((result, error) -> {
					// Only apply results if this is the most recent run.
					if (runCounter.get() != runId)
						return;

					showLoading(false);
					if (error != null) {
						// Keep the pane in a consistent empty/error state instead of leaving
						// stale overview or graph content on screen after a failed recomputation.
						model.clear();
						statusLabel.setText(Lang.get("service.analysis.areas.error"));
						showPlaceholder(Lang.get("service.analysis.areas.error"), true);
						refreshPane();
						return;
					}

					model.setResult(result);
					if (result.groups().isEmpty())
						showPlaceholder(Lang.get("service.analysis.areas.empty"), false);
				}, FxThreadUtil.executor());
	}

	/**
	 * Configures loading and placeholder overlays that sit above display.
	 */
	private void configureOverlays() {
		progressIndicator.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
		progressBox.getChildren().setAll(progressIndicator, progressLabel);
		progressBox.setAlignment(Pos.CENTER);
		progressBox.setSpacing(18);

		loadingOverlay.getChildren().add(progressBox);
		loadingOverlay.getStyleClass().add("background-dark-transparent");
		loadingOverlay.setVisible(false);
		loadingOverlay.setManaged(false);
		StackPane.setAlignment(loadingOverlay, Pos.CENTER);

		placeholderBox.setAlignment(Pos.CENTER);
		placeholderBox.setSpacing(10);
		placeholderBox.getChildren().addAll(placeholderLabel);
		placeholderBox.setVisible(false);
		placeholderBox.setManaged(false);
		StackPane.setAlignment(placeholderBox, Pos.CENTER);
	}

	/**
	 * Configures the top toolbar discovery/filter controls.
	 */
	private void configureToolbarControls() {
		resourceSelector.setItems(FXCollections.observableArrayList(workspace.getAllResources(false)));
		resourceSelector.setCellFactory(ignored -> new ResourceCell());
		resourceSelector.setButtonCell(new ResourceCell());
		resourceSelector.setPrefWidth(260);
		resourceSelector.valueProperty().bindBidirectional(selectedResource);
		selectedResource.addListener((ob, old, cur) -> {
			if (!suppressSelectionAnalyze && cur != null && cur != old)
				analyze(cur);
		});

		searchField.setPromptText(Lang.get("service.analysis.areas.search.prompt"));
		searchField.setLeft(new FontIconView(CarbonIcons.SEARCH, 16));
		searchField.setPrefWidth(260);
		searchField.textProperty().addListener((ob, old, cur) -> applyDiscoveryControlsToModel());

		purposeSelector.setItems(FXCollections.observableArrayList(buildPurposeOptions()));
		purposeSelector.getSelectionModel().selectFirst();
		purposeSelector.setPrefWidth(190);
		purposeSelector.setCellFactory(ignored -> new PurposeOptionCell());
		purposeSelector.setButtonCell(new PurposeOptionCell());
		purposeSelector.valueProperty().addListener((ob, old, cur) -> applyDiscoveryControlsToModel());

		entryPointsFilter.setOnAction(event -> applyDiscoveryControlsToModel());
		largeGroupsFilter.setOnAction(event -> applyDiscoveryControlsToModel());
		highFanInFilter.setOnAction(event -> applyDiscoveryControlsToModel());
		highFanOutFilter.setOnAction(event -> applyDiscoveryControlsToModel());

		clearSelectionButton.textProperty().bind(Lang.getBinding("service.analysis.areas.overview"));
		clearSelectionButton.getStyleClass().addAll(Styles.BUTTON_OUTLINED);
		clearSelectionButton.setOnAction(event -> model.showOverview());
	}

	/**
	 * Configures the ranked explorer list shown in the left dock.
	 */
	private void configureExplorer() {
		explorerList.setItems(model.getExplorerGroups());
		explorerList.setPlaceholder(new BoundLabel(Lang.getBinding("service.analysis.areas.section.matching.empty")));
		explorerList.setCellFactory(ignored -> new ListCell<>() {
			@Override
			protected void updateItem(AreaGroup item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || item == null) {
					setText(null);
					setGraphic(null);
				} else {
					setGraphic(createGroupSummaryNode(item, true));
				}
			}
		});
		explorerList.getSelectionModel().selectedItemProperty().addListener((ob, old, cur) -> {
			// Avoid redundant selection churn when model and list are already in sync.
			if (cur != null && !Objects.equals(cur, model.getSelectedGroup()))
				model.selectGroup(cur);
		});
	}

	/**
	 * Configures the class list used by the dedicated classes dock.
	 */
	private void configureClassList() {
		classList.setItems(model.getDisplayedClasses());
		classList.setPlaceholder(new BoundLabel(Lang.getBinding("service.analysis.areas.none.classes")));
		classList.setCellFactory(ignored -> new ListCell<>() {
			@Override
			protected void updateItem(ClassPathNode item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || item == null) {
					setText(null);
					setGraphic(null);
				} else {
					setText(cellConfigurationService.textOf(item));
					setGraphic(cellConfigurationService.graphicOf(item));
				}
			}
		});
		classList.setOnMouseClicked(event -> {
			if (event.getClickCount() == 2) {
				ClassPathNode selected = classList.getSelectionModel().getSelectedItem();
				if (selected != null) {
					try {
						actions.gotoDeclaration(selected);
					} catch (IncompletePathException ex) {
						throw new IllegalStateException("Cannot navigate incomplete class path", ex);
					}
				}
			}
		});
	}

	/**
	 * Configures the graph canvas and its overlays.
	 */
	private void configureGraphSurface() {
		relationshipGraph.setMinHeight(420);
		relationshipGraph.setPrefHeight(420);
		relationshipGraph.getStyleClass().addAll("border-muted","background-inset");
		relationshipGraph.getRenderFactory().register(GRAPH_NODE_RENDERER_KEY, new AreaGraphNodeRenderer());
		relationshipGraph.getRenderFactory().register(GRAPH_EDGE_RENDERER_KEY, new AreaGraphEdgeRenderer());

		VBox graphEmptyBox = new VBox(new FontIconView(CarbonIcons.CHART_CUSTOM, 28), graphEmptyLabel);
		graphEmptyBox.setAlignment(Pos.CENTER);
		graphEmptyBox.setSpacing(12);
		graphEmptyBox.setPadding(new Insets(24));
		graphModeOverlay.getChildren().setAll(focusInboundToggle, focusBothToggle, focusOutboundToggle);
		graphModeOverlay.setPadding(new Insets(12));
		graphModeOverlayGroup.getChildren().setAll(graphModeOverlay);
		graphLegendOverlayGroup.getChildren().setAll(createGraphLegend());
		graphSurface.getChildren().setAll(relationshipGraph, graphEmptyBox, graphModeOverlayGroup, graphLegendOverlayGroup);
		graphSurface.setAlignment(Pos.CENTER);
		StackPane.setAlignment(graphEmptyBox, Pos.CENTER);
		StackPane.setAlignment(graphModeOverlayGroup, Pos.TOP_RIGHT);
		StackPane.setAlignment(graphLegendOverlayGroup, Pos.BOTTOM_RIGHT);
		StackPane.setMargin(graphLegendOverlayGroup, new Insets(12));

		focusBothToggle.setToggleGroup(focusModeGroup);
		focusInboundToggle.setToggleGroup(focusModeGroup);
		focusOutboundToggle.setToggleGroup(focusModeGroup);
		focusInboundToggle.getStyleClass().add(Styles.LEFT_PILL);
		focusBothToggle.getStyleClass().add(Styles.CENTER_PILL);
		focusOutboundToggle.getStyleClass().add(Styles.RIGHT_PILL);

		focusBothToggle.setSelected(true);
		focusModeGroup.selectedToggleProperty().addListener((ob, old, cur) -> {
			if (cur == null) {
				if (old != null)
					old.setSelected(true);
				return;
			}

			// The mode buttons behave like radios.
			// Clicking the active toggle should not leave the control in a "no mode selected" state.
			if (cur instanceof ToggleButton toggle && toggle.getUserData() instanceof AreaAnalysisPaneModel.AreaViewMode mode)
				model.setViewMode(mode);
		});

		overviewScroll.setFitToWidth(true);
		overviewScroll.setContent(overviewSections);
		overviewSections.setPadding(new Insets(12));
	}

	/**
	 * Configures the edge details subsection of the right-side details dock.
	 */
	private void configureDetailsPanel() {
		edgeDetailsBox.setSpacing(6);
		edgeDetailsBox.getChildren().setAll(
				sectionLabel("service.analysis.areas.edge.selected"),
				kvRow("service.analysis.areas.edge.source", edgeSourceValue),
				kvRow("service.analysis.areas.edge.target", edgeTargetValue),
				kvRow("service.analysis.areas.edge.weight", edgeWeightValue),
				kvRow("service.analysis.areas.edge.count", edgeCountValue)
		);
		edgeDetailsBox.setVisible(false);
		edgeDetailsBox.setManaged(false);
	}

	/**
	 * Wires model state changes to their UI projections.
	 * <p>
	 * The model is the source of truth for selection, scoped graph construction, and
	 * filtered overview content. The pane reacts by refreshing only the affected regions.
	 */
	private void configureModelListeners() {
		model.resultProperty().addListener((ob, old, cur) -> {
			statusLabel.setText(cur == null ? "" : AreaAnalysisSummarySupport.formatCounts(cur));
			updatePlaceholderForResult(cur);
			refreshPane();
		});
		model.selectedGroupProperty().addListener((ob, old, cur) -> {
			syncExplorerSelection();
			updateCenterContent();
			updateFocusedGraphTitle();
			updateDetails();
			updateGraph();
		});
		model.selectedEdgeProperty().addListener((ob, old, cur) -> {
			updateDetails();

			// Edge selection only changes visual emphasis and the detail panel.
			// Rebuilding the graph would reset the viewport (which is very annoying).
			refreshGraphVisualState();
		});
		model.scopedGraphProperty().addListener((ob, old, cur) -> {
			updateDetails();
			updateGraph();
		});
		model.viewModeProperty().addListener((ob, old, cur) -> {
			updateCenterContent();
			updateFocusModeControls();
			updateFocusedGraphTitle();
			updateDetails();
			updateGraph();
		});
		model.ungroupedSelectionProperty().addListener((ob, old, cur) -> {
			syncExplorerSelection();
			updateCenterContent();
			updateFocusedGraphTitle();
			updateDetails();
			updateGraph();
		});
	}

	/**
	 * @return Toolbar containing resource selection, discovery controls, and status text.
	 */
	@Nonnull
	private Node createToolbar() {
		FlowPane filters = new FlowPane(0, 6, entryPointsFilter, largeGroupsFilter, highFanInFilter, highFanOutFilter);

		entryPointsFilter.getStyleClass().add(Styles.LEFT_PILL);
		largeGroupsFilter.getStyleClass().add(Styles.CENTER_PILL);
		highFanInFilter.getStyleClass().add(Styles.CENTER_PILL);
		highFanOutFilter.getStyleClass().add(Styles.RIGHT_PILL);

		HBox toolbar = new HBox(10, resourceSelector, searchField, purposeSelector, filters, new Spacer(), statusLabel);
		toolbar.setPadding(new Insets(10));
		toolbar.setAlignment(Pos.CENTER_LEFT);
		toolbar.getStyleClass().add("background-dark");
		return toolbar;
	}

	/**
	 * Builds the docked three-region split (left/right sections are collapsible tool-tabs).
	 * <p>
	 * The center region is swapped between overview and focused graph modes when the selection
	 * model observes a value being assigned.
	 *
	 * @return Root dock region.
	 */
	@Nonnull
	private Node createShell() {
		DockBuilding builder = bento.dockBuilding();

		DockContainerLeaf leftLeaf = builder.leaf("area-analysis-explorer");
		leftLeaf.setCanSplit(false);
		leftLeaf.setSide(Side.LEFT);
		leftLeaf.addDockable(createDockable(Lang.get("service.analysis.areas.explorer"), CarbonIcons.LIST, createExplorerPanel()));

		Dockable centerDockable = builder.dockable();
		centerDockable.setNode(centerWrapper);
		centerDockable.setCanBeDragged(false);
		centerDockable.setClosable(false);
		centerDockable.setDragGroupMask(DockingManager.GROUP_NEVER_RECEIVE);
		centerWrapper.getStyleClass().add("border-muted");

		DockContainerLeaf centerLeaf = builder.leaf();
		centerLeaf.setSide(null);
		centerLeaf.addDockable(centerDockable);

		DockContainerLeaf rightLeaf = builder.leaf("area-analysis-details");
		rightLeaf.setCanSplit(false);
		rightLeaf.setSide(Side.RIGHT);
		rightLeaf.addDockable(createDockable(Lang.get("service.analysis.areas.details"), CarbonIcons.INFORMATION, createDetailsPanel()));
		rightLeaf.addDockable(createDockable(Lang.get("service.analysis.areas.classes"), CarbonIcons.LIST_BOXES, createClassesPanel()));

		DockContainerRootBranch root = builder.root();
		root.addContainer(leftLeaf);
		root.addContainer(centerLeaf);
		root.addContainer(rightLeaf);
		root.setContainerSizePx(leftLeaf, 310);
		root.setContainerSizePx(rightLeaf, 350);
		SplitPane.setResizableWithParent(leftLeaf.asRegion(), false);
		SplitPane.setResizableWithParent(rightLeaf.asRegion(), false);
		bento.registerRoot(root);

		updateCenterContent();
		refreshOverview();
		updateFocusModeControls();
		updateFocusedGraphTitle();
		updateDetails();
		updateGraph();

		root.setContainerCollapsed(leftLeaf, true);
		root.setContainerCollapsed(rightLeaf, true);

		return root.asRegion();
	}

	/**
	 * @param title
	 * 		Dock title.
	 * @param icon
	 * 		Dock icon.
	 * @param content
	 * 		Dock content.
	 *
	 * @return Configured dockable instance, that is non-draggable/closable.
	 */
	@Nonnull
	private Dockable createDockable(@Nonnull String title, @Nonnull Ikon icon, @Nonnull Node content) {
		Dockable dockable = bento.dockBuilding().dockable();
		dockable.setNode(content);
		dockable.setTitle(title);
		dockable.setIconFactory(ignored -> new FontIconView(icon));
		dockable.setCanBeDragged(false);
		dockable.setClosable(false);
		dockable.setDragGroupMask(DockingManager.GROUP_NEVER_RECEIVE);
		return dockable;
	}

	/**
	 * @return Explorer content shown in the left dock.
	 */
	@Nonnull
	private Node createExplorerPanel() {
		Label title = sectionLabel("service.analysis.areas.explorer.ranked");
		Label subtitle = new BoundLabel(Lang.getBinding("service.analysis.areas.explorer.filtered"));
		subtitle.getStyleClass().add(Styles.TEXT_SUBTLE);
		FlowPane quickActions = new FlowPane(8, 8, clearSelectionButton);
		VBox.setVgrow(explorerList, Priority.ALWAYS);
		VBox panel = new VBox(10, title, subtitle, quickActions, explorerList);
		panel.setPadding(new Insets(12));
		panel.getStyleClass().add("background-dark");
		return panel;
	}

	/**
	 * @return Center pane used when a focused graph is active.
	 */
	@Nonnull
	private Node createCenterGraphPane() {
		VBox box = new VBox(10, focusedGraphTitle, graphSurface);
		box.setPadding(new Insets(12));
		VBox.setVgrow(graphSurface, Priority.ALWAYS);
		return box;
	}

	/**
	 * @return Details content shown in the right-side details dock.
	 */
	@Nonnull
	private Node createDetailsPanel() {
		GridPane grid = new GridPane();
		grid.setHgap(8);
		grid.setVgap(6);
		addGridRow(grid, 0, "service.analysis.areas.summary.kind", detailKindValue);
		addGridRow(grid, 1, "service.analysis.areas.summary.purpose", detailPurposeValue);
		addGridRow(grid, 2, "service.analysis.areas.summary.confidence", detailConfidenceValue);
		addGridRow(grid, 3, "service.analysis.areas.summary.classes", detailClassCountValue);
		addGridRow(grid, 4, "service.analysis.areas.summary.entry-short", detailEntryValue);
		addGridRow(grid, 5, "service.analysis.areas.links.inbound", detailInboundValue);
		addGridRow(grid, 6, "service.analysis.areas.links.outbound", detailOutboundValue);
		addGridRow(grid, 7, "service.analysis.areas.summary.visible-inbound", detailVisibleInboundValue);
		addGridRow(grid, 8, "service.analysis.areas.summary.visible-outbound", detailVisibleOutboundValue);
		addGridRow(grid, 9, "service.analysis.areas.summary.hidden-inbound", detailHiddenInboundValue);
		addGridRow(grid, 10, "service.analysis.areas.summary.hidden-outbound", detailHiddenOutboundValue);
		addGridRow(grid, 11, "service.analysis.areas.summary.hidden-edges", detailHiddenEdgesValue);
		VBox panel = new VBox(10,
				detailTitle,
				detailSubtitle,
				grid,
				spaghettiWarning,
				edgeDetailsBox);
		panel.setPadding(new Insets(12));
		panel.getStyleClass().add("background-dark");
		VBox.setVgrow(edgeDetailsBox, Priority.NEVER);
		return panel;
	}

	/**
	 * @return Class list content shown in the right-side classes dock.
	 */
	@Nonnull
	private Node createClassesPanel() {
		Label title = sectionLabel("service.analysis.areas.classes");
		Label subtitle = new BoundLabel(Lang.getBinding("service.analysis.areas.classes.selected"));
		subtitle.getStyleClass().add(Styles.TEXT_SUBTLE);
		VBox panel = new VBox(10, title, subtitle, classList);
		panel.setPadding(new Insets(12));
		panel.getStyleClass().add("background-dark");
		VBox.setVgrow(classList, Priority.ALWAYS);
		return panel;
	}

	/**
	 * Refreshes all UI regions from the current model state.
	 */
	private void refreshPane() {
		refreshOverview();
		syncExplorerSelection();
		updateCenterContent();
		updateFocusModeControls();
		updateFocusedGraphTitle();
		updateDetails();
		updateGraph();
	}

	/**
	 * Rebuilds the overview section contents from the current analysis result and filters.
	 */
	private void refreshOverview() {
		overviewSections.getChildren().clear();
		AreaAnalysisResult result = model.getResult();
		if (result == null) {
			overviewSections.getChildren().add(new BoundLabel(Lang.getBinding("service.analysis.areas.result.none")));
			return;
		}

		overviewSections.getChildren().add(createOverviewSection(Lang.get("service.analysis.areas.overview.program"), createSummaryCardContent(result), true));
		if (model.hasActiveDiscoveryFilters())
			overviewSections.getChildren().add(createGroupSection(Lang.get("service.analysis.areas.section.matching"), List.copyOf(model.getExplorerGroups()), Lang.get("service.analysis.areas.section.matching.empty")));

		overviewSections.getChildren().add(createGroupSection(Lang.get("service.analysis.areas.section.largest"), model.getLargestGroups(), Lang.get("service.analysis.areas.section.largest.empty")));
		overviewSections.getChildren().add(createGroupSection(Lang.get("service.analysis.areas.section.entry-points"), model.getEntryPointGroups(), Lang.get("service.analysis.areas.section.entry-points.empty")));
		overviewSections.getChildren().add(createGroupSection(Lang.get("service.analysis.areas.section.high-fan-in"), model.getHighFanInGroups(), Lang.get("service.analysis.areas.section.high-fan-in.empty")));
		overviewSections.getChildren().add(createGroupSection(Lang.get("service.analysis.areas.section.high-fan-out"), model.getHighFanOutGroups(), Lang.get("service.analysis.areas.section.high-fan-out.empty")));
	}

	/**
	 * Creates the overview summary card shown at the top of the overview page.
	 *
	 * @param result
	 * 		Result to summarize.
	 *
	 * @return Summary card content.
	 */
	@Nonnull
	private Node createSummaryCardContent(@Nonnull AreaAnalysisResult result) {
		AreaAnalysisSummarySupport.DistributionSummary groupSizeSummary = AreaAnalysisSummarySupport.summarizeGroupSizes(result);
		AreaAnalysisSummarySupport.DistributionSummary linkCountSummary = AreaAnalysisSummarySupport.summarizeLinkCounts(result);
		AreaAnalysisSummarySupport.ConfidenceSummary confidenceSummary = AreaAnalysisSummarySupport.summarizeConfidence(result);
		AreaAnalysisSummarySupport.PurposeSummary purposeSummary = AreaAnalysisSummarySupport.summarizePurposes(result);

		GridPane facts = new GridPane();
		facts.setHgap(24);
		facts.setVgap(6);
		addMetricColumn(facts, 0,
				Lang.get("service.analysis.areas.metric.analyzed-classes"), String.valueOf(result.analyzedClassCount()),
				Lang.get("service.analysis.areas.metric.group-count"), String.valueOf(result.groupCount()),
				Lang.get("service.analysis.areas.metric.link-count"), String.valueOf(result.linkCount()));
		addMetricColumn(facts, 1,
				Lang.get("service.analysis.areas.metric.average-group-size"), AreaAnalysisSummarySupport.formatMetric(groupSizeSummary.average()),
				Lang.get("service.analysis.areas.metric.median-group-size"), AreaAnalysisSummarySupport.formatMetric(groupSizeSummary.median()),
				Lang.get("service.analysis.areas.metric.mode-group-size"), String.valueOf(groupSizeSummary.mode()));
		addMetricColumn(facts, 2,
				Lang.get("service.analysis.areas.metric.average-link-count"), AreaAnalysisSummarySupport.formatMetric(linkCountSummary.average()),
				Lang.get("service.analysis.areas.metric.median-link-count"), AreaAnalysisSummarySupport.formatMetric(linkCountSummary.median()),
				Lang.get("service.analysis.areas.metric.mode-link-count"), String.valueOf(linkCountSummary.mode()));
		addMetricColumn(facts, 3,
				Lang.get("service.analysis.areas.metric.average-confidence"), AreaAnalysisSummarySupport.formatConfidence(confidenceSummary.average()),
				Lang.get("service.analysis.areas.metric.median-confidence"), AreaAnalysisSummarySupport.formatConfidence(confidenceSummary.median()),
				Lang.get("service.analysis.areas.metric.low-confidence-groups"), String.valueOf(confidenceSummary.lowConfidenceCount()));
		addMetricColumn(facts, 4,
				Lang.get("service.analysis.areas.metric.top-purpose"), PackagePurpose.toString(purposeSummary.topPurpose()),
				Lang.get("service.analysis.areas.metric.second-purpose"), "None".equals(purposeSummary.secondPurpose()) ? Lang.get("misc.none") : PackagePurpose.toString(purposeSummary.secondPurpose()),
				Lang.get("service.analysis.areas.metric.distinct-purposes"), String.valueOf(purposeSummary.distinctPurposeCount()));

		VBox card = new VBox(10, facts);
		if (result.spaghettiDetected())
			card.getChildren().add(new BoundLabel(Lang.getBinding("service.analysis.areas.warning.dominant")));
		return card;
	}

	/**
	 * Creates an overview section of clickable group cards.
	 *
	 * @param title
	 * 		Section title.
	 * @param groups
	 * 		Groups to render.
	 * @param emptyText
	 * 		Fallback text when the section is empty.
	 *
	 * @return Collapsible overview section.
	 */
	@Nonnull
	private Node createGroupSection(@Nonnull String title, @Nonnull List<AreaGroup> groups, @Nonnull String emptyText) {
		VBox content = new VBox(8);
		if (groups.isEmpty()) {
			Label label = new Label(emptyText);
			label.getStyleClass().add(Styles.TEXT_SUBTLE);
			content.getChildren().add(label);
		} else {
			groups.forEach(group -> content.getChildren().add(createOverviewGroupButton(group)));
		}
		return createOverviewSection(title, content, false);
	}

	/**
	 * Creates a full-width button that selects the given group.
	 *
	 * @param group
	 * 		Group represented by the button.
	 *
	 * @return Group selection button.
	 */
	@Nonnull
	private Button createOverviewGroupButton(@Nonnull AreaGroup group) {
		Button button = new Button();
		button.setGraphic(createGroupSummaryNode(group, false));
		button.setMaxWidth(Double.MAX_VALUE);
		button.setAlignment(Pos.CENTER_LEFT);
		button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
		button.setOnAction(event -> model.selectGroup(group));
		button.getStyleClass().addAll("background-dark", "border-muted");
		return button;
	}

	/**
	 * Creates the shared group summary block used by overview cards and explorer rows.
	 *
	 * @param group
	 * 		Group to summarize.
	 * @param compact
	 * 		Whether to use tighter spacing for dense list rows.
	 *
	 * @return Summary node for the given group.
	 */
	@Nonnull
	private Node createGroupSummaryNode(@Nonnull AreaGroup group, boolean compact) {
		String purposeKey = group.purpose();
		Label title = new Label(String.format(Lang.get("service.analysis.areas.summary.group"), group.id()));
		Label purpose = new Label(PackagePurpose.toString(purposeKey));
		Label meta = new Label(String.format(Lang.get("service.analysis.areas.group.meta"),
				group.classes().size(),
				group.inboundLinkCount(),
				group.outboundLinkCount(),
				AreaAnalysisSummarySupport.formatConfidence(group.confidence())));
		Color purposeColor = colorForPurpose(purposeKey);
		Color purposeColorFaded = purposeColor.deriveColor(0, 0.7, 1, 1);
		title.getStyleClass().add(Styles.TEXT_BOLD);
		purpose.getStyleClass().add(Styles.TEXT_SUBTLE);
		title.setTextFill(purposeColorFaded);
		purpose.setTextFill(purposeColor);
		meta.getStyleClass().add(Styles.TEXT_SUBTLE);

		Label entry = new Label(group.containsEntryPoint() ? Lang.get("service.analysis.areas.entry.short") : "");
		entry.setVisible(group.containsEntryPoint());
		entry.setManaged(group.containsEntryPoint());
		entry.setTextFill(Color.WHITE);
		entry.setPadding(new Insets(2, 6, 2, 6));
		entry.setBackground(new Background(new BackgroundFill(Color.color(0.16, 0.58, 0.28), CornerRadii.EMPTY, Insets.EMPTY)));

		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);
		HBox top = new HBox(8, new FontIconView(iconForPurpose(purposeKey), 16, purposeColor), title, spacer, entry);
		top.setAlignment(Pos.CENTER_LEFT);

		VBox content = compact ? new VBox(4, top, purpose, meta) : new VBox(6, top, purpose, meta);
		content.setFillWidth(true);
		return content;
	}

	/**
	 * Swaps the center content between overview and focused-graph modes.
	 */
	private void updateCenterContent() {
		boolean graphActive = model.getSelectedGroup() != null && model.getViewMode().isGraphFocus();
		centerWrapper.setCenter(graphActive ? createCenterGraphPane() : overviewScroll);
	}

	/**
	 * Synchronizes the mode toggle buttons with current model state.
	 */
	private void updateFocusModeControls() {
		boolean hasGroup = model.getSelectedGroup() != null;
		focusBothToggle.setDisable(!hasGroup);
		focusInboundToggle.setDisable(!hasGroup);
		focusOutboundToggle.setDisable(!hasGroup);
		switch (model.getViewMode()) {
			case GROUP_FOCUS_INBOUND -> focusModeGroup.selectToggle(focusInboundToggle);
			case GROUP_FOCUS_OUTBOUND -> focusModeGroup.selectToggle(focusOutboundToggle);
			case GROUP_FOCUS_BOTH, OVERVIEW -> focusModeGroup.selectToggle(focusBothToggle);
		}
	}

	/**
	 * Updates the title shown above the graph surface.
	 */
	private void updateFocusedGraphTitle() {
		AreaGroup group = model.getSelectedGroup();
		if (group == null) {
			focusedGraphTitle.setText(Lang.get("service.analysis.areas.graph.focused"));
		} else {
			focusedGraphTitle.setText(String.format(Lang.get("service.analysis.areas.graph.focused.group"),
					String.format(Lang.get("service.analysis.areas.summary.group"), group.id())));
		}
	}

	/**
	 * Recomputes the right-side detail values from current group and edge selection.
	 */
	private void updateDetails() {
		AreaAnalysisResult result = model.getResult();
		AreaGroup group = model.getSelectedGroup();
		AreaAnalysisPaneModel.ScopedGraphResult scoped = model.getScopedGraphResult();
		spaghettiWarning.setVisible(result != null && result.spaghettiDetected());
		spaghettiWarning.setManaged(result != null && result.spaghettiDetected());

		if (group != null && result != null) {
			detailTitle.setText(String.format(Lang.get("service.analysis.areas.summary.group"), group.id()));
			detailSubtitle.setText(String.format(Lang.get("service.analysis.areas.details.focused-neighborhood"), modeLabel(model.getViewMode())));
			detailKindValue.setText(group.formationKind().name());
			detailPurposeValue.setText(group.purpose());
			detailConfidenceValue.setText(AreaAnalysisSummarySupport.formatConfidence(group.confidence()));
			detailClassCountValue.setText(String.valueOf(group.classes().size()));
			detailEntryValue.setText(Lang.get(group.containsEntryPoint() ? "misc.yes" : "misc.no"));
			detailInboundValue.setText(String.valueOf(group.inboundLinkCount()));
			detailOutboundValue.setText(String.valueOf(group.outboundLinkCount()));
			detailVisibleInboundValue.setText(String.valueOf(scoped.visibleInboundCount()));
			detailVisibleOutboundValue.setText(String.valueOf(scoped.visibleOutboundCount()));
			detailHiddenInboundValue.setText("+" + scoped.hiddenInboundNeighborCount());
			detailHiddenOutboundValue.setText("+" + scoped.hiddenOutboundNeighborCount());
			detailHiddenEdgesValue.setText("+" + scoped.hiddenEdgeCount());
		} else {
			detailTitle.setText(Lang.get("service.analysis.areas.selection.none"));
			detailSubtitle.setText(Lang.get("service.analysis.areas.details.use-overview"));
			detailKindValue.setText("");
			detailPurposeValue.setText("");
			detailConfidenceValue.setText("");
			detailClassCountValue.setText("");
			detailEntryValue.setText("");
			detailInboundValue.setText("");
			detailOutboundValue.setText("");
			detailVisibleInboundValue.setText("");
			detailVisibleOutboundValue.setText("");
			detailHiddenInboundValue.setText("");
			detailHiddenOutboundValue.setText("");
			detailHiddenEdgesValue.setText("");
		}

		AreaAnalysisPaneModel.EdgeSelection edge = model.getSelectedEdge();
		if (edge == null) {
			edgeDetailsBox.setVisible(false);
			edgeDetailsBox.setManaged(false);
			edgeSourceValue.setText("");
			edgeTargetValue.setText("");
			edgeWeightValue.setText("");
			edgeCountValue.setText("");
		} else {
			edgeDetailsBox.setVisible(true);
			edgeDetailsBox.setManaged(true);
			edgeSourceValue.setText(String.format(Lang.get("service.analysis.areas.summary.group"), edge.sourceGroup().id()));
			edgeTargetValue.setText(String.format(Lang.get("service.analysis.areas.summary.group"), edge.targetGroup().id()));
			edgeWeightValue.setText(String.valueOf(edge.link().weight()));
			edgeCountValue.setText(String.valueOf(edge.link().edgeCount()));
		}
	}

	/**
	 * Rebuilds the current graph view from the model's scoped graph result.
	 * @see #refreshGraphVisualState() Lightweight visual refreshes that preserve viewport state.
	 */
	private void updateGraph() {
		AreaAnalysisPaneModel.ScopedGraphResult scoped = model.getScopedGraphResult();
		Node emptyOverlay = graphSurface.getChildren().get(1);
		if (scoped.groups().isEmpty() || scoped.links().isEmpty()) {
			relationshipGraph.setContentModel(null);
			if (model.getSelectedGroup() == null) {
				graphEmptyLabel.setText(Lang.get("service.analysis.areas.graph.empty.select"));
			} else {
				graphEmptyLabel.setText(String.format(Lang.get("service.analysis.areas.graph.empty.no-links"),
						modeLabel(model.getViewMode()).toLowerCase()));
			}
			emptyOverlay.setVisible(true);
			return;
		}

		// The model already bounded and sorted the neighborhood.
		// Rendering simply mirrors that deterministic projection into the SpatialCanvas graph model.
		Graph graph = new Graph();
		for (AreaGroup group : scoped.groups()) {
			graph.addNode(new GraphNodeSpec<>(
					group.id(),
					GRAPH_NODE_RENDERER_KEY,
					group,
					GRAPH_NODE_WIDTH,
					GRAPH_NODE_HEIGHT
			));
		}
		for (AreaLink link : scoped.links()) {
			graph.addEdge(new GraphEdgeSpec<>(
					link.sourceGroupId() + "->" + link.targetGroupId(),
					link.sourceGroupId(),
					link.targetGroupId(),
					GRAPH_EDGE_RENDERER_KEY,
					link,
					EdgeArrowVisibility.TARGET,
					EdgeShape.ORTHOGONAL
			));
		}

		GraphContentModel contentModel = new GraphContentModel(graph, GRAPH_LAYOUT_ENGINE);
		relationshipGraph.setContentModel(contentModel);
		relationshipGraph.fitTo(contentModel.bounds(), GRAPH_PADDING);
		emptyOverlay.setVisible(false);
	}

	/**
	 * Requests a lightweight visual refresh of the current graph without replacing its
	 * content model or resetting the viewport.
	 */
	private void refreshGraphVisualState() {
		relationshipGraph.requestLayout();
		relationshipGraph.layout();
	}

	/**
	 * Applies toolbar search and filter controls to the backing model.
	 */
	private void applyDiscoveryControlsToModel() {
		model.setSearchQuery(searchField.getText());
		PurposeOption selectedPurpose = purposeSelector.getValue();
		model.setSelectedPurpose(selectedPurpose == null ? null : selectedPurpose.value());
		model.setEntryPointsOnly(entryPointsFilter.isSelected());
		model.setLargeGroupsOnly(largeGroupsFilter.isSelected());
		model.setHighFanInOnly(highFanInFilter.isSelected());
		model.setHighFanOutOnly(highFanOutFilter.isSelected());
		refreshOverview();
		syncExplorerSelection();
		updateCenterContent();
		updateFocusedGraphTitle();
		updateDetails();
		updateGraph();
	}

	/**
	 * Keeps the explorer list selection aligned with current model selection.
	 */
	private void syncExplorerSelection() {
		if (model.isUngroupedSelection() || model.getSelectedGroup() == null) {
			explorerList.getSelectionModel().clearSelection();
		} else if (!Objects.equals(explorerList.getSelectionModel().getSelectedItem(), model.getSelectedGroup())) {
			explorerList.getSelectionModel().select(model.getSelectedGroup());
			explorerList.scrollTo(model.getSelectedGroup());
		}
	}

	/**
	 * Updates the resource selector without triggering a redundant re-analysis.
	 *
	 * @param resource
	 * 		Resource to select.
	 */
	private void selectResourceWithoutAnalysis(@Nonnull WorkspaceResource resource) {
		if (!resourceSelector.getItems().contains(resource))
			resourceSelector.getItems().add(resource);

		suppressSelectionAnalyze = true;
		try {
			resourceSelector.getSelectionModel().select(resource);
		} finally {
			suppressSelectionAnalyze = false;
		}
	}

	/**
	 * Shows or hides the loading overlay.
	 *
	 * @param show
	 * 		Whether the loading overlay should be visible.
	 */
	private void showLoading(boolean show) {
		if (show) {
			progressIndicator.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
			loadingOverlay.setVisible(true);
			loadingOverlay.setManaged(true);
		} else {
			loadingOverlay.setVisible(false);
			loadingOverlay.setManaged(false);
			progressIndicator.setProgress(0);
		}
	}

	/**
	 * Shows the placeholder overlay.
	 *
	 * @param text
	 * 		Placeholder text.
	 * @param showRetry
	 * 		Unused retry flag kept for parity with prior pane behavior.
	 */
	private void showPlaceholder(@Nonnull String text, boolean showRetry) {
		placeholderLabel.setText(text);
		placeholderBox.setVisible(true);
		placeholderBox.setManaged(true);
	}

	/**
	 * Hides the placeholder overlay.
	 */
	private void hidePlaceholder() {
		placeholderBox.setVisible(false);
		placeholderBox.setManaged(false);
	}

	/**
	 * Updates placeholder state from the current result.
	 *
	 * @param result
	 * 		Result to inspect.
	 */
	private void updatePlaceholderForResult(@Nullable AreaAnalysisResult result) {
		if (result == null)
			return;
		if (result.groups().isEmpty())
			showPlaceholder(Lang.get("service.analysis.areas.empty"), false);
		else
			hidePlaceholder();
	}

	/**
	 * @param key
	 * 		Filter label key.
	 *
	 * @return Toolbar filter toggle.
	 */
	@Nonnull
	private static ToggleButton createFilterToggle(@Nonnull String key) {
		ToggleButton button = new ToggleButton();
		button.textProperty().bind(Lang.getBinding(key));
		return button;
	}

	/**
	 * @param key
	 * 		Button label key.
	 * @param icon
	 * 		Button icon.
	 * @param mode
	 * 		View mode represented by the button.
	 *
	 * @return Focus mode toggle button.
	 */
	@Nonnull
	private static ToggleButton createModeToggle(@Nonnull String key, @Nonnull Ikon icon, @Nonnull AreaAnalysisPaneModel.AreaViewMode mode) {
		ToggleButton button = new ToggleButton();
		button.textProperty().bind(Lang.getBinding(key));
		button.setGraphic(new FontIconView(icon));
		button.setUserData(mode);
		return button;
	}

	/**
	 * @param key
	 * 		Section title key.
	 *
	 * @return Styled section label.
	 */
	@Nonnull
	private static Label sectionLabel(@Nonnull String key) {
		Label label = new BoundLabel(Lang.getBinding(key));
		label.getStyleClass().add(Styles.TITLE_4);
		return label;
	}

	/**
	 * Creates a collapsible overview section.
	 *
	 * @param title
	 * 		Section title.
	 * @param content
	 * 		Section content.
	 * @param expanded
	 * 		Initial expanded state.
	 *
	 * @return Collapsible section node.
	 */
	@Nonnull
	private static Node createOverviewSection(@Nonnull String title, @Nonnull Node content, boolean expanded) {
		TitledPane pane = new TitledPane(title, content);
		pane.setExpanded(expanded);
		pane.setAnimated(false);
		pane.setCollapsible(true);
		return pane;
	}

	/**
	 * Creates a key/value row used in the details and edge summary panes.
	 *
	 * @param labelKey
	 * 		Row key.
	 * @param value
	 * 		Row value label.
	 *
	 * @return Key/value row node.
	 */
	@Nonnull
	private static Node kvRow(@Nonnull String labelKey, @Nonnull Label value) {
		value.getStyleClass().add(Styles.TEXT_SUBTLE);
		HBox row = new HBox(8, new Label(Lang.get(labelKey) + ":"), value);
		row.setAlignment(Pos.CENTER_LEFT);
		return row;
	}

	/**
	 * Adds a two-column key/value row to the given grid.
	 *
	 * @param grid
	 * 		Grid to update.
	 * @param row
	 * 		Target row.
	 * @param labelKey
	 * 		Key label.
	 * @param value
	 * 		Value label.
	 */
	private static void addGridRow(@Nonnull GridPane grid, int row, @Nonnull String labelKey, @Nonnull Label value) {
		Label key = new BoundLabel(Lang.getBinding(labelKey));
		key.getStyleClass().add(Styles.TEXT_BOLD);
		grid.add(key, 0, row);
		grid.add(value, 1, row);
	}

	/**
	 * Adds a vertical metric column composed of three key/value lines.
	 *
	 * @param grid
	 * 		Grid to update.
	 * @param column
	 * 		Target column.
	 * @param labelOne
	 * 		First metric label.
	 * @param valueOne
	 * 		First metric value.
	 * @param labelTwo
	 * 		Second metric label.
	 * @param valueTwo
	 * 		Second metric value.
	 * @param labelThree
	 * 		Third metric label.
	 * @param valueThree
	 * 		Third metric value.
	 */
	private static void addMetricColumn(@Nonnull GridPane grid,
	                                    int column,
	                                    @Nonnull String labelOne,
	                                    @Nonnull String valueOne,
	                                    @Nonnull String labelTwo,
	                                    @Nonnull String valueTwo,
	                                    @Nonnull String labelThree,
	                                    @Nonnull String valueThree) {
		VBox columnBox = new VBox(6,
				createMetricLine(labelOne, valueOne),
				createMetricLine(labelTwo, valueTwo),
				createMetricLine(labelThree, valueThree));
		grid.add(columnBox, column, 0);
	}

	/**
	 * Creates one line of summary-card metric content.
	 *
	 * @param label
	 * 		Metric label.
	 * @param value
	 * 		Metric value.
	 *
	 * @return Metric line node.
	 */
	@Nonnull
	private static Node createMetricLine(@Nonnull String label, @Nonnull String value) {
		Label key = new Label(label);
		key.getStyleClass().add(Styles.TEXT_BOLD);
		Label val = new Label(value);
		val.getStyleClass().add(Styles.TEXT_SUBTLE);
		return new VBox(2, key, val);
	}

	/**
	 * Creates the bottom-right graph legend overlay.
	 *
	 * @return Legend overlay node.
	 */
	@Nonnull
	private Node createGraphLegend() {
		VBox purposeItems = new VBox(6);
		for (String purpose : PackagePurpose.buckets())
			purposeItems.getChildren().add(createLegendPurposeRow(purpose));

		VBox edgeItems = new VBox(6,
				createLegendEdgeRow(Lang.get("service.analysis.areas.legend.edge.inbound"), inboundEdgeColor()),
				createLegendEdgeRow(Lang.get("service.analysis.areas.legend.edge.outbound"), outboundEdgeColor()),
				createLegendEdgeRow(Lang.get("service.analysis.areas.legend.edge.selected"), selectedEdgeColor()));

		VBox box = new VBox(10,
				createLegendSection(Lang.get("service.analysis.areas.legend.purpose"), purposeItems),
				createLegendSection(Lang.get("service.analysis.areas.legend.edge"), edgeItems));
		box.setPadding(new Insets(12));
		box.setMaxWidth(250);
		box.getStyleClass().addAll("border-muted", "background-dark-transparent");

		TitledPane pane = new TitledPane(Lang.get("service.analysis.areas.legend"), box);
		pane.setAnimated(false);

		// Initially collapsed to save screen real estate, since the legend is mostly for first-time reference
		// and not useful beyond that.
		pane.setExpanded(false);
		pane.setCollapsible(true);

		return pane;
	}

	/**
	 * Creates a titled legend subsection.
	 *
	 * @param title
	 * 		Subsection title.
	 * @param content
	 * 		Subsection content.
	 *
	 * @return Legend subsection node.
	 */
	@Nonnull
	private static Node createLegendSection(@Nonnull String title, @Nonnull Node content) {
		Label label = new Label(title);
		label.getStyleClass().add(Styles.TEXT_BOLD);
		return new VBox(6, label, content);
	}

	/**
	 * Creates one legend row for a purpose color/icon mapping.
	 *
	 * @param purpose
	 * 		Purpose bucket key.
	 *
	 * @return Legend row.
	 */
	@Nonnull
	private static Node createLegendPurposeRow(@Nonnull String purpose) {
		Rectangle swatch = new Rectangle(12, 12, colorForPurpose(purpose));
		swatch.setArcWidth(4);
		swatch.setArcHeight(4);
		Label label = new Label(PackagePurpose.toString(purpose));
		label.getStyleClass().add(Styles.TEXT_SUBTLE);
		return new HBox(8, swatch, new FontIconView(iconForPurpose(purpose), 14, colorForPurpose(purpose)), label);
	}

	/**
	 * Creates one legend row for an edge color mapping.
	 *
	 * @param labelText
	 * 		Legend label.
	 * @param color
	 * 		Edge color.
	 *
	 * @return Legend row.
	 */
	@Nonnull
	private static Node createLegendEdgeRow(@Nonnull String labelText, @Nonnull Color color) {
		Region line = new Region();
		line.setMinSize(18, 3);
		line.setPrefSize(18, 3);
		line.setMaxSize(18, 3);
		line.setBackground(new Background(new BackgroundFill(color, CornerRadii.EMPTY, Insets.EMPTY)));
		Label label = new Label(labelText);
		label.getStyleClass().add(Styles.TEXT_SUBTLE);
		HBox row = new HBox(8, line, label);
		row.setAlignment(Pos.CENTER_LEFT);
		return row;
	}

	/**
	 * @return Purpose selector options, including the synthetic "all purposes" choice.
	 */
	@Nonnull
	private static List<PurposeOption> buildPurposeOptions() {
		List<PurposeOption> options = new java.util.ArrayList<>();
		options.add(new PurposeOption(null, Lang.get("service.analysis.areas.purpose.all")));
		for (String purpose : PackagePurpose.buckets())
			options.add(new PurposeOption(purpose, PackagePurpose.toString(purpose)));
		return List.copyOf(options);
	}

	/**
	 * @param mode
	 * 		View mode to display.
	 *
	 * @return Human-readable label for the given mode.
	 */
	@Nonnull
	private static String modeLabel(@Nonnull AreaAnalysisPaneModel.AreaViewMode mode) {
		return switch (mode) {
			case GROUP_FOCUS_INBOUND -> Lang.get("service.analysis.areas.mode.inbound");
			case GROUP_FOCUS_OUTBOUND -> Lang.get("service.analysis.areas.mode.outbound");
			case GROUP_FOCUS_BOTH -> Lang.get("service.analysis.areas.mode.both");
			case OVERVIEW -> Lang.get("service.analysis.areas.overview");
		};
	}

	private final class ResourceCell extends ListCell<WorkspaceResource> {
		/**
		 * Updates resource selector cell content.
		 *
		 * @param item
		 * 		Resource item.
		 * @param empty
		 * 		Whether the cell is empty.
		 */
		@Override
		protected void updateItem(WorkspaceResource item, boolean empty) {
			super.updateItem(item, empty);
			if (empty || item == null) {
				setText(null);
				setGraphic(null);
			} else {
				setText(textProviderService.getResourceTextProvider(workspace, item).makeText());
				setGraphic(iconProviderService.getResourceIconProvider(workspace, item).makeIcon());
			}
		}
	}

	private static final class PurposeOptionCell extends ListCell<PurposeOption> {
		/**
		 * Updates purpose selector cell content.
		 *
		 * @param item
		 * 		Purpose option item.
		 * @param empty
		 * 		Whether the cell is empty.
		 */
		@Override
		protected void updateItem(PurposeOption item, boolean empty) {
			super.updateItem(item, empty);
			if (empty || item == null) {
				setText(null);
				setGraphic(null);
			} else {
				setText(item.label());
				Ikon icon = item.value() == null ? CarbonIcons.CATEGORIES : iconForPurpose(item.value());
				setGraphic(new FontIconView(icon, 16));
			}
		}
	}

	/**
	 * Renderer for group nodes within the scoped graph view.
	 */
	private final class AreaGraphNodeRenderer implements ContentRenderer<GraphNodeItem, AreaGraphNodeRenderer.NodeView> {
		private static final Color SELECTED_COLOR = Color.color(0.20, 0.84, 0.44);

		/**
		 * @return Reusable node view instance.
		 */
		@Nonnull
		@Override
		public NodeView createNode() {
			return new NodeView();
		}

		/**
		 * Projects a group payload into a styled fixed-size node card.
		 *
		 * @param context
		 * 		Render context.
		 * @param item
		 * 		Graph node item.
		 * @param node
		 * 		Reusable node view.
		 */
		@Override
		public void updateNode(@Nonnull RenderContext context, @Nonnull GraphNodeItem item, @Nonnull NodeView node) {
			node.resizeRelocate(item.bounds().x(), item.bounds().y(), item.bounds().width(), item.bounds().height());
			AreaGroup group = (AreaGroup) item.spec().payload();
			boolean selected = model.getSelectedGroup() != null && model.getSelectedGroup().id() == group.id();
			Color base = colorForPurpose(group.purpose());
			Color confidenceColor = confidenceColor(group.confidence());
			Color borderColor = selected ? SELECTED_COLOR : base;

			node.title.setText(String.format(Lang.get("service.analysis.areas.summary.group"), group.id()));
			node.purpose.setText(PackagePurpose.toString(group.purpose()));
			node.size.setText(String.format(Lang.get(group.classes().size() == 1 ?
					"service.analysis.areas.class-count.single" :
					"service.analysis.areas.class-count.multiple"), group.classes().size()));
			node.confidence.setText(AreaAnalysisSummarySupport.formatConfidence(group.confidence()));
			node.entryBadge.setVisible(group.containsEntryPoint());
			node.entryBadge.setManaged(group.containsEntryPoint());
			node.iconHolder.getChildren().setAll(new FontIconView(iconForPurpose(group.purpose()), 18, base.brighter()));
			node.confidence.setTextFill(confidenceColor);
			node.setBackground(new Background(new BackgroundFill(base.deriveColor(0, 1, 0.2, 1), CornerRadii.EMPTY, Insets.EMPTY)));
			node.setBorder(new Border(new BorderStroke(borderColor, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(selected ? 2.5 : 1.35))));
			node.setEffect(selected ? new DropShadow(18, borderColor.deriveColor(0, 1, 0.8, 1)) : null);

			// Consume drag/click events so canvas panning does not interfere with selection.
			node.setOnMousePressed(Event::consume);
			node.setOnMouseDragged(Event::consume);
			node.setOnMouseReleased(Event::consume);
			node.setOnMouseClicked(event -> {
				model.selectGroup(group);
				event.consume();
			});
		}

		/**
		 * Clears handlers and effects from a recycled node view.
		 *
		 * @param node
		 * 		Node view to reset.
		 */
		@Override
		public void resetNode(@Nonnull NodeView node) {
			node.setOnMousePressed(null);
			node.setOnMouseDragged(null);
			node.setOnMouseReleased(null);
			node.setOnMouseClicked(null);
			node.setEffect(null);
			node.setBorder(Border.EMPTY);
		}

		/**
		 * Fixed-size node card used by {@link AreaGraphNodeRenderer}.
		 */
		private final class NodeView extends StackPane {
			private final Label title = new Label();
			private final Label purpose = new Label();
			private final Label size = new Label();
			private final Label confidence = new Label();
			private final Label entryBadge = new Label(Lang.get("service.analysis.areas.entry.short"));
			private final StackPane iconHolder = new StackPane();

			private NodeView() {
				title.getStyleClass().add(Styles.TEXT_BOLD);
				purpose.getStyleClass().add(Styles.TEXT_SUBTLE);
				size.getStyleClass().add(Styles.TEXT_SUBTLE);
				confidence.getStyleClass().add(Styles.TEXT_SUBTLE);
				entryBadge.setTextFill(Color.WHITE);
				entryBadge.setPadding(new Insets(2, 6, 2, 6));
				entryBadge.setBackground(new Background(new BackgroundFill(Color.color(0.16, 0.58, 0.28), CornerRadii.EMPTY, Insets.EMPTY)));
				iconHolder.setMinSize(22, 22);
				iconHolder.setPrefSize(22, 22);
				Region spacer = new Region();
				HBox.setHgrow(spacer, Priority.ALWAYS);
				HBox header = new HBox(8, title, spacer, entryBadge, iconHolder);
				header.setAlignment(Pos.CENTER_LEFT);
				Region footerSpacer = new Region();
				HBox.setHgrow(footerSpacer, Priority.ALWAYS);
				HBox footer = new HBox(8, size, footerSpacer, confidence);
				footer.setAlignment(Pos.CENTER_LEFT);
				VBox content = new VBox(8, header, purpose, footer);
				content.setAlignment(Pos.CENTER_LEFT);
				getChildren().add(content);
				setManaged(false);
				setPadding(new Insets(12));
				setPrefSize(GRAPH_NODE_WIDTH, GRAPH_NODE_HEIGHT);
				setMinSize(GRAPH_NODE_WIDTH, GRAPH_NODE_HEIGHT);
				setMaxSize(GRAPH_NODE_WIDTH, GRAPH_NODE_HEIGHT);
			}
		}
	}

	/**
	 * Renderer for directed edges in the scoped graph view.
	 */
	private final class AreaGraphEdgeRenderer extends EdgeRenderer<GraphEdgeItem> {
		private static final double ARROW_LENGTH = 10.0;
		private static final double ARROW_WIDTH = 7.0;

		/**
		 * Updates the visible path, arrow, and click behavior of the given edge node.
		 *
		 * @param context
		 * 		Render context.
		 * @param item
		 * 		Graph edge item.
		 * @param node
		 * 		Reusable edge node.
		 */
		@Override
		public void updateNode(@Nonnull RenderContext context, @Nonnull GraphEdgeItem item, @Nonnull EdgeNode node) {
			// Tweak the arrow path so that it touches the edge of the node card rather than the center.
			// If the arrow heads were in the center of the node they wouldn't show up since they render behind nodes.
			List<Point2D> points = adjustedPoints(item);
			updatePath(node.getPath(), points);
			updateArrow(node.getSourceArrow(), points, true, false);
			updateArrow(node.getTargetArrow(), points, false, true);

			AreaLink link = (AreaLink) item.spec().payload();
			double strokeWidth = 1 + Math.min(10.0, Math.log1p(link.weight()) * 1.1);
			boolean selectedEdge = model.getSelectedEdge() != null && model.getSelectedEdge().link().equals(link);
			boolean inboundToSelected = model.getSelectedGroup() != null && link.targetGroupId() == model.getSelectedGroup().id();
			Color color = selectedEdge ? selectedEdgeColor() :
					inboundToSelected ? inboundEdgeColor() : outboundEdgeColor();
			node.getPath().setStroke(color);
			node.getPath().setStrokeWidth(strokeWidth);
			node.getPath().setOpacity(selectedEdge || model.getSelectedEdge() == null ? 0.95 : 0.40);
			node.getPath().setFill(null);
			node.getSourceArrow().setVisible(false);
			node.getTargetArrow().setScaleX(Math.max(2, strokeWidth) / 2);
			node.getTargetArrow().setScaleY(Math.max(2, strokeWidth) / 2);
			node.getTargetArrow().setFill(node.getPath().getStroke());
			node.getPath().setOnMouseClicked(event -> {
				AreaGroup source = model.findGroup(link.sourceGroupId());
				AreaGroup target = model.findGroup(link.targetGroupId());
				if (source != null && target != null)
					model.selectEdge(new AreaAnalysisPaneModel.EdgeSelection(source, target, link));
				event.consume();
			});
		}

		/**
		 * @param node
		 * 		Edge node to reset.
		 */
		@Override
		public void resetNode(@Nonnull EdgeNode node) {
			node.getPath().setOnMouseClicked(null);
		}

		/**
		 * Rebuilds the path from routed edge points.
		 *
		 * @param path
		 * 		Path to update.
		 * @param points
		 * 		Routed points.
		 */
		private static void updatePath(@Nonnull Path path, @Nonnull List<Point2D> points) {
			path.getElements().clear();
			Point2D start = points.getFirst();
			path.getElements().add(new MoveTo(start.getX(), start.getY()));
			for (int i = 1; i < points.size(); i++) {
				Point2D point = points.get(i);
				path.getElements().add(new LineTo(point.getX(), point.getY()));
			}
		}

		/**
		 * Updates the arrow polygon for the start or end of an edge.
		 *
		 * @param arrow
		 * 		Arrow polygon.
		 * @param points
		 * 		Routed points.
		 * @param sourceArrow
		 * 		Whether the arrow corresponds to the source side.
		 * @param visible
		 * 		Whether the arrow should be visible.
		 */
		private static void updateArrow(@Nonnull Polygon arrow, @Nonnull List<Point2D> points, boolean sourceArrow, boolean visible) {
			arrow.setVisible(visible);
			arrow.getPoints().clear();
			if (!visible)
				return;
			Point2D tip = sourceArrow ? points.getFirst() : points.getLast();
			Point2D prior = sourceArrow ? points.get(1) : points.get(points.size() - 2);
			Point2D direction = tip.subtract(prior);
			if (direction.magnitude() == 0) {
				arrow.setVisible(false);
				return;
			}
			Point2D unit = direction.normalize();
			Point2D baseCenter = tip.subtract(unit.multiply(ARROW_LENGTH));
			Point2D normal = new Point2D(-unit.getY(), unit.getX()).multiply(ARROW_WIDTH / 2.0);
			Point2D left = baseCenter.add(normal);
			Point2D right = baseCenter.subtract(normal);
			arrow.getPoints().setAll(tip.getX(), tip.getY(), left.getX(), left.getY(), right.getX(), right.getY());
		}

		/**
		 * Adjusts routed points so edges meet node borders instead of node centers.
		 *
		 * @param item
		 * 		Edge item to inspect.
		 *
		 * @return Adjusted routed points.
		 */
		@Nonnull
		private static List<Point2D> adjustedPoints(@Nonnull GraphEdgeItem item) {
			List<Point2D> points = item.points();
			if (points.size() < 2)
				return points;
			Point2D start = projectToBoundary(item.source().bounds(), points.getFirst(), points.get(1));
			Point2D end = projectToBoundary(item.target().bounds(), points.getLast(), points.get(points.size() - 2));
			if (points.size() == 2)
				return List.of(start, end);
			List<Point2D> adjusted = new java.util.ArrayList<>(points);
			adjusted.set(0, start);
			adjusted.set(adjusted.size() - 1, end);
			return List.copyOf(adjusted);
		}

		/**
		 * Projects a point from a node center to that node's visual boundary.
		 *
		 * @param bounds
		 * 		Node bounds.
		 * @param center
		 * 		Node center.
		 * @param toward
		 * 		Direction to project towards.
		 *
		 * @return Boundary point in the given direction.
		 */
		@Nonnull
		private static Point2D projectToBoundary(@Nonnull software.coley.spatialcanvas.BoundingBox bounds,
		                                         @Nonnull Point2D center,
		                                         @Nonnull Point2D toward) {
			double dx = toward.getX() - center.getX();
			double dy = toward.getY() - center.getY();
			if (dx == 0 && dy == 0)
				return center;
			double minT = Double.POSITIVE_INFINITY;
			if (dx > 0) minT = Math.min(minT, (bounds.maxX() - center.getX()) / dx);
			else if (dx < 0) minT = Math.min(minT, (bounds.x() - center.getX()) / dx);
			if (dy > 0) minT = Math.min(minT, (bounds.maxY() - center.getY()) / dy);
			else if (dy < 0) minT = Math.min(minT, (bounds.y() - center.getY()) / dy);
			if (!Double.isFinite(minT) || minT <= 0)
				return center;
			return new Point2D(center.getX() + dx * minT, center.getY() + dy * minT);
		}
	}

	/**
	 * @param confidence
	 * 		Confidence score in {@code [0,1]}.
	 *
	 * @return Color used to indicate confidence bands.
	 */
	@Nonnull
	private static Color confidenceColor(double confidence) {
		if (confidence >= 0.80)
			return Color.color(0.21, 0.72, 0.39);
		if (confidence >= 0.70)
			return Color.color(0.86, 0.66, 0.18);
		return Color.color(0.82, 0.35, 0.27);
	}

	/**
	 * @param purpose
	 * 		Purpose bucket key.
	 *
	 * @return Primary display color for the given purpose.
	 */
	@Nonnull
	private static Color colorForPurpose(@Nonnull String purpose) {
		return switch (purpose) {
			case "SECURITY" -> Color.color(0.3, 1.0, 0.6);
			case "NETWORKING" -> Color.color(1.0, 0.3, 1.0);
			case "UI" -> Color.color(0.2, 0.9, 1.0);
			case "IO" -> Color.color(0.0, 1.0, 0.0);
			case "BYTECODE" -> Color.color(0.6, 0.4, 1.0);
			case "REFLECTION" -> Color.color(1.0, 1.0, 0.0);
			case "NATIVE" -> Color.color(1.0, 0.0, 0.0);
			case "ENTERPRISE" -> Color.color(0.7, 0.8, 0.0);
			case "UTIL" -> Color.color(0.7, 0.8, 1.0);
			case "MISC" -> Color.color(1.0, 1.0, 1.0);
			default -> Color.color(0.6, 0.6, 0.6);
		};
	}

	/**
	 * @return Color used for inbound edges relative to the currently focused group.
	 */
	@Nonnull
	private static Color inboundEdgeColor() {
		return Color.color(0.24, 0.64, 0.95);
	}

	/**
	 * @return Color used for outbound edges relative to the currently focused group.
	 */
	@Nonnull
	private static Color outboundEdgeColor() {
		return Color.color(0.27, 0.82, 0.42);
	}

	/**
	 * @return Color used for the currently selected edge.
	 */
	@Nonnull
	private static Color selectedEdgeColor() {
		return Color.color(1.0, 0.67, 0.20);
	}

	/**
	 * @param purpose
	 * 		Purpose bucket key.
	 *
	 * @return Icon representing the given purpose bucket.
	 */
	@Nonnull
	private static Ikon iconForPurpose(@Nonnull String purpose) {
		return switch (purpose) {
			case PackagePurpose.BUCKET_SECURITY -> CarbonIcons.LOCKED;
			case PackagePurpose.BUCKET_NETWORKING -> CarbonIcons.WEATHER_STATION;
			case PackagePurpose.BUCKET_UI -> CarbonIcons.SCREEN;
			case PackagePurpose.BUCKET_IO -> CarbonIcons.FOLDER;
			case PackagePurpose.BUCKET_BYTECODE -> CarbonIcons.CODE;
			case PackagePurpose.BUCKET_REFLECTION -> CarbonIcons.MAGNIFY;
			case PackagePurpose.BUCKET_NATIVE -> CarbonIcons.LAPTOP;
			case PackagePurpose.BUCKET_ENTERPRISE -> CarbonIcons.WORKSPACE;
			case PackagePurpose.BUCKET_UTIL -> CarbonIcons.CATEGORIES;
			default -> CarbonIcons.UNKNOWN_FILLED;
		};
	}

	/**
	 * Purpose selector option pairing an internal purpose value with display text.
	 *
	 * @param value
	 * 		Purpose bucket key, or {@code null} for the synthetic "all purposes" option.
	 * @param label
	 * 		User-facing label.
	 */
	private record PurposeOption(@Nullable String value, @Nonnull String label) {}
}
