package software.coley.recaf.ui.window;

import atlantafx.base.controls.ModalPane;
import atlantafx.base.controls.Popover;
import atlantafx.base.controls.Spacer;
import atlantafx.base.theme.Styles;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import me.darknet.assembler.error.Error;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import software.coley.observables.ObservableObject;
import software.coley.recaf.analytics.logging.DebuggingLogger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.BasicConfigValue;
import software.coley.recaf.config.ConfigContainer;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.config.ConfigValue;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.services.assembler.AssemblerPipelineManager;
import software.coley.recaf.services.assembler.JvmAssemblerPipeline;
import software.coley.recaf.services.cell.CellConfigurationService;
import software.coley.recaf.services.config.ConfigComponentFactory;
import software.coley.recaf.services.config.ConfigComponentManager;
import software.coley.recaf.services.decompile.DecompilerManager;
import software.coley.recaf.services.info.association.FileTypeSyntaxAssociationService;
import software.coley.recaf.services.navigation.Actions;
import software.coley.recaf.services.transform.ClassTransformer;
import software.coley.recaf.services.transform.CollectionTransformer;
import software.coley.recaf.services.transform.JvmClassTransformer;
import software.coley.recaf.services.transform.JvmTransformResult;
import software.coley.recaf.services.transform.TransformationApplier;
import software.coley.recaf.services.transform.TransformationApplierService;
import software.coley.recaf.services.transform.TransformationException;
import software.coley.recaf.services.transform.TransformationFeedback;
import software.coley.recaf.services.transform.TransformationManager;
import software.coley.recaf.services.transform.TransformationParameter;
import software.coley.recaf.services.transform.TransformationParameters;
import software.coley.recaf.services.transform.TransformationPreset;
import software.coley.recaf.services.transform.TransformationPreset.TransformerPreset;
import software.coley.recaf.services.transform.TransformationPresetManager;
import software.coley.recaf.services.workspace.WorkspaceManager;
import software.coley.recaf.ui.LanguageStylesheets;
import software.coley.recaf.ui.config.WorkspaceExplorerConfig;
import software.coley.recaf.ui.control.ActionButton;
import software.coley.recaf.ui.control.BoundIntSpinner;
import software.coley.recaf.ui.control.BoundLabel;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.ui.control.ReorderableListCell;
import software.coley.recaf.ui.control.popup.ClassSelectionPopup;
import software.coley.recaf.ui.control.popup.NamePopup;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.ui.control.richtext.bracket.SelectedBracketTracking;
import software.coley.recaf.ui.control.richtext.highlight.SelectedWordHighlighting;
import software.coley.recaf.ui.control.richtext.search.SearchBar;
import software.coley.recaf.ui.pane.editing.text.TextConfig;
import software.coley.recaf.util.ErrorDialogs;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.Lang;
import software.coley.recaf.util.StringUtil;
import software.coley.recaf.util.threading.Batch;
import software.coley.recaf.util.threading.ThreadPoolFactory;
import software.coley.recaf.util.threading.ThreadUtil;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.ClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import static software.coley.recaf.util.StringUtil.startsWithAny;

/**
 * Window for applying transformations <i>(Primarily with the intent to deobfuscate)</i> to a workspace.
 *
 * @author Matt Coley
 */
@Dependent
public class DeobfuscationWindow extends RecafStage {
	private static final DebuggingLogger logger = Logging.get(DeobfuscationWindow.class);

	private static final int MAX_PASSES = 50;

	private final TransformationManager transformationManager;
	private final TransformationApplierService transformationApplierService;
	private final WorkspaceManager workspaceManager;
	private final DecompilerManager decompilerManager;
	private final AssemblerPipelineManager assemblerPipelineManager;
	private final ConfigComponentManager configComponentManager;
	private final TransformationPresetManager presetManager;
	private final Actions actions;
	private final CellConfigurationService configurationService;
	private final WorkspaceExplorerConfig explorerConfig;

	private final List<TransformerDescriptor> descriptors;
	private final ObservableList<SelectedTransformer> selectedTransformers = FXCollections.observableArrayList();
	private final BooleanProperty hasSelection = new SimpleBooleanProperty();
	private final IntegerProperty maxPasses = new SimpleIntegerProperty(5);
	private final BooleanProperty dropFaultyClasses = new SimpleBooleanProperty();
	private final ObjectProperty<TransformationPreset.ScopeMode> scopeMode = new SimpleObjectProperty<>(TransformationPreset.ScopeMode.NONE);
	private final StringProperty packagePrefixes = new SimpleStringProperty("");
	private final ObjectProperty<FullFeedback> transformFeedback = new SimpleObjectProperty<>();
	private final BooleanProperty transformRunning = new SimpleBooleanProperty();

	private final BorderPane stage1Pane = new BorderPane();
	private final BorderPane stage2Pane = new BorderPane();
	private final StackPane root = new StackPane();
	private final TransformPreview beforePreview;
	private final TransformPreview afterPreview;
	private TreeView<TransformerTreeNode> availableTree;

	private ClassInfo previewClass;

	@Inject
	public DeobfuscationWindow(@Nonnull TransformationManager transformationManager,
	                           @Nonnull TransformationApplierService transformationApplierService,
	                           @Nonnull WorkspaceManager workspaceManager,
	                           @Nonnull DecompilerManager decompilerManager,
	                           @Nonnull AssemblerPipelineManager assemblerPipelineManager,
	                           @Nonnull ConfigComponentManager configComponentManager,
	                           @Nonnull TransformationPresetManager presetManager,
	                           @Nonnull FileTypeSyntaxAssociationService languageAssociation,
	                           @Nonnull CellConfigurationService configurationService,
	                           @Nonnull Actions actions,
	                           @Nonnull TextConfig textConfig,
	                           @Nonnull WorkspaceExplorerConfig explorerConfig,
	                           @Nonnull Instance<SearchBar> searchBarProvider) {
		this.transformationManager = transformationManager;
		this.transformationApplierService = transformationApplierService;
		this.workspaceManager = workspaceManager;
		this.decompilerManager = decompilerManager;
		this.assemblerPipelineManager = assemblerPipelineManager;
		this.configComponentManager = configComponentManager;
		this.presetManager = presetManager;
		this.actions = actions;
		this.configurationService = configurationService;
		this.explorerConfig = explorerConfig;

		// Build the flat, sorted descriptor list from every registered JVM transformer.
		descriptors = transformationManager.getJvmClassTransformers().stream()
				.map(this::instantiateDescriptor)
				.filter(Objects::nonNull)
				.sorted(Comparator.comparing(TransformerDescriptor::name, String.CASE_INSENSITIVE_ORDER)
						.thenComparing(d -> d.type().getName()))
				.toList();

		beforePreview = new TransformPreview(languageAssociation, textConfig, searchBarProvider, false);
		afterPreview = new TransformPreview(languageAssociation, textConfig, searchBarProvider, true);

		setupStage1();
		setupStage2();
		setupProgressModal();

		// Gate stage actions on whether any transformer is selected and keep the available tree synchronized.
		selectedTransformers.addListener((ListChangeListener<SelectedTransformer>) change -> {
			hasSelection.set(!selectedTransformers.isEmpty());
			availableTree.refresh();
		});
		hasSelection.set(false);

		root.getChildren().addAll(stage1Pane, stage2Pane);
		showStage1();

		titleProperty().bind(Lang.getBinding("deobf"));
		setScene(new RecafScene(root, 1200, 800));
		setWidth(1200);
		setHeight(800);
	}

