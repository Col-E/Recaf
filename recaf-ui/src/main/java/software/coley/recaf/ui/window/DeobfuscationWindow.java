package software.coley.recaf.ui.window;

import atlantafx.base.controls.Spacer;
import atlantafx.base.theme.Styles;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import org.slf4j.Logger;
import software.coley.collections.Unchecked;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.services.cell.CellConfigurationService;
import software.coley.recaf.services.decompile.DecompilerManager;
import software.coley.recaf.services.deobfuscation.transform.generic.CycleClassRemovingTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.DeadCodeRemovingTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.DuplicateAnnotationRemovingTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.DuplicateCatchMergingTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.EnumNameRestorationTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.GotoInliningTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.IllegalAnnotationRemovingTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.IllegalSignatureRemovingTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.IllegalVarargsRemovingTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.KotlinNameRestorationTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.LinearOpaqueConstantFoldingTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.LongAnnotationRemovingTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.OpaquePredicateFoldingTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.RedundantTryCatchRemovingTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.SourceNameRestorationTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.StackOperationFoldingTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.StaticValueInliningTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.VariableFoldingTransformer;
import software.coley.recaf.services.info.association.FileTypeSyntaxAssociationService;
import software.coley.recaf.services.navigation.Actions;
import software.coley.recaf.services.transform.ClassTransformer;
import software.coley.recaf.services.transform.JvmClassTransformer;
import software.coley.recaf.services.transform.JvmTransformResult;
import software.coley.recaf.services.transform.TransformationApplier;
import software.coley.recaf.services.transform.TransformationApplierService;
import software.coley.recaf.services.transform.TransformationException;
import software.coley.recaf.services.transform.TransformationManager;
import software.coley.recaf.services.workspace.WorkspaceManager;
import software.coley.recaf.ui.config.WorkspaceExplorerConfig;
import software.coley.recaf.ui.control.ActionButton;
import software.coley.recaf.ui.control.BoundLabel;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.ui.control.ReorderableListCell;
import software.coley.recaf.ui.control.popup.ClassSelectionPopup;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.ui.control.richtext.search.SearchBar;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.Lang;
import software.coley.recaf.util.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Pane for previewing deobfuscation transformers.
 *
 * @author Matt Coley
 */
@Dependent
public class DeobfuscationWindow extends RecafStage {
	private static final Logger logger = Logging.get(DeobfuscationWindow.class);
	private final TransformationManager transformationManager;
	private final TransformationApplierService transformationApplierService;
	private final DecompilerManager decompilerManager;
	private final ObservableList<CategorizedTransformer> transformerOrder = FXCollections.observableArrayList();
	private final BooleanProperty hasSelection = new SimpleBooleanProperty();
	private final WorkspaceManager workspaceManager;

