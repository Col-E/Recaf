package me.coley.recaf.ui;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.controlsfx.control.PropertySheet;
import org.controlsfx.control.PropertySheet.Item;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import me.coley.recaf.bytecode.search.*;
import me.coley.recaf.config.Conf;
import me.coley.recaf.ui.component.ActionButton;
import me.coley.recaf.ui.component.ActionMenuItem;
import me.coley.recaf.ui.component.ReflectivePropertySheet;
import me.coley.recaf.ui.component.editor.StagedCustomEditor;
import me.coley.recaf.util.Icons;
import me.coley.recaf.util.JavaFX;
import me.coley.recaf.util.Lang;
import me.coley.recaf.util.Reflect;

/**
 * Window for handling config options.
 * 
 * @author Matt
 */
public class FxSearch extends Stage {
	private final List<SearchObj> searchPanels = new ArrayList<>();
	private final Tab results = new Tab(Lang.get("ui.search.results"));

	private FxSearch() {
		setTitle(Lang.get("ui.search"));
		getIcons().add(Icons.LOGO);
		// Prevent close, instead hide stage.
		setOnCloseRequest(event -> {
			event.consume();
			hide();
		});

		setupSearches();

		// Create new property-sheets per panel
		TabPane tabs = new TabPane();
		for (SearchObj searchType : searchPanels) {
			BorderPane pane = new BorderPane();
			PropertySheet propertySheet = new ReflectivePropertySheet(searchType) {
				@Override
				protected void setupItems(Object instance) {
					for (Field field : Reflect.fields(instance.getClass())) {
						// Require conf annotation
						Conf conf = field.getDeclaredAnnotation(Conf.class);
						if (conf == null) continue;
						else if (conf.hide()) continue;
						// Setup item & add to list
						getItems().add(new SearchItem(instance, field, conf.category(), conf.key()));
					}
				}

				class SearchItem extends ReflectiveItem {
					public SearchItem(Object owner, Field field, String categoryKey, String translationKey) {
						super(owner, field, categoryKey, translationKey);
					}

					@Override
					protected Class<?> getEditorType() {
						Field f = getField();
						if (f.getName().equals("ignored")) {
							// interfaces
							return IgnoreList.class;
						} else if (f.getName().equals("opcodes")) {
							// interfaces
							return OpcodeList.class;
						}
						return super.getEditorType();
					}
				}
			};
			propertySheet.setSearchBoxVisible(false);
			propertySheet.setModeSwitcherVisible(false);
			VBox.setVgrow(propertySheet, Priority.ALWAYS);
			ActionButton search = new ActionButton(Lang.get("ui.search"), () -> {
				searchType.run();
				setupSearches();
			});
			search.prefWidthProperty().bind(pane.widthProperty());
			Tab tab = new Tab(Lang.get(searchType.title()));
			tab.closableProperty().set(false);
			pane.setCenter(propertySheet);
			pane.setBottom(search);
			tab.setContent(pane);
			tabs.getTabs().add(tab);
		}
		results.closableProperty().set(false);
		tabs.getTabs().add(results);
		setScene(JavaFX.scene(tabs, 600, 300));
	}