	/**
	 * @param type
	 * 		Transformer type to instantiate.
	 *
	 * @return Instantiated transformer descriptor, or {@code null} if instantiation fails or the type is skipped.
	 */
	@Nullable
	private TransformerDescriptor instantiateDescriptor(@Nonnull Class<? extends JvmClassTransformer> type) {
		try {
			JvmClassTransformer instance = transformationManager.newJvmTransformer(type);
			String identifier = instance.identifier();
			return new TransformerDescriptor(type, instance, identifier, Lang.get("deobf." + identifier), instance.getParameterDefinitions());
		} catch (TransformationException e) {
			logger.error("Failed to initialize instance of {}", type, e);
			return null;
		}
	}

	/**
	 * Builds stage 1 controls.
	 */
	private void setupStage1() {
		// Stage 1: transformer selection, ordering, and parameter editing
		BoundLabel title = new BoundLabel(Lang.getBinding("deobf.stage.selection"));
		title.getStyleClass().add(Styles.TITLE_4);
		title.setPadding(new Insets(0, 0, 5, 5));

		availableTree = new TreeView<>(buildTransformerTree(descriptors));
		availableTree.setShowRoot(false);
		availableTree.setCellFactory(view -> new TreeCell<>() {
			@Override
			protected void updateItem(TransformerTreeNode item, boolean isEmpty) {
				TransformerTreeNode oldItem = getItem();

				super.updateItem(item, isEmpty);

				if (item == null || isEmpty) {
					setText(null);
					setGraphic(null);
					return;
				}

				// The uniform gap makes the text vs text+graphic lines align better vertically.
				int gap = 8;
				setGraphicTextGap(gap);

				// Skip re-rendering if the item is the same as before.
				// Otherwise, you'll see a flicker for a split second which is annoying...
				TransformerDescriptor descriptor = item.descriptor();
				if (item.equals(oldItem)) {
					// We may still need to update the toggle state if the selection changed.
					if (descriptor != null
							&& getGraphic() instanceof HBox graphic
							&& !graphic.getChildren().isEmpty()
							&& graphic.getChildren().getFirst() instanceof CheckBox toggle)
						toggle.setSelected(findSelected(descriptor) != null);

					return;
				}

				if (descriptor == null) {
					setText(item.name());
					setGraphic(null);
					return;
				}

				CheckBox toggle = new CheckBox();
				toggle.setSelected(findSelected(descriptor) != null);
				toggle.setOnAction(e -> {
					// We use 'onAction' over a binding on 'selectedProperty' because the
					// binding will trigger when the cell is recycled and re-rendered while
					// the action event only fires when the user clicks the toggle.
					if (toggle.isSelected())
						select(descriptor);
					else
						deselect(descriptor);
				});

				HBox graphic = new HBox(gap, toggle);
				graphic.setAlignment(Pos.CENTER_LEFT);

				if (!descriptor.parameters().isEmpty()) {
					Button[] settingsRef = new Button[1];
					Button settings = new ActionButton(CarbonIcons.SETTINGS, () -> showParameterEditor(descriptor, settingsRef[0]));
					settingsRef[0] = settings;
					settings.setPadding(new Insets(2, 4, 2, 4));
					settings.getStyleClass().addAll(Styles.ACCENT, Styles.FLAT);
					settings.disableProperty().bind(toggle.selectedProperty().not());
					settings.setFocusTraversable(false);
					graphic.getChildren().addAll(new Label(item.name()), settings);

					setText(null);
					setGraphic(graphic);
				} else {
					setText(descriptor.name());
					setGraphic(graphic);
				}
			}
		});

		BorderPane availablePane = new BorderPane();
		availablePane.setTop(new BoundLabel(Lang.getBinding("deobf.selection.title")));
		availablePane.setCenter(availableTree);

		ListView<SelectedTransformer> orderList = new ListView<>(selectedTransformers);
		orderList.setCellFactory(view -> new ReorderableListCell<>() {
			@Override
			protected void updateItem(SelectedTransformer item, boolean isEmpty) {
				super.updateItem(item, isEmpty);
				if (item == null || isEmpty) {
					setText(null);
					setGraphic(null);
					return;
				}

				VBox content = new VBox();
				content.setPadding(new Insets(10));
				content.setSpacing(10);
				content.setAlignment(Pos.CENTER_LEFT);
				content.getChildren().add(new Label(item.descriptor().name()));

				List<String> missingPredecessors = findMissingPredecessors(item);
				List<String> missingSuccessors = findMissingSuccessors(item);
				addRecommendationLabels(content, Lang.get("deobf.order.pre"), missingPredecessors);
				addRecommendationLabels(content, Lang.get("deobf.order.suc"), missingSuccessors);
				setGraphic(content);
			}
		});

		// Stage actions:
		//  - Sort transformers by recommended order.
		//  - Go to stage 2
		Button sortRecommended = new ActionButton(CarbonIcons.CLEAN, Lang.getBinding("deobf.sort-recommended"), this::sortRecommended);
		sortRecommended.disableProperty().bind(hasSelection.not());
		Button next = new ActionButton(CarbonIcons.ARROW_RIGHT, Lang.getBinding("deobf.next"), this::showStage2);
		next.disableProperty().bind(hasSelection.not());

		HBox actions = new HBox(8, sortRecommended, new Spacer(), next);
		actions.setAlignment(Pos.CENTER_RIGHT);
		actions.setPadding(new Insets(8, 0, 0, 0));

		BorderPane orderPane = new BorderPane();
		orderPane.setTop(new BoundLabel(Lang.getBinding("deobf.order.title")));
		orderPane.setCenter(orderList);
		orderPane.setBottom(actions);

		HBox split = new HBox(availablePane, orderPane);
		split.setSpacing(8);
		split.setPadding(new Insets(4));
		HBox.setHgrow(availablePane, javafx.scene.layout.Priority.ALWAYS);
		HBox.setHgrow(orderPane, javafx.scene.layout.Priority.ALWAYS);

		stage1Pane.setTop(title);
		stage1Pane.setCenter(split);
	}

