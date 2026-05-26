package software.coley.recaf.ui.pane.mapping;

import atlantafx.base.controls.ModalPane;
import atlantafx.base.controls.Popover;
import atlantafx.base.controls.RingProgressIndicator;
import atlantafx.base.theme.Styles;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.AndroidClassInfo;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.path.PathNodes;
import software.coley.recaf.path.ResourcePathNode;
import software.coley.recaf.services.cell.CellConfigurationService;
import software.coley.recaf.services.cell.context.ContextSource;
import software.coley.recaf.services.decompile.DecompileResult;
import software.coley.recaf.services.decompile.DecompilerManager;
import software.coley.recaf.services.info.association.FileTypeSyntaxAssociationService;
import software.coley.recaf.services.mapping.MappingApplier;
import software.coley.recaf.services.mapping.MappingApplierService;
import software.coley.recaf.services.mapping.MappingResults;
import software.coley.recaf.services.mapping.matching.SimilarityClassMatch;
import software.coley.recaf.services.mapping.matching.SimilarityMappingOptions;
import software.coley.recaf.services.mapping.matching.SimilarityMappingService;
import software.coley.recaf.services.mapping.matching.SimilarityMappingsReport;
import software.coley.recaf.services.navigation.Actions;
import software.coley.recaf.services.search.CancellableSearchFeedback;
import software.coley.recaf.services.workspace.WorkspaceManager;
import software.coley.recaf.ui.config.WorkspaceExplorerConfig;
import software.coley.recaf.ui.control.ActionButton;
import software.coley.recaf.ui.control.BoundIntSpinner;
import software.coley.recaf.ui.control.BoundLabel;
import software.coley.recaf.ui.control.PathNodeTree;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.ui.control.richtext.bracket.SelectedBracketTracking;
import software.coley.recaf.ui.control.richtext.search.SearchBar;
import software.coley.recaf.ui.control.tree.TreeItems;
import software.coley.recaf.ui.control.tree.WorkspaceRootTreeNode;
import software.coley.recaf.ui.control.tree.WorkspaceTreeCell;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.Lang;
import software.coley.recaf.util.StringUtil;
import software.coley.recaf.util.threading.ThreadUtil;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.FileBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Pane for similarity-based resource mapping preview and application.
 *
 * @author Matt Coley
 */
@Dependent
public class SimilarityMappingPane extends StackPane {
	private static final Logger logger = Logging.get(SimilarityMappingPane.class);

	private final CellConfigurationService configurationService;
	private final SimilarityMappingService similarityMappingService;
	private final MappingApplierService mappingApplierService;
	private final DecompilerManager decompilerManager;
	private final FileTypeSyntaxAssociationService languageAssociation;
	private final Instance<SearchBar> searchBarProvider;
	private final Workspace workspace;
	private final WorkspaceResource primaryResource;
	private final BorderPane contentPane = new BorderPane();
	private final ModalPane modal = new ModalPane();
	private final RingProgressIndicator progressIndicator = new RingProgressIndicator(ProgressIndicator.INDETERMINATE_PROGRESS);
	private final Label progressLabel = new Label();
	private final VBox progressBox = new VBox(progressIndicator, progressLabel);

	private final ObjectProperty<WorkspaceResource> targetResource = new SimpleObjectProperty<>();
	private final IntegerProperty classThreshold = new SimpleIntegerProperty(95);
	private final IntegerProperty certaintyGapThreshold = new SimpleIntegerProperty(1);
	private final IntegerProperty memberThreshold = new SimpleIntegerProperty(95);
	private final IntegerProperty maxFullScoreCandidates = new SimpleIntegerProperty(25);
	private final IntegerProperty shortlistGapThresholdPercent = new SimpleIntegerProperty(5);
	private final ObjectProperty<SimilarityMappingsReport> report = new SimpleObjectProperty<>();
	private final BooleanProperty generating = new SimpleBooleanProperty();
	private final BooleanProperty applying = new SimpleBooleanProperty();
	private final PathNodeTree classTree;
	private final PreviewPane sourcePreview;
	private final PreviewPane targetPreview;
	private ActionButton optionsButton;
	private Popover optionsPopover;
	private Runnable applyCallback;
	private CancellableSearchFeedback activeFeedback;

