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
import javafx.scene.control.TabPane;
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
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.path.*;
import software.coley.recaf.services.cell.CellConfigurationService;
import software.coley.recaf.services.cell.context.ContextSource;
import software.coley.recaf.services.navigation.Actions;
import software.coley.recaf.services.window.WindowManager;
import software.coley.recaf.services.workspace.WorkspaceCloseListener;
import software.coley.recaf.services.workspace.WorkspaceManager;
import software.coley.recaf.ui.config.TextFormatConfig;
import software.coley.recaf.ui.control.AbstractSearchBar;
import software.coley.recaf.ui.control.BoundTab;
import software.coley.recaf.ui.control.FontIconView;
import software.coley.recaf.util.Icons;
import software.coley.recaf.util.Lang;
import software.coley.recaf.util.RegexUtil;
import software.coley.recaf.workspace.model.Workspace;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
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
	public QuickNavWindow(@Nonnull WorkspaceManager workspaceManager,
						  @Nonnull Actions actions, @Nonnull TextFormatConfig formatConfig,
						  @Nonnull CellConfigurationService configurationService) {
		super(WindowManager.WIN_QUICK_NAV);

		ContentPane<ClassPathNode> classContent = new ContentPane<>(actions, this, () -> {
			Workspace current = workspaceManager.getCurrent();
			if (current == null)
				return Stream.empty();
			return current.classesStream();
		}, path -> path.getValue().getName(), cell -> {
			ClassPathNode classPath = cell.getItem();
			DirectoryPathNode packagePath = Objects.requireNonNull(classPath.getParent());
			String packageName = packagePath.getValue();
			packageName = formatConfig.filterEscape(packageName);
			packageName = formatConfig.filterMaxLength(packageName);

			Label classDisplay = new Label();
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
		});
		ContentPane<ClassMemberPathNode> memberContent = new ContentPane<>(actions, this, () -> {
			Workspace current = workspaceManager.getCurrent();
			if (current == null)
				return Stream.empty();
			return current.classesStream().flatMap(p -> {
				ClassInfo classInfo = p.getValue();
				Stream<ClassMemberPathNode> fields = classInfo.getFields().stream().map(p::child);
				Stream<ClassMemberPathNode> methods = classInfo.getMethods().stream().map(p::child);
				return Stream.concat(fields, methods);
			});
		}, path -> path.getValue().getName(), cell -> {
			ClassMemberPathNode memberPath = cell.getItem();
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
		});
		ContentPane<FilePathNode> fileContent = new ContentPane<>(actions, this, () -> {
			Workspace current = workspaceManager.getCurrent();
			if (current == null)
				return Stream.empty();
			return current.filesStream();
		}, path -> path.getValue().getName(), cell -> {
			FilePathNode filePath = cell.getItem();
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
		});
		// TODO: Need to rework internals to support differentiating the result type and the type scanned
		//  - Need to *efficiently* search text line by line, not as one blob
		ContentPane<FilePathNode> textContent = new ContentPane<>(actions, this, () -> {
			Workspace current = workspaceManager.getCurrent();
			if (current == null)
				return Stream.empty();
			return current.filesStream()
					.filter(f -> f.getValue().isTextFile());
		}, path -> path.getValue().asTextFile().getText(), cell -> {
			FilePathNode filePath = cell.getItem();

			Label fileDisplay = new Label();
			fileDisplay.setText(configurationService.textOf(filePath));
			fileDisplay.setGraphic(configurationService.graphicOf(filePath));

			Label textDisplay = new Label();
			textDisplay.setText("TODO: show multiple lines of matched text + line number");
			textDisplay.setOpacity(0.5);
			textDisplay.setTextOverrun(OverrunStyle.CENTER_WORD_ELLIPSIS);

			Spacer spacer = new Spacer();
			HBox box = new HBox(fileDisplay, spacer, textDisplay);
			HBox.setHgrow(spacer, Priority.ALWAYS);
			cell.setText(null);
			cell.setGraphic(box);

			cell.setOnMouseClicked(configurationService.contextMenuHandlerOf(cell, filePath, ContextSource.REFERENCE));
		});
		List<ContentPane<?>> contentPanes = List.of(classContent, memberContent, fileContent/*, textContent*/);
		contentPanes.forEach(workspaceManager::addWorkspaceCloseListener);

		BoundTab tabClasses = new BoundTab(Lang.getBinding("dialog.quicknav.tab.classes"), Icons.getIconView(Icons.CLASS), classContent);
		BoundTab tabMembers = new BoundTab(Lang.getBinding("dialog.quicknav.tab.members"), Icons.getIconView(Icons.FIELD_N_METHOD), memberContent);
		BoundTab tabFiles = new BoundTab(Lang.getBinding("dialog.quicknav.tab.files"), new FontIconView(CarbonIcons.DOCUMENT), fileContent);
		//BoundTab tabText = new BoundTab(Lang.getBinding("dialog.quicknav.tab.text"), new FontIconView(CarbonIcons.STRING_TEXT), textContent);

		TabPane tabs = new TabPane();
		tabs.getTabs().addAll(tabClasses, tabMembers, tabFiles/*, tabText*/);
		tabs.getTabs().forEach(tab -> tab.setClosable(false));

		// Layout
		titleProperty().bind(Lang.getBinding("dialog.quicknav"));
		setMinWidth(300);
		setMinHeight(300);
		setScene(new RecafScene(tabs, 750, 550));
	}

	/**
	 * Pane for displaying the search bar + results pane for some content type.
	 *
	 * @param <T>
	 * 		Content / result type.
	 */
	private static class ContentPane<T extends PathNode<?>> extends BorderPane implements WorkspaceCloseListener {
		private final PathResultsPane<T> results;

		private ContentPane(@Nonnull Actions actions,
							@Nonnull Stage stage,
							@Nonnull Supplier<Stream<T>> valueProvider,
							@Nonnull Function<T, String> valueTextMapper,
							@Nonnull Consumer<ListCell<T>> renderCell) {
			results = new PathResultsPane<>(actions, stage, renderCell);
			setTop(new NavSearchBar<>(results, valueProvider, valueTextMapper));
			setCenter(results);
		}

		@Override
		public void onWorkspaceClosed(@Nonnull Workspace workspace) {
			results.list.clear();
		}
	}

	/**
	 * Pane for displaying the results of a search.
	 *
	 * @param <T>
	 * 		Result type.
	 */
	private static class PathResultsPane<T extends PathNode<?>> extends BorderPane {
		private static final PseudoClass PSEUDO_HOVER = PseudoClass.getPseudoClass("hover");
		private final ObservableList<T> list = FXCollections.observableArrayList();

		private PathResultsPane(@Nonnull Actions actions, @Nonnull Stage stage,
								@Nonnull Consumer<ListCell<T>> renderCell) {
			VirtualFlow<T, Cell<T, ?>> flow = VirtualFlow.createVertical(list,
					initial -> new ResultCell(initial, actions, stage, renderCell));
			setCenter(new VirtualizedScrollPane<>(flow));
		}

		private class ResultCell implements Cell<T, Node> {
			private final ListCell<T> cell = new ListCell<>();
			private final Consumer<ListCell<T>> renderCell;

			private ResultCell(@Nullable T initial, @Nonnull Actions actions, @Nonnull Stage stage,
							   @Nonnull Consumer<ListCell<T>> renderCell) {
				this.renderCell = renderCell;

				updateItem(initial);
				cell.getStyleClass().add("search-result-list-cell");
				cell.setOnMouseEntered(e -> {
					if (cell.getItem() != null)
						cell.pseudoClassStateChanged(PSEUDO_HOVER, true);
				});
				cell.setOnMouseExited(e -> cell.pseudoClassStateChanged(PSEUDO_HOVER, false));
				cell.setOnMousePressed(e -> {
					if (e.isPrimaryButtonDown() && e.getClickCount() == 2) {
						try {
							T item = cell.getItem();
							if (item != null) {
								actions.gotoDeclaration(item);
								stage.hide();
							}
						} catch (IncompletePathException ex) {
							logger.error("Failed to open path", ex);
						}
					}
				});
			}

			@Override
			public void updateItem(T item) {
				if (item == null) {
					reset();
				} else {
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
			}

			@Override
			public void dispose() {
				reset();
			}

			@Override
			public Node getNode() {
				return cell;
			}
		}
	}

	/**
	 * Search bar implementation, tied to a {@link PathResultsPane}.
	 *
	 * @param <T>
	 * 		Result type.
	 */
	private static class NavSearchBar<T extends PathNode<?>> extends AbstractSearchBar {
		private final PathResultsPane<T> results;
		private final Supplier<Stream<T>> valueProvider;
		private final Function<T, String> valueTextMapper;

		private NavSearchBar(@Nonnull PathResultsPane<T> results,
							@Nonnull Supplier<Stream<T>> valueProvider,
							@Nonnull Function<T, String> valueTextMapper) {
			this.results = results;
			this.valueProvider = valueProvider;
			this.valueTextMapper = valueTextMapper;

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

			List<T> tempResultsList = new ArrayList<>();
			if (regex.get()) {
				// Validate the regex.
				RegexUtil.RegexValidation validation = RegexUtil.validate(search);
				Popover popoverValidation = null;
				if (validation.valid()) {
					// It's valid, match against values
					Pattern pattern = RegexUtil.pattern(search);
					valueProvider.get().forEach(item -> {
						String text = valueTextMapper.apply(item);
						Matcher matcher = pattern.matcher(text);
						if (matcher.find())
							tempResultsList.add(item);
					});
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
				// Modify the text/search for case-insensitive searches.
				Function<T, String> localValueTextMapper;
				if (!caseSensitivity.get()) {
					search = search.toLowerCase();
					localValueTextMapper = valueTextMapper.andThen(String::toLowerCase);
				} else {
					localValueTextMapper = valueTextMapper;
				}

				// Match against values
				String finalSearch = search;
				valueProvider.get().forEach(item -> {
					String text = localValueTextMapper.apply(item);
					if (text.contains(finalSearch))
						tempResultsList.add(item);
				});
			}
			tempResultsList.sort(Comparator.naturalOrder());
			results.list.setAll(tempResultsList);
		}
	}
}