	/**
	 * Builds stage 2 controls: the before/after preview tabs and the transformation scope.
	 */
	private void setupStage2() {
		// Stage 2: preview and scope filtering
		BoundLabel title = new BoundLabel(Lang.getBinding("deobf.stage.preview"));
		title.getStyleClass().add(Styles.TITLE_4);
		title.setPadding(new Insets(0, 0, 5, 5));

		// TODO: We have a before/after tab you click between.
		//  Later it would be nice to have a split view where you can see both at the same time.
		Tab beforeTab = new Tab();
		Tab afterTab = new Tab();
		beforeTab.setClosable(false);
		afterTab.setClosable(false);
		beforeTab.setContent(beforePreview);
		afterTab.setContent(afterPreview);
		beforeTab.textProperty().bind(Lang.getBinding("misc.before"));
		afterTab.textProperty().bind(Lang.getBinding("misc.after"));
		beforeTab.setGraphic(new FontIconView(CarbonIcons.LICENSE));
		afterTab.setGraphic(new FontIconView(CarbonIcons.LICENSE_MAINTENANCE));
		TabPane tabs = new TabPane(beforeTab, afterTab);
		tabs.getSelectionModel().select(afterTab);

		// Preview tools:
		//  - Pick class from workspace
		//  - Toggle preview mode between decompiler/disassembler
		Button pickClass = new ActionButton(CarbonIcons.ADD, Lang.getBinding("deobf.preview.pick"), this::pickPreviewClass);
		Button togglePreview = new ActionButton(CarbonIcons.ARROWS_HORIZONTAL, Lang.getBinding("deobf.preview.toggle-mode"), () -> {
			beforePreview.togglePreviewMode();
			afterPreview.togglePreviewMode();
		});
		HBox previewTools = new HBox(8, pickClass, new Spacer(), togglePreview);
		previewTools.setAlignment(Pos.CENTER_LEFT);
		previewTools.setPadding(new Insets(0, 4, 4, 4));

		// Scope controls:
		//  - Whitelist packages
		//  - Blacklist packages
		BoundIntSpinner maxPassesSpinner = new BoundIntSpinner(maxPasses, 1, MAX_PASSES);
		CheckBox dropFaulty = new CheckBox();
		dropFaulty.selectedProperty().bindBidirectional(dropFaultyClasses);
		dropFaulty.textProperty().bind(Lang.getBinding("deobf.scope.drop-faulty"));

		ComboBox<TransformationPreset.ScopeMode> scopeCombo = new ComboBox<>();
		scopeCombo.setMaxWidth(Integer.MAX_VALUE);
		scopeCombo.getItems().addAll(TransformationPreset.ScopeMode.values());
		scopeCombo.getSelectionModel().select(scopeMode.get());
		scopeCombo.getSelectionModel().selectedItemProperty().addListener((ob, old, cur) -> {
			if (cur != null)
				scopeMode.set(cur);
		});
		scopeMode.addListener((ob, old, cur) -> {
			if (scopeCombo.getSelectionModel().getSelectedItem() != cur)
				scopeCombo.getSelectionModel().select(cur);
		});
		scopeCombo.setButtonCell(new ScopeModeCell());
		scopeCombo.setCellFactory(v -> new ScopeModeCell());

		TextField packageField = new TextField();
		packageField.textProperty().bindBidirectional(packagePrefixes);
		packageField.setPromptText(Lang.get("deobf.scope.packages"));

		GridPane scope = new GridPane();
		ColumnConstraints col1 = new ColumnConstraints();
		ColumnConstraints col2 = new ColumnConstraints();
		col1.setFillWidth(true);
		col2.setFillWidth(true);
		col1.setHgrow(Priority.ALWAYS);
		scope.getColumnConstraints().addAll(col1, col2);
		scope.setHgap(8);
		scope.setVgap(8);
		scope.setPadding(new Insets(4));
		scope.add(new BoundLabel(Lang.getBinding("deobf.max-passes")), 0, 0);
		scope.add(maxPassesSpinner, 1, 0);
		scope.add(dropFaulty, 0, 1, 2, 1);
		scope.add(new BoundLabel(Lang.getBinding("deobf.scope.mode")), 0, 2);
		scope.add(scopeCombo, 1, 2);
		scope.add(packageField, 0, 3, 2, 1);

		// Stack the preview toolbar above the scope grid in the sidebar.
		VBox side = new VBox(8, previewTools, scope);
		side.setFillWidth(true);
		side.setPadding(new Insets(4));
		BorderPane sideWrapper = new BorderPane(side);

		// Bottom tools:
		//  - Go to stage 1 (change transformer selection/order)
		//  - Save preset for later use
		//  - Apply to workspace
		Button back = new ActionButton(CarbonIcons.ARROW_LEFT, Lang.getBinding("deobf.back"), this::showStage1);
		Button savePreset = new ActionButton(CarbonIcons.SAVE, Lang.getBinding("deobf.save-preset"), this::savePreset);
		Button apply = new ActionButton(CarbonIcons.PLAY, Lang.getBinding("deobf.apply"), this::applyToWorkspace).async();
		savePreset.disableProperty().bind(hasSelection.not());
		apply.disableProperty().bind(hasSelection.not().or(transformRunning));
		HBox bottom = new HBox(8, back, new Spacer(), savePreset, new Spacer(), apply);
		bottom.setAlignment(Pos.CENTER_RIGHT);
		bottom.setPadding(new Insets(4));

		BorderPane previewPane = new BorderPane();
		previewPane.setCenter(tabs);
		previewPane.getStyleClass().add(Styles.BORDER_DEFAULT);
		sideWrapper.setBottom(bottom);

		HBox split = new HBox(previewPane, sideWrapper);
		split.setSpacing(8);
		split.setPadding(new Insets(4));
		HBox.setHgrow(previewPane, javafx.scene.layout.Priority.ALWAYS);

		stage2Pane.setTop(title);
		stage2Pane.setCenter(split);

		// Refresh previews whenever the execution profile changes.
		ListChangeListener<SelectedTransformer> selectionListener = change -> {
			beforePreview.updatePreview();
			afterPreview.updatePreview();
		};
		selectedTransformers.addListener(selectionListener);
		maxPasses.addListener((ob, old, cur) -> {
			beforePreview.updatePreview();
			afterPreview.updatePreview();
		});
		dropFaultyClasses.addListener((ob, old, cur) -> {
			beforePreview.updatePreview();
			afterPreview.updatePreview();
		});
		scopeMode.addListener((ob, old, cur) -> {
			beforePreview.updatePreview();
			afterPreview.updatePreview();
		});
		packagePrefixes.addListener((ob, old, cur) -> {
			// TODO: While typing in the package prefix textfield, lot of refreshes get triggered...
			//  Should throttle the refreshes to only happen after a pause in typing.
			beforePreview.updatePreview();
			afterPreview.updatePreview();
		});
	}

	/**
	 * Builds the transformer tree from identifier categories.
	 *
	 * @param descriptors
	 * 		Transformer descriptors to place in the tree.
	 *
	 * @return Root tree item with category nodes expanded.
	 */
	@Nonnull
	private static TreeItem<TransformerTreeNode> buildTransformerTree(@Nonnull List<TransformerDescriptor> descriptors) {
		TreeItem<TransformerTreeNode> root = new TreeItem<>(new TransformerTreeNode("", null));
		for (TransformerDescriptor descriptor : descriptors) {
			TreeItem<TransformerTreeNode> parent = root;
			String[] segments = descriptor.identifier().split("\\.");

			// Create category nodes for every identifier segment except the transformer name.
			StringBuilder categoryPath = new StringBuilder();
			for (int i = 0; i < segments.length - 1; i++) {
				if (!categoryPath.isEmpty())
					categoryPath.append('.');
				categoryPath.append(segments[i]);
				String category = Lang.get("deobf." + categoryPath);
				parent = getOrCreateTransformerChild(parent, new TransformerTreeNode(category, null));
			}

			// Keep the localized transformer name on the selectable leaf.
			parent.getChildren().add(new TreeItem<>(new TransformerTreeNode(descriptor.name(), descriptor)));
		}

		// Keep both categories and transformers deterministic despite registration order.
		sortTransformerTree(root);
		return root;
	}

	/**
	 * Finds or creates a category child beneath a parent.
	 *
	 * @param parent
	 * 		Parent tree item.
	 * @param value
	 * 		Category node to find or create.
	 */
	@Nonnull
	private static TreeItem<TransformerTreeNode> getOrCreateTransformerChild(@Nonnull TreeItem<TransformerTreeNode> parent,
	                                                                         @Nonnull TransformerTreeNode value) {
		for (TreeItem<TransformerTreeNode> child : parent.getChildren())
			if (value.equals(child.getValue()))
				return child;
		TreeItem<TransformerTreeNode> child = new TreeItem<>(value);
		parent.getChildren().add(child);
		return child;
	}

	/**
	 * Sorts a transformer tree and expands every category so available transformers are immediately visible.
	 *
	 * @param parent
	 * 		Parent tree item to sort children of.
	 */
	private static void sortTransformerTree(@Nonnull TreeItem<TransformerTreeNode> parent) {
		parent.getChildren().sort(Comparator.comparing(child -> child.getValue().name(), String.CASE_INSENSITIVE_ORDER));
		for (TreeItem<TransformerTreeNode> child : parent.getChildren()) {
			if (!child.isLeaf()) {
				child.setExpanded(true);
				sortTransformerTree(child);
			}
		}
	}

