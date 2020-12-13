package me.coley.recaf.ui.controls.pane;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import me.coley.recaf.Recaf;
import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.plugin.api.InternalPlugin;
import me.coley.recaf.plugin.api.WorkspacePlugin;
import me.coley.recaf.ui.controls.ActionButton;
import me.coley.recaf.ui.controls.SubLabeled;
import me.coley.recaf.util.ClassUtil;
import me.coley.recaf.util.LangUtil;
import me.coley.recaf.util.UiUtil;
import me.coley.recaf.util.struct.InternalBiConsumer;
import me.coley.recaf.workspace.History;
import me.coley.recaf.workspace.JavaResource;
import me.coley.recaf.workspace.Workspace;
import org.apache.commons.codec.digest.DigestUtils;
import org.plugface.core.annotations.Plugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * UI for managing save-states for classes and files.
 *
 * @author Matt
 */
public class HistoryPane extends BorderPane {
	private final ListView<History> list = new ListView<>();
	private final BorderPane view = new BorderPane();
	private final GuiController controller;

	/**
	 * Create the history pane.
	 *
	 * @param controller
	 * 		Controller to pull from.
	 */
	public HistoryPane(GuiController controller) {
		this.controller = controller;
		setup();
		// Ensure access to current/future workspace history maps
		if(controller.getWorkspace() != null) {
			rehookWorkspace();
			update();
		}
	}

	private void setup() {
		list.setCellFactory(cb -> new HistCell());
		list.getSelectionModel().selectedItemProperty().addListener((ob, o, n) -> {
			view.setCenter(createHistoryView(n == null ? o : n));
		});
		view.getStyleClass().add("hist-view");
		SplitPane split = new SplitPane(list, view);
		SplitPane.setResizableWithParent(list, Boolean.FALSE);
		split.setDividerPositions(0.37);
		setCenter(split);
	}

	private Node createHistoryView(History history) {
		byte[] data = history.peek();
		boolean isClass = ClassUtil.isClass(data);
		String type = isClass ? "class" : "file";
		BorderPane pane = new BorderPane();
		// Content
		int count = history.size() - 1;
		String sub = count > 0 ? "[" + count + " states]" : "[Initial state]";
		BorderPane display = new BorderPane();
		display.setTop(new SubLabeled(history.name, sub));
		display.setCenter(new VBox(
				new TextArea(
						"Last updated:          " + history.getMostRecentUpdate() + "\n" +
						"Content length:        " + data.length + "\n" +
						"Content hash (MD5):    " + DigestUtils.md5Hex(data) + "\n" +
						"Content hash (SHA1):   " + DigestUtils.sha1Hex(data) + "\n" +
						"Content hash (SHA256): " + DigestUtils.sha256Hex(data))
		));
		display.getCenter().getStyleClass().add("hist-data");
		pane.setCenter(display);
		// Buttons
		HBox horizontal = new HBox();
		horizontal.getStyleClass().add("hist-buttons");
		horizontal.getChildren().addAll(
				new ActionButton(LangUtil.translate("ui.history.pop"), () -> {
					pop(history, isClass);
					// Update content sub-text
					int updatedCount = history.size() - 1;
					String updatedSub = updatedCount > 0 ? "[" + updatedCount + " states]" : "[Initial state]";
					display.setTop(new SubLabeled(history.name, updatedSub));
					// Update content data
					byte[] updatedData = history.peek();
					display.setCenter(new VBox(
							new Label("Last updated:        " + history.getMostRecentUpdate()),
							new Label("Content length:      " + updatedData.length),
							new Label("Content hash (MD5):  " + DigestUtils.md5Hex(updatedData)),
							new Label("Content hash (SHA1): " + DigestUtils.sha1Hex(updatedData))
					));
					display.getCenter().getStyleClass().add("monospaced");
				}),
				new ActionButton(LangUtil.translate("ui.history.open." + type), () -> open(history, isClass)));
		pane.setBottom(horizontal);
		return pane;
	}

