package me.coley.recaf.ui.pane;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import me.coley.recaf.RecafUI;
import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.FileInfo;
import me.coley.recaf.code.MemberInfo;
import me.coley.recaf.search.NumberMatchMode;
import me.coley.recaf.search.Search;
import me.coley.recaf.search.TextMatchMode;
import me.coley.recaf.search.query.QueryVisitor;
import me.coley.recaf.search.result.*;
import me.coley.recaf.ui.CommonUX;
import me.coley.recaf.ui.control.ActionButton;
import me.coley.recaf.ui.control.BoundLabel;
import me.coley.recaf.ui.control.ColumnPane;
import me.coley.recaf.ui.control.EnumComboBox;
import me.coley.recaf.ui.docking.DockTab;
import me.coley.recaf.ui.docking.DockingRegion;
import me.coley.recaf.ui.docking.RecafDockingManager;
import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.util.NumberUtil;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.util.observable.BatchObservableSet;
import me.coley.recaf.util.observable.BatchObservableSetWrapper;
import me.coley.recaf.util.threading.FxThreadUtil;
import me.coley.recaf.util.threading.ThreadUtil;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.resource.Resource;
import org.objectweb.asm.ClassReader;
import org.slf4j.Logger;

import java.awt.Toolkit;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Panel for search operations.
 *
 * @author Matt Coley
 */
public class SearchPane extends BorderPane {
	private static final Logger logger = Logging.get(SearchPane.class);
	private final DockTab containingTab;
	private Instant lastTyped = Instant.EPOCH;
	private CompletableFuture<?> typeCooldownFuture = null;


	private SearchPane(ObservableValue<String> title, Node content) {
		DockingWrapperPane wrapper = DockingWrapperPane.builder()
				.title(title)
				.content(content)
				.size(600, 300)
				.build();
		containingTab = wrapper.getTab();
		setCenter(wrapper);
	}


	/**
	 * @return Empty text search panel.
	 */
	public static SearchPane createTextSearch() {
		return createTextSearch("");
	}

	/**
	 * @param text
	 * 		Initial query.
	 *
	 * @return Text search panel.
	 */
	public static SearchPane createTextSearch(String text) {
		StringBinding title = Lang.formatBy("%s: %s", Lang.getBinding("menu.search"),
				Lang.getBinding("menu.search.string"));
		// Inputs
		TextField txtText = new TextField(text);
		EnumComboBox<TextMatchMode> comboMode = new EnumComboBox<>(TextMatchMode.class, TextMatchMode.CONTAINS);
		// Layout
		ColumnPane columns = new ColumnPane();
		SearchPane searchPane = new SearchPane(title, columns);
		Label textLabel = new BoundLabel(Lang.getBinding("search.text"));
		columns.add(textLabel, txtText);
		Label modeLabel = new BoundLabel(Lang.getBinding("search.textmode"));
		columns.add(modeLabel, comboMode);
		columns.add(null, new ActionButton(Lang.getBinding("search.run"), () -> {
			searchPane.searchText(txtText.getText(), comboMode.getValue());
		}));
		var check = new CheckBox();
		columns.add(new BoundLabel(Lang.getBinding("search.live")), check);
		var obSet = new BatchObservableSetWrapper<>(new TreeSet<Result>(), 25);
		var obList = FXCollections.observableList(new ArrayList<Result>());
		var listView = new ListView<>(obList);
		listView.getStyleClass().add("seemless");
		obSet.batchListenAndApplyInto(obList);
		listView.setCellFactory(param -> new ResultListCell(txtText));
		check.selectedProperty().addListener((observable, oldValue, newValue) -> {
			searchPane.toggleLiveSearch(txtText.getText(), comboMode.getValue(), columns, obSet, obList, listView, newValue);
		});
		txtText.textProperty().addListener((observable, oldValue, newValue) -> {
			searchPane.lastTyped = Instant.now();
			if (searchPane.typeCooldownFuture == null && check.isSelected()) {
				searchPane.typeCooldownFuture = ThreadUtil.runDelayed(1000, searchPane.new TextChangeCooldown(obSet, txtText, comboMode));
			}
		});
		return searchPane;
	}