	@Inject
	@SuppressWarnings("unchecked")
	public DeobfuscationWindow(@Nonnull TransformationManager transformationManager,
	                           @Nonnull TransformationApplierService transformationApplierService,
	                           @Nonnull WorkspaceManager workspaceManager,
	                           @Nonnull DecompilerManager decompilerManager,
	                           @Nonnull FileTypeSyntaxAssociationService languageAssociation,
	                           @Nonnull CellConfigurationService configurationService,
	                           @Nonnull Actions actions,
	                           @Nonnull WorkspaceExplorerConfig explorerConfig,
	                           @Nonnull Instance<SearchBar> searchBarProvider) {
		this.transformationManager = transformationManager;
		this.transformationApplierService = transformationApplierService;
		this.workspaceManager = workspaceManager;
		this.decompilerManager = decompilerManager;

		BorderPane transformerTreePane = new BorderPane();
		{
			BoundLabel title = new BoundLabel(Lang.getBinding("deobf.selection.title"));
			title.getStyleClass().add(Styles.TITLE_4);
			title.setPadding(new Insets(0, 0, 5, 5));

			// TODO: It would be nice to have the tree be auto-generated in some fashion.
			//  But we may not want a flat category system, so an enum probably is too short-sighted.
			//  It also shouldn't be annoying to configure for each transformer.
			TreeItem<Selection> root = new TreeItem<>(new Selection.Root());
			TreeItem<Selection> generic = new TreeItem<>(new Selection.Category("deobf.tree.generic", CarbonIcons.CIRCLE_DASH));
			TreeItem<Selection> anti = new TreeItem<>(new Selection.Category("deobf.tree.generic.anticrasher", CarbonIcons.DEBUG));
			anti.getChildren().addAll(of(
					CycleClassRemovingTransformer.class,
					DuplicateAnnotationRemovingTransformer.class,
					LongAnnotationRemovingTransformer.class,
					IllegalAnnotationRemovingTransformer.class,
					IllegalSignatureRemovingTransformer.class,
					IllegalVarargsRemovingTransformer.class
			));
			TreeItem<Selection> optimize = new TreeItem<>(new Selection.Category("deobf.tree.generic.optimize", CarbonIcons.CLEAN));
			optimize.getChildren().addAll(of(
					DeadCodeRemovingTransformer.class,
					DuplicateCatchMergingTransformer.class,
					GotoInliningTransformer.class,
					LinearOpaqueConstantFoldingTransformer.class,
					OpaquePredicateFoldingTransformer.class,
					RedundantTryCatchRemovingTransformer.class,
					StackOperationFoldingTransformer.class,
					StaticValueInliningTransformer.class,
					VariableFoldingTransformer.class
			));
			TreeItem<Selection> restoration = new TreeItem<>(new Selection.Category("deobf.tree.generic.restoration", CarbonIcons.AI_RESULTS));
			restoration.getChildren().addAll(of(
					EnumNameRestorationTransformer.class,
					KotlinNameRestorationTransformer.class,
					SourceNameRestorationTransformer.class
			));
			generic.getChildren().addAll(
					anti,
					optimize,
					restoration
			);
			TreeItem<Selection> specific = new TreeItem<>(new Selection.Category("deobf.tree.specific", CarbonIcons.CENTER_CIRCLE));
			root.getChildren().addAll(
					generic,
					specific
			);
			generic.setExpanded(true);
			specific.setExpanded(true);

			TreeView<Selection> transformerTree = new TreeView<>();
			transformerTree.setCellFactory(view -> new TreeCell<>() {
				protected void updateItem(Selection item, boolean isEmpty) {
					super.updateItem(item, isEmpty);

					if (item == null || isEmpty) {
						setText(null);
						setGraphic(null);
						return;
					}

					switch (item) {
						case Selection.Root ir -> {
							// Not intended to be shown.
							setText(null);
							setGraphic(null);
						}
						case Selection.Category ic -> {
							setText(Lang.get(ic.key));
							setGraphic(new FontIconView(ic.icon));
						}
						case Selection.Transformer it -> {
							ClassTransformer transformer = it.transformer();
							Class<? extends ClassTransformer> transformerClass = transformer.getClass();
							CheckBox toggle = new CheckBox();
							toggle.setSelected(transformerOrder.stream().anyMatch(i -> i.matches(transformerClass)));
							toggle.selectedProperty().addListener((ob, old, cur) -> {
								if (cur) {
									Selection.Category parentCategory = Unchecked.cast(getTreeItem().getParent().getValue());
									transformerOrder.add(new CategorizedTransformer(it, parentCategory));
								} else {
									transformerOrder.removeIf(i -> i.matches(transformerClass));
								}
								hasSelection.set(!transformerOrder.isEmpty());
							});
							setText(transformer.name());
							setGraphic(toggle);
						}
					}
				}
			});
			transformerTree.setRoot(root);
			transformerTree.setShowRoot(false);
			transformerTreePane.setTop(title);
			transformerTreePane.setCenter(transformerTree);

			// TODO: Having the ability to load/save presets would be nice.
			//  - We should offer a basic preset with *everything* turned on but with the most optimal order pre-defined
		}
		BorderPane transformerOrderPane = new BorderPane();
		{
			BoundLabel title = new BoundLabel(Lang.getBinding("deobf.order.title"));
			title.getStyleClass().add(Styles.TITLE_4);
			title.setPadding(new Insets(0, 0, 5, 5));

			BoundLabel hint = new BoundLabel(Lang.getBinding("deobf.order.hint"));
			hint.setTextAlignment(TextAlignment.CENTER);
			ListView<CategorizedTransformer> transformerOrderList = new ListView<>(transformerOrder);
			transformerOrderList.setCellFactory(view -> new ReorderableListCell<>() {
				@Override
				public void updateIndex(int newIndex) {
					super.updateIndex(newIndex);

					// Refresh the UI so recommendation labels are updated.
					updateItem(getItem(), isEmpty());
				}

				@Override
				protected void updateItem(CategorizedTransformer item, boolean isEmpty) {
					super.updateItem(item, isEmpty);

					if (item == null || isEmpty) {
						setText(null);
						setGraphic(null);
					} else {
						// TODO: If we add "CompositeTransformer" to our system (a transformer that wraps several others in a specific order)
						//  then this logic will need to be updated to "unroll" those wrapped transformers.
						int index = getIndex();
						List<? extends Class<? extends ClassTransformer>> transformerClasses = transformerOrder.stream()
								.map(t -> t.transformer().transformer().getClass())
								.toList();
						ClassTransformer transformer = item.transformer().transformer();
						List<ClassTransformer> missingPredecessors = Unchecked.cast(transformer.recommendedPredecessors().stream()
								.filter(pre -> {
									int i = transformerClasses.indexOf(pre);
									return i < 0 || i > index;
								})
								.map(pre -> {
									try {
										if (JvmClassTransformer.class.isAssignableFrom(pre))
											return transformationManager.newJvmTransformer(Unchecked.cast(pre));
									} catch (TransformationException e) {
										logger.error("Failed to initialize instance of {}", pre, e);
									}
									return null;
								})
								.filter(Objects::nonNull)
								.toList());
						List<ClassTransformer> missingSuccessors = Unchecked.cast(transformer.recommendedSuccessors().stream()
								.filter(post -> {
									int i = transformerClasses.indexOf(post);
									return i < 0 || i < index;
								})
								.map(post -> {
									try {
										if (JvmClassTransformer.class.isAssignableFrom(post))
											return transformationManager.newJvmTransformer(Unchecked.cast(post));
									} catch (TransformationException e) {
										logger.error("Failed to initialize instance of {}", post, e);
									}
									return null;
								})
								.filter(Objects::nonNull)
								.toList());

						Label name = new Label(item.transformer().name(), new FontIconView(item.category().icon));
						BorderPane wrapper = new BorderPane();
						VBox missing = new VBox(name);
						missing.setPadding(new Insets(10));
						missing.setSpacing(10);
						missing.setAlignment(Pos.CENTER_LEFT);
						if (!missingPredecessors.isEmpty()) {
							String missingTransformers = Lang.get("deobf.order.pre") + ":\n - " + missingPredecessors.stream().map(ClassTransformer::name).collect(Collectors.joining("\n - "));
							Label missingLabel = new Label(missingTransformers);
							missingLabel.setPadding(new Insets(0, 0, 0, 20));
							missingLabel.getStyleClass().add(Styles.TEXT_SUBTLE);
							missing.getChildren().add(missingLabel);
						}
						if (!missingSuccessors.isEmpty()) {
							String missingTransformers = Lang.get("deobf.order.suc") + ":\n - " + missingSuccessors.stream().map(ClassTransformer::name).collect(Collectors.joining("\n - "));
							Label missingLabel = new Label(missingTransformers);
							missingLabel.setPadding(new Insets(0, 0, 0, 20));
							missingLabel.getStyleClass().add(Styles.TEXT_SUBTLE);
							missing.getChildren().add(missingLabel);
						}
						wrapper.setBottom(missing);
						setGraphic(wrapper);
					}
				}
			});
			StackPane wrapper = new StackPane(transformerOrderList, hint);
			wrapper.setAlignment(Pos.CENTER);
			transformerOrderPane.setTop(title);
			transformerOrderPane.setCenter(wrapper);
			hint.visibleProperty().bind(hasSelection.not());
		}
		BorderPane transformPreviewPane = new BorderPane();
		{
			BoundLabel title = new BoundLabel(Lang.getBinding("deobf.preview.title"));
			title.getStyleClass().add(Styles.TITLE_4);
			title.setPadding(new Insets(0, 0, 5, 5));

			Tab beforeTab = new Tab();
			Tab afterTab = new Tab();
			beforeTab.setClosable(false);
			afterTab.setClosable(false);
			TransformPreview beforePreview = new TransformPreview(languageAssociation, searchBarProvider.get(), false);
			TransformPreview afterPreview = new TransformPreview(languageAssociation, searchBarProvider.get(), true);
			beforeTab.setContent(beforePreview);
			afterTab.setContent(afterPreview);
			beforeTab.textProperty().bind(Lang.getBinding("misc.before"));
			afterTab.textProperty().bind(Lang.getBinding("misc.after"));
			beforeTab.setGraphic(new FontIconView(CarbonIcons.LICENSE));
			afterTab.setGraphic(new FontIconView(CarbonIcons.LICENSE_MAINTENANCE));
			TabPane tabs = new TabPane(beforeTab, afterTab);
			tabs.getSelectionModel().select(afterTab);

			Button pickClass = new ActionButton(CarbonIcons.ADD, Lang.getBinding("deobf.preview.pick"), () -> {
				new ClassSelectionPopup(actions, configurationService, explorerConfig, workspaceManager.getCurrent(), path -> {
					ClassInfo selection = path.getValue();

					beforePreview.setClassInfo(selection);
					afterPreview.setClassInfo(selection);

					beforePreview.decompile();
					afterPreview.decompile();
				}).showAndWait();
			});
			BooleanProperty working = new SimpleBooleanProperty();
			Button applyToWorkspace = new ActionButton(CarbonIcons.PLAY, Lang.getBinding("deobf.apply"), () -> {
				TransformationApplier applier = transformationApplierService.newApplierForCurrentWorkspace();
				if (applier == null)
					return;

				List<Class<? extends JvmClassTransformer>> list = Unchecked.cast(transformerOrder.stream()
						.map(c -> c.transformer().type())
						.filter(JvmClassTransformer.class::isAssignableFrom)
						.toList());
				try {
					working.set(true);
					JvmTransformResult result = applier.transformJvm(list);
					result.apply();
					FxThreadUtil.run(this::hide);
				} catch (TransformationException e) {
					// TODO: A tooltip or something showing would also be nice to have here since this is in a separate
					//  window which could mean the user cannot see the logging pane output.
					logger.error("Failed applying transformations to workspace", e);
				}
				working.set(false);
			}).async();
			applyToWorkspace.disableProperty().bind(hasSelection.not().or(working));
			transformerOrder.addListener((ListChangeListener<CategorizedTransformer>) change -> {
				beforePreview.decompile();
				afterPreview.decompile();
			});

			HBox tools = new HBox(pickClass, new Spacer(), applyToWorkspace);
			StackPane wrapper = new StackPane(tabs, tools);
			StackPane.setMargin(tools, new Insets(20));
			wrapper.setAlignment(Pos.BOTTOM_RIGHT);
			transformPreviewPane.setTop(title);
			transformPreviewPane.setCenter(wrapper);
			transformPreviewPane.setBottom(tools);
		}

		SplitPane split = new SplitPane(transformerTreePane, transformerOrderPane, transformPreviewPane);
		SplitPane.setResizableWithParent(transformerTreePane, false);
		SplitPane.setResizableWithParent(transformerOrderPane, false);
		split.setDividerPositions(0.35, 0.7);
		split.setPadding(new Insets(4));
		transformerTreePane.setPadding(new Insets(4));
		transformerOrderPane.setPadding(new Insets(4));
		transformPreviewPane.setPadding(new Insets(4));

		// Window setup
		titleProperty().bind(Lang.getBinding("deobf"));
		setScene(new RecafScene(new BorderPane(split)));
		setWidth(900);
		setHeight(600);
	}