	/**
	 * Adds indented recommendation labels under a heading to the given content box.
	 *
	 * @param content
	 * 		Content box to append labels to.
	 * @param title
	 * 		Heading text for the recommendation group.
	 * @param recommendations
	 * 		Recommendation texts to display.
	 */
	private static void addRecommendationLabels(@Nonnull VBox content, @Nonnull String title,
	                                            @Nonnull List<String> recommendations) {
		if (recommendations.isEmpty())
			return;

		Label heading = new Label(title + ":");
		heading.setPadding(new Insets(0, 0, 0, 20));
		heading.getStyleClass().add(Styles.TEXT_SUBTLE);
		content.getChildren().add(heading);

		for (String recommendation : recommendations) {
			Label entry = new Label("- " + recommendation);
			entry.setPadding(new Insets(0, 0, 0, 36));
			entry.getStyleClass().add(Styles.TEXT_SUBTLE);
			content.getChildren().add(entry);
		}
	}

	/**
	 * Builds the progress overlay shown while transformations are running.
	 */
	private void setupProgressModal() {
		ModalPane modal = new ModalPane();
		modal.setPersistent(true);  // Prevent escape key from closing the dialog while transforming.
		modal.setAlignment(Pos.CENTER);

		// Show or hide the overlay as feedback arrives from the running transformation.
		transformFeedback.addListener((ob, oldFeedback, newFeedback) -> FxThreadUtil.run(() -> {
			// The 'apply' button is configured to run asynchronously, so this callback is not on the FX thread.
			// This is why we have the wrapping run call above.
			if (newFeedback != null) {
				// Basic progress bar + label to display how far along the transformation process is.
				Button cancel = new ActionButton(CarbonIcons.CLOSE, Lang.getBinding("dialog.cancel"), newFeedback::cancel).once();
				Label label = new Label();
				label.setMinWidth(200);
				ProgressBar progressBar = new ProgressBar(0);
				progressBar.setMinWidth(250);

				// Manually sizing the controls above is easier than messing with auto-sizing.
				// I've tried various approaches with the GridPane, and it refuses to expand the progress bar properly.
				GridPane content = new GridPane();
				content.getStyleClass().addAll(Styles.BORDER_DEFAULT, Styles.BG_DEFAULT);
				content.setAlignment(Pos.CENTER);
				content.setPadding(new Insets(20));
				content.setHgap(10);
				content.setVgap(20);
				content.add(progressBar, 0, 0, 2, 1);
				content.add(label, 0, 1);
				content.add(cancel, 1, 1);
				content.setMinSize(400, 100);

				// Attach an observer that throttles progress updates to the UI thread.
				newFeedback.observer = new FullFeedback.FeedbackObserver() {
					long lastUpdate = 0;

					@Override
					public void update() {
						long now = System.currentTimeMillis();
						if (now - lastUpdate > 100) {
							lastUpdate = now;
							FxThreadUtil.run(() -> {
								int classes = newFeedback.classesVisited.size();
								int maxClasses = newFeedback.maxClasses;
								progressBar.setProgress((double) classes / maxClasses);
								label.setText(classes + " / " + maxClasses + " (Pass: " + newFeedback.currentPass + ")");
							});
						}
					}
				};
				modal.show(new Group(content));
			} else {
				// Detach the finished observer and hide the overlay.
				if (oldFeedback != null)
					oldFeedback.observer = null;
				modal.hide();
			}
		}));
		root.getChildren().add(modal);
	}

	/**
	 * Show stage 1.
	 */
	private void showStage1() {
		availableTree.refresh();
		stage1Pane.setVisible(true);
		stage1Pane.setManaged(true);
		stage2Pane.setVisible(false);
		stage2Pane.setManaged(false);
	}

	/**
	 * Show stage 2.
	 */
	private void showStage2() {
		stage1Pane.setVisible(false);
		stage1Pane.setManaged(false);
		stage2Pane.setVisible(true);
		stage2Pane.setManaged(true);
	}

	/**
	 * @param descriptor
	 * 		Transformer descriptor to look for.
	 *
	 * @return The matching selected transformer, or {@code null} if not selected.
	 */
	@Nullable
	private SelectedTransformer findSelected(@Nonnull TransformerDescriptor descriptor) {
		for (SelectedTransformer selected : selectedTransformers)
			if (selected.descriptor().type().equals(descriptor.type()))
				return selected;
		return null;
	}

	/**
	 * @param descriptor
	 * 		Transformer descriptor to add to the selection.
	 */
	private void select(@Nonnull TransformerDescriptor descriptor) {
		if (findSelected(descriptor) == null)
			selectedTransformers.add(new SelectedTransformer(descriptor, defaultParameterValues(descriptor)));
	}

	/**
	 * @param descriptor
	 * 		Transformer descriptor to remove from the selection.
	 */
	private void deselect(@Nonnull TransformerDescriptor descriptor) {
		selectedTransformers.removeIf(selected -> selected.descriptor().type().equals(descriptor.type()));
	}

	/**
	 * @param descriptor
	 * 		Transformer descriptor to read parameter defaults from.
	 *
	 * @return Map of parameter keys to their default values.
	 */
	@Nonnull
	private static Map<String, Object> defaultParameterValues(@Nonnull TransformerDescriptor descriptor) {
		Map<String, Object> values = new HashMap<>();
		for (TransformationParameter<?> parameter : descriptor.parameters())
			values.put(parameter.key(), parameter.defaultValue());
		return values;
	}

	/**
	 * Opens a parameter editor popover for the given transformer, anchored to the given node.
	 *
	 * @param descriptor
	 * 		Transformer descriptor whose parameters to edit.
	 * @param anchor
	 * 		Node to anchor the popover to.
	 */
	private void showParameterEditor(@Nonnull TransformerDescriptor descriptor, @Nonnull Node anchor) {
		// Ignore requests for transformers that aren't selected.
		SelectedTransformer selected = findSelected(descriptor);
		if (selected == null)
			return;

		// Hide any previous popover before showing a new one.
		if (selected.popover != null)
			selected.popover.hide();
		selected.popover = new Popover(buildParameterContent(selected, anchor));
		selected.popover.setArrowLocation(Popover.ArrowLocation.BOTTOM_RIGHT);
		selected.popover.show(anchor);
	}

	/**
	 * @param selected
	 * 		Selected transformer to build the editor for.
	 * @param anchor
	 * 		Anchor node for the parameter editor popover.
	 *
	 * @return Parameter editor content node.
	 */
	@Nonnull
	private Node buildParameterContent(@Nonnull SelectedTransformer selected, @Nonnull Node anchor) {
		VBox box = new VBox(8);
		box.setPadding(new Insets(10));
		box.setMinWidth(260);

		BoundLabel title = new BoundLabel(Lang.getBinding("deobf.params"));
		title.getStyleClass().add(Styles.TITLE_4);
		box.getChildren().add(title);

		// Build an editor row for each parameter definition.
		for (TransformationParameter<?> parameter : selected.descriptor().parameters()) {
			ConfigContainer container = parameterContainer(parameter.translationKey());
			ConfigValue<Object> value = parameterValue(parameter, selected);
			ConfigComponentFactory<Object> factory = configComponentManager.getFactory(container, value);
			Node editor = factory.create(container, value);

			// Standalone editors render without a separate label.
			if (factory.isStandAlone()) {
				box.getChildren().add(editor);
			} else {
				Label label = new BoundLabel(Lang.getBinding(parameter.translationKey()));
				box.getChildren().addAll(label, editor);
			}
		}

		// Reset restores defaults and refreshes previews since editor observables are bypassed.
		Button reset = new ActionButton(CarbonIcons.RESET, Lang.getBinding("deobf.reset-params"), () -> {
			selected.parameterValues.clear();
			selected.parameterValues.putAll(defaultParameterValues(selected.descriptor()));

			// Resetting bypasses the editor's observable values, so refresh explicitly.
			beforePreview.updatePreview();
			afterPreview.updatePreview();
			showParameterEditor(selected.descriptor(), anchor);
		});
		HBox resetRow = new HBox(reset);
		resetRow.setAlignment(Pos.CENTER_RIGHT);
		box.getChildren().add(resetRow);
		return box;
	}

