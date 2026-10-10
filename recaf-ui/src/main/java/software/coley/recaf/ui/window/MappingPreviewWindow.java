package software.coley.recaf.ui.window;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import software.coley.recaf.info.AndroidClassInfo;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.path.PathNodes;
import software.coley.recaf.services.cell.CellConfigurationService;
import software.coley.recaf.services.cell.context.ContextSource;
import software.coley.recaf.services.decompile.DecompileResult;
import software.coley.recaf.services.decompile.DecompilerManager;
import software.coley.recaf.services.info.association.FileTypeSyntaxAssociationService;
import software.coley.recaf.services.mapping.MappingResults;
import software.coley.recaf.services.navigation.Actions;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.ui.control.PathNodeTree;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.ui.control.richtext.bracket.SelectedBracketTracking;
import software.coley.recaf.ui.control.richtext.search.SearchBar;
import software.coley.recaf.ui.control.tree.TreeItems;
import software.coley.recaf.ui.control.tree.WorkspaceTreeCell;
import software.coley.recaf.ui.control.tree.WorkspaceTreeNode;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.Lang;
import software.coley.recaf.util.StringUtil;
import software.coley.recaf.workspace.model.Workspace;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Preview window for inspecting class changes produced by a mapping operation without applying them.
 */
@Dependent
public class MappingPreviewWindow extends RecafStage {
	private final CellConfigurationService configurationService;
	private final PathNodeTree classTree;
	private final PreviewPane beforePreview;
	private final PreviewPane afterPreview;
	private final Label noChangesLabel = new Label();
	private Workspace workspace;
	private MappingResults mappingResults;

	@Inject
	public MappingPreviewWindow(@Nonnull CellConfigurationService configurationService,
	                           @Nonnull DecompilerManager decompilerManager,
	                           @Nonnull FileTypeSyntaxAssociationService languageAssociation,
	                           @Nonnull Instance<SearchBar> searchBarProvider,
	                           @Nonnull Actions actions) {
		this.configurationService = configurationService;
		classTree = createClassTree(actions);
		beforePreview = new PreviewPane(decompilerManager, languageAssociation, searchBarProvider);
		afterPreview = new PreviewPane(decompilerManager, languageAssociation, searchBarProvider);

		Tab beforeTab = new Tab();
		Tab afterTab = new Tab();
		beforeTab.setClosable(false);
		afterTab.setClosable(false);
		beforeTab.textProperty().bind(Lang.getBinding("misc.before"));
		afterTab.textProperty().bind(Lang.getBinding("misc.after"));
		beforeTab.setGraphic(new FontIconView(CarbonIcons.LICENSE));
		afterTab.setGraphic(new FontIconView(CarbonIcons.LICENSE_MAINTENANCE));
		beforeTab.setContent(beforePreview);
		afterTab.setContent(afterPreview);
		TabPane previewTabs = new TabPane(beforeTab, afterTab);
		previewTabs.getSelectionModel().select(afterTab);

		classTree.getSelectionModel().selectedItemProperty().addListener((ob, old, cur) -> updatePreviews(cur));
		noChangesLabel.setText(Lang.get("mapapply.preview.no-changes"));
		noChangesLabel.setWrapText(true);
		noChangesLabel.setMouseTransparent(true);
		noChangesLabel.setOpacity(0.65);
		noChangesLabel.setVisible(false);
		StackPane treeContent = new StackPane(classTree, noChangesLabel);
		BorderPane treePane = new BorderPane(treeContent);
		treePane.setPrefWidth(320);
		SplitPane content = new SplitPane(treePane, previewTabs);
		content.setDividerPositions(0.3);
		content.getStyleClass().add("background");
		SplitPane.setResizableWithParent(treePane, false);

		titleProperty().bind(Lang.getBinding("mapapply.preview.title"));
		setMinWidth(800);
		setMinHeight(500);
		setScene(new RecafScene(content, 1100, 750));
		hideOnEscape();
	}

	/**
	 * Populates the class tree with the post-mapping results and selects the first modified class.
	 *
	 * @param workspace
	 * 		Workspace used to compute the results.
	 * @param results
	 * 		Non-applied mapping results.
	 */
	public void setMappingResults(@Nonnull Workspace workspace, @Nonnull MappingResults results) {
		this.workspace = workspace;
		mappingResults = results;

		WorkspaceTreeNode root = new WorkspaceTreeNode(PathNodes.workspacePath(workspace));
		List<ClassPathNode> modifiedPaths = results.getPostMappingPaths().values().stream()
				.sorted(Comparator.comparing(path -> path.getValue().getName()))
				.toList();
		for (ClassPathNode path : modifiedPaths)
			WorkspaceTreeNode.getOrInsertIntoTree(root, path);
		boolean hasModifiedClasses = !modifiedPaths.isEmpty();
		if (hasModifiedClasses)
			TreeItems.recurseOpen(root);
		classTree.setRoot(hasModifiedClasses ? root : null);
		noChangesLabel.setVisible(!hasModifiedClasses);

		if (hasModifiedClasses) {
			WorkspaceTreeNode firstNode = root.getNodeByPath(modifiedPaths.getFirst());
			if (firstNode != null)
				classTree.getSelectionModel().select(firstNode);
		} else {
			beforePreview.setPlaceholder(Lang.get("mapapply.preview.no-changes"));
			afterPreview.setPlaceholder(Lang.get("mapapply.preview.no-changes"));
		}
	}

