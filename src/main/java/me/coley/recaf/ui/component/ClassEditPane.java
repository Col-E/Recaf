package me.coley.recaf.ui.component;

import java.util.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import me.coley.event.*;
import me.coley.recaf.event.*;
import me.coley.recaf.Input;
import me.coley.recaf.Logging;
import me.coley.recaf.ui.component.editor.*;
import me.coley.recaf.util.*;
import org.objectweb.asm.tree.*;

/**
 * Pane displaying edit capabilities of a ClassNode.
 * 
 * @author Matt
 *
 */
public class ClassEditPane extends TabPane {
	/**
	 * Cache of tab titles to their respective tabs.
	 */
	private final Map<String, Tab> cache = new HashMap<>();

	public ClassEditPane() {
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
	private void onClassRevert(ClassRecompileEvent event) {
		reloadTab(event.getOldNode().name);
	}

	@Listener
	private void onClassRevert(HistoryRevertEvent event) {
		reloadTab(event.getName());
	}

	@Listener
	private void onFieldRename(FieldRenameEvent rename) {
		String name = rename.getOwner().name;
		reloadTab(name);
		Threads.runLater(30, () -> {
			Tab tab = cache.get(name);
			ClassEditTabs edit = (ClassEditTabs) ((BorderPane) tab.getContent()).getCenter();
			edit.getSelectionModel().select(edit.getTabs().get(1));
		});
	}

	@Listener
	private void onMethodRename(MethodRenameEvent rename) {
		String name = rename.getOwner().name;
		Tab tab = cache.get(name);
		ClassEditTabs edit = (ClassEditTabs) ((BorderPane) tab.getContent()).getCenter();
		// reload the tab, then select it
		reloadTab(name);
		Threads.runLater(30, () -> {
			edit.getSelectionModel().select(edit.getTabs().get(2));
		});
	}

	private void reloadTab(String name) {
		reloadTab(name, name);
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
		pane.setCenter(new ClassEditTabs(node));
		return pane;
	}
}