	private void toggleLiveSearch(
			String search, TextMatchMode matchMode,
			ColumnPane columns, Collection<Result> results,
			ObservableList<Result> obList, ListView<Result> listView,
			boolean newValue
	) {
		results.clear();
		if (!newValue) {
			containingTab.setContent(columns);
			return;
		}
		var size = Bindings.size(obList);
		containingTab.setContent(new VBox(columns,
				new BoundLabel(Bindings.createStringBinding(() -> "Found " + size.get(), size)),
				listView));
		VBox.setVgrow(listView, Priority.ALWAYS);
		if (search.isEmpty())
			return;
		runSearch(new Search().text(search, matchMode), results, () -> {
			if (results instanceof BatchObservableSet)
				((BatchObservableSet<Result>) results).triggerBatchListeners(true);
			typeCooldownFuture = null;
		});
	}

	private static Region representLocation(Location location) {
		if (location instanceof FileLocation) {
			FileInfo fileInfo = ((FileLocation) location).getContainingFile();
			TextFlow filePathText = new TextFlow(new Label(fileInfo.getName()));
			int line = ((FileLocation) location).getLine();
			if (line != -1) {
				Text lineNbText = new Text(String.valueOf(((FileLocation) location).getLine()));
				lineNbText.setFill(Color.STEELBLUE);
				filePathText.getChildren().addAll(new Label(" at "), lineNbText);
			}
			HBox box = new HBox(Icons.getFileIcon(fileInfo), filePathText);
			box.setOnMouseClicked(line != -1 ?
					(e -> CommonUX.openFile(fileInfo, line + 1)) :
					(e -> CommonUX.openFile(fileInfo)));
			box.setSpacing(6);
			return box;
		} else if (location instanceof ClassLocation) {
			final ClassLocation classLocation = (ClassLocation) location;
			CommonClassInfo classInfo = classLocation.getContainingClass();
			HBox box = new HBox(Icons.getClassIcon(classInfo), new Label(classInfo.getName()));
			MemberInfo member = classLocation.getContainingMember();
			box.setOnMouseClicked(member != null ?
					(e -> CommonUX.openMember(classInfo, member)) :
					(e -> CommonUX.openClass(classInfo)));
			box.setSpacing(6);
			return box;
		}
		return new Label(location.toString());
	}

	/**
	 * @return Empty number search panel.
	 */
	public static SearchPane createNumberSearch() {
		return createNumberSearch(null);
	}

	/**
	 * @param number
	 * 		Initial query.
	 *
	 * @return Number search panel.
	 */
	public static SearchPane createNumberSearch(String number) {
		StringBinding title = Lang.formatBy("%s: %s", Lang.getBinding("menu.search"),
				Lang.getBinding("menu.search.number"));
		// Inputs
		TextField txtNumber = new TextField(number);
		EnumComboBox<NumberMatchMode> comboMode = new EnumComboBox<>(NumberMatchMode.class, NumberMatchMode.EQUALS);
		// Layout
		ColumnPane columns = new ColumnPane();
		SearchPane searchPane = new SearchPane(title, columns);
		Label numberLabel = new BoundLabel(Lang.getBinding("search.number"));
		columns.add(numberLabel, txtNumber);
		Label modeLabel = new BoundLabel(Lang.getBinding("search.numbermode"));
		columns.add(modeLabel, comboMode);
		columns.add(null, new ActionButton(Lang.getBinding("search.run"), () -> {
			searchPane.searchNumber(NumberUtil.parse(txtNumber.getText()), comboMode.getValue());
		}));
		return searchPane;
	}

	/**
	 * @return Empty reference search panel.
	 */
	public static SearchPane createReferenceSearch() {
		return createReferenceSearch(null, null, null);
	}

	/**
	 * @param owner
	 * 		Internal name of owner.
	 * @param name
	 * 		Reference name.
	 * @param desc
	 * 		Reference descriptor.
	 *
	 * @return Reference search panel.
	 */
	public static SearchPane createReferenceSearch(String owner, String name, String desc) {
		return createReferenceSearch(owner, name, desc, TextMatchMode.CONTAINS);
	}