	@Nonnull
	private PathNodeTree createClassTree(@Nonnull Actions actions) {
		PathNodeTree tree = new PathNodeTree(configurationService, actions) {
			@Override
			protected void handleEnter(@Nonnull Actions actions, @Nonnull TreeItem<PathNode<?>> selected) {
				if (!selected.isLeaf())
					super.handleEnter(actions, selected);
			}
		};
		tree.setCellFactory(param -> new WorkspaceTreeCell(ContextSource.REFERENCE, configurationService) {
			@Override
			protected void populate(@Nonnull PathNode<?> path) {
				configurationService.configureStyle(this, path);
				setGraphic(configurationService.graphicOf(path));
				setContextMenu(null);
				setOnMouseClicked(e -> configurationService.clickHandlerOf(this, path, false).handle(e));
				setText(configurationService.textOf(path));
			}
		});
		return tree;
	}

	private void updatePreviews(@Nullable TreeItem<PathNode<?>> selectedItem) {
		if (workspace == null || mappingResults == null || selectedItem == null || !(selectedItem.getValue() instanceof ClassPathNode postPath)) {
			beforePreview.setPlaceholder(Lang.get("mapapply.preview.noselection"));
			afterPreview.setPlaceholder(Lang.get("mapapply.preview.noselection"));
			return;
		}

		ClassPathNode prePath = mappingResults.getPreMappingPath(postPath.getValue().getName());
		if (prePath == null)
			beforePreview.setPlaceholder(Lang.get("mapapply.preview.before-unavailable"));
		else
			beforePreview.showClass(workspace, prePath.getValue());
		afterPreview.showClass(workspace, postPath.getValue());
	}

	/**
	 * Read-only class decompilation preview.
	 */
	private static class PreviewPane extends BorderPane {
		private final Editor editor = new Editor();
		private final DecompilerManager decompilerManager;
		private final AtomicInteger generation = new AtomicInteger();

		private PreviewPane(@Nonnull DecompilerManager decompilerManager,
		                    @Nonnull FileTypeSyntaxAssociationService languageAssociation,
		                    @Nonnull Instance<SearchBar> searchBarProvider) {
			this.decompilerManager = decompilerManager;
			editor.setSelectedBracketTracking(new SelectedBracketTracking());
			editor.getRootLineGraphicFactory().addDefaultCodeGraphicFactories();
			editor.getCodeArea().setEditable(false);
			languageAssociation.configureEditorSyntax("java", editor);
			searchBarProvider.get().install(editor);
			setCenter(editor);
			setPlaceholder(Lang.get("mapapply.preview.noselection"));
		}

		private void showClass(@Nonnull Workspace workspace, @Nonnull ClassInfo classInfo) {
			int currentGeneration = generation.incrementAndGet();
			setText("// " + classInfo.getName() + "\n// " + Lang.get("java.decompiling"));

			if (classInfo.isJvmClass()) {
				JvmClassInfo jvmClass = classInfo.asJvmClass();
				decompilerManager.decompile(workspace, jvmClass).whenCompleteAsync((result, error) ->
						showResult(currentGeneration, classInfo, result, error), FxThreadUtil.executor());
			} else if (classInfo.isAndroidClass()) {
				AndroidClassInfo androidClass = classInfo.asAndroidClass();
				decompilerManager.decompile(workspace, androidClass).whenCompleteAsync((result, error) ->
						showResult(currentGeneration, classInfo, result, error), FxThreadUtil.executor());
			} else {
				setText("// Unsupported class type: " + classInfo.getName());
			}
		}

		private void showResult(int expectedGeneration, @Nonnull ClassInfo classInfo,
		                        @Nullable DecompileResult result, @Nullable Throwable error) {
			if (expectedGeneration != generation.get())
				return;
			if (error != null) {
				setText("/*\nDecompilation failure\n" + StringUtil.traceToString(error) + "\n*/");
				return;
			}
			if (result == null) {
				setText("// No decompilation result for " + classInfo.getName());
				return;
			}

			String text = result.getText();
			switch (result.getType()) {
				case SUCCESS, SKIPPED -> setText(text == null ? "// No decompilation output" : text);
				case FAILURE -> {
					Throwable exception = result.getException();
					setText(exception == null ? "/*\nDecompilation failure\n*/"
							: "/*\nDecompilation failure\n" + StringUtil.traceToString(exception) + "\n*/");
				}
			}
		}

		private void setPlaceholder(@Nonnull String text) {
			generation.incrementAndGet();
			setText(text);
		}

		private void setText(@Nonnull String text) {
			editor.setText(text);
		}
	}
}