	@Inject
	public SimilarityMappingPane(@Nonnull WorkspaceManager workspaceManager,
	                             @Nonnull CellConfigurationService configurationService,
	                             @Nonnull SimilarityMappingService similarityMappingService,
	                             @Nonnull MappingApplierService mappingApplierService,
	                             @Nonnull DecompilerManager decompilerManager,
	                             @Nonnull FileTypeSyntaxAssociationService languageAssociation,
	                             @Nonnull Instance<SearchBar> searchBarProvider,
	                             @Nonnull WorkspaceExplorerConfig explorerConfig,
	                             @Nonnull Actions actions) {
		this.configurationService = configurationService;
		this.similarityMappingService = similarityMappingService;
		this.mappingApplierService = mappingApplierService;
		this.decompilerManager = decompilerManager;
		this.languageAssociation = languageAssociation;
		this.searchBarProvider = searchBarProvider;
		this.workspace = workspaceManager.getCurrent();
		this.primaryResource = workspaceManager.hasCurrentWorkspace() ? workspace.getPrimaryResource() : null;

		classTree = createTree(actions);
		sourcePreview = new PreviewPane("mapsim.preview.source");
		targetPreview = new PreviewPane("mapsim.preview.target");

		progressLabel.textProperty().bind(Bindings.createStringBinding(() ->
						applying.get() ? Lang.get("mapsim.progress.applying") : Lang.get("mapsim.progress.generating"),
				applying, generating));
		progressBox.setAlignment(Pos.CENTER);
		progressBox.setSpacing(20);

		contentPane.setTop(createToolbar());
		contentPane.setCenter(createContent());
		getChildren().addAll(contentPane, modal);

		generating.addListener((ob, old, cur) -> updateProgressOverlay());
		applying.addListener((ob, old, cur) -> updateProgressOverlay());

		// If there is no primary resource, then there is nothing to map from.
		// Disable the UI and show a message in the preview panes indicating that no selection has been made.
		if (primaryResource == null) {
			setDisable(true);
			sourcePreview.setPlaceholder("mapsim.preview.noselection");
			targetPreview.setPlaceholder("mapsim.preview.noselection");
			return;
		}

		// Default to the first non-primary resource as the target if any exist.
		List<WorkspaceResource> targetResources = workspace.getAllResources(false).stream()
				.filter(resource -> resource != primaryResource)
				.filter(resource -> !resource.isInternal())
				.toList();
		if (!targetResources.isEmpty())
			targetResource.set(targetResources.getFirst());

		// Build tree with only the primary resource and its embedded resources visible, since those are the only ones relevant to mapping.
		WorkspaceRootTreeNode root = new WorkspaceRootTreeNode(explorerConfig, PathNodes.workspacePath(workspace)) {
			@Override
			protected boolean shouldIncludeResource(@Nonnull ResourcePathNode resourcePath, @Nonnull WorkspaceResource resource) {
				return isPrimaryResourceBranch(resource);
			}

			@Override
			protected boolean shouldIncludeFiles(@Nonnull ResourcePathNode containingResourcePath, @Nonnull FileBundle bundle) {
				return false;
			}
		};
		root.build();
		TreeItems.recurseOpen(root);
		classTree.setRoot(root);

		// When clicking an item in the tree show the matched classes as decompiled code.
		classTree.getSelectionModel().selectedItemProperty().addListener((ob, old, cur) -> updatePreviews(cur));

		// When a report is done refresh the tree to show mapping status of classes and update the previews if necessary.
		report.addListener((ob, old, cur) -> {
			classTree.refresh();
			updatePreviews(classTree.getSelectionModel().getSelectedItem());
		});
		sourcePreview.setPlaceholder("mapsim.preview.noselection");
		targetPreview.setPlaceholder("mapsim.preview.noselection");
	}

