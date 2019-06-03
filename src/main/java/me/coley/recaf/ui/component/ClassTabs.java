package me.coley.recaf.ui.component;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import me.coley.event.Bus;
import me.coley.event.Listener;
import me.coley.recaf.Input;
import me.coley.recaf.Logging;
import me.coley.recaf.event.*;
import me.coley.recaf.ui.component.editor.InsnListEditor;
import me.coley.recaf.util.*;
import org.objectweb.asm.tree.ClassNode;

import java.util.HashMap;
import java.util.Map;

/**
 * Tab control of classes being edited.
 * 
 * @author Matt
 *
 */
public class ClassTabs extends TabPane {
	/**
	 * Cache of tab titles to their respective tabs.
	 */
	private final Map<String, Tab> classToTab = new HashMap<>();

	public ClassTabs() {
		Bus.subscribe(this);
		setTabClosingPolicy(TabClosingPolicy.ALL_TABS);
	}

	@Listener
	private void onInputChange(NewInputEvent event) {
		// New input --> clear tabs
		classToTab.clear();
		getTabs().clear();
	}

	@Listener
	private void onClassReload(ClassReloadEvent event) {
		// TODO: Check if names are in #classToTab
		// TODO: Check if name is referenced as a super/interface
		Threads.runFx(() -> reloadTab(event.getName(), event.getNewName()));
	}

	@Listener
	private void onFieldRename(FieldRenameEvent rename) {
		// The tab will be reloaded externally, use this to reselect the fields's tab
		Threads.runLaterFx(100, () -> {
			String name = rename.getOwner().name;
			Tab tab = classToTab.get(name);
			TabPane edit = (TabPane) ((BorderPane) tab.getContent()).getCenter();
			edit.getSelectionModel().select(edit.getTabs().get(1));
		});
	}

	@Listener
	private void onMethodRename(MethodRenameEvent rename) {
		// The tab will be reloaded externally, use this to reselect the method's tab
		Threads.runLaterFx(100, () -> {
			String name = rename.getOwner().name;
			Tab tab = classToTab.get(name);
			TabPane edit = (TabPane) ((BorderPane) tab.getContent()).getCenter();
			edit.getSelectionModel().select(edit.getTabs().get(2));
		});
	}

	private void reloadTab(String name) {
		reloadTab(name, name);
	}

	private void reloadTab(String originalName, String newName) {
		// Close tab of edited class
		Tab tab = classToTab.remove(originalName);
		// reopen tab
		if (tab != null && getTabs().remove(tab)) {
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
			Tab tab = classToTab.get(name);
			if (tab == null) {
				// Does not exist, create new tab
				tab = new Tab(name);
				tab.setGraphic(Icons.getClass(event.getNode().access));
				tab.setContent(createContent(event.getNode()));
				// Exit listener to dispose of unused tabs.
				final Tab _tab = tab;
				tab.setOnClosed(o -> {
					classToTab.remove(name);
					getTabs().remove(_tab);
				});
				classToTab.put(name, tab);
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
			// InsnListEditor listens to ClassDirtyEvent so that
			// bad-code changes can show warnings.
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
			Scene scene = JavaFX.scene(new MethodEditor(event), 400, 520);
			Stage stage = JavaFX.stage(scene, event.getMethod().name + event.getMethod().desc, true);
			if (event.getContainerNode() != null && event.getContainerNode() instanceof MethodTable) {
				stage.setOnCloseRequest(a -> ((MethodTable) event.getContainerNode()).refresh());
			}
			stage.show();
		} catch (Exception e) {
			Logging.error(e);
		}
	}

	@Listener
	private void onFieldSelect(FieldOpenEvent event) {
		try {
			Scene scene = JavaFX.scene(new FieldEditor(event), 400, 300);
			Stage stage = JavaFX.stage(scene, event.getNode().name, true);
			if (event.getContainerNode() != null && event.getContainerNode() instanceof FieldTable) {
				stage.setOnCloseRequest(a -> ((FieldTable) event.getContainerNode()).refresh());
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
		// TabPane representing a class:
		// - class info
		// - field table
		// - method table
		TabPane editPane = new TabPane();
		editPane.setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);
		editPane.getTabs().add(new Tab(Lang.get("ui.edit.tab.classinfo"), new ClassInfoSheet(node)));
		editPane.getTabs().add(new Tab(Lang.get("ui.edit.tab.fields"), new FieldTable(node, node.fields)));
		editPane.getTabs().add(new Tab(Lang.get("ui.edit.tab.methods"), new MethodTable(node, node.methods)));
		pane.setCenter(editPane);
		return pane;
	}
}