	private void setupSearches() {
		searchPanels.clear();
		// String
		searchPanels.add(new SearchObj(this) {
			@Conf(category = "params", key = "text")
			public String text;
			@Conf(category = "params", key = "mode")
			public StringMode mode = StringMode.CONTAINS;
			@Conf(category = "params", key = "sensitive")
			public boolean sensitive;
			@Conf(category = "params", key = "ignored")
			public List<String> ignored = new ArrayList<>();

			@Override
			public String title() {
				return "ui.search.string";
			}

			@Override
			public void run() {
				Parameter p = Parameter.string(text);
				p.setCaseSenstive(sensitive);
				p.setStringMode(mode);
				p.getSkipList().addAll(ignored);
				update(Search.search(p));
			}
		});
		// Int value
		searchPanels.add(new SearchObj(this) {
			@Conf(category = "params", key = "value")
			public int value;
			@Conf(category = "params", key = "ignored")
			public List<String> ignored = new ArrayList<>();

			@Override
			public String title() {
				return "ui.search.value";
			}

			@Override
			public void run() {
				Parameter p = Parameter.value(value);
				p.getSkipList().addAll(ignored);
				update(Search.search(p));
			}
		});
		// declaration
		searchPanels.add(new SearchObj(this) {
			@Conf(category = "params", key = "owner")
			public String owner;
			@Conf(category = "params", key = "name")
			public String name;
			@Conf(category = "params", key = "desc")
			public String desc;
			@Conf(category = "params", key = "mode")
			public StringMode mode = StringMode.CONTAINS;
			@Conf(category = "params", key = "sensitive")
			public boolean sensitive;
			@Conf(category = "params", key = "ignored")
			public List<String> ignored = new ArrayList<>();

			@Override
			public String title() {
				return "ui.search.declare";
			}

			@Override
			public void run() {
				Parameter p = Parameter.declaration(owner, name, desc);
				p.setCaseSenstive(sensitive);
				p.setStringMode(mode);
				p.getSkipList().addAll(ignored);
				update(Search.search(p));
			}
		});
		// reference
		searchPanels.add(new SearchObj(this) {
			@Conf(category = "params", key = "owner")
			public String owner;
			@Conf(category = "params", key = "name")
			public String name;
			@Conf(category = "params", key = "desc")
			public String desc;
			@Conf(category = "params", key = "mode")
			public StringMode mode = StringMode.CONTAINS;
			@Conf(category = "params", key = "sensitive")
			public boolean sensitive;
			@Conf(category = "params", key = "ignored")
			public List<String> ignored = new ArrayList<>();

			@Override
			public String title() {
				return "ui.search.reference";
			}

			@Override
			public void run() {
				Parameter p = Parameter.references(owner, name, desc);
				p.setCaseSenstive(sensitive);
				p.setStringMode(mode);
				p.getSkipList().addAll(ignored);
				update(Search.search(p));
			}
		});
		// opcodes
		searchPanels.add(new SearchObj(this) {
			@Conf(category = "params", key = "opcode")
			public List<String> opcodes = new ArrayList<>();
			@Conf(category = "params", key = "ignored")
			public List<String> ignored = new ArrayList<>();
			@Conf(category = "params", key = "mode")
			public StringMode mode = StringMode.STARTS_WITH;
			@Conf(category = "params", key = "sensitive")
			public boolean sensitive;

			@Override
			public String title() {
				return "ui.search.opcode";
			}

			@Override
			public void run() {
				Parameter p = Parameter.opcodes(opcodes);
				p.getSkipList().addAll(ignored);
				p.setStringMode(mode);
				p.setCaseSenstive(sensitive);
				update(Search.search(p));
			}
		});
	}

	/**
	 * Display search window.
	 */
	public static void open() {
		new FxSearch().show();
	}

	/**
	 * Display search window and search given search
	 * 
	 * @param param
	 *            Search parameter
	 */
	public static void open(Parameter param) {
		FxSearch fx = new FxSearch();
		fx.opacityProperty();
		SearchObj obj = new SearchObj(fx) {
			@Override
			public String title() {
				return "dummy";
			}

			@Override
			public void run() {
				update(Search.search(param));
			}
		};
		obj.run();
		fx.show();
	}

	/**
	 * Wrapper. Implementations will have field values to match with proper
	 * search queries so the ReflectivePropertySheet can access them and run
	 * searches.
	 * 
	 * @author Matt
	 */
	public static abstract class SearchObj {
		private final FxSearch search;

		public SearchObj(FxSearch search) {
			this.search = search;
		}

		/**
		 * @return Translation key for tab title.
		 */
		public abstract String title();

		/**
		 * Run search.
		 */
		public abstract void run();

		/**
		 * Update tab with results.
		 * 
		 * @param search
		 *            The results of a search.
		 */
		protected void update(List<Result> search) {
			ResultTree tree = new ResultTree();
			tree.setSearchResults(search);
			SingleSelectionModel<Tab> selectionModel = this.search.results.getTabPane().getSelectionModel();
			selectionModel.select(this.search.results);
			this.search.results.setContent(tree);
		}

	}

	/**
	 * Editor for list of class name prefixes to ignore in searched.
	 * 
	 * @author Matt
	 *
	 * @param <T>
	 */
	public static class IgnoreList<T extends List<String>> extends StagedCustomEditor<T> {
		public IgnoreList(Item item) {
			super(item);
		}

		@Override
		public Node getEditor() {
			return new ActionButton(Lang.get("ui.search.skipped"), () -> open(this));
		}