	@PreDestroy
	private void destroy() {
		// Kill search when closing.
		cancelActiveAnalysis();
		sourcePreview.close();
		targetPreview.close();
	}

	/**
	 * @param applyCallback
	 * 		Callback to run after mappings are applied.
	 */
	public void setApplyCallback(@Nullable Runnable applyCallback) {
		this.applyCallback = applyCallback;
	}

	@Nonnull
	private Node createToolbar() {
		ComboBox<WorkspaceResource> targetSelector = new ComboBox<>();
		targetSelector.setItems(FXCollections.observableArrayList(
				workspace == null ? List.of() : workspace.getAllResources(false).stream()
						.filter(resource -> resource != primaryResource)
						.filter(resource -> !resource.isInternal())
						.toList()));
		targetSelector.valueProperty().bindBidirectional(targetResource);
		targetSelector.setCellFactory(param -> new ResourceCell());
		targetSelector.setButtonCell(new ResourceCell());
		targetSelector.setPlaceholder(new BoundLabel(Lang.getBinding("mapsim.target.none")));
		targetSelector.setPrefWidth(260);

		Button generateButton = new ActionButton(CarbonIcons.SEARCH, Lang.getBinding("mapsim.generate"), this::generate);
		Button applyButton = new ActionButton(CarbonIcons.CAFE, Lang.getBinding("mapsim.apply"), this::apply);
		optionsButton = new ActionButton(CarbonIcons.SETTINGS, this::showOptionsPopover);
		optionsButton.withTooltip("dialog.search.options");
		optionsButton.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT);

		BooleanBinding hasTarget = targetResource.isNotNull();
		generateButton.disableProperty().bind(hasTarget.not().or(generating).or(applying));
		BooleanBinding hasNoMappings = Bindings.createBooleanBinding(() -> {
			SimilarityMappingsReport currentReport = report.get();
			return currentReport == null || currentReport.getMappings().isEmpty();
		}, report);
		applyButton.disableProperty().bind(report.isNull()
				.or(hasNoMappings)
				.or(generating)
				.or(applying));

