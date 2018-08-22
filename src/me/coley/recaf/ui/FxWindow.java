package me.coley.recaf.ui;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Supplier;
import org.controlsfx.control.PropertySheet;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import com.sun.javafx.scene.control.skin.ListViewSkin;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.stage.*;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import me.coley.event.*;
import me.coley.recaf.*;
import me.coley.recaf.bytecode.TypeUtil;
import me.coley.recaf.bytecode.search.Parameter;
import me.coley.recaf.config.impl.*;
import me.coley.recaf.event.*;
import me.coley.recaf.plugins.Plugins;
import me.coley.recaf.plugins.Stageable;
import me.coley.recaf.ui.component.*;
import me.coley.recaf.util.*;
import me.coley.recaf.ui.FxWindow.EditPane.EditTabs.FieldInfo;
import me.coley.recaf.ui.FxWindow.EditPane.EditTabs.MethodInfo;

public class FxWindow extends Application {
	private Stage stage;

	@Override
	public void start(Stage stage) {
		// Actions
		Runnable rExport = () -> FileChoosers.export();
		Runnable rLoad = () -> FileChoosers.open();
		Runnable rSave = () -> Bus.post(new RequestSaveStateEvent());
		Runnable rAgentSave = () -> Bus.post(new RequestAgentSaveEvent());
		Runnable rSearch = () -> FxSearch.open();
		Runnable rConfig = () -> FxConfig.open();
		Runnable rHistory = () -> FxHistory.open();
		Runnable rAttach = () -> FxAttach.open();
		BorderPane borderPane = new BorderPane();
		// Menubar
		Menu menuFile = new Menu(Lang.get("ui.menubar.file"));
		menuFile.getItems().add(new ActionMenuItem(Lang.get("ui.menubar.load"), rLoad));
		menuFile.getItems().add(new ActionMenuItem(Lang.get("ui.menubar.export"), rExport));
		if (isAgent()) {
			menuFile.getItems().add(new ActionMenuItem(Lang.get("ui.menubar.agentexport"), rAgentSave));
		}
		Menu menuConfig = new ActionMenu(Lang.get("ui.menubar.config"), rConfig);
		Menu menuSearch = new ActionMenu(Lang.get("ui.menubar.search"), rSearch);
		Menu menuHistory = new Menu(Lang.get("ui.menubar.history"));
		menuHistory.getItems().add(new ActionMenuItem(Lang.get("ui.menubar.history.new"), rSave));
		menuHistory.getItems().add(new ActionMenuItem(Lang.get("ui.menubar.history.view"), rHistory));
		Menu menuAttach = new ActionMenu(Lang.get("ui.menubar.attach"), rAttach);
		Menu menuPlugins = new Menu(Lang.get("ui.menubar.plugins"));
		MenuBar menubar = new MenuBar(menuFile, menuSearch, menuConfig, menuHistory);
		menubar.getStyleClass().add("ui-menu-bar");
		if (Misc.isJDK()) {
			// only add if it is offered by the runtime
			menubar.getMenus().add(menuAttach);
		}
		Threads.runFx(() -> {
			Collection<Stageable> plugins = Plugins.getStageables();
			if (plugins.size() > 0) {
				// only add if there are plugins
				plugins.forEach(pl -> menuPlugins.getItems().add(pl.createMenuItem()));
				menubar.getMenus().add(menuPlugins);
			}
		});
		// Toolbar (easy access menu-bar)
		Button btnNew = new ToolButton(Icons.T_LOAD, rLoad);
		Button btnExport = new ToolButton(Icons.T_EXPORT, rExport);
		Button btnSaveState = new ToolButton(Icons.T_SAVE, rSave);
		Button btnSearch = new ToolButton(Icons.T_SEARCH, rSearch);
		Button btnConfig = new ToolButton(Icons.T_CONFIG, rConfig);
		ToolBar toolbar = new ToolBar(btnNew, btnExport, btnSaveState, btnSearch, btnConfig);
		toolbar.getStyleClass().add("ui-tool-bar");
		// Info tab
		TabPane tabInfo = new TabPane();
		Tab tab = new Tab(Lang.get("ui.info.logging"));
		tab.closableProperty().set(false);
		tab.setContent(new LogPane());
		tabInfo.getTabs().add(tab);
		tab = new Tab(Lang.get("ui.info.other"));
		tab.closableProperty().set(false);
		tab.setContent(new BorderPane());
		// tabInfo.getTabs().add(tab);
		// Organization
		SplitPane vertical = new SplitPane(new EditPane(), tabInfo);
		SplitPane horizontal = new SplitPane(new FilePane(), vertical);
		horizontal.setDividerPositions(0.25);
		vertical.setDividerPositions(0.75);
		vertical.setOrientation(Orientation.VERTICAL);
		VBox top = new VBox();
		top.setPadding(new Insets(0, 0, 0, 0));
		top.setSpacing(0);
		top.getChildren().add(menubar);
		if (ConfDisplay.instance().toolbar) {
			top.getChildren().add(toolbar);
		}
		borderPane.setTop(top);
		borderPane.setCenter(horizontal);
		Scene scene = JavaFX.scene(borderPane, 1200, 800);
		stage.setOnCloseRequest(we -> {
			// closing the primary stage should exit the program
			if (isAgent()) {
				// only exit the javafx platform, the targeted process should
				// still be allowed to run
				Platform.exit();
			} else {
				// kill independent process
				System.exit(0);
			}

		});
		stage.setTitle("Recaf");
		stage.getIcons().add(Icons.LOGO);
		stage.setScene(scene);
		stage.show();
		stage.addEventHandler(KeyEvent.KEY_RELEASED, (KeyEvent e) -> {
			// Only continue of control is held
			ConfKeybinds keys = ConfKeybinds.instance();
			if (keys.active && !e.isControlDown()) {
				return;
			}
			String code = e.getCode().getName();
			if (code.equals(keys.save.toUpperCase())) {
				rSave.run();
			} else if (code.equals(keys.export.toUpperCase())) {
				if (isAgent()) {
					rAgentSave.run();
				} else {
					rExport.run();
				}
			} else if (code.equals(keys.open.toUpperCase())) {
				rLoad.run();
			} else if (code.equals(keys.search.toUpperCase())) {
				rSearch.run();
			}
		});
		this.stage = stage;
		// post notification of completion
		Bus.post(new UiInitEvent(getParameters()));
		Bus.subscribe(this);
	}

