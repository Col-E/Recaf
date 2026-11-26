package software.coley.recaf.ui.window;

import atlantafx.base.controls.Popover;
import atlantafx.base.controls.Spacer;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.beans.InvalidationListener;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
import org.fxmisc.flowless.Cell;
import org.fxmisc.flowless.VirtualFlow;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.kordamp.ikonli.carbonicons.CarbonIcons;
import org.slf4j.Logger;
import regexodus.Matcher;
import regexodus.Pattern;
import software.coley.collections.Unchecked;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.TextFileInfo;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.path.ClassMemberPathNode;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.DirectoryPathNode;
import software.coley.recaf.path.FilePathNode;
import software.coley.recaf.path.IncompletePathException;
import software.coley.recaf.path.LineNumberPathNode;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.services.cell.CellConfigurationService;
import software.coley.recaf.services.cell.context.ContextSource;
import software.coley.recaf.services.comment.ClassComments;
import software.coley.recaf.services.comment.CommentManager;
import software.coley.recaf.services.comment.DelegatingClassComments;
import software.coley.recaf.services.comment.WorkspaceComments;
import software.coley.recaf.services.navigation.Actions;
import software.coley.recaf.services.text.TextFormatConfig;
import software.coley.recaf.services.window.WindowManager;
import software.coley.recaf.services.workspace.WorkspaceCloseListener;
import software.coley.recaf.services.workspace.WorkspaceManager;
import software.coley.recaf.ui.control.AbstractSearchBar;
import software.coley.recaf.ui.control.BoundTab;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.Icons;
import software.coley.recaf.util.Lang;
import software.coley.recaf.util.RegexUtil;
import software.coley.recaf.util.StringUtil;
import software.coley.recaf.workspace.model.Workspace;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Window for quickly opening classes, fields, methods, files, and other supported content.
 *
 * @author Matt Coley
 */
@Dependent
public class QuickNavWindow extends AbstractIdentifiableStage {
	private static final Logger logger = Logging.get(QuickNavWindow.class);