	/**
	 * @param translationKey
	 * 		Translation key of the parameter the container is for.
	 *
	 * @return Config container scoped to the parameter translation key.
	 */
	@Nonnull
	private static ConfigContainer parameterContainer(@Nonnull String translationKey) {
		return new BasicConfigContainer(ConfigGroups.SERVICE_UI, "deobfuscation-parameter") {
			@Override
			public String getScopedId(ConfigValue<?> value) {
				return translationKey;
			}
		};
	}

	/**
	 * @param parameter
	 * 		Parameter definition to build a value for.
	 * @param selected
	 * 		Selected transformer holding the parameter values.
	 * @param <T>
	 * 		Parameter value type.
	 *
	 * @return Config value backed by the selection's parameter values.
	 */
	@Nonnull
	@SuppressWarnings("unchecked")
	private <T> ConfigValue<T> parameterValue(@Nonnull TransformationParameter<?> parameter,
	                                          @Nonnull SelectedTransformer selected) {
		ObservableObject<T> observable = new ObservableObject<>((T) selected.parameterValues.get(parameter.key()));
		observable.addChangeListener((ob, old, cur) -> {
			// Propagate editor changes into the selection.
			selected.parameterValues.put(parameter.key(), cur);

			// Parameter changes affect the transformed preview just like scope changes do.
			beforePreview.updatePreview();
			afterPreview.updatePreview();
		});
		return new BasicConfigValue<>(parameter.key(), (Class<T>) parameter.type(), observable);
	}

	/**
	 * Sorts the selected transformers by the recommended execution order.
	 */
	private void sortRecommended() {
		// Rebuild the selection in recommended order, keeping each transformer's parameter values.
		List<ClassTransformer> instances = selectedTransformers.stream()
				.map(selected -> selected.descriptor().instance())
				.map(ClassTransformer.class::cast)
				.collect(Collectors.toList());
		List<ClassTransformer> sorted = TransformationApplier.sortRecommended(instances);
		Map<Class<? extends ClassTransformer>, SelectedTransformer> byType = selectedTransformers.stream()
				.collect(Collectors.toMap(selected -> selected.descriptor().type(), selected -> selected));
		selectedTransformers.setAll(sorted.stream().map(t -> byType.get(t.getClass())).toList());
	}

	/**
	 * @param selected
	 * 		Selected transformer to check recommendations for.
	 *
	 * @return Names of recommended predecessors missing from the selection or ordered after this transformer.
	 */
	@Nonnull
	private List<String> findMissingPredecessors(@Nonnull SelectedTransformer selected) {
		int selectedIndex = selectedTransformers.indexOf(selected);
		List<Class<? extends JvmClassTransformer>> selectedTypes = selectedTransformers.stream()
				.map(s -> s.descriptor().type())
				.collect(Collectors.toList());
		JvmClassTransformer transformer = selected.descriptor().instance();
		return transformer.recommendedPredecessors().stream()
				.filter(type -> {
					int index = selectedTypes.indexOf(type);
					return index < 0 || index > selectedIndex;
				})
				.map(this::transformerDisplayName)
				.toList();
	}

	/**
	 * @param selected
	 * 		Selected transformer to check recommendations for.
	 *
	 * @return Names of recommended successors missing from the selection or ordered before this transformer.
	 */
	@Nonnull
	private List<String> findMissingSuccessors(@Nonnull SelectedTransformer selected) {
		int selectedIndex = selectedTransformers.indexOf(selected);
		List<Class<? extends JvmClassTransformer>> selectedTypes = selectedTransformers.stream()
				.map(s -> s.descriptor().type())
				.collect(Collectors.toList());
		JvmClassTransformer transformer = selected.descriptor().instance();
		return transformer.recommendedSuccessors().stream()
				.filter(type -> {
					int index = selectedTypes.indexOf(type);
					return index < 0 || index < selectedIndex;
				})
				.map(this::transformerDisplayName)
				.toList();
	}

	/**
	 * @param type
	 * 		Transformer type to look up.
	 *
	 * @return Display name of the transformer, or its simple class name if unknown.
	 */
	@Nonnull
	private String transformerDisplayName(@Nonnull Class<? extends ClassTransformer> type) {
		for (TransformerDescriptor descriptor : descriptors)
			if (descriptor.type().equals(type))
				return descriptor.name();
		return type.getSimpleName();
	}

	/**
	 * @param type
	 * 		Transformer type to look up.
	 *
	 * @return The descriptor's transformer instance, or {@code null} if unknown.
	 */
	@Nullable
	private ClassTransformer descriptorInstance(@Nonnull Class<? extends ClassTransformer> type) {
		for (TransformerDescriptor descriptor : descriptors)
			if (descriptor.type().equals(type))
				return descriptor.instance();
		return null;
	}

	/**
	 * @return Transformer types of the current selection, in order.
	 */
	@Nonnull
	private List<Class<? extends JvmClassTransformer>> transformerTypes() {
		List<Class<? extends JvmClassTransformer>> types = new ArrayList<>(selectedTransformers.size());
		for (SelectedTransformer selected : selectedTransformers)
			types.add(selected.descriptor().type());
		return types;
	}

	/**
	 * @return Parameters aggregating every selected transformer's values.
	 *
	 * @throws TransformationException
	 * 		When a parameter key is duplicated across the selection.
	 */
	@Nonnull
	private TransformationParameters buildParameters() throws TransformationException {
		// Aggregate values, rejecting duplicate keys since they'd silently overwrite.
		Map<String, Object> values = new HashMap<>();
		for (SelectedTransformer selected : selectedTransformers) {
			for (TransformationParameter<?> parameter : selected.descriptor().parameters()) {
				String key = parameter.key();
				if (values.containsKey(key))
					throw new TransformationException("Duplicate transformer parameter key: " + key);
				values.put(key, selected.parameterValues.get(key));
			}
		}
		return new TransformationParameters(values);
	}