	@Listener
	private void onTitleChange(TitleChangeEvent event) {
		stage.setTitle(event.getTitle());
	}

	/**
	 * @return Command line args indicate client invoked as an agent.
	 */
	private boolean isAgent() {
		List<String> jfxArgs = getParameters().getRaw();
		return jfxArgs.contains("-a") || jfxArgs.contains("--agent");
	}

	/**
	 * Pane displaying edit capabilities of a ClassNode.
	 * 
	 * @author Matt
	 *
	 */
	public final static class EditPane extends TabPane {
		/**
		 * Cache of tab titles to their respective tabs.
		 */
		private final Map<String, Tab> cache = new HashMap<>();

		EditPane() {
			Bus.subscribe(this);
			setTabClosingPolicy(TabClosingPolicy.ALL_TABS);
		}

		@Listener
		private void onInputChange(NewInputEvent event) {
			// New input --> clear tabs
			cache.clear();
			getTabs().clear();
		}

		@Listener
		private void onClassRename(ClassRenameEvent event) {
			reloadTab(event.getOriginalName(), event.getNewName());
		}

		@Listener
		private void onFieldRename(FieldRenameEvent rename) {
			String name = rename.getOwner().name;
			reloadTab(name, name);
			Threads.runLater(30, () -> {
				Tab tab = cache.get(name);
				EditTabs edit = (EditTabs) ((BorderPane) tab.getContent()).getCenter();
				edit.getSelectionModel().select(edit.getTabs().get(1));
			});
		}

		@Listener
		private void onMethodRename(MethodRenameEvent rename) {
			String name = rename.getOwner().name;
			Tab tab = cache.get(name);
			EditTabs edit = (EditTabs) ((BorderPane) tab.getContent()).getCenter();
			// reload the tab, then select it
			reloadTab(name, name);
			Threads.runLater(30, () -> {
				edit.getSelectionModel().select(edit.getTabs().get(2));
			});
		}

		private void reloadTab(String originalName, String newName) {
			// Close tab of edited class
			Tab tab = cache.remove(originalName);
			// reopen tab
			if (getTabs().remove(tab)) {
				Threads.runLaterFx(20, () -> {
					Bus.post(new ClassOpenEvent(Input.get().getClass(newName)));
				});
			}
		}

		@Listener
		private void onClassSelect(ClassOpenEvent event) {
			try {
				// Get cached tab via name
				String name = event.getNode().name;
				Tab tab = cache.get(name);
				if (tab == null) {
					// Does not exist, create new tab
					tab = new Tab(name);
					tab.setGraphic(Icons.getClass(event.getNode().access));
					tab.setContent(createContent(event.getNode()));
					// Exit listener to dispose of unused tabs.
					final Tab _tab = tab;
					tab.setOnClosed(o -> {
						cache.remove(name);
						getTabs().remove(_tab);
					});
					cache.put(name, tab);
				}
				// Select tab
				if (getTabs().contains(tab)) {
					getSelectionModel().select(tab);
				} else {
					getTabs().add(tab);
					getSelectionModel().select(tab);
				}
			} catch (Exception e) {
				Logging.error(e);
			}
		}