		Label summary = new Label();
		summary.getStyleClass().add(Styles.TEXT_SUBTLE);
		summary.textProperty().bind(Bindings.createStringBinding(() -> {
			if (workspace == null || primaryResource == null)
				return Lang.get("mapsim.summary.empty");
			SimilarityMappingsReport currentReport = report.get();
			if (currentReport == null)
				return Lang.get("mapsim.summary.empty");
			int total = workspace.findClasses(false, cls -> true).stream()
					.filter(path -> path.getValueOfType(WorkspaceResource.class) != null)
					.map(path -> path.getValueOfType(WorkspaceResource.class))
					.filter(Objects::nonNull)
					.filter(this::isPrimaryResourceBranch)
					.toList()
					.size();
			return String.format(Lang.get("mapsim.summary.result"), currentReport.getAcceptedMatches().size(), total);
		}, report));

		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);

		HBox toolbar = new HBox(10,
				new BoundLabel(Lang.getBinding("mapsim.target")), targetSelector,
				spacer,
				summary,
				optionsButton,
				generateButton,
				applyButton);
		toolbar.setAlignment(Pos.CENTER_LEFT);
		toolbar.setPadding(new Insets(10));
		toolbar.getStyleClass().add("config-toolbar");
		return toolbar;
	}

	private void showOptionsPopover() {
		if (optionsPopover == null) {
			optionsPopover = new Popover(createOptionsContent());
			optionsPopover.setArrowLocation(Popover.ArrowLocation.TOP_RIGHT);
		}
		optionsPopover.show(optionsButton);
	}

	@Nonnull
	private GridPane createOptionsContent() {
		BoundIntSpinner classThresholdSpinner = new BoundIntSpinner(classThreshold, 0, 100)
				.withTooltip("mapsim.threshold.class.tooltip");
		BoundIntSpinner gapThresholdSpinner = new BoundIntSpinner(certaintyGapThreshold, 0, 100)
				.withTooltip("mapsim.threshold.gap.tooltip");
		BoundIntSpinner memberThresholdSpinner = new BoundIntSpinner(memberThreshold, 0, 100)
				.withTooltip("mapsim.threshold.member.tooltip");
		BoundIntSpinner maxFullScoreCandidatesSpinner = new BoundIntSpinner(maxFullScoreCandidates, 1, Integer.MAX_VALUE)
				.withTooltip("mapsim.threshold.shortlist.max-candidates.tooltip");
		BoundIntSpinner shortlistGapThresholdSpinner = new BoundIntSpinner(shortlistGapThresholdPercent, 0, 100)
				.withTooltip("mapsim.threshold.shortlist.gap.tooltip");
		classThresholdSpinner.setPrefWidth(110);
		gapThresholdSpinner.setPrefWidth(110);
		memberThresholdSpinner.setPrefWidth(110);
		maxFullScoreCandidatesSpinner.setPrefWidth(110);
		shortlistGapThresholdSpinner.setPrefWidth(110);

		GridPane content = new GridPane();
		ColumnConstraints labelColumn = new ColumnConstraints();
		ColumnConstraints controlColumn = new ColumnConstraints();
		controlColumn.setFillWidth(true);
		controlColumn.setHgrow(Priority.ALWAYS);
		controlColumn.setHalignment(HPos.RIGHT);
		content.getColumnConstraints().addAll(labelColumn, controlColumn);
		content.setHgap(10);
		content.setVgap(8);

		content.add(new BoundLabel(Lang.getBinding("mapsim.threshold.class")), 0, 0);
		content.add(classThresholdSpinner, 1, 0);
		content.add(new BoundLabel(Lang.getBinding("mapsim.threshold.gap")), 0, 1);
		content.add(gapThresholdSpinner, 1, 1);
		content.add(new BoundLabel(Lang.getBinding("mapsim.threshold.member")), 0, 2);
		content.add(memberThresholdSpinner, 1, 2);
		content.add(new BoundLabel(Lang.getBinding("mapsim.threshold.shortlist.max-candidates")), 0, 3);
		content.add(maxFullScoreCandidatesSpinner, 1, 3);
		content.add(new BoundLabel(Lang.getBinding("mapsim.threshold.shortlist.gap")), 0, 4);
		content.add(shortlistGapThresholdSpinner, 1, 4);
		return content;
	}

	@Nonnull
	private Node createContent() {
		BorderPane treePane = new BorderPane(classTree);
		treePane.setPadding(new Insets(8));
		treePane.setPrefWidth(360);

		SplitPane previewSplit = new SplitPane(sourcePreview, targetPreview);
		previewSplit.setDividerPositions(0.5);
		SplitPane content = new SplitPane(treePane, previewSplit);
		content.setDividerPositions(0.35);
		SplitPane.setResizableWithParent(treePane, false);
		return content;
	}

	@Nonnull
	private PathNodeTree createTree(@Nonnull Actions actions) {
		PathNodeTree tree = new PathNodeTree(configurationService, actions);
		tree.setCellFactory(param -> new WorkspaceTreeCell(ContextSource.REFERENCE, configurationService) {
			@Override
			protected void populate(@Nonnull PathNode<?> path) {
				// Configure cell based on path type and mapping status.
				configurationService.configureStyle(this, path);
				setGraphic(configurationService.graphicOf(path));
				setContextMenu(createContextMenu(path));
				setOnMouseClicked(e -> configurationService.clickHandlerOf(this, path, false).handle(e));

				// For class paths show the matched class and mapping status in the text.
				if (path instanceof ClassPathNode classPath) {
					SimilarityClassMatch match = report.get() == null ? null : report.get().getAcceptedMatch(classPath);
					String sourceText = configurationService.textOf(classPath);
					String targetText = match == null ? Lang.get("mapsim.tree.unmapped") : configurationService.textOf(match.targetPath());
					setText(sourceText + " -> " + targetText);
				} else {
					setText(configurationService.textOf(path));
				}
			}
		});
		return tree;
	}

	@Nullable
	private ContextMenu createContextMenu(@Nonnull PathNode<?> path) {
		return configurationService.contextMenuOf(ContextSource.REFERENCE, path);
	}

	private void updatePreviews(@Nullable TreeItem<PathNode<?>> selectedItem) {
		// If no class is selected or the selected item is not a class, show placeholders.
		if (selectedItem == null || !(selectedItem.getValue() instanceof ClassPathNode classPath)) {
			sourcePreview.setPlaceholder("mapsim.preview.noselection");
			targetPreview.setPlaceholder("mapsim.preview.noselection");
			return;
		}

		// Show decompiled source for the selected class and its matched class if any.
		sourcePreview.showClass(workspace, classPath.getValue());
		SimilarityMappingsReport currentReport = report.get();
		SimilarityClassMatch match = currentReport == null ? null : currentReport.getAcceptedMatch(classPath);
		if (match == null)
			targetPreview.setPlaceholder("mapsim.preview.unmapped");
		else
			targetPreview.showClass(workspace, match.targetPath().getValue());
	}

	/**
	 * Generate similarity mappings with the current settings and selected target resource.
	 */
	private void generate() {
		if (workspace == null || primaryResource == null || targetResource.get() == null)
			return;

		// Kill any old analysis if any are running.
		cancelActiveAnalysis();

		// Setup feedback for future cancellation and mark as generating before starting async work.
		CancellableSearchFeedback feedback = new CancellableSearchFeedback();
		activeFeedback = feedback;
		generating.set(true);

		// Generate the mappings, and when done clear the active feedback and generating state before setting the report.
		SimilarityMappingOptions options = new SimilarityMappingOptions(
				classThreshold.get(),
				certaintyGapThreshold.get(),
				memberThreshold.get(),
				maxFullScoreCandidates.get(),
				shortlistGapThresholdPercent.get());
		CompletableFuture.supplyAsync(() ->
								similarityMappingService.analyze(workspace, primaryResource, targetResource.get(), options, feedback),
						ThreadUtil.executor())
				.whenCompleteAsync((result, error) -> {
					if (activeFeedback != feedback)
						return;
					activeFeedback = null;
					generating.set(false);
					if (error != null) {
						logger.error("Failed generating similarity mappings", error);
						return;
					}
					report.set(result);
				}, FxThreadUtil.executor());
	}

	/**
	 * Applies the currently generated mappings to the workspace.
	 */
	private void apply() {
		if (workspace == null)
			return;

		// Skip if there are no mappings to apply.
		SimilarityMappingsReport currentReport = report.get();
		if (currentReport == null || currentReport.getMappings().isEmpty())
			return;

		// Skip if no mapping applier is available in the current workspace.
		MappingApplier applier = mappingApplierService.inCurrentWorkspace();
		if (applier == null)
			return;

		// Mark as applying before starting.
		applying.set(true);

		// Apply mappings then unset the applying state when done.
		CompletableFuture.supplyAsync(() -> {
			MappingResults results = applier.applyToPrimaryResource(currentReport.getMappings());
			results.apply();
			return results;
		}, ThreadUtil.executor()).whenCompleteAsync((result, error) -> {
			applying.set(false);
			if (error != null) {
				logger.error("Failed applying similarity mappings", error);
				return;
			}
			if (applyCallback != null)
				applyCallback.run();
		}, FxThreadUtil.executor());
	}

	/**
	 * Cancels any active similarity analysis if one is running.
	 */
	private void cancelActiveAnalysis() {
		if (activeFeedback != null) {
			activeFeedback.cancel();
			activeFeedback = null;
		}
	}

	/**
	 * Updates the visibility of the progress overlay based on the current generation/applying state.
	 */
	private void updateProgressOverlay() {
		boolean busy = generating.get() || applying.get();
		if (busy) {
			progressIndicator.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
			FxThreadUtil.delayedRun(1, () -> {
				if (generating.get() || applying.get())
					modal.show(new Group(progressBox));
			});
		} else {
			modal.hide(true);
			progressIndicator.setProgress(0);
		}
	}

	/**
	 * @param resource
	 * 		Resource to check.
	 *
	 * @return {@code true} when the resource is the primary resource or an embedded resource within the primary resource.
	 */
	private boolean isPrimaryResourceBranch(@Nonnull WorkspaceResource resource) {
		WorkspaceResource current = resource;
		while (current != null) {
			if (current == primaryResource)
				return true;
			current = current.getContainingResource();
		}
		return false;
	}

	/**
	 * Cell for displaying workspace resources in the target selector.
	 */
	private class ResourceCell extends ListCell<WorkspaceResource> {
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
	}

	/**
	 * Pane for previewing decompiled source of a class.
	 */
	private class PreviewPane extends BorderPane {
		private final Editor editor = new Editor();
		private final AtomicInteger generation = new AtomicInteger();

		private PreviewPane(@Nonnull String titleKey) {
			Label title = new Label();
			title.textProperty().bind(Lang.getBinding(titleKey));
			title.getStyleClass().add(Styles.TITLE_4);
			title.setPadding(new Insets(0, 0, 6, 0));

			// Setup editor
			editor.setSelectedBracketTracking(new SelectedBracketTracking());
			editor.getRootLineGraphicFactory().addDefaultCodeGraphicFactories();
			editor.getCodeArea().setEditable(false);
			languageAssociation.configureEditorSyntax("java", editor);
			searchBarProvider.get().install(editor);

			setTop(title);
			setCenter(editor);
			setPadding(new Insets(8));
		}

		private void showClass(@Nonnull Workspace workspace, @Nonnull ClassInfo classInfo) {
			JvmClassInfo targetClass;
			if (classInfo.isJvmClass()) {
				targetClass = classInfo.asJvmClass();
			} else if (classInfo.isAndroidClass()) {
				AndroidClassInfo androidClass = classInfo.asAndroidClass();
				if (!androidClass.canMapToJvmClass()) {
					setText("// " + classInfo.getName() + "\n// Android class cannot be previewed as JVM source");
					return;
				}
				targetClass = androidClass.asJvmClass();
			} else {
				setText("// Unsupported class type");
				return;
			}

			// Track decompile generation to avoid displaying results from old decompilations after a new one has started.
			int currentGeneration = generation.incrementAndGet();
			setText("// " + classInfo.getName() + "\n// " + Lang.get("java.decompiling"));
			decompilerManager.decompile(workspace, targetClass).whenCompleteAsync((result, error) -> {
				if (currentGeneration != generation.get())
					return;
				if (error != null) {
					setText("/*\nDecompilation failure\n" + StringUtil.traceToString(error) + "\n*/");
					return;
				}
				if (result == null) {
					setText("// No decompilation result");
					return;
				}
				DecompileResult.ResultType resultType = result.getType();
				String text = result.getText();
				switch (resultType) {
					case SUCCESS, SKIPPED -> setText(text == null ? "// No decompilation output" : text);
					case FAILURE -> {
						Throwable exception = result.getException();
						if (exception == null)
							setText("/*\nDecompilation failure\n*/");
						else
							setText("/*\nDecompilation failure\n" + StringUtil.traceToString(exception) + "\n*/");
					}
				}
			}, FxThreadUtil.executor());
		}

		private void setPlaceholder(@Nonnull String key) {
			generation.incrementAndGet();
			setText(Lang.get(key));
		}

		private void setText(@Nonnull String text) {
			editor.setText(text);
		}

		private void close() {
			editor.close();
		}
	}
}
