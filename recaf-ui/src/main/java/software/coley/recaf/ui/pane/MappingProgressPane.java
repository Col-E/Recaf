package software.coley.recaf.ui.pane;

import atlantafx.base.theme.Styles;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.effect.Effect;
import javafx.scene.effect.Glow;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.reactfx.EventStreams;
import software.coley.collections.tree.SortedTreeImpl;
import software.coley.collections.tree.Tree;
import software.coley.recaf.info.AndroidClassInfo;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.info.properties.builtin.HasMappedReferenceProperty;
import software.coley.recaf.info.properties.builtin.OriginalClassNameProperty;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.services.cell.CellConfigurationService;
import software.coley.recaf.services.cell.context.ContextSource;
import software.coley.recaf.services.mapping.IntermediateMappings;
import software.coley.recaf.services.mapping.Mappings;
import software.coley.recaf.services.mapping.aggregate.AggregateMappingManager;
import software.coley.recaf.ui.config.WorkspaceExplorerConfig;
import software.coley.recaf.ui.control.PannableView;
import software.coley.recaf.ui.window.MappingProgressWindow;
import software.coley.recaf.util.*;
import software.coley.recaf.util.threading.ThreadPoolFactory;
import software.coley.recaf.services.workspace.WorkspaceManager;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.AndroidClassBundle;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.ResourceAndroidClassListener;
import software.coley.recaf.workspace.model.resource.ResourceJvmClassListener;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;
import software.coley.treemap.TreeMapPane;
import software.coley.treemap.content.SimpleHierarchicalTreeContent;
import software.coley.treemap.content.TreeContent;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.function.ToDoubleFunction;

/**
 * Pane to display the current mapping progress of classes in the workspace.
 * Classes change color to indicate if they are mapped or not, and how fully mapped their declared members are.
 *
 * @author Matt Coley
 * @see MappingProgressWindow
 */
@Dependent
public class MappingProgressPane extends BorderPane implements ResourceJvmClassListener, ResourceAndroidClassListener {
	private static final Color RED = Color.web("rgb(80, 0, 0)");
	private static final Color GREEN = Color.web("rgb(0, 80, 0)");
	private static final Color MID = Colors.interpolateHsb(RED, GREEN, 0.5F);
	private static final ExecutorService pool = ThreadPoolFactory.newSingleThreadExecutor("mapping-preview");
	private static final Metric[] metrics = new Metric[]{
			new Metric.MetricSize(), new Metric.MetricMemberCount()
	};
	private final CellConfigurationService configurationService;
	private final ObjectProperty<Metric> metric = new SimpleObjectProperty<>();
	private final ObjectProperty<SelectionInfo> selectedPath = new SimpleObjectProperty<>();
	private final ObservableList<TreeContent> treeContentListDelegate = FXCollections.observableArrayList();
	private final ObservableList<TreeContent> treeContentList;
	private final WorkspaceManager workspaceManager;
	private final BooleanProperty active = new SimpleBooleanProperty();
	private final WorkspaceExplorerConfig explorerConfig;
	private IntermediateMappings mappings;
	private Callable<Boolean> pendingUpdate;