	/**
	 * Pop the most recent change of the given history.
	 *
	 * @param history
	 * 		History of some class/file.
	 * @param isClass
	 * 		If the item is a class.
	 */
	private void pop(History history, boolean isClass) {
		// If the file is open, undo it via the editor viewport.
		String key = history.name;
		if (controller.windows().getMainWindow().getTabs().isOpen(key)) {
			if (isClass)
				controller.windows().getMainWindow().getClassViewport(key).undo();
			else
				controller.windows().getMainWindow().getFileViewport(key).undo();
		}
		// Otherwise, simply pop it.
		else {
			history.pop();
		}
	}

	/**
	 * Open the class/file wrapped by the given history.
	 *
	 * @param history
	 * 		History of some class/file.
	 * @param isClass
	 * 		If the item is a class.
	 */
	private void open(History history, boolean isClass) {
		String key = history.name;
		JavaResource resource = controller.getWorkspace().getPrimary();
		if (isClass)
			controller.windows().getMainWindow().openClass(resource, key);
		else
			controller.windows().getMainWindow().openFile(resource, key);
	}

	/**
	 * Add class/file map listeners so file updates call {@link #update()}.
	 */
	private void rehookWorkspace() {
		// Whenever a class/file is updated, call "update()"
		controller.getWorkspace().getPrimary().getClasses().getPutListeners()
				.add(InternalBiConsumer.internal((name, value) -> update()));
		controller.getWorkspace().getPrimary().getFiles().getPutListeners()
				.add(InternalBiConsumer.internal((name, value) -> update()));
	}

	/**
	 * Update displayed history wrappers in the list-view.
	 */
	private void update() {
		Platform.runLater(() -> {
			History oldSelection = list.getSelectionModel().getSelectedItem();
			list.getItems().clear();
			JavaResource resource = controller.getWorkspace().getPrimary();
			// Get class/file histories that have multiple states, then sort them
			List<History> orderedClasses = new ArrayList<>(resource.getClassHistory().values());
			List<History> orderedFiles = new ArrayList<>(resource.getFileHistory().values());
			orderedClasses.removeIf(History::isAtInitial);
			orderedClasses.sort(Comparator.comparing(o -> o.name));
			orderedFiles.removeIf(History::isAtInitial);
			orderedFiles.sort(Comparator.comparing(o -> o.name));
			// Combine and set as new list-view item-list
			List<History> ordered = new ArrayList<>();
			ordered.addAll(orderedClasses);
			ordered.addAll(orderedFiles);
			list.getItems().addAll(ordered);
			// Update selected view
			if (oldSelection != null && list.getItems().contains(oldSelection))
				list.getSelectionModel().select(oldSelection);
		});
	}

	/**
	 * Cell to display class histories.
	 */
	private static class HistCell extends ListCell<History> {
		@Override
		public void updateItem(History item, boolean empty) {
			super.updateItem(item, empty);
			if(!empty) {
				Node g = null;
				String text = item.name;
				// Create graphic based on history content
				// - (only way to differ between class/file)
				int count = item.size() - 1;
				byte[] data = item.peek();
				if (ClassUtil.isClass(data)) {
					g = UiUtil.createClassGraphic(ClassUtil.getAccess(data));
				} else {
					g = UiUtil.createFileGraphic(text);
				}
				getStyleClass().add("hist-cell");
				BorderPane wrap = new BorderPane();
				String sub = count > 0 ? "[" + count + " states]" : "[Initial state]";
				wrap.setLeft(g);
				wrap.setLeft(new BorderPane(wrap.getLeft()));
				wrap.getLeft().getStyleClass().add("hist-icon");
				wrap.setCenter(new SubLabeled(text, sub, "bold"));
				setGraphic(wrap);
			} else {
				setGraphic(null);
				setText(null);
			}
		}
	}

	/**
	 * Plugin to rehook workspace.
	 */
	@Plugin(name = "History")
	public static final class HistoryPlugin implements WorkspacePlugin, InternalPlugin {
		private final HistoryPane history;

		/**
		 * @param history
		 * 		History pane.
		 */
		public HistoryPlugin(HistoryPane history) {
			this.history = history;
		}

		@Override
		public void onOpened(Workspace workspace) {
			history.rehookWorkspace();
		}

		@Override
		public void onClosed(Workspace workspace) { }

		@Override
		public String getVersion() {
			return Recaf.VERSION;
		}

		@Override
		public String getDescription() {
			return "UI to display items history.";
		}
	}
}