		/**
		 * Open another window to handle editing of the value.
		 * 
		 * @param ignoredList
		 *            CustomEditor instance to for value get/set callbacks.
		 */
		private void open(IgnoreList<T> ignoredList) {
			if (staged()) {
				return;
			}
			BorderPane listPane = new BorderPane();
			BorderPane menuPane = new BorderPane();
			ListView<String> view = new ListView<>();
			ObservableList<String> list = FXCollections.observableArrayList(ignoredList.getValue());
			list.addListener(new ListChangeListener<String>() {
				@SuppressWarnings("unchecked")
				@Override
				public void onChanged(Change<? extends String> c) {
					setValue((T) list);
				}
			});
			view.setOnKeyPressed(new EventHandler<KeyEvent>() {
				@Override
				public void handle(KeyEvent event) {
					if (event.getCode().equals(KeyCode.DELETE)) {
						delete(view);
					}
				}
			});
			view.setItems(list);
			view.setEditable(true);
			ContextMenu contextMenu = new ContextMenu();
			contextMenu.getItems().add(new ActionMenuItem(Lang.get("misc.remove"), () -> delete(view)));
			view.setContextMenu(contextMenu);
			listPane.setCenter(view);
			listPane.setBottom(menuPane);
			TextField newInterface = new TextField();
			newInterface.setOnAction((e) -> add(newInterface, view));
			Button addInterface = new ActionButton(Lang.get("misc.add"), () -> add(newInterface, view));
			menuPane.setCenter(newInterface);
			menuPane.setRight(addInterface);
			setStage("ui.search.skipped", listPane, 300, 500);
		}

		/**
		 * Add member in TextField to ListView.
		 * 
		 * @param text
		 * @param view
		 */
		private void add(TextField text, ListView<String> view) {
			view.itemsProperty().get().add(text.textProperty().get());
			text.textProperty().setValue("");
		}

		/**
		 * Remove selected items from ListView.
		 * 
		 * @param view
		 */
		private void delete(ListView<String> view) {
			MultipleSelectionModel<String> selection = view.selectionModelProperty().getValue();
			for (int index : selection.getSelectedIndices()) {
				view.getItems().remove(index);
			}
		}
	}

	/**
	 * Editor for list of opcode names.
	 * 
	 * @author Matt
	 *
	 * @param <T>
	 */
	public static class OpcodeList<T extends List<String>> extends StagedCustomEditor<T> {
		public OpcodeList(Item item) {
			super(item);
		}

		@Override
		public Node getEditor() {
			return new ActionButton(Lang.get("ui.search.opcode"), () -> open(this));
		}

		/**
		 * Open another window to handle editing of the value.
		 * 
		 * @param opcodes
		 *            CustomEditor instance to for value get/set callbacks.
		 */
		private void open(OpcodeList<T> opcodes) {
			if (staged()) {
				return;
			}
			BorderPane listPane = new BorderPane();
			BorderPane menuPane = new BorderPane();
			ListView<String> view = new ListView<>();
			ObservableList<String> list = FXCollections.observableArrayList(opcodes.getValue());
			list.addListener(new ListChangeListener<String>() {
				@SuppressWarnings("unchecked")
				@Override
				public void onChanged(Change<? extends String> c) {
					setValue((T) list);
				}
			});
			view.setOnKeyPressed(new EventHandler<KeyEvent>() {
				@Override
				public void handle(KeyEvent event) {
					if (event.getCode().equals(KeyCode.DELETE)) {
						delete(view);
					}
				}
			});
			view.setItems(list);
			view.setEditable(true);
			ContextMenu contextMenu = new ContextMenu();
			contextMenu.getItems().add(new ActionMenuItem(Lang.get("misc.remove"), () -> delete(view)));
			view.setContextMenu(contextMenu);
			listPane.setCenter(view);
			listPane.setBottom(menuPane);
			TextField newInterface = new TextField();
			newInterface.setOnAction((e) -> add(newInterface, view));
			Button addInterface = new ActionButton(Lang.get("misc.add"), () -> add(newInterface, view));
			menuPane.setCenter(newInterface);
			menuPane.setRight(addInterface);
			setStage("ui.search.opcode", listPane, 300, 500);
		}

		/**
		 * Add member in TextField to ListView.
		 * 
		 * @param text
		 * @param view
		 */
		private void add(TextField text, ListView<String> view) {
			view.itemsProperty().get().add(text.textProperty().get());
			text.textProperty().setValue("");
		}

		/**
		 * Remove selected items from ListView.
		 * 
		 * @param view
		 */
		private void delete(ListView<String> view) {
			MultipleSelectionModel<String> selection = view.selectionModelProperty().getValue();
			for (int index : selection.getSelectedIndices()) {
				view.getItems().remove(index);
			}
		}
	}
}