	@Inject
	public MappingProgressPane(@Nonnull CellConfigurationService configurationService,
							   @Nonnull WorkspaceExplorerConfig explorerConfig,
							   @Nonnull Instance<AggregateMappingManager> aggregateMappingManagerInstance,
							   @Nonnull WorkspaceManager workspaceManager) {
		this.workspaceManager = workspaceManager;
		this.configurationService = configurationService;
		this.explorerConfig = explorerConfig;

		// Create the tree-map pane
		TreeMapPane<TreeContent> treeMapPane = TreeMapPane.forTreeContent();
		PannableView treeMapWrapper = new PannableView(treeMapPane);

		// Create an initial dummy tree.
		treeContentList = FXCollections.observableArrayList();
		treeMapPane.valueListProperty().set(treeContentList);

		// When a workspace is opened, refresh the tree and listen for changes on the new workspace.
		workspaceManager.addWorkspaceOpenListener(workspace -> {
			int classes = workspace.findClasses(c -> true).size();

			treeMapPane.setPrefWidth(40 * classes);
			treeMapPane.setPrefHeight(5 * classes);
			treeMapWrapper.resetTranslation();
			treeMapWrapper.resetZoom();

			// When the mappings update, refresh the tree and mappings reference.
			aggregateMappingManagerInstance.get().addAggregatedMappingsListener(this::updateTreeAndMappings);

			// Initial outline
			updateTree();

			// Listen for updates
			workspace.getPrimaryResource().addListener(this);
		});

		workspaceManager.addWorkspaceCloseListener(workspace -> {
			// The pending update can be cleared once the workspace is closed.
			pendingUpdate = null;

			// And the tree content can be cleared.
			treeContentListDelegate.clear();

			// Clear selection, as it holds a path which contains workspace references,
			// which can prevent GC from freeing it.
			selectedPath.set(null);
		});

		// The tree update passes data to this delegate list.
		// We use ReactFX to merge rapid changes to limit redundant work.
		EventStreams.changesOf(treeContentListDelegate)
				.reduceSuccessions((change, change2) -> change2, Duration.ofMillis(500))
				.addObserver(c -> {
					FxThreadUtil.run(() -> treeContentList.setAll(treeContentListDelegate));
				});

		// When the metric measurement changes, update the tree.
		metric.addListener((ob, old, cur) -> updateTree());

		// Only update the UI when visible
		active.addListener((ob, old, cur) -> {
			Callable<Boolean> update = pendingUpdate;
			if (cur && update != null) {
				pendingUpdate = null;
				pool.submit(update);
			}
		});

		// Layout
		ComboBox<Metric> comboMetric = new ComboBox<>(FXCollections.observableArrayList(metrics));
		comboMetric.setConverter(ToStringConverter.from(metric -> metric.name().get()));
		metric.bind(comboMetric.getSelectionModel().selectedItemProperty());
		comboMetric.getSelectionModel().select(0);
		BorderPane wrapper = new BorderPane();
		wrapper.centerProperty().bind(selectedPath.map(selection -> {
			// No selection? Empty region.
			if (selection == null)
				return new Region();

			ClassPathNode path = selection.path();
			Label className = new Label(configurationService.textOf(path));

			boolean isNameMapped = OriginalClassNameProperty.get(path.getValue()) != null;
			Label nameIsMapped = new Label(String.format(" - %s", isNameMapped ? "class remapped" : "class not mapped"));
			Label fieldsMapped = new Label(String.format(" - %d/%d fields", selection.mappedFields, selection.fields));
			Label methodsMapped = new Label(String.format(" - %d/%d methods", selection.mappedMethods, selection.methods));
			if (isNameMapped)
				nameIsMapped.setTextFill(Color.GREEN.brighter());
			if (selection.mappedFields == selection.fields)
				fieldsMapped.setTextFill(Color.GREEN.brighter());
			if (selection.mappedMethods == selection.methods)
				methodsMapped.setTextFill(Color.GREEN.brighter());
			className.setGraphic(configurationService.graphicOf(path));
			className.getStyleClass().add(Styles.TITLE_4);

			return new VBox(className, nameIsMapped, fieldsMapped, methodsMapped);
		}));
		VBox legend = new VBox(
				makeLegendEntry("Unmapped", RED),
				makeLegendEntry("Partially mapped", MID),
				makeLegendEntry("Fully mapped", GREEN)
		);
		VBox rightContent = new VBox(legend, comboMetric);
		rightContent.setSpacing(4);
		wrapper.setRight(rightContent);
		wrapper.setPadding(new Insets(8));
		wrapper.setStyle("-fx-background-color: rgba(0, 0, 0, 0.2)");
		setCenter(treeMapWrapper);
		setBottom(wrapper);
	}

	@Nonnull
	private static Label makeLegendEntry(@Nonnull String text, @Nonnull Color color) {
		Rectangle rectangle = new Rectangle(10, 10);
		rectangle.setFill(color);
		rectangle.setStroke(Color.DARKGRAY);

		Label label = new Label(text);
		label.setGraphic(rectangle);
		return label;
	}

	@Override
	public void onNewClass(@Nonnull WorkspaceResource resource, @Nonnull AndroidClassBundle bundle, @Nonnull AndroidClassInfo cls) {
		updateTree();
	}

	@Override
	public void onUpdateClass(@Nonnull WorkspaceResource resource, @Nonnull AndroidClassBundle bundle, @Nonnull AndroidClassInfo oldCls, @Nonnull AndroidClassInfo newCls) {
		updateTree();
	}

	@Override
	public void onRemoveClass(@Nonnull WorkspaceResource resource, @Nonnull AndroidClassBundle bundle, @Nonnull AndroidClassInfo cls) {
		updateTree();
	}