	@Inject
	public QuickNavWindow(@Nonnull WorkspaceManager workspaceManager, @Nonnull CommentManager commentManager,
	                      @Nonnull Actions actions, @Nonnull TextFormatConfig formatConfig,
	                      @Nonnull CellConfigurationService configurationService) {
		super(WindowManager.WIN_QUICK_NAV);

		// Giant chunk of code, ought to clean this up later.
		Supplier<Stream<ClassPathNode>> classProvider = () -> {
			if (!workspaceManager.hasCurrentWorkspace())
				return Stream.empty();
			return workspaceManager.getCurrent().classesStream(false);
		};
		Function<ClassPathNode, String> classTextMapper = classPath -> classPath.getValue().getSimpleName();
		ContainmentMatcher<ClassPathNode> classTextMatcher = (classPath, search, caseSensitive) -> {
			String text = search.indexOf('/') >= 0 ?
					classPath.getValue().getName() :
					classPath.getValue().getSimpleName();
			if (!caseSensitive)
				text = text.toLowerCase();
			return text.contains(search);
		};
		BiConsumer<ListCell<?>, ClassPathNode> renderClass = (cell, classPath) -> {
			DirectoryPathNode packagePath = Objects.requireNonNull(classPath.getParent());
			String packageName = packagePath.getValue();
			packageName = formatConfig.filterEscape(packageName);
			packageName = formatConfig.filterMaxLength(packageName);

			Label classDisplay = new Label();
			configurationService.configureStyle(classDisplay, classPath);
			classDisplay.setText(configurationService.textOf(classPath));
			classDisplay.setGraphic(configurationService.graphicOf(classPath));

			Label packageDisplay = new Label();
			packageDisplay.setText(packageName);
			packageDisplay.setGraphic(Icons.getIconView(Icons.FOLDER_PACKAGE));
			packageDisplay.setOpacity(0.5);
			packageDisplay.setTextOverrun(OverrunStyle.CENTER_WORD_ELLIPSIS);

			Spacer spacer = new Spacer();
			HBox box = new HBox(classDisplay, spacer, packageDisplay);
			HBox.setHgrow(spacer, Priority.ALWAYS);
			cell.setText(null);
			cell.setGraphic(box);

			cell.setOnMouseClicked(configurationService.contextMenuHandlerOf(cell, classPath, ContextSource.REFERENCE));
		};
		Consumer<ListCell<ClassPathNode>> classRenderer = cell -> renderClass.accept(cell, cell.getItem());
		OneToOneContentPane<ClassPathNode> classContent = new OneToOneContentPane<>(actions, this,
				classProvider, classTextMapper, classTextMatcher, classRenderer);

		Supplier<Stream<ClassMemberPathNode>> memberProvider = () -> {
			if (!workspaceManager.hasCurrentWorkspace())
				return Stream.empty();
			return workspaceManager.getCurrent().classesStream(false).flatMap(p -> {
				ClassInfo classInfo = p.getValue();
				Stream<ClassMemberPathNode> fields = classInfo.getFields().stream().map(p::child);
				Stream<ClassMemberPathNode> methods = classInfo.getMethods().stream().map(p::child);
				return Stream.concat(fields, methods);
			});
		};
		Function<ClassMemberPathNode, String> memberTextMapper = classMemberPath -> classMemberPath.getValue().getName();
		BiConsumer<ListCell<?>, ClassMemberPathNode> renderMember = (cell, memberPath) -> {
			ClassPathNode classPath = Objects.requireNonNull(memberPath.getParent());

			Label memberDisplay = new Label();
			memberDisplay.setText(configurationService.textOf(memberPath));
			memberDisplay.setGraphic(configurationService.graphicOf(memberPath));

			Label classDisplay = new Label();
			classDisplay.setText(configurationService.textOf(classPath));
			classDisplay.setGraphic(configurationService.graphicOf(classPath));
			classDisplay.setOpacity(0.5);

			Spacer spacer = new Spacer();
			HBox box = new HBox(memberDisplay, spacer, classDisplay);
			HBox.setHgrow(spacer, Priority.ALWAYS);
			cell.setText(null);
			cell.setGraphic(box);

			cell.setOnMouseClicked(configurationService.contextMenuHandlerOf(cell, memberPath, ContextSource.REFERENCE));
		};
		Consumer<ListCell<ClassMemberPathNode>> memberRenderer = cell -> renderMember.accept(cell, cell.getItem());
		OneToOneContentPane<ClassMemberPathNode> memberContent = new OneToOneContentPane<>(actions, this,
				memberProvider, memberTextMapper, memberRenderer);

		Supplier<Stream<FilePathNode>> fileProvider = () -> {
			if (!workspaceManager.hasCurrentWorkspace())
				return Stream.empty();
			return workspaceManager.getCurrent().filesStream();
		};
		Function<FilePathNode, String> fileTextMapper = filePath -> filePath.getValue().getName();
		BiConsumer<ListCell<?>, FilePathNode> renderFile = (cell, filePath) -> {
			DirectoryPathNode directoryPath = Objects.requireNonNull(filePath.getParent());
			String directoryName = directoryPath.getValue();
			directoryName = formatConfig.filterEscape(directoryName);
			directoryName = formatConfig.filterMaxLength(directoryName);

			Label fileDisplay = new Label();
			fileDisplay.setText(configurationService.textOf(filePath));
			fileDisplay.setGraphic(configurationService.graphicOf(filePath));

			Label packageDisplay = new Label();
			packageDisplay.setText(directoryName);
			packageDisplay.setGraphic(Icons.getIconView(Icons.FOLDER_RES));
			packageDisplay.setOpacity(0.5);
			packageDisplay.setTextOverrun(OverrunStyle.CENTER_WORD_ELLIPSIS);

			Spacer spacer = new Spacer();
			HBox box = new HBox(fileDisplay, spacer, packageDisplay);
			HBox.setHgrow(spacer, Priority.ALWAYS);
			cell.setText(null);
			cell.setGraphic(box);

			cell.setOnMouseClicked(configurationService.contextMenuHandlerOf(cell, filePath, ContextSource.REFERENCE));
		};
		Consumer<ListCell<FilePathNode>> fileRenderer = cell -> renderFile.accept(cell, cell.getItem());
		OneToOneContentPane<FilePathNode> fileContent = new OneToOneContentPane<>(actions, this,
				fileProvider, fileTextMapper, fileRenderer);

		Supplier<Stream<FilePathNode>> textProvider = () -> {
			if (!workspaceManager.hasCurrentWorkspace())
				return Stream.empty();
			return workspaceManager.getCurrent().filesStream()
					.filter(f -> f.getValue().isTextFile());
		};
		Function<FilePathNode, Stream<LineNumberPathNode>> lineUnroller = filePath -> {
			TextFileInfo textFile = filePath.getValue().asTextFile();
			int lineCount = StringUtil.count('\n', textFile.getText());
			return IntStream.rangeClosed(1, lineCount).mapToObj(filePath::child);
		};
		Function<LineNumberPathNode, String> lineTextMapper = lineNumberPath -> {
			int index = lineNumberPath.getValue().intValue() - 1;
			String[] lines = lineNumberPath.getParent().getValue().asTextFile().getTextLines();
			return lines[index];
		};
		BiConsumer<ListCell<?>, LineNumberPathNode> renderLine = (cell, linePath) -> {
			FilePathNode filePath = linePath.getParent();
			TextFileInfo textFile = filePath.getValue().asTextFile();
			int line = linePath.getValue().intValue();

			Label fileDisplay = new Label();
			fileDisplay.setText(configurationService.textOf(filePath) + ":" + line);
			fileDisplay.setGraphic(configurationService.graphicOf(filePath));

			Label textDisplay = new Label();
			textDisplay.setText(textFile.getTextLines()[line - 1]);
			textDisplay.setOpacity(0.5);
			textDisplay.setTextOverrun(OverrunStyle.CENTER_WORD_ELLIPSIS);

			Spacer spacer = new Spacer();
			HBox box = new HBox(fileDisplay, spacer, textDisplay);
			HBox.setHgrow(spacer, Priority.ALWAYS);
			cell.setText(null);
			cell.setGraphic(box);

			cell.setOnMouseClicked(configurationService.contextMenuHandlerOf(cell, filePath, ContextSource.REFERENCE));
		};
		Consumer<ListCell<LineNumberPathNode>> lineRenderer = cell -> renderLine.accept(cell, cell.getItem());
		OneToManyContentPane<FilePathNode, LineNumberPathNode> textContent = new OneToManyContentPane<>(actions, this,
				textProvider, lineUnroller, lineTextMapper, lineRenderer);

		Supplier<Stream<PathNode<?>>> commentProvider = () -> {
			if (!workspaceManager.hasCurrentWorkspace())
				return Stream.empty();

			WorkspaceComments comments = commentManager.getCurrentWorkspaceComments();
			if (comments == null)
				return Stream.empty();

			List<PathNode<?>> paths = new ArrayList<>();
			for (ClassComments classComments : comments) {
				if (classComments instanceof DelegatingClassComments delegatingClassComments) {
					// Add class comment
					ClassPathNode classPath = delegatingClassComments.getPath();
					if (classComments.getClassComment() != null)
						paths.add(classPath);

					// Add field/method comments
					ClassInfo classInfo = classPath.getValue();
					for (FieldMember field : classInfo.getFields())
						if (classComments.getFieldComment(field) != null)
							paths.add(classPath.child(field));
					for (MethodMember method : classInfo.getMethods())
						if (classComments.getMethodComment(method) != null)
							paths.add(classPath.child(method));
				}
			}

			return paths.stream();
		};
		Function<PathNode<?>, String> commentTextMapper = path -> {
			WorkspaceComments comments = commentManager.getCurrentWorkspaceComments();
			if (comments == null)
				return null;
			return comments.getComment(path);
		};
		BiConsumer<ListCell<?>, PathNode<?>> renderComment = (cell, path) -> {
			WorkspaceComments comments = commentManager.getCurrentWorkspaceComments();
			String comment = (comments == null ? "" : comments.getComment(path));
			if (comment == null)
				comment = "";
			else
				comment = formatConfig.filterMaxLength(comment.replace('\n', ' '));

			Label pathDisplay = new Label();
			pathDisplay.setText(configurationService.textOf(path));
			pathDisplay.setGraphic(configurationService.graphicOf(path));

			Label commentDisplay = new Label();
			commentDisplay.setText(comment);
			commentDisplay.setOpacity(0.7);

			Spacer spacer = new Spacer();
			HBox box = new HBox(pathDisplay, spacer, commentDisplay);
			HBox.setHgrow(spacer, Priority.ALWAYS);
			cell.setText(null);
			cell.setGraphic(box);

			cell.setOnMouseClicked(configurationService.contextMenuHandlerOf(cell, path, ContextSource.REFERENCE));
		};
		Consumer<ListCell<PathNode<?>>> commentRenderer = cell -> renderComment.accept(cell, cell.getItem());
		OneToOneContentPane<PathNode<?>> commentContent = new OneToOneContentPane<>(actions, this,
				commentProvider, commentTextMapper, commentRenderer);

		AllContentPane allContent = new AllContentPane(actions, this, () -> Stream.of(
				classProvider.get().map(path -> new QuickNavResult(QuickNavResultType.CLASS, path, classTextMapper.apply(path))),
				memberProvider.get().map(path -> new QuickNavResult(QuickNavResultType.MEMBER, path, memberTextMapper.apply(path))),
				fileProvider.get().map(path -> new QuickNavResult(QuickNavResultType.FILE, path, fileTextMapper.apply(path))),
				textProvider.get().flatMap(path -> lineUnroller.apply(path)
						.map(linePath -> new QuickNavResult(QuickNavResultType.TEXT, linePath, lineTextMapper.apply(linePath)))),
				commentProvider.get().map(path -> new QuickNavResult(QuickNavResultType.COMMENT, path, commentTextMapper.apply(path)))
		).flatMap(Function.identity()), cell -> {
			QuickNavResult result = cell.getItem();
			switch (result.type()) {
				case CLASS -> renderClass.accept(cell, (ClassPathNode) result.path());
				case MEMBER -> renderMember.accept(cell, (ClassMemberPathNode) result.path());
				case FILE -> renderFile.accept(cell, (FilePathNode) result.path());
				case TEXT -> renderLine.accept(cell, (LineNumberPathNode) result.path());
				case COMMENT -> renderComment.accept(cell, result.path());
			}
		}, (result, search, caseSensitive) -> {
			if (result.type() == QuickNavResultType.CLASS)
				return classTextMatcher.matches((ClassPathNode) result.path(), search, caseSensitive);

			String text = result.text();
			if (!caseSensitive && text != null)
				text = text.toLowerCase();
			return contains(text, search);
		});
		List<ContentPaneBase> contentPanes = List.of(allContent, classContent, memberContent, fileContent, textContent, commentContent);
		contentPanes.forEach(workspaceManager::addWorkspaceCloseListener);

		BoundTab tabAll = new BoundTab(Lang.getBinding("dialog.quicknav.tab.all"), new FontIconView(CarbonIcons.SEARCH), allContent);
		BoundTab tabClasses = new BoundTab(Lang.getBinding("dialog.quicknav.tab.classes"), Icons.getIconView(Icons.CLASS), classContent);
		BoundTab tabMembers = new BoundTab(Lang.getBinding("dialog.quicknav.tab.members"), Icons.getIconView(Icons.FIELD_N_METHOD), memberContent);
		BoundTab tabFiles = new BoundTab(Lang.getBinding("dialog.quicknav.tab.files"), new FontIconView(CarbonIcons.DOCUMENT), fileContent);
		BoundTab tabText = new BoundTab(Lang.getBinding("dialog.quicknav.tab.text"), new FontIconView(CarbonIcons.STRING_TEXT), textContent);
		BoundTab tabCommented = new BoundTab(Lang.getBinding("dialog.quicknav.tab.commented"), new FontIconView(CarbonIcons.CHAT), commentContent);

		TabPane tabs = new TabPane();
		tabs.getTabs().addAll(tabAll, tabClasses, tabMembers, tabFiles, tabText, tabCommented);
		tabs.getTabs().forEach(tab -> tab.setClosable(false));

		// Add event filter to handle closing the window when escape is pressed.
		hideOnEscape();

		// And event filter to tabs to handle "a-z" and "0-9" delegating to current displayed search input field.
		for (Tab tab : tabs.getTabs()) {
			tabs.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
				if (!tab.isSelected() || e.getTarget() != tabs)
					return;
				KeyCode code = e.getCode();
				if ((code.isLetterKey() || code.isDigitKey()) && tab.getContent() instanceof ContentPaneBase content)
					content.focusSearchBar();
			});
		}

		// Layout
		titleProperty().bind(Lang.getBinding("dialog.quicknav"));
		setMinWidth(300);
		setMinHeight(300);
		setScene(new RecafScene(tabs, 750, 550));
	}

	private static boolean contains(@Nullable String text, @Nonnull String search) {
		return text != null && text.contains(search);
	}

	/**
	 * Base for displaying the search bar + results pane for some content type.
	 */
	private static class ContentPaneBase extends BorderPane implements WorkspaceCloseListener {
		protected final PathResultsPane<?> results;
		protected AbstractSearchBar searchBar;

		protected ContentPaneBase(@Nonnull PathResultsPane<?> results) {
			this.results = results;
		}

		protected void focusSearchBar() {
			if (searchBar != null)
				searchBar.requestSearchFocus();
		}

		protected void setSearchBar(@Nonnull AbstractSearchBar searchBar) {
			this.searchBar = searchBar;
			setTop(searchBar);

			// Register event filter which will allow jumping from the search bar to other controls.
			// - UP ----> Select the containing tab-pane so users can switch tabs.
			// - DOWN --> Select the results so the user can navigate through results.
			searchBar.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
				if (e.getCode() == KeyCode.UP)
					getParent().requestFocus();
				else if (e.getCode() == KeyCode.DOWN)
					results.selectCell();
			});
		}

		@Override
		public void onWorkspaceClosed(@Nonnull Workspace workspace) {
			FxThreadUtil.run(results.list::clear);
		}
	}

	/**
	 * Quick-nav result type for the combined tab.
	 */
	private enum QuickNavResultType {
		CLASS,
		MEMBER,
		FILE,
		TEXT,
		COMMENT
	}

	/**
	 * Quick-nav result wrapper for the combined tab.
	 *
	 * @param type
	 * 		Result category.
	 * @param path
	 * 		Path to navigate to.
	 * @param text
	 * 		Text to search against.
	 */
	private record QuickNavResult(@Nonnull QuickNavResultType type,
	                              @Nonnull PathNode<?> path,
	                              @Nullable String text) implements Comparable<QuickNavResult> {
		@Override
		public int compareTo(@Nonnull QuickNavResult other) {
			int cmp = type.compareTo(other.type);
			if (cmp == 0)
				cmp = path.compareTo(other.path);
			if (cmp == 0)
				cmp = Comparator.nullsFirst(String::compareTo).compare(text, other.text);
			return cmp;
		}
	}

	/**
	 * Item matcher for plain-text containment searches.
	 *
	 * @param <T>
	 * 		Item type.
	 */
	@FunctionalInterface
	private interface ContainmentMatcher<T> {
		boolean matches(@Nonnull T item, @Nonnull String search, boolean caseSensitive);
	}

	/**
	 * Pane for displaying all supported quick-nav results.
	 */
	private static class AllContentPane extends ContentPaneBase {
		private AllContentPane(@Nonnull Actions actions,
		                       @Nonnull Stage stage,
		                       @Nonnull Supplier<Stream<QuickNavResult>> valueProvider,
		                       @Nonnull Consumer<ListCell<QuickNavResult>> renderCell,
		                       @Nonnull ContainmentMatcher<QuickNavResult> containmentMatcher) {
			super(new PathResultsPane<>(actions, stage, QuickNavResult::path, renderCell));
			setSearchBar(new OneToOneNavSearchBar<>(Unchecked.cast(results), valueProvider, QuickNavResult::text, containmentMatcher));
			setCenter(results);
		}
	}

	/**
	 * Pane for displaying results that come from a one-to-one lookup.
	 *
	 * @param <T>
	 * 		Input/Result type.
	 */
	private static class OneToOneContentPane<T extends PathNode<?>> extends ContentPaneBase {
		private OneToOneContentPane(@Nonnull Actions actions,
		                            @Nonnull Stage stage,
		                            @Nonnull Supplier<Stream<T>> valueProvider,
		                            @Nonnull Function<T, String> valueTextMapper,
		                            @Nonnull Consumer<ListCell<T>> renderCell) {
			this(actions, stage, valueProvider, valueTextMapper, null, renderCell);
		}

		private OneToOneContentPane(@Nonnull Actions actions,
		                            @Nonnull Stage stage,
		                            @Nonnull Supplier<Stream<T>> valueProvider,
		                            @Nonnull Function<T, String> valueTextMapper,
		                            @Nullable ContainmentMatcher<T> containmentMatcher,
		                            @Nonnull Consumer<ListCell<T>> renderCell) {
			super(new PathResultsPane<>(actions, stage, item -> item, renderCell));
			setSearchBar(new OneToOneNavSearchBar<>(Unchecked.cast(results), valueProvider, valueTextMapper, containmentMatcher));
			setCenter(results);
		}
	}

	/**
	 * Pane for displaying results that come from a one-to-many lookup.
	 *
	 * @param <T>
	 * 		Input type.
	 * @param <R>
	 * 		Result type.
	 */
	private static class OneToManyContentPane<T extends PathNode<?>, R extends PathNode<?>> extends ContentPaneBase {
		private OneToManyContentPane(@Nonnull Actions actions,
		                             @Nonnull Stage stage,
		                             @Nonnull Supplier<Stream<T>> valueProvider,
		                             @Nonnull Function<T, Stream<R>> valueUnroller,
		                             @Nonnull Function<R, String> valueTextMapper,
		                             @Nonnull Consumer<ListCell<R>> renderCell) {
			super(new PathResultsPane<>(actions, stage, item -> item, renderCell));
			setSearchBar(new OneToManyNavSearchBar<>(Unchecked.cast(results), valueProvider, valueUnroller, valueTextMapper));
			setCenter(results);
		}
	}

	/**
	 * Pane for displaying the results of a search.
	 *
	 * @param <T>
	 * 		Result type.
	 */
	private static class PathResultsPane<T> extends BorderPane {
		private static final PseudoClass PSEUDO_HOVER = PseudoClass.getPseudoClass("hover");
		private final ObservableList<T> list = FXCollections.observableArrayList();
		private final VirtualFlow<T, Cell<T, Node>> flow;
		private final Function<T, PathNode<?>> pathMapper;

		private PathResultsPane(@Nonnull Actions actions, @Nonnull Stage stage,
		                        @Nonnull Function<T, PathNode<?>> pathMapper,
		                        @Nonnull Consumer<ListCell<T>> renderCell) {
			this.pathMapper = pathMapper;
			flow = VirtualFlow.createVertical(list, initial -> new ResultCell(initial, actions, stage, renderCell));
			setCenter(new VirtualizedScrollPane<>(flow));
			list.addListener((InvalidationListener) e -> {
				flow.setFocusTraversable(!list.isEmpty());
			});
		}

		/**
		 * Select the first visible cell.
		 */
		private void selectCell() {
			if (!list.isEmpty()) {
				int index = flow.getFirstVisibleIndex();
				flow.getCell(index).getNode().requestFocus();
			}
		}

		private class ResultCell implements Cell<T, Node> {
			private final ListCell<T> cell = new ListCell<>();
			private final Consumer<ListCell<T>> renderCell;
			private final Runnable select;
			private int index;

			private ResultCell(@Nullable T initial, @Nonnull Actions actions, @Nonnull Stage stage,
			                   @Nonnull Consumer<ListCell<T>> renderCell) {
				this.renderCell = renderCell;

				select = () -> {
					try {
						T item = cell.getItem();
						if (item != null) {
							actions.gotoDeclaration(pathMapper.apply(item));
							stage.hide();
						}
					} catch (IncompletePathException ex) {
						logger.error("Failed to open path", ex);
					}
				};

				updateItem(initial);
				cell.getStyleClass().add("search-result-list-cell");
				cell.setOnMouseEntered(e -> {
					if (cell.getItem() != null)
						cell.pseudoClassStateChanged(PSEUDO_HOVER, true);
				});
				cell.setOnMouseExited(e -> cell.pseudoClassStateChanged(PSEUDO_HOVER, false));
				cell.setOnMousePressed(e -> {
					if (e.isPrimaryButtonDown() && e.getClickCount() == 2)
						select.run();
				});
			}

			@Override
			public void updateIndex(int index) {
				this.index = index;
			}

			@Override
			public void updateItem(T item) {
				if (item == null) {
					reset();
				} else {
					setupNavigation(cell);
					cell.setItem(item);
					renderCell.accept(cell);
				}
			}

			@Override
			public void reset() {
				cell.setItem(null);
				cell.setText(null);
				cell.setGraphic(null);
				cell.setOnMouseClicked(null);
				cell.setOnMouseEntered(null);
				cell.setOnMouseExited(null);
				clearNavigation(cell);
			}

			@Override
			public void dispose() {
				reset();
			}

			@Override
			public Node getNode() {
				return cell;
			}

			private void setupNavigation(ListCell<T> cell) {
				cell.setFocusTraversable(true);
				cell.setOnKeyPressed(e -> {
					KeyCode code = e.getCode();
					int size = list.size();
					int bigInc = Math.max(1, size / 25);
					double cellHeight = cell.getHeight();
					if (code == KeyCode.DOWN) {
						int nextIndex = Math.min(size - 1, index + (e.isShiftDown() ? bigInc : 1));
						if (nextIndex != index) {
							if (nextIndex >= flow.getLastVisibleIndex()) {
								flow.scrollYBy(cellHeight);
								flow.showAtOffset(nextIndex, flow.getHeight() - cellHeight * 2);
							}
							Cell<T, Node> nextCell = flow.getCell(nextIndex);
							if (nextCell instanceof ResultCell resultCell)
								resultCell.cell.requestFocus();
						}
						e.consume();
					} else if (code == KeyCode.UP) {
						int prevIndex = Math.max(0, index - (e.isShiftDown() ? bigInc : 1));
						if (prevIndex != index) {
							if (prevIndex <= flow.getFirstVisibleIndex()) {
								flow.scrollYBy(-cellHeight);
								flow.showAtOffset(prevIndex, cellHeight);
							}
							Cell<T, Node> nextCell = flow.getCell(prevIndex);
							if (nextCell instanceof ResultCell resultCell)
								resultCell.cell.requestFocus();
						}
						e.consume();
					} else if (code == KeyCode.ENTER) {
						select.run();
					}
				});
			}

			private void clearNavigation(ListCell<T> cell) {
				cell.setOnKeyPressed(null);
				cell.setFocusTraversable(false);
			}
		}
	}

	/**
	 * Search bar base implementation, tied to a {@link PathResultsPane}.
	 *
	 * @param <T>
	 * 		Input type.
	 * @param <R>
	 * 		Result type.
	 */
	private abstract static class NavSearchBarBase<T, R extends Comparable<? super R>> extends AbstractSearchBar {
		protected final PathResultsPane<R> results;
		protected final Supplier<Stream<T>> valueProvider;

		private NavSearchBarBase(@Nonnull PathResultsPane<R> results, @Nonnull Supplier<Stream<T>> valueProvider) {
			this.results = results;
			this.valueProvider = valueProvider;

			setup();
		}

		@Override
		protected void bindResultCountDisplay(@Nonnull StringProperty resultTextProperty) {
			results.list.addListener((InvalidationListener) observable -> {
				int size = results.list.size();
				if (size > 0) {
					hasResults.set(true);
					resultTextProperty.set(String.valueOf(size));
				} else {
					hasResults.set(false);
					resultTextProperty.set(Lang.get("menu.search.noresults"));
				}
			});
		}


		@Override
		protected void refreshResults() {
			// Skip when there is nothing
			String search = searchInput.getText();
			if (search == null || search.isBlank()) {
				results.list.clear();
				hasResults.set(false);
				return;
			}

			List<R> tempResultsList = new ArrayList<>();
			if (regex.get()) {
				// Validate the regex.
				RegexUtil.RegexValidation validation = RegexUtil.validate(search);
				Popover popoverValidation = null;
				if (validation.valid()) {
					// It's valid, match against values
					Pattern pattern = RegexUtil.pattern(search);
					regexSearch(pattern, tempResultsList);
				} else {
					// It's not valid. Tell the user what went wrong.
					popoverValidation = new Popover(new Label(validation.message()));
					popoverValidation.setHeaderAlwaysVisible(true);
					popoverValidation.titleProperty().bind(Lang.getBinding("find.regexinvalid"));
					popoverValidation.show(searchInput);
				}

				// Hide the prior popover if any exists.
				Object old = searchInput.getProperties().put("regex-popover", popoverValidation);
				if (old instanceof Popover oldPopover)
					oldPopover.hide();
			} else {
				containmentSearch(search, tempResultsList);
			}
			try {
				tempResultsList.sort(Comparator.naturalOrder());
			} catch (Throwable t) {
				logger.warn("Failed to sort search results, falling back to unsorted display", t);
			}
			results.list.setAll(tempResultsList);
		}

		/**
		 * @param pattern
		 * 		Pattern to search with.
		 * @param results
		 * 		Results sink to add to.
		 */
		protected abstract void regexSearch(@Nonnull Pattern pattern, @Nonnull List<R> results);

		/**
		 * @param search
		 * 		The {@link #searchInput}'s text content to search with.
		 * @param results
		 * 		Results sink to add to.
		 */
		protected abstract void containmentSearch(@Nonnull String search, @Nonnull List<R> results);
	}

	/**
	 * Search bar implementation where we match against the content of {@code <T>} directly.
	 *
	 * @param <T>
	 * 		Input/result type.
	 */
	private static class OneToOneNavSearchBar<T extends Comparable<? super T>> extends NavSearchBarBase<T, T> {
		private final Function<T, String> valueTextMapper;
		private final ContainmentMatcher<T> containmentMatcher;

		private OneToOneNavSearchBar(@Nonnull PathResultsPane<T> results,
		                             @Nonnull Supplier<Stream<T>> valueProvider,
		                             @Nonnull Function<T, String> valueTextMapper) {
			this(results, valueProvider, valueTextMapper, null);
		}

		private OneToOneNavSearchBar(@Nonnull PathResultsPane<T> results,
		                             @Nonnull Supplier<Stream<T>> valueProvider,
		                             @Nonnull Function<T, String> valueTextMapper,
		                             @Nullable ContainmentMatcher<T> containmentMatcher) {
			super(results, valueProvider);
			this.valueTextMapper = valueTextMapper;
			this.containmentMatcher = containmentMatcher == null ? (item, search, caseSensitive) -> {
				String text = valueTextMapper.apply(item);
				if (!caseSensitive && text != null)
					text = text.toLowerCase();
				return QuickNavWindow.contains(text, search);
			} : containmentMatcher;
		}

		@Override
		protected void regexSearch(@Nonnull Pattern pattern, @Nonnull List<T> results) {
			valueProvider.get().forEach(item -> {
				String text = valueTextMapper.apply(item);
				if (text == null)
					return;
				Matcher matcher = pattern.matcher(text);
				if (matcher.find())
					results.add(item);
			});
		}

		@Override
		protected void containmentSearch(@Nonnull String search, @Nonnull List<T> results) {
			// Modify the text/search for case-insensitive searches.
			if (!caseSensitivity.get())
				search = search.toLowerCase();

			String finalSearch = search;
			boolean caseSensitive = caseSensitivity.get();
			valueProvider.get().forEach(item -> {
				if (containmentMatcher.matches(item, finalSearch, caseSensitive))
					results.add(item);
			});
		}
	}

	/**
	 * Search bar implementation where we match against multiple string values within a {@code <T>} value.
	 *
	 * @param <T>
	 * 		Input type.
	 * @param <R>
	 * 		Result type.
	 */
	private static class OneToManyNavSearchBar<T extends PathNode<?>, R extends PathNode<?>> extends NavSearchBarBase<T, R> {
		private final Function<T, Stream<R>> valueUnroller;
		private final Function<R, String> valueTextMapper;

		private OneToManyNavSearchBar(@Nonnull PathResultsPane<R> results,
		                              @Nonnull Supplier<Stream<T>> valueProvider,
		                              @Nonnull Function<T, Stream<R>> valueUnroller,
		                              @Nonnull Function<R, String> valueTextMapper) {
			super(results, valueProvider);
			this.valueUnroller = valueUnroller;
			this.valueTextMapper = valueTextMapper;
		}

		@Override
		protected void regexSearch(@Nonnull Pattern pattern, @Nonnull List<R> results) {
			valueProvider.get().forEach(item -> {
				valueUnroller.apply(item).forEach(mappedItem -> {
					String text = valueTextMapper.apply(mappedItem);
					if (text == null)
						return;
					Matcher matcher = pattern.matcher(text);
					if (matcher.find())
						results.add(mappedItem);
				});
			});
		}

		@Override
		protected void containmentSearch(@Nonnull String search, @Nonnull List<R> results) {
			if (!caseSensitivity.get())
				search = search.toLowerCase();
			String finalSearch = search;
			valueProvider.get().forEach(item -> {
				valueUnroller.apply(item).forEach(mappedItem -> {
					String text = valueTextMapper.apply(mappedItem);
					if (!caseSensitivity.get() && text != null)
						text = text.toLowerCase();
					if (QuickNavWindow.contains(text, finalSearch))
						results.add(mappedItem);
				});
			});
		}
	}
}