		@Listener
		private void onInsnSelect(InsnOpenEvent event) {
			try {
				InsnListEditor editor = new InsnListEditor(event.getOwner(), event.getMethod(), event.getInsn());
				Scene scene = JavaFX.scene(editor, 600, 615);
				Stage stage = JavaFX.stage(scene, event.getMethod().name + event.getMethod().desc, true);
				// Handle subscription to events.
				// InsnListEditor listens to ClassDirtyEvent to alert users to
				// bad
				// changes.
				Bus.subscribe(editor);
				stage.setOnHiding(e -> Bus.unsubscribe(editor));
				stage.show();
			} catch (Exception e) {
				Logging.error(e);
			}
		}

		@Listener
		private void onMethodSelect(MethodOpenEvent event) {
			try {
				Scene scene = JavaFX.scene(new MethodEditor(event), 400, 334);
				Stage stage = JavaFX.stage(scene, event.getMethod().name + event.getMethod().desc, true);
				if (event.getContainerNode() instanceof MethodInfo) {
					stage.setOnCloseRequest(a -> ((MethodInfo) event.getContainerNode()).refresh());
				}
				stage.show();
			} catch (Exception e) {
				Logging.error(e);
			}
		}

		@Listener
		private void onFieldSelect(FieldOpenEvent event) {
			try {
				Scene scene = JavaFX.scene(new FieldEditor(event), 400, 214);
				Stage stage = JavaFX.stage(scene, event.getNode().name, true);
				if (event.getContainerNode() instanceof FieldInfo) {
					stage.setOnCloseRequest(a -> ((FieldInfo) event.getContainerNode()).refresh());
				}
				stage.show();
			} catch (Exception e) {
				Logging.error(e);
			}
		}

		/**
		 * Handles creation of the class editor pane.
		 * 
		 * @param node
		 *            ClassNode to edit.
		 * @return
		 */
		private Node createContent(ClassNode node) {
			BorderPane pane = new BorderPane();
			Menu menu = new Menu(node.name, Icons.getClass(node.access));
			MenuBar bar = new MenuBar(menu);
			bar.getStyleClass().add("menubar-class-name");
			pane.setTop(bar);
			pane.setCenter(new EditTabs(node));
			return pane;
		}

		/**
		 * Editor pane for some MethodNode.
		 * 
		 * @author Matt
		 */
		private final static class MethodEditor extends BorderPane {
			public MethodEditor(MethodOpenEvent event) {
				PropertySheet propertySheet = new ReflectivePropertySheet(event.getMethod()) {
					@Override
					protected void setupItems(Object instance) {
						for (Field field : Reflect.fields(instance.getClass())) {
							String name = field.getName();
							// Skip attrs
							if (name.equals("attrs") || name.equals("visited")) {
								continue;
							}
							// Skip stack info if it will be recalculated
							// on-exit
							if (ConfASM.instance().ignoreMaxs() && (name.contains("max"))) {
								continue;
							}
							// TODO: Skip annos until AnnoListEditor is done
							if (name.contains("Anno")) {
								continue;
							}
							String group = "ui.bean.method";
							field.setAccessible(true);
							// Setup item & add to list
							getItems().add(new ReflectiveMethodNodeItem(event.getOwner(), (MethodNode) instance, field, group,
									field.getName().toLowerCase()));
						}
					}
				};
				propertySheet.setModeSwitcherVisible(false);
				propertySheet.setSearchBoxVisible(false);
				VBox.setVgrow(propertySheet, Priority.ALWAYS);
				setCenter(propertySheet);
			}
		}

		/**
		 * Editor pane for some FieldNode.
		 * 
		 * @author Matt
		 */
		private final static class FieldEditor extends BorderPane {
			public FieldEditor(FieldOpenEvent event) {
				PropertySheet propertySheet = new ReflectivePropertySheet(event.getNode()) {
					@Override
					protected void setupItems(Object instance) {
						for (Field field : Reflect.fields(instance.getClass())) {
							String name = field.getName();
							// Skip attrs
							if (name.equals("attrs")) {
								continue;
							}
							// TODO: Skip annos until AnnoListEditor is done
							if (name.contains("Anno")) {
								continue;
							}
							String group = "ui.bean.field";
							field.setAccessible(true);
							// Setup item & add to list
							getItems().add(new ReflectiveFieldNodeItem(event.getOwner(), (FieldNode) instance, field, group, field
									.getName().toLowerCase()));
						}
					}
				};
				propertySheet.setModeSwitcherVisible(false);
				propertySheet.setSearchBoxVisible(false);
				VBox.setVgrow(propertySheet, Priority.ALWAYS);
				setCenter(propertySheet);
			}
		}

		/**
		 * Pane displaying editable attributes of a ClassNode.
		 * 
		 * @author Matt
		 */
		public final static class EditTabs extends TabPane {
			public EditTabs(ClassNode node) {
				setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);
				getTabs().add(new Tab(Lang.get("ui.edit.tab.classinfo"), new ClassInfo(node)));
				getTabs().add(new Tab(Lang.get("ui.edit.tab.fields"), new FieldInfo(node, node.fields)));
				getTabs().add(new Tab(Lang.get("ui.edit.tab.methods"), new MethodInfo(node, node.methods)));
			}