	@Override
	public void onNewClass(@Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle, @Nonnull JvmClassInfo cls) {
		updateTree();
	}

	@Override
	public void onUpdateClass(@Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle, @Nonnull JvmClassInfo oldCls, @Nonnull JvmClassInfo newCls) {
		updateTree();
	}

	@Override
	public void onRemoveClass(@Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle, @Nonnull JvmClassInfo cls) {
		updateTree();
	}

	private void updateTreeAndMappings(@Nullable Mappings mappings) {
		this.mappings = mappings == null ? null : mappings.exportIntermediate();
		updateTree();
	}

	private void updateTree() {
		if (workspaceManager.hasCurrentWorkspace()) {
			Workspace workspace = workspaceManager.getCurrent();
			Callable<Boolean> action = () -> treeContentListDelegate.setAll(buildTreeRoots(workspaceToTree(workspace)));
			if (active.get())
				pool.submit(action);
			else
				pendingUpdate = action;
		}
	}

	@Nonnull
	private Tree<String, ClassPathNode> workspaceToTree(@Nonnull Workspace workspace) {
		Tree<String, ClassPathNode> tree = new SortedTreeImpl<>();
		workspace.findClasses(classInfo -> true)
				.forEach(classPath -> {
					// Only match classes in the primary resource
					WorkspaceResource resource = classPath.getValueOfType(WorkspaceResource.class);
					if (resource != workspace.getPrimaryResource())
						return;

					ClassInfo info = classPath.getValue();
					String name = info.getName();
					String[] sections = name.split("/");
					int maxSplit = explorerConfig.getMaxTreeDirectoryDepth();
					if (sections.length > maxSplit) {
						name = StringUtil.cutOffAtNth(name, '/', maxSplit) + "/" + StringUtil.shortenPath(name);
						sections = name.split("/");
					}

					Tree<String, ClassPathNode> path = tree;
					for (int i = 0; i < sections.length; i++) {
						String section = sections[i];
						if (i == sections.length - 1)
							path = path.computeIfAbsent(section, s -> new SortedTreeImpl<>(classPath));
						else
							path = path.computeIfAbsent(section, s -> new SortedTreeImpl<>());
					}
				});
		return tree;
	}

	@Nonnull
	private List<TreeContent> buildTreeRoots(@Nonnull Tree<String, ClassPathNode> tree) {
		if (tree.isBranch()) {
			ObservableList<TreeContent> list = FXCollections.observableArrayList();
			for (Tree<String, ClassPathNode> subtree : tree.values())
				list.addAll(buildTreeRoots(subtree));
			return Collections.singletonList(new SimpleHierarchicalTreeContent(new SimpleListProperty<>(list)));
		} else {
			ClassPathNode value = tree.getValue();
			if (value == null)
				return Collections.emptyList();
			return Collections.singletonList(toContent(value));
		}
	}

	@Nonnull
	private TreeContent toContent(@Nonnull ClassPathNode value) {
		return new ContextualClassTreeContent(value);
	}

	/**
	 * @return Property controlling UI updates.
	 */
	@Nonnull
	public BooleanProperty activeProperty() {
		return active;
	}

	/**
	 * Metric base.
	 */
	private static class Metric {
		private final StringBinding name;
		private final ToDoubleFunction<ClassPathNode> func;

		protected Metric(@Nonnull String name, @Nonnull ToDoubleFunction<ClassPathNode> func) {
			this.name = Lang.getBinding(name);
			this.func = func;
		}

		/**
		 * @param node
		 * 		Input path to class.
		 *
		 * @return Weighted value of class, per the metric conversion function.
		 */
		public double toWeight(@Nonnull ClassPathNode node) {
			return func.applyAsDouble(node);
		}

		/**
		 * @return Name of the metric.
		 */
		@Nonnull
		public StringBinding name() {
			return name;
		}

		/**
		 * Metric for raw-size of classes.
		 */
		private static class MetricSize extends Metric {
			public MetricSize() {
				super("mapprog.metric.size", MetricSize::map);
			}

			private static double map(@Nonnull ClassPathNode path) {
				double weight;
				ClassInfo info = path.getValue();
				if (info instanceof JvmClassInfo jvmClassInfo) {
					weight = jvmClassInfo.getBytecode().length;
				} else {
					// TODO: Compute rough android size (can estimate based on insn count and such)
					weight = 1;
				}
				return weight;
			}
		}