	@SuppressWarnings("unchecked")
	private List<TreeItem<Selection>> of(Class<? extends ClassTransformer>... transformerClasses) {
		List<TreeItem<Selection>> results = new ArrayList<>(transformerClasses.length);
		for (Class<? extends ClassTransformer> transformerClass : transformerClasses) {
			try {
				if (JvmClassTransformer.class.isAssignableFrom(transformerClass)) {
					Class<JvmClassTransformer> jvmTransformerClass = (Class<JvmClassTransformer>) transformerClass;
					JvmClassTransformer transformer = transformationManager.newJvmTransformer(jvmTransformerClass);
					results.add(new TreeItem<>(new Selection.Transformer(transformer)));
				}
				// TODO: Android transformer case when we get to implementing that system
			} catch (TransformationException e) {
				logger.error("Failed to initialize instance of {}", transformerClass, e);
			}
		}
		return results;
	}

	private class TransformPreview extends BorderPane {
		private final boolean andApply;
		private final Editor editor;
		private ClassInfo classInfo;

		private TransformPreview(@Nonnull FileTypeSyntaxAssociationService languageAssociation, @Nonnull SearchBar searchBar, boolean andApply) {
			this.andApply = andApply;

			editor = new Editor();
			languageAssociation.configureEditorSyntax("java", editor);
			searchBar.install(editor);
			setCenter(editor);

			decompile();
		}