	/**
	 * @param owner
	 * 		Internal name of owner.
	 * @param name
	 * 		Reference name.
	 * @param desc
	 * 		Reference descriptor.
	 * @param mode
	 * 		Text match mode for member input.
	 *
	 * @return Reference search panel.
	 */
	public static SearchPane createReferenceSearch(String owner, String name, String desc, TextMatchMode mode) {
		StringBinding title = Lang.formatBy("%s: %s", Lang.getBinding("menu.search"),
				Lang.getBinding("menu.search.references"));
		// Inputs
		TextField txtOwner = new TextField(owner);
		TextField txtName = new TextField(name);
		TextField txtDesc = new TextField(desc);
		EnumComboBox<TextMatchMode> comboMode = new EnumComboBox<>(TextMatchMode.class, mode);
		// Layout
		ColumnPane columns = new ColumnPane();
		SearchPane searchPane = new SearchPane(title, columns);
		Label ownerLabel = new BoundLabel(Lang.getBinding("search.refowner"));
		columns.add(ownerLabel, txtOwner);
		Label nameLabel = new BoundLabel(Lang.getBinding("search.refname"));
		columns.add(nameLabel, txtName);
		Label descLabel = new BoundLabel(Lang.getBinding("search.refdesc"));
		columns.add(descLabel, txtDesc);
		Label modeLabel = new BoundLabel(Lang.getBinding("search.textmode"));
		columns.add(modeLabel, comboMode);
		columns.add(null, new ActionButton(Lang.getBinding("search.run"), () -> {
			searchPane.searchReference(
					txtOwner.getText(), txtName.getText(), txtDesc.getText(),
					comboMode.getValue());
		}));
		return searchPane;
	}

	/**
	 * @return Empty declaration search panel.
	 */
	public static SearchPane createDeclarationSearch() {
		return createDeclarationSearch(null, null, null);
	}

	/**
	 * @param owner
	 * 		Internal name of owner.
	 * @param name
	 * 		Declaration name.
	 * @param desc
	 * 		Declaration descriptor.
	 *
	 * @return Declaration search panel.
	 */
	public static SearchPane createDeclarationSearch(String owner, String name, String desc) {
		StringBinding title = Lang.formatBy("%s: %s", Lang.getBinding("menu.search"),
				Lang.getBinding("menu.search.declarations"));
		// Inputs
		TextField txtOwner = new TextField(owner);
		TextField txtName = new TextField(name);
		TextField txtDesc = new TextField(desc);
		EnumComboBox<TextMatchMode> comboMode = new EnumComboBox<>(TextMatchMode.class, TextMatchMode.CONTAINS);
		// Layout
		ColumnPane columns = new ColumnPane();
		SearchPane searchPane = new SearchPane(title, columns);
		Label ownerLabel = new BoundLabel(Lang.getBinding("search.refowner"));
		columns.add(ownerLabel, txtOwner);
		Label nameLabel = new BoundLabel(Lang.getBinding("search.refname"));
		columns.add(nameLabel, txtName);
		Label descLabel = new BoundLabel(Lang.getBinding("search.refdesc"));
		columns.add(descLabel, txtDesc);
		Label modeLabel = new BoundLabel(Lang.getBinding("search.textmode"));
		columns.add(modeLabel, comboMode);
		columns.add(null, new ActionButton(Lang.getBinding("search.run"), () -> {
			searchPane.searchDeclaration(
					txtOwner.getText(), txtName.getText(), txtDesc.getText(),
					comboMode.getValue());
		}));
		return searchPane;
	}

	private void runSearch(Search search) {
		final Set<Result> results = ConcurrentHashMap.newKeySet(1024);
		runSearch(search, results, () -> onSearchFinish(search, results));
	}


	private void runSearch(Search search, Collection<Result> results, Runnable onFinish) {
		Workspace workspace = RecafUI.getController().getWorkspace();
		if (workspace == null) {
			logger.error("Cannot search since no workspace is open!");
			Toolkit.getDefaultToolkit().beep();
			return;
		}
		Resource resource = workspace.getResources().getPrimary();
		// Multi-thread search, using an atomic integer to track progress across threads
		AtomicInteger latch = new AtomicInteger(resource.getClasses().size() + resource.getFiles().size());
		for (ClassInfo info : new HashSet<>(resource.getClasses().values())) {
			ThreadUtil.run(() -> {
				QueryVisitor visitor = search.createQueryVisitor(resource);
				if (visitor != null) {
					info.getClassReader().accept(visitor, ClassReader.SKIP_FRAMES);
					visitor.storeResults(results);
				}
				if (latch.decrementAndGet() == 0) {
					onFinish.run();
				}
			});
		}
		for (FileInfo info : new HashSet<>(resource.getFiles().values())) {
			ThreadUtil.run(() -> {
				QueryVisitor visitor = search.createQueryVisitor(resource);
				if (visitor != null) {
					visitor.visitFile(info);
					visitor.storeResults(results);
				}
				if (latch.decrementAndGet() == 0) {
					onFinish.run();
				}
			});
		}
	}