		/**
		 * Metric for number of members in classes.
		 */
		private static class MetricMemberCount extends Metric {
			public MetricMemberCount() {
				super("mapprog.metric.membercount", MetricMemberCount::map);
			}

			private static double map(@Nonnull ClassPathNode path) {
				ClassInfo info = path.getValue();
				return info.getMethods().size() + info.getFields().size();
			}
		}
	}

	/**
	 * Contextual tree-content allowing context-actions to be used on the display.
	 * <br>
	 * The mapped content held by each content item transitions from red to green with more mapping progress.
	 */
	private class ContextualClassTreeContent implements TreeContent, ContextSource {
		private final ClassPathNode path;
		private final double weight;
		private Node node;

		public ContextualClassTreeContent(@Nonnull ClassPathNode path) {
			this.path = path;
			this.weight = metric.get().toWeight(path);
		}

		@Override
		public boolean isDeclaration() {
			return false;
		}

		@Override
		public boolean isReference() {
			return true;
		}

		@Override
		public double getValueWeight() {
			return weight;
		}

		@Nonnull
		@Override
		public Node getNode() {
			if (node == null) {
				ClassInfo classInfo = path.getValue();
				String text = configurationService.textOf(path);
				Node graphic = configurationService.graphicOf(path);
				ContextMenu contextMenu = configurationService.contextMenuOf(this, path);
				Label label = new Label(text);
				label.setGraphic(graphic);
				label.setMouseTransparent(true);
				Effect effect = new Glow();
				BorderPane wrapper = new BorderPane(label);
				wrapper.setOnContextMenuRequested(e -> contextMenu.show(wrapper, e.getScreenX(), e.getScreenY()));
				wrapper.setOnMouseExited(e -> wrapper.setEffect(null));

				// Track number of mapped members
				int fields = 0;
				int mappedFields = 0;
				for (FieldMember field : classInfo.getFields()) {
					fields++;
					if (mappings != null && mappings.getMappedFieldName(classInfo, field) != null)
						mappedFields++;
				}
				int methods = 0;
				int mappedMethods = 0;
				for (MethodMember method : classInfo.getMethods()) {
					methods++;
					if (mappings != null && mappings.getMappedMethodName(classInfo, method) != null)
						mappedMethods++;
				}

				boolean hasMappedRefs = HasMappedReferenceProperty.get(classInfo);
				if (hasMappedRefs) {
					boolean isClassMapped = OriginalClassNameProperty.get(classInfo) != null;
					int total = fields + methods;
					int mapped = mappedFields + mappedMethods;

					// Percent calc:
					//  50% from member mappings
					//  25% from class being mapped
					//  25% for any reference being mapped
					float membersMappedContrib = total == 0 ? 0.5F : (mapped / (float) total) * 0.5F;
					float nameMappedContrib = (isClassMapped ? 0.25F : 0);
					float percentMapped = 0.25F + membersMappedContrib + nameMappedContrib;
					Color interpolatedColor = Colors.interpolateHsb(RED, GREEN, percentMapped);
					wrapper.setStyle("-fx-background-color: rgb(%d, %d, %d); -fx-border-color: black; -fx-border-width: 1px".formatted(
							(int) (interpolatedColor.getRed() * 255),
							(int) (interpolatedColor.getGreen() * 255),
							(int) (interpolatedColor.getBlue() * 255)
					));
				} else {
					// Not touched by any mapping
					wrapper.setStyle("-fx-background-color: rgb(80, 0, 0); -fx-border-color: black; -fx-border-width: 1px");
				}
				SelectionInfo selectionInfo = new SelectionInfo(path, fields, mappedFields, methods, mappedMethods);
				wrapper.setOnMouseEntered(e -> {
					wrapper.setEffect(effect);
					selectedPath.set(selectionInfo);
				});
				node = wrapper;
			}
			return node;
		}
	}

	/**
	 * Wrapper of what is selected.
	 *
	 * @param path
	 * 		Selected content.
	 * @param fields
	 * 		Fields declared.
	 * @param mappedFields
	 * 		Fields mapped.
	 * @param methods
	 * 		Methods declared.
	 * @param mappedMethods
	 * 		Methods mapped.
	 */
	private record SelectionInfo(ClassPathNode path, int fields, int mappedFields, int methods, int mappedMethods) {

	}
}