		public void setClassInfo(@Nonnull ClassInfo classInfo) {
			this.classInfo = classInfo;
		}

		private void decompile() {
			if (classInfo == null) {
				editor.setText(Lang.get("deobf.preview.noselection"));
				return;
			}

			JvmClassInfo jvmClass = classInfo.asJvmClass();

			if (andApply) {
				List<Class<? extends JvmClassTransformer>> transformers = Unchecked.cast(transformerOrder.stream()
						.map(ct -> ct.transformer().type())
						.filter(JvmClassTransformer.class::isAssignableFrom)
						.toList());

				try {
					TransformationApplier applier = transformationApplierService.newApplierForCurrentWorkspace();
					if (applier == null)
						throw new TransformationException("No workspace is open");
					JvmTransformResult result = applier
							.transformJvm(transformers, (_, _, _, targetClass) -> targetClass.getName().equals(classInfo.getName()));
					if (!result.getTransformedClasses().isEmpty())
						jvmClass = result.getTransformedClasses().values().iterator().next();
				} catch (TransformationException e) {
					editor.setText("// Failed to transform: " + e.getMessage() + "\n"
							+ "// " + StringUtil.traceToString(e).replace("\n", "\n// "));
					return;
				}
			}

			// TODO: Pull out common decompile code with "AbstractDecompilePane" to do this more aesthetically
			//  - And by that I mean include the animation
			//  - And the error handling
			//  - But not the rest of the unrelated "editing" capabilities of the "AbstractDecompilePane"
			decompilerManager.decompile(workspaceManager.getCurrent(), jvmClass).whenCompleteAsync((result, error) -> {
				if (result != null) {
					editor.setText(result.getText());
				}
			}, FxThreadUtil.executor());
		}
	}

	private sealed interface Selection {
		record Root() implements Selection {}

		record Category(@Nonnull String key, @Nonnull Ikon icon) implements Selection {}

		record Transformer(@Nonnull ClassTransformer transformer) implements Selection {
			@Nonnull
			public String name() {
				return transformer().name();
			}

			@Nonnull
			public Class<? extends ClassTransformer> type() {
				return transformer.getClass();
			}
		}
	}

	private record CategorizedTransformer(@Nonnull Selection.Transformer transformer,
	                                      @Nonnull Selection.Category category) {
		public boolean matches(@Nonnull Class<? extends ClassTransformer> cls) {
			return transformer.type() == cls;
		}

		@Override
		public String toString() {
			return category().key() + ':' + transformer().type().getName();
		}
	}
}