			private final static class ClassInfo extends BorderPane {
				public ClassInfo(ClassNode node) {
					PropertySheet propertySheet = new ReflectivePropertySheet(node) {
						@Override
						protected void setupItems(Object instance) {
							for (Field field : Reflect.fields(instance.getClass())) {
								// Skip fields/methods/attrs
								String name = field.getName();
								// skip experimental values
								if (name.toLowerCase().contains("exper")) {
									continue;
								}
								// skip, we have separate tabs for these
								if (name.equals("fields") || name.equals("methods")) {
									continue;
								}
								// TODO: Implement module, figure out kinds of
								// data allowed in attrs
								if (name.equals("module") || name.equals("attrs")) {
									continue;
								}
								// Set accessible and check determine if field
								// type is simply represented.
								field.setAccessible(true);
								// Setup item & add to list
								getItems().add(new ReflectiveClassNodeItem(instance, field, "ui.bean.class", field.getName()
										.toLowerCase()));
							}
						}
					};
					propertySheet.setModeSwitcherVisible(false);
					propertySheet.setSearchBoxVisible(false);
					propertySheet.getItems().add(new DecompileItem(node));
					VBox.setVgrow(propertySheet, Priority.ALWAYS);
					setCenter(propertySheet);
				}
			}

			/**
			 * Table of MethodNodes.
			 * 
			 * @author Matt
			 */
			public final static class MethodInfo extends TableView<MethodNode> {
				@SuppressWarnings("unchecked")
				public MethodInfo(ClassNode owner, List<MethodNode> methods) {
					MethodInfo info = this;
					setOnMouseClicked(new EventHandler<MouseEvent>() {
						@Override
						public void handle(MouseEvent e) {
							// Double click to open class
							if ((e.getClickCount() == 2 && e.getButton() == MouseButton.PRIMARY) || (e
									.getButton() == MouseButton.MIDDLE)) {
								MethodNode mn = getSelectionModel().getSelectedItem();
								Bus.post(new MethodOpenEvent(owner, mn, info));
							}
						}
					});
					getItems().addListener((ListChangeListener.Change<? extends MethodNode> c) -> {
						while (c.next()) {
							if (c.wasRemoved() || c.wasAdded()) {
								Bus.post(new ClassDirtyEvent(owner));
							}
						}
					});
					TableColumn<MethodNode, Integer> colFlags = new TableColumn<>(Lang.get("ui.edit.tab.methods.flags"));
					TableColumn<MethodNode, String> colName = new TableColumn<>(Lang.get("ui.edit.tab.methods.name"));
					TableColumn<MethodNode, Type> colRet = new TableColumn<>(Lang.get("ui.edit.tab.methods.return"));
					TableColumn<MethodNode, Type[]> colArgs = new TableColumn<>(Lang.get("ui.edit.tab.methods.args"));
					setFixedCellSize(20); // fixed cell height
					getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
					getColumns().addAll(colFlags, colRet, colName, colArgs);
					colFlags.setCellValueFactory(cell -> JavaFX.observable(cell.getValue().access));
					colFlags.setCellFactory(cell -> new TableCell<MethodNode, Integer>() {
						@Override
						protected void updateItem(Integer flags, boolean empty) {
							super.updateItem(flags, empty);
							if (empty || flags == null) {
								setGraphic(null);
							} else {
								setGraphic(Icons.getMember(true, flags));
							}
						}
					});
					colFlags.setComparator(Integer::compare);
					colName.setCellValueFactory(cell -> JavaFX.observable(cell.getValue().name));
					colName.setCellFactory(cell -> new TableCell<MethodNode, String>() {
						@Override
						protected void updateItem(String name, boolean empty) {
							super.updateItem(name, empty);
							if (empty || name == null) {
								setGraphic(null);
							} else {
								setGraphic(FormatFactory.name(name));
							}
						}
					});
					colName.setComparator(Comparator.comparing(String::toString));
					colRet.setCellValueFactory(cell -> JavaFX.observable(Type.getType(cell.getValue().desc).getReturnType()));
					colRet.setCellFactory(cell -> new TableCell<MethodNode, Type>() {
						@Override
						protected void updateItem(Type type, boolean empty) {
							super.updateItem(type, empty);
							if (empty || type == null) {
								setGraphic(null);
							} else {
								setGraphic(FormatFactory.type(type));
							}
						}
					});
					colRet.setComparator(Comparator.comparing(Type::toString));
					colRet.setComparator(new Comparator<Type>() {
						@Override
						public int compare(Type o1, Type o2) {
							// Compare, ensure if descriptors are simplified
							// they are sorted properly to match displayed
							// results.
							String s1 = TypeUtil.filter(o1);
							String s2 = TypeUtil.filter(o2);
							return Comparator.comparing(String::toString).compare(s1, s2);
						}
					});
					colArgs.setCellValueFactory(cell -> JavaFX.observable(Type.getType(cell.getValue().desc).getArgumentTypes()));
					colArgs.setCellFactory(cell -> new TableCell<MethodNode, Type[]>() {
						@Override
						protected void updateItem(Type[] types, boolean empty) {
							super.updateItem(types, empty);
							if (empty || types == null || types.length == 0) {
								setGraphic(null);
							} else {
								setGraphic(FormatFactory.typeArray(types));
							}
						}
					});
					colArgs.setComparator(new Comparator<Type[]>() {
						@Override
						public int compare(Type[] o1, Type[] o2) {
							int len = Math.min(o1.length, o2.length);
							for (int i = 0; i < len; i++) {
								// Compare, ensure if descriptors are simplified
								// they are sorted properly to match displayed
								// results.
								int c = Comparator.comparing(String::toString).compare(TypeUtil.filter(o1[i]), TypeUtil.filter(
										o2[i]));
								if (c != 0) {
									return c;
								}
							}
							// in case of recurring matches
							if (o1.length == o2.length) {
								return 0;
							}
							return o1.length < o2.length ? -1 : 1;
						}
					});
					setItems(FXCollections.observableArrayList(methods));
					// context menu
					ContextMenu ctxBase = new ContextMenu();
					ContextMenu ctx = new ContextMenu();
					ctxBase.getItems().add(new ActionMenuItem(Lang.get("misc.add"), () -> {
						MethodNode mn = new MethodNode(0, "temp", "()V", null, null);
						methods.add(mn);
						getItems().add(mn);
					}));
					setContextMenu(ctxBase);
					ctx.getItems().add(new ActionMenuItem(Lang.get("ui.bean.method.instructions.name"), () -> {
						MethodNode mn = getSelectionModel().getSelectedItem();
						Bus.post(new InsnOpenEvent(owner, mn, null));
					}));
					ctx.getItems().add(new ActionMenuItem(Lang.get("ui.bean.class.decompile.name"), () -> {
						MethodNode mn = getSelectionModel().getSelectedItem();
						DecompileItem decomp = new DecompileItem(owner, mn);
						decomp.decompile();
					}));
					ctx.getItems().add(new ActionMenuItem(Lang.get("ui.search.reference"), () -> {
						MethodNode mn = getSelectionModel().getSelectedItem();
						FxSearch.open(Parameter.references(owner.name, mn.name, mn.desc));
					}));
					ctx.getItems().add(new ActionMenuItem(Lang.get("misc.add"), () -> {
						MethodNode mn = new MethodNode(0, "temp", "()V", null, null);
						methods.add(mn);
						getItems().add(mn);
					}));
					ctx.getItems().add(new ActionMenuItem(Lang.get("misc.remove"), () -> {
						int i = getSelectionModel().getSelectedIndex();
						methods.remove(i);
						getItems().remove(i);
					}));
					ctx.getItems().add(new ActionMenuItem(Lang.get("misc.duplicate"), () -> {
						MethodNode sel = getSelectionModel().getSelectedItem();
						String[] exceptions = sel.exceptions.toArray(new String[0]);
						MethodNode copy = new MethodNode(sel.access, sel.name + "_copy", sel.desc, sel.signature, exceptions);
						sel.accept(copy);
						methods.add(copy);
						getItems().add(copy);
					}));
					// only allow when item is selected
					getSelectionModel().selectedIndexProperty().addListener((c) -> {
						setContextMenu(getSelectionModel().getSelectedIndex() == -1 ? null : ctx);
					});
					// mark class as dirty when items list changes.
					getItems().addListener((ListChangeListener.Change<? extends MethodNode> c) -> {
						Bus.post(new ClassDirtyEvent(owner));
					});
				}
			}