	/**
	 * @param text
	 * 		Comma-separated prefixes, or {@code null}.
	 *
	 * @return Trimmed non-empty prefix list.
	 */
	@Nonnull
	private List<String> parsePrefixes(@Nullable String text) {
		if (text == null || text.isBlank())
			return List.of();
		return Arrays.stream(text.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
	}

	/**
	 * @param cls
	 * 		Class to check.
	 *
	 * @return {@code true} if the class is within the configured package scope.
	 */
	private boolean isClassInScope(@Nonnull ClassInfo cls) {
		// Treat the default package as empty so prefix checks still apply.
		String pkg = cls.getPackageName();
		if (pkg == null)
			pkg = "";
		List<String> prefixes = parsePrefixes(packagePrefixes.get());
		return switch (scopeMode.get()) {
			case NONE -> true;
			case BLACKLIST -> !startsWithAny(pkg, prefixes);
			case WHITELIST -> prefixes.isEmpty() || startsWithAny(pkg, prefixes);
		};
	}

	/**
	 * @return Scope predicate including collection transformers and in-scope classes.
	 */
	@Nonnull
	private BiPredicate<ClassTransformer, ClassInfo> fullScope() {
		return (transformer, cls) -> transformer instanceof CollectionTransformer || isClassInScope(cls);
	}

	/**
	 * @param target
	 * 		Previewed class.
	 *
	 * @return Scope predicate including collection transformers, the target class, and its inner classes.
	 */
	@Nonnull
	private BiPredicate<ClassTransformer, ClassInfo> previewScope(@Nonnull ClassInfo target) {
		// We include the inner classes because those will show up in the decompilation output,
		// and having random parts of the output not transformed looks bad.
		return (transformer, cls) -> transformer instanceof CollectionTransformer
				|| (isClassInScope(cls)
				&& (cls.getName().equals(target.getName()) || cls.isInnerClassOf(target.getName())));
	}

	/**
	 * Prompts for a preset name and saves the current profile.
	 */
	private void savePreset() {
		NamePopup popup = new NamePopup(name -> {
			try {
				TransformationPreset preset = buildPreset();
				presetManager.putPreset(name, preset);
			} catch (IllegalArgumentException ex) {
				logger.error("Failed to save deobfuscation preset", ex);
				ErrorDialogs.show(Lang.get("deobf.error.preset.title"), Lang.get("deobf.error.preset.header"),
						Lang.get("deobf.error.preset.content"), ex);
			}
		});
		popup.titleProperty().bind(Lang.getBinding("deobf.preset.save-title"));
		popup.show();
	}

	/**
	 * @return Preset snapshot of the current profile.
	 */
	@Nonnull
	private TransformationPreset buildPreset() {
		List<TransformerPreset> transformerPresets = selectedTransformers.stream()
				.map(selected -> new TransformerPreset(selected.descriptor().type().getName(), new HashMap<>(selected.parameterValues)))
				.toList();
		return new TransformationPreset(transformerPresets, maxPasses.get(), dropFaultyClasses.get(),
				scopeMode.get(), parsePrefixes(packagePrefixes.get()));
	}

	/**
	 * Loads and applies the given preset to the current workspace.
	 * <p>
	 * Notifies the user of any missing transformers or invalid preset values,
	 * and aborts without mutating any window state.
	 *
	 * @param preset
	 * 		Preset to apply.
	 */
	public void applyPreset(@Nonnull TransformationPreset preset) {
		// Index descriptors by transformer type name for preset resolution.
		Map<String, TransformerDescriptor> descriptorByName = new HashMap<>();
		for (TransformerDescriptor descriptor : descriptors)
			descriptorByName.put(descriptor.type().getName(), descriptor);

		// Resolve every preset entry, collecting any unavailable transformers.
		List<String> missing = new ArrayList<>();
		List<SelectedTransformer> resolved = new ArrayList<>();
		for (TransformerPreset transformerPreset : preset.transformers()) {
			TransformerDescriptor descriptor = descriptorByName.get(transformerPreset.transformerClassName());
			if (descriptor == null) {
				missing.add(transformerPreset.transformerClassName());
				continue;
			}
			Map<String, Object> values = mapValues(descriptor, transformerPreset.parameters());
			resolved.add(new SelectedTransformer(descriptor, values));
		}

		// Abort when any referenced transformer is unavailable.
		//
		// Generally this shouldn't occur since I don't really foresee me removing transformers,
		// but moreso if there are plugins that define transformers that aren't present in this run.
		if (!missing.isEmpty()) {
			TransformationException error = new TransformationException("Preset references unavailable transformers: " + String.join(", ", missing));
			showExecutionError(error);
			return;
		}

		// Validate the resolved profile before mutating any window state.
		try {
			validatePreset(resolved, preset.maxPasses());
		} catch (TransformationException error) {
			showExecutionError(error);
			return;
		}

		// Apply the profile and jump to the preview stage.
		selectedTransformers.setAll(resolved);
		maxPasses.set(preset.maxPasses());
		dropFaultyClasses.set(preset.dropFaultyClasses());
		scopeMode.set(preset.scopeMode());
		packagePrefixes.set(String.join(", ", preset.packagePrefixes()));
		showStage2();
	}

	/**
	 * @param resolved
	 * 		Resolved preset transformers.
	 * @param presetMaxPasses
	 * 		Max passes value from the preset.
	 *
	 * @throws TransformationException
	 * 		When the max passes are out of range, or transformers or parameter keys are duplicated.
	 */
	private static void validatePreset(@Nonnull List<SelectedTransformer> resolved, int presetMaxPasses)
			throws TransformationException {
		if (presetMaxPasses < 1 || presetMaxPasses > MAX_PASSES)
			throw new TransformationException("Preset max passes must be between 1 and " + MAX_PASSES);

		// Reject duplicate transformers or parameter keys.
		Set<Class<? extends JvmClassTransformer>> transformerTypes = new HashSet<>();
		Set<String> parameterKeys = new HashSet<>();
		for (SelectedTransformer selected : resolved) {
			if (!transformerTypes.add(selected.descriptor().type()))
				throw new TransformationException("Preset contains duplicate transformer: " + selected.descriptor().type().getName());
			for (TransformationParameter<?> parameter : selected.descriptor().parameters())
				if (!parameterKeys.add(parameter.key()))
					throw new TransformationException("Duplicate transformer parameter key: " + parameter.key());
		}
	}

	/**
	 * @param descriptor
	 * 		Transformer descriptor the values belong to.
	 * @param savedValues
	 * 		Parameter values loaded from the preset.
	 *
	 * @return Values mapped to the descriptor's parameter types.
	 */
	@Nonnull
	private static Map<String, Object> mapValues(@Nonnull TransformerDescriptor descriptor,
	                                             @Nonnull Map<String, Object> savedValues) {
		Map<String, Object> values = TransformationPresetManager.mapParameters(descriptor.parameters(), savedValues);
		for (String key : savedValues.keySet()) {
			boolean known = descriptor.parameters().stream().anyMatch(p -> p.key().equals(key));
			if (!known)
				logger.warn("Ignoring unknown transformer parameter '{}' for '{}'", key, descriptor.type().getName());
		}
		return values;
	}

	/**
	 * @param error
	 * 		Error to log and display.
	 */
	private void showExecutionError(@Nonnull Throwable error) {
		logger.error("Failed applying deobfuscation transformations", error);
		ErrorDialogs.show(Lang.get("deobf.error.apply.title"),
				Lang.get("deobf.error.apply.header"),
				Lang.get("deobf.error.apply.content"), error);
	}

	/**
	 * Opens a class picker and refreshes both preview editors with the selection.
	 */
	private void pickPreviewClass() {
		new ClassSelectionPopup(actions, configurationService, explorerConfig, workspaceManager.getCurrent(), path -> {
			ClassInfo selection = path.getValue();
			previewClass = selection;
			beforePreview.setClassInfo(selection);
			afterPreview.setClassInfo(selection);
			beforePreview.updatePreview();
			afterPreview.updatePreview();
		}).showAndWait();
	}

	/**
	 * Applies the current profile to the workspace and closes the window on success.
	 */
	private void applyToWorkspace() {
		// Skip if no workspace is open.
		TransformationApplier applier = transformationApplierService.newApplierForCurrentWorkspace();
		if (applier == null) {
			showExecutionError(new TransformationException("No workspace is open"));
			return;
		}

		// Run the pipeline with progress feedback, applying results unless cancelled.
		try {
			List<Class<? extends JvmClassTransformer>> types = transformerTypes();
			TransformationParameters parameters = buildParameters();
			FullFeedback feedback = new FullFeedback();
			transformRunning.set(true);
			transformFeedback.set(feedback);
			applier.setMaxPasses(maxPasses.get());
			applier.setDropFaultyClasses(dropFaultyClasses.get());
			JvmTransformResult result = applier.transformJvm(types, parameters, feedback);
			if (!feedback.hasRequestedCancellation()) {
				result.apply();
				FxThreadUtil.run(this::hide);
			}
		} catch (TransformationException e) {
			showExecutionError(e);
		} finally {
			// Always clear running state and feedback, even on failure.
			transformRunning.set(false);
			transformFeedback.set(null);
		}
	}

	/**
	 * Preview pane capable of disassembly/decompilation for both before and after states.
	 */
	private class TransformPreview extends BorderPane {
		private final Batch deobfuscationBatch = ThreadUtil.batch(ThreadPoolFactory.newSingleThreadExecutor("deobfuscation-preview"));
		private final AtomicInteger previewGeneration = new AtomicInteger();
		private final boolean andApply;
		private final Editor editorDecompile;
		private final Editor editorAssembly;
		private ClassInfo classInfo;

		/**
		 * @param languageAssociation
		 * 		Service configuring editor syntax highlighting.
		 * @param textConfig
		 * 		Editor text configuration.
		 * @param searchBarProvider
		 * 		Provider of search bars for the editors.
		 * @param andApply
		 * 		Whether the preview transforms the class before rendering.
		 */
		private TransformPreview(@Nonnull FileTypeSyntaxAssociationService languageAssociation,
		                         @Nonnull TextConfig textConfig,
		                         @Nonnull Instance<SearchBar> searchBarProvider,
		                         boolean andApply) {
			this.andApply = andApply;

			// Configure the decompile editor with Java syntax.
			editorDecompile = new Editor();
			if (textConfig.doHighlightWord())
				editorDecompile.setSelectedWordHighlighting(new SelectedWordHighlighting());
			if (textConfig.doTrackBrackets())
				editorDecompile.setSelectedBracketTracking(new SelectedBracketTracking());
			editorDecompile.getRootLineGraphicFactory().addDefaultCodeGraphicFactories();
			editorDecompile.getCodeArea().setEditable(false);
			languageAssociation.configureEditorSyntax("java", editorDecompile);

			// Configure the assembly editor with jasm syntax.
			editorAssembly = new Editor();
			editorAssembly.getCodeArea().getStylesheets().add(LanguageStylesheets.getJasmStylesheet());
			editorAssembly.getRootLineGraphicFactory().addDefaultCodeGraphicFactories();
			if (textConfig.doHighlightWord())
				editorAssembly.setSelectedWordHighlighting(new SelectedWordHighlighting());
			if (textConfig.doTrackBrackets())
				editorAssembly.setSelectedBracketTracking(new SelectedBracketTracking());
			editorAssembly.getCodeArea().setEditable(false);
			languageAssociation.configureEditorSyntax("jasm", editorAssembly);

			// Install search bars over both editors.
			SearchBar searchBar1 = searchBarProvider.get();
			SearchBar searchBar2 = searchBarProvider.get();
			searchBar1.install(editorDecompile);
			searchBar2.install(editorAssembly);

			// TODO: Make it a preference which is shown first
			setCenter(editorDecompile);
			updatePreview();
		}

		/**
		 * @param classInfo
		 * 		Class to preview.
		 */
		public void setClassInfo(@Nonnull ClassInfo classInfo) {
			this.classInfo = classInfo;
		}

		/**
		 * Switches between decompile and assembly preview modes.
		 */
		public void togglePreviewMode() {
			setCenter(isDecompilePreview() ? editorAssembly : editorDecompile);
			updatePreview();
		}

		/**
		 * @return {@code true} if the decompile editor is the active preview.
		 */
		public boolean isDecompilePreview() {
			return getCenter() == editorDecompile;
		}

		/**
		 * Queues a preview refresh, coalescing rapid profile changes into the latest build.
		 */
		private void updatePreview() {
			// TODO: We can't really stop expensive decompilations, they don't check for interrupts on the current thread
			//  and we have no good way of force killing them (rip Thread.stop) so the best we can really do is only
			//  allow one of these tasks to run at a time, with one next 'pending' task.
			//  While a task is executing we update what is the next 'pending' task when we call this method.
			//  Once the current task is done, or there is nothing running we make the 'pending' task the current task and run it.
			int generation = previewGeneration.incrementAndGet();
			deobfuscationBatch.add(() -> {
				deobfuscationBatch.clear();
				if (isDecompilePreview())
					decompile(generation);
				else
					disassemble(generation);
			});
			deobfuscationBatch.executeNewest();
		}

		/**
		 * Displays the transformed class as jasm disassembly.
		 */
		private void disassemble(int generation) {
			// Show the placeholder hint until a class is selected.
			if (classInfo == null) {
				FxThreadUtil.run(() -> {
					if (generation == previewGeneration.get())
						editorAssembly.setText("// Preview: Disassembly\n" + Lang.get("deobf.preview.noselection"));
				});
				return;
			}

			JvmClassInfo jvmClass = getProcessedClass(generation);
			if (jvmClass == null || generation != previewGeneration.get())
				return;

			// Resolve the transformed class path within the workspace, rebuild with our updated class, and disassemble.
			Workspace workspace = workspaceManager.getCurrent();
			String className = jvmClass.getName();
			ClassPathNode path = workspace.findClass(className);
			if (path == null) {
				logger.error("Couldn't find class {} in workspace for deobfuscation preview", className);
				return;
			}
			path = Objects.requireNonNull(path.getParent()).child(jvmClass);
			JvmAssemblerPipeline pipeline = assemblerPipelineManager.newJvmAssemblerPipeline(workspace);
			pipeline.disassemble(path)
					.ifOk(disassembly -> FxThreadUtil.run(() -> {
						if (generation == previewGeneration.get())
							editorAssembly.setText(disassembly);
					}))
					.ifErr((errors) -> {
						String errorListStr = errors.stream().map(Error::toString).collect(Collectors.joining("\n - "));
						logger.warn("Errors processing {} for deobfuscation preview:\n - {}", className, errorListStr);
						FxThreadUtil.run(() -> {
							if (generation == previewGeneration.get())
								showPreviewFailure(new IllegalStateException(errorListStr));
						});
					});
		}

		/**
		 * Displays the transformed class as decompiled source.
		 */
		private void decompile(int generation) {
			// Show the placeholder hint until a class is selected.
			if (classInfo == null) {
				FxThreadUtil.run(() -> {
					if (generation == previewGeneration.get())
						editorDecompile.setText("// Preview: Decompile\n" + Lang.get("deobf.preview.noselection"));
				});
				return;
			}

			// TODO: There are cases where the target class was not transformed, but an inner class was.
			//  In these cases we should still be able to show the decompilation of the target class while
			//  reflecting the inner class changes. However, our current decompiler API just pulls from the
			//  workspace, which won't have the transformed inner class until after we apply.
			//  - Will need to create a lightweight view of the workspace that overlays transformed classes for decompilation.
			JvmClassInfo jvmClass = getProcessedClass(generation);
			if (jvmClass == null || generation != previewGeneration.get())
				return;

			// TODO: Pull out common decompile code with "AbstractDecompilePane" to do this more aesthetically
			//  - And by that I mean include the animation
			//  - And the error handling for decompilation failures
			//  - But not the rest of the unrelated "editing" capabilities of the "AbstractDecompilePane"
			decompilerManager.decompile(workspaceManager.getCurrent(), jvmClass).whenCompleteAsync((result, error) -> {
				if (generation != previewGeneration.get())
					return;
				if (result != null) {
					editorDecompile.setText(result.getText());
				} else if (error != null) {
					String trace = StringUtil.traceToString(error);
					editorDecompile.setText("/*\nDecompilation failure\n" + trace + "\n*/");
				}
			}, FxThreadUtil.executor());
		}

		/**
		 * @param error
		 * 		Failure to display in both editors.
		 */
		private void showPreviewFailure(@Nonnull Throwable error) {
			String text = "// Failed to transform: " + error.getMessage() + "\n"
					+ "// " + StringUtil.traceToString(error).replace("\n", "\n// ");
			editorAssembly.setText(text);
			editorDecompile.setText("/*\n" + text + "\n*/");
		}

		/**
		 * @return The previewed class, transformed when in apply mode, or {@code null} if transformation failed.
		 */
		@Nullable
		private JvmClassInfo getProcessedClass(int generation) {
			JvmClassInfo jvmClass = classInfo.asJvmClass();

			// Apply mode transforms the class without committing anything to the workspace.
			if (andApply) {
				try {
					TransformationApplier applier = transformationApplierService.newApplierForCurrentWorkspace();
					if (applier == null)
						throw new TransformationException("No workspace is open");
					applier.setMaxPasses(maxPasses.get());
					applier.setDropFaultyClasses(dropFaultyClasses.get());
					JvmTransformResult result = applier.transformJvm(transformerTypes(), buildParameters(),
							new PreviewFeedback(classInfo));
					result.getTransformerFailures().forEach((_, map) -> map.forEach((transformer, error) ->
							logger.debugging(l -> l.warn("Transformer '{}' failure: ", transformer.getSimpleName(), error))));
					for (JvmClassInfo resultClass : result.getTransformedClasses().values()) {
						if (resultClass.getName().equals(classInfo.getName())) {
							jvmClass = resultClass;
							break;
						}
					}
				} catch (TransformationException e) {
					FxThreadUtil.run(() -> {
						if (generation == previewGeneration.get())
							showPreviewFailure(e);
					});
					return null;
				}
			}

			return jvmClass;
		}
	}

	/**
	 * Transformation feedback that tracks all classes transformed for progress reporting.
	 */
	private class FullFeedback implements TransformationFeedback {
		private final Set<ClassBundle<?>> targetBundles = Collections.newSetFromMap(new IdentityHashMap<>());
		private final Set<String> classesVisited = Collections.newSetFromMap(new ConcurrentHashMap<>());
		private final BiPredicate<ClassTransformer, ClassInfo> scope = fullScope();
		private int currentPass;
		private int maxClasses;
		private boolean cancelled;
		private FeedbackObserver observer;

		/**
		 * Requests cancellation of the running transformation.
		 */
		public void cancel() {
			cancelled = true;
		}

		@Override
		public boolean hasRequestedCancellation() {
			return cancelled;
		}

		@Override
		public boolean shouldTransform(@Nonnull Workspace workspace, @Nonnull WorkspaceResource resource,
		                               @Nonnull ClassBundle<?> bundle, @Nonnull ClassInfo classInfo,
		                               @Nonnull ClassTransformer transformer, int pass) {
			return scope.test(transformer, classInfo);
		}

		@Override
		public void onTransformFailure(@Nonnull Workspace workspace, @Nonnull WorkspaceResource resource,
		                               @Nonnull ClassBundle<?> bundle, @Nonnull ClassInfo classInfo,
		                               @Nonnull ClassTransformer transformer, int pass,
		                               @Nullable Throwable error) {
			// TODO: In the UI we should show errors affecting classes grouped by transformer as they occur.
			//  - Mainly so that users can report bugs on specific transformers when they run into issues.
			onTransformed(workspace, resource, bundle, classInfo, transformer, pass);
		}

		@Override
		public void onTransformedWithoutWork(@Nonnull Workspace workspace, @Nonnull WorkspaceResource resource,
		                                     @Nonnull ClassBundle<?> bundle, @Nonnull ClassInfo classInfo,
		                                     @Nonnull ClassTransformer transformer, int pass) {
			onTransformed(workspace, resource, bundle, classInfo, transformer, pass);
		}

		@Override
		public void onTransformed(@Nonnull Workspace workspace, @Nonnull WorkspaceResource resource,
		                          @Nonnull ClassBundle<?> bundle, @Nonnull ClassInfo classInfo,
		                          @Nonnull ClassTransformer transformer, int pass) {
			// Reset per-pass tracking.
			if (currentPass != pass) {
				currentPass = pass;
				classesVisited.clear();
			}

			// Update max class count based on unique bundles seen.
			synchronized (targetBundles) {
				if (targetBundles.add(bundle))
					maxClasses += bundle.size();
			}

			// Track unique classes transformed this pass.
			classesVisited.add(classInfo.getName());

			// Notify observer of progress.
			if (observer != null)
				observer.update();
		}

		interface FeedbackObserver {
			void update();
		}
	}

	/**
	 * Transformation feedback that filters to only the preview class <i>(and its inner classes)</i>.
	 */
	private class PreviewFeedback implements TransformationFeedback {
		private final BiPredicate<ClassTransformer, ClassInfo> scope;

		/**
		 * @param targetClass
		 * 		Class to preview.
		 */
		public PreviewFeedback(@Nonnull ClassInfo targetClass) {
			this.scope = previewScope(targetClass);
		}

		@Override
		public boolean shouldTransform(@Nonnull Workspace workspace, @Nonnull WorkspaceResource resource,
		                               @Nonnull ClassBundle<?> bundle, @Nonnull ClassInfo classInfo,
		                               @Nonnull ClassTransformer transformer, int pass) {
			return scope.test(transformer, classInfo);
		}
	}

	/**
	 * Registered transformer metadata used by the selection tree and execution profile.
	 *
	 * @param type
	 * 		Transformer implementation type.
	 * @param instance
	 * 		Transformer instance used for metadata and ordering recommendations.
	 * @param identifier
	 * 		Dot-separated transformer identifier.
	 * @param name
	 * 		Translated transformer display name.
	 * @param parameters
	 * 		Transformer parameter definitions.
	 */
	private record TransformerDescriptor(@Nonnull Class<? extends JvmClassTransformer> type,
	                                     @Nonnull JvmClassTransformer instance,
	                                     @Nonnull String identifier,
	                                     @Nonnull String name,
	                                     @Nonnull List<TransformationParameter<?>> parameters) {}

	/**
	 * Tree node containing either a transformer category or a selectable transformer.
	 *
	 * @param name
	 * 		Display name for the node.
	 * @param descriptor
	 * 		Transformer descriptor for leaves, or {@code null} for categories.
	 */
	private record TransformerTreeNode(@Nonnull String name, @Nullable TransformerDescriptor descriptor) {}

	/**
	 * Selected transformer with its parameter values and active parameter editor popover.
	 */
	private static final class SelectedTransformer {
		private final TransformerDescriptor descriptor;
		private final Map<String, Object> parameterValues;
		private Popover popover;

		/**
		 * @param descriptor
		 * 		Transformer descriptor.
		 * @param parameterValues
		 * 		Parameter values, copied to isolate the selection.
		 */
		private SelectedTransformer(@Nonnull TransformerDescriptor descriptor, @Nonnull Map<String, Object> parameterValues) {
			this.descriptor = descriptor;
			this.parameterValues = new HashMap<>(parameterValues);
		}

		/**
		 * @return Transformer descriptor.
		 */
		@Nonnull
		TransformerDescriptor descriptor() {
			return descriptor;
		}

		/**
		 * @return Parameter values of the selection.
		 */
		@Nonnull
		Map<String, Object> parameterValues() {
			return parameterValues;
		}

		/**
		 * @return The active parameter editor popover, or {@code null} if none.
		 */
		@Nullable
		Popover popover() {
			return popover;
		}

		/**
		 * @param popover
		 * 		Parameter editor popover to track.
		 */
		void popover(@Nonnull Popover popover) {
			this.popover = popover;
		}
	}

	/**
	 * List cell for displaying the scope mode enum with a translated name.
	 */
	private static final class ScopeModeCell extends ListCell<TransformationPreset.ScopeMode> {
		@Override
		protected void updateItem(TransformationPreset.ScopeMode item, boolean isEmpty) {
			super.updateItem(item, isEmpty);

			if (item == null || isEmpty) {
				setText(null);
			} else {
				setText(switch (item) {
					case NONE -> Lang.get("deobf.scope.none");
					case BLACKLIST -> Lang.get("deobf.scope.blacklist");
					case WHITELIST -> Lang.get("deobf.scope.whitelist");
				});
			}
		}
	}
}