	private void onSearchFinish(Search search, Set<Result> results) {
		logger.info("Search yielded {} results", results.size());
		TreeSet<Result> sorted = new TreeSet<>(results);
		ResultsPane resultsPane = new ResultsPane(search, sorted);
		FxThreadUtil.run(() -> {
			DockingRegion region = containingTab.getParent();
			DockTab tab = RecafDockingManager.getInstance().createTabIn(region,
					() -> new DockTab(Lang.getBinding("search.results"), resultsPane));
			region.getSelectionModel().select(tab);
		});
	}

	private void searchText(String text, TextMatchMode mode) {
		Search search = new Search().text(text, mode);
		runSearch(search);
	}

	private void searchNumber(Number number, NumberMatchMode mode) {
		Search search = new Search().number(number, mode);
		runSearch(search);
	}

	private void searchReference(String owner, String name, String desc, TextMatchMode mode) {
		Search search = new Search().reference(owner, name, desc, mode);
		runSearch(search);
	}

	private void searchDeclaration(String owner, String name, String desc, TextMatchMode mode) {
		Search search = new Search().declaration(owner, name, desc, mode);
		runSearch(search);
	}

	private class TextChangeCooldown implements Runnable {
		private final BatchObservableSet<Result> results;
		private final TextField txtText;
		private final EnumComboBox<TextMatchMode> comboMode;

		public TextChangeCooldown(BatchObservableSet<Result> results, TextField txtText, EnumComboBox<TextMatchMode> comboMode) {
			this.results = results;
			this.txtText = txtText;
			this.comboMode = comboMode;
		}

		@Override
		public void run() {
			if (typeCooldownFuture == null)
				return;
			if (lastTyped.until(Instant.now(), ChronoUnit.MILLIS) <= 700) {
				ThreadUtil.runDelayed(400, this);
				return;
			}
			results.clear();
			String text = txtText.getText();
			if (!text.isEmpty()) {
				runSearch(
						new Search().text(text, comboMode.getValue()), results,
						() -> {
							results.triggerBatchListeners(true);
							typeCooldownFuture = null;
						}
				);
			}
		}
	}

	private static class ResultListCell extends ListCell<Result> {

		private final TextField txtText;

		public ResultListCell(TextField txtText) {
			this.txtText = txtText;
			addEventFilter(MouseEvent.MOUSE_PRESSED, Event::consume);
		}

		@Override
		protected void updateItem(Result item, boolean empty) {
			super.updateItem(item, empty);
			//						updateSelected(false);
			if (empty || !(item instanceof TextResult)) {
				setGraphic(null);
				setText(null);
			} else {
				String input = txtText.getText();
				String matchedText = ((TextResult) item).getMatchedText();
				// put a blue highlight on found match
				Text inputText = new Text(input);
				inputText.setFill(Color.DODGERBLUE);
				int index = matchedText.indexOf(input);
				if (index == -1) {
					setText(matchedText);
					return;
				}
				final Region locationNode = representLocation(item.getLocation());
				AnchorPane pane = new AnchorPane(locationNode);
				AnchorPane.setRightAnchor(locationNode, 0.0);
				AnchorPane.setTopAnchor(locationNode, 0.0);
				AnchorPane.setBottomAnchor(locationNode, 0.0);
				locationNode.setBackground(Background.fill(Color.rgb(50, 50, 50)));
				pane.setMaxWidth(Region.USE_COMPUTED_SIZE);
				StackPane stackPane = new StackPane(
						new TextFlow(new Label(matchedText.substring(0, index)),
								inputText,
								new Label(matchedText.substring(index + input.length()))),
						pane
				);
				setGraphic(stackPane);
			}
		}
	}
}