			/**
			 * Table of FieldNodes.
			 * 
			 * @author Matt
			 */
			public final static class FieldInfo extends TableView<FieldNode> {
				@SuppressWarnings("unchecked")
				public FieldInfo(ClassNode owner, List<FieldNode> fields) {
					FieldInfo info = this;
					setOnMouseClicked(new EventHandler<MouseEvent>() {
						@Override
						public void handle(MouseEvent e) {
							// Double click to open class
							if ((e.getClickCount() == 2 && e.getButton() == MouseButton.PRIMARY) || (e
									.getButton() == MouseButton.MIDDLE)) {
								FieldNode fn = getSelectionModel().getSelectedItem();
								Bus.post(new FieldOpenEvent(owner, fn, info));
							}
						}
					});
					getItems().addListener((ListChangeListener.Change<? extends FieldNode> c) -> {
						while (c.next()) {
							if (c.wasRemoved() || c.wasAdded()) {
								Bus.post(new ClassDirtyEvent(owner));
							}
						}
					});
					TableColumn<FieldNode, Integer> colFlags = new TableColumn<>(Lang.get("ui.edit.tab.fields.flags"));
					TableColumn<FieldNode, String> colName = new TableColumn<>(Lang.get("ui.edit.tab.fields.name"));
					TableColumn<FieldNode, Type> colRet = new TableColumn<>(Lang.get("ui.edit.tab.fields.type"));
					getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
					setFixedCellSize(20); // fixed cell height
					getColumns().addAll(colFlags, colRet, colName);
					colFlags.setCellValueFactory(cell -> JavaFX.observable(cell.getValue().access));
					colFlags.setCellFactory(cell -> new TableCell<FieldNode, Integer>() {
						@Override
						protected void updateItem(Integer flags, boolean empty) {
							super.updateItem(flags, empty);
							if (empty || flags == null) {
								setGraphic(null);
							} else {
								setGraphic(Icons.getMember(false, flags));
							}
						}
					});
					colFlags.setComparator(Integer::compare);
					colName.setCellValueFactory(cell -> JavaFX.observable(cell.getValue().name));
					colName.setCellFactory(cell -> new TableCell<FieldNode, String>() {
						@Override
						protected void updateItem(String name, boolean empty) {
							super.updateItem(name, empty);
							if (empty || name == null) {
								setGraphic(null);
							} else {
								setGraphic(FormatFactory.name(name));
							}
						}
					});
					colName.setComparator(Comparator.comparing(String::toString));
					colRet.setCellValueFactory(cell -> JavaFX.observable(Type.getType(cell.getValue().desc)));
					colRet.setCellFactory(cell -> new TableCell<FieldNode, Type>() {
						@Override
						protected void updateItem(Type type, boolean empty) {
							super.updateItem(type, empty);
							if (empty || type == null) {
								setGraphic(null);
							} else {
								setGraphic(FormatFactory.type(type));
							}
						}
					});
					colRet.setComparator(Comparator.comparing(Type::toString));
					colRet.setComparator(new Comparator<Type>() {
						@Override
						public int compare(Type o1, Type o2) {
							// Compare, ensure if descriptors are simplified
							// they are sorted properly to match displayed
							// results.
							String s1 = TypeUtil.filter(o1);
							String s2 = TypeUtil.filter(o2);
							return Comparator.comparing(String::toString).compare(s1, s2);
						}
					});
					setItems(FXCollections.observableArrayList(fields));
					// context menu
					ContextMenu ctxBase = new ContextMenu();
					ContextMenu ctx = new ContextMenu();
					ctxBase.getItems().add(new ActionMenuItem(Lang.get("misc.add"), () -> {
						FieldNode fn = new FieldNode(0, "temp", "I", null, null);
						fields.add(fn);
						getItems().add(fn);
					}));
					setContextMenu(ctxBase);
					ctx.getItems().add(new ActionMenuItem(Lang.get("ui.search.reference"), () -> {
						FieldNode fn = getSelectionModel().getSelectedItem();
						FxSearch.open(Parameter.references(owner.name, fn.name, fn.desc));
					}));
					ctx.getItems().add(new ActionMenuItem(Lang.get("misc.add"), () -> {
						FieldNode fn = new FieldNode(0, "temp", "I", null, null);
						fields.add(fn);
						getItems().add(fn);
					}));
					ctx.getItems().add(new ActionMenuItem(Lang.get("misc.remove"), () -> {
						int i = getSelectionModel().getSelectedIndex();
						fields.remove(i);
						getItems().remove(i);
					}));
					// only allow when item is selected
					getSelectionModel().selectedIndexProperty().addListener((c) -> {
						setContextMenu(getSelectionModel().getSelectedIndex() == -1 ? null : ctx);
					});
					// mark class as dirty when items list changes.
					getItems().addListener((ListChangeListener.Change<? extends FieldNode> c) -> {
						Bus.post(new ClassDirtyEvent(owner));
					});
				}
			}
		}
	}

	/**
	 * Pane displaying file-tree of loaded classes.
	 * 
	 * @author Matt
	 */
	private static final class FilePane extends BorderPane {
		private final TreeView<String> tree = new TreeView<>();
		private Input input;

		FilePane() {
			Bus.subscribe(this);
			setCenter(tree);
			// drag-drop support for inputs
			tree.setOnDragOver(new EventHandler<DragEvent>() {
				@Override
				public void handle(DragEvent e) {
					if (e.getGestureSource() != tree && e.getDragboard().hasFiles()) {
						e.acceptTransferModes(TransferMode.COPY_OR_MOVE);
					}
					e.consume();
				}
			});
			tree.setOnDragDropped(e -> {
				Dragboard db = e.getDragboard();
				if (db.hasFiles()) {
					NewInputEvent.call(db.getFiles().get(0));
				}
			});
			// Custom tree renderering.
			tree.setShowRoot(false);
			tree.setCellFactory(param -> new TreeCell<String>() {
				@Override
				public void updateItem(String item, boolean empty) {
					super.updateItem(item, empty);
					if (empty || item == null) {
						// Hide elements.
						// Items enter this state when 'hidden' in the tree.
						setText(null);
						setGraphic(null);
					} else {
						boolean cont = input.getClasses().containsKey(item);
						Node fxImage = cont ? Icons.getClass(input.getClass(item).access) : new ImageView(Icons.CL_PACKAGE);
						setGraphic(fxImage);
						String name = cont ? trim(item) : item;
						int max = ConfDisplay.instance().maxLengthTree;
						if (name.length() > max) {
							name = name.substring(0, max);
						}
						setText(name);
					}
				}
			});
			tree.setOnMouseClicked(new EventHandler<MouseEvent>() {
				@Override
				public void handle(MouseEvent e) {
					// Double click to open class
					if (e.getClickCount() == 2) {
						FileTreeItem item = (FileTreeItem) tree.getSelectionModel().getSelectedItem();
						if (item != null && !item.isDir) {
							ClassNode cn = item.get();
							if (cn != null) {
								Bus.post(new ClassOpenEvent(cn));
							}
						}
					}
				}
			});
			Bus.subscribe(this);
			Platform.runLater(() -> tree.requestFocus());
		}

		/**
		 * Resets the tree to match content of input.
		 * 
		 * @param input
		 */
		@Listener
		private void onInputChange(NewInputEvent input) {
			this.input = input.get();
			tree.setRoot(getNodesForDirectory(this.input));
			this.input.registerLoadListener();
		}

		/**
		 * Add new files to the tree as they are loaded.
		 * 
		 * @param load
		 */
		@Listener
		private void onInstrumentedClassLoad(ClassLoadInstrumentedEvent load) {
			FileTreeItem root = (FileTreeItem) tree.getRoot();
			if (root == null) {
				root = new FileTreeItem("root");
				tree.setRoot(root);
			}
			String name = load.getName();
			addToRoot(root, name);
		}

		/**
		 * Move a class being renamed to a new tree-path.
		 * 
		 * @param rename
		 */
		@Listener
		private void onClassRenamed(ClassRenameEvent rename) {
			Platform.runLater(() -> {
				FileTreeItem item = getNode(rename.getOriginalName());
				FileTreeItem parent = (FileTreeItem) item.getParent();
				parent.remove(item);
				addToRoot((FileTreeItem) tree.getRoot(), rename.getNewName());
			});
		}

		/**
		 * Create root for input.
		 * 
		 * @param input
		 * @return {@code FileTreeItem}.
		 */
		private final FileTreeItem getNodesForDirectory(Input input) {
			FileTreeItem root = new FileTreeItem("root");
			input.classes.forEach(name -> {
				addToRoot(root, name);
			});
			return root;
		}

		/**
		 * Add name to root assuming it is loaded in the current Input.
		 * 
		 * @param root
		 *            Root node.
		 * @param name
		 *            Name of class in input.
		 */
		private void addToRoot(FileTreeItem root, String name) {
			FileTreeItem r = root;
			String[] parts = name.split("/");
			for (int i = 0; i < parts.length; i++) {
				String part = parts[i];
				if (i == parts.length - 1) {
					// add final file
					r.addFile(part, name);
				} else if (r.hasDir(part)) {
					// navigate to sub-directory
					r = r.getDir(part);
				} else {
					// add sub-dir
					r = r.addDir(part);
				}
			}
		}

		private FileTreeItem getNode(String name) {
			FileTreeItem r = (FileTreeItem) tree.getRoot();
			String[] parts = name.split("/");
			for (int i = 0; i < parts.length; i++) {
				String part = parts[i];
				if (i == parts.length - 1) {
					// get final file
					r = r.getFile(part);
				} else {
					// get sub-dir
					r = r.getDir(part);
				}
			}
			return r;
		}

		/**
		 * Trim the text to a last section, if needed.
		 * 
		 * @param item
		 *            Internal class name.
		 * @return Simple name.
		 */
		private static String trim(String item) {
			return item.indexOf("/") > 0 ? item.substring(item.lastIndexOf("/") + 1) : item;
		}

		/**
		 * Wrapper for TreeItem children set. Allows more file-system-like
		 * access.
		 * 
		 * @author Matt
		 */
		private class FileTreeItem extends TreeItem<String> implements Comparable<String> {
			// Split in case of cases like:
			// a/a/a.class
			// a/a/a/a.class
			private final Map<String, FileTreeItem> dirs = new TreeMap<>();
			private final Map<String, FileTreeItem> files = new TreeMap<>();
			private final Supplier<ClassNode> file;
			private final boolean isDir;

			// unused 'i' for differing constructors
			private FileTreeItem(String name, int i) {
				this.file = () -> input.getClass(name);
				setValue(name);
				isDir = false;
				get();
			}

			private FileTreeItem(String name) {
				this.file = () -> null;
				setValue(name);
				isDir = true;
			}

			public ClassNode get() {
				return file.get();
			}

			FileTreeItem addDir(String name) {
				FileTreeItem fti = new FileTreeItem(name);
				dirs.put(name, fti);
				addOrdered(fti);
				return fti;
			}

			void addFile(String part, String name) {
				FileTreeItem fti = new FileTreeItem(name, 0);
				files.put(part, fti);
				addOrdered(fti);
			}

			private void addOrdered(FileTreeItem fti) {
				try {
					int sizeD = dirs.size();
					int sizeF = files.size();
					int size = sizeD + sizeF;
					if (size == 0) {
						getChildren().add(fti);
						return;
					}
					if (fti.isDir) {
						FileTreeItem[] array = dirs.values().toArray(new FileTreeItem[0]);
						int index = Arrays.binarySearch(array, fti.getValue());
						if (index < 0) {
							index = (index * -1) - 1;
						}
						getChildren().add(index, fti);
					} else {
						FileTreeItem[] array = files.values().toArray(new FileTreeItem[0]);
						int index = Arrays.binarySearch(array, fti.getValue());
						if (index < 0) {
							index = (index * -1) - 1;
						}
						getChildren().add(sizeD + index, fti);
					}
				} catch (Exception e) {
					Logging.fatal(e);
				}
			}

			public void remove(FileTreeItem item) {
				String name = trim(item.getValue());
				if (item.isDir) {
					dirs.remove(name);
				} else {
					files.remove(name);
				}
				getChildren().remove(item);
			}

			FileTreeItem getDir(String name) {
				return dirs.get(name);
			}

			boolean hasDir(String name) {
				return dirs.containsKey(name);
			}

			FileTreeItem getFile(String name) {
				return files.get(name);
			}

			@Override
			public int compareTo(String s) {
				return getValue().compareTo(s);
			}
		}
	}

	/**
	 * Pane displaying logging information.
	 * 
	 * @author Matt
	 */
	private final static class LogPane extends BorderPane {
		private final ListView<LogEvent> list = new ListView<>();

		LogPane() {
			Bus.subscribe(this);
			setCenter(list);
			list.setSkin(new RefreshableSkin(list));
			// Click-to-toggle log expansion
			list.setOnMousePressed(new EventHandler<MouseEvent>() {
				@Override
				public void handle(MouseEvent e) {
					((RefreshableSkin) list.getSkin()).refresh();
				}
			});
			// Log rendering
			list.setCellFactory(param -> new ListCell<LogEvent>() {
				@Override
				public void updateItem(LogEvent item, boolean empty) {
					super.updateItem(item, empty);
					if (empty) {
						// Reset 'hidden' items
						setGraphic(null);
						setText(null);
					} else {
						// Get icon for quick level identification
						Image fxImage = Icons.getLog(item.getLevel());
						ImageView imageView = new ImageView(fxImage);
						setGraphic(imageView);
						// Set log, check if it should be collapsed.
						LogEvent selected = list.selectionModelProperty().getValue().getSelectedItem();
						boolean isSelected = (selected != null) && selected.equals(item);
						if (isSelected) {
							setText(item.getMessage());
						} else {
							String substr = item.getMessage();
							if (substr.contains("\n")) {
								substr = substr.substring(0, substr.indexOf("\n"));
							}
							setText(substr);
						}
					}
				}
			});
		}

		@Listener
		public void onLog(LogEvent event) {
			// print if within logging detail level
			if (event.getLevel().ordinal() >= ConfDisplay.instance().loglevel.ordinal()) {
				list.getItems().add(event);
				Platform.runLater(() -> {
					list.scrollTo(list.getItems().size() - 1);
				});

			}
		}

		/**
		 * Skin that allows access to recreation of cells.
		 * 
		 * @author Matt
		 */
		static class RefreshableSkin extends ListViewSkin<LogEvent> {
			public RefreshableSkin(ListView<LogEvent> listView) {
				super(listView);
			}

			/**
			 * Recreate cells.
			 */
			public void refresh() {
				// publicise protected data
				super.flow.recreateCells();
			}
		}
	}

	/**
	 * Graphic-only button that runs an action when clicked.
	 * 
	 * @author Matt
	 */
	public final static class ToolButton extends Button {
		public ToolButton(ImageView graphic, Runnable action) {
			setGraphic(graphic);
			getStyleClass().add("toolbutton");
			setOnAction(o -> action.run());
		}
	}

	/**
	 * Menu <i>(Has no children)</i> that runs an action when clicked.
	 * 
	 * @author Matt
	 */
	private final static class ActionMenu extends Menu {
		ActionMenu(String text, Runnable action) {
			// This is a hack: https://stackoverflow.com/a/10317260
			// Works well enough without having to screw with CSS.
			Label label = new Label(text);
			setGraphic(label);
			label.setOnMouseClicked(o -> action.run());
		}
	}

	/**
	 * Entry point. Explicitly declared because invoking launch from another
	 * class doesn't fly well with JavaFX.
	 * 
	 * @param args
	 */
	public static void init(String[] args) {
		launch(args);
	}
}
