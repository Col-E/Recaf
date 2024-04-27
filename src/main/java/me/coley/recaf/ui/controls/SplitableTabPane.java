package me.coley.recaf.ui.controls;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.WritableImage;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.transform.Transform;
import javafx.stage.Stage;

import java.awt.*;

/**
 * Simplistic drag-and-drop support for tabs so that they can be split into multiple windows.
 *
 * @author Matt
 */
public class SplitableTabPane extends TabPane {
	private static final SnapshotParameters SNAPSHOT_PARAMETERS;
	private static final String DROP_TARGET_STYLE = "drag-target";
	private static final String TAB_DRAG_KEY = "split-tab";
	private static final ObjectProperty<Tab> draggedTab = new SimpleObjectProperty<>();

	/**
	 * Creates the splittable tab pane.
	 */
	public SplitableTabPane() {
		TabPane selfPane = this;
		/* Disabled in the 2.21.14 update because the splitting logic is garbage. 4x docking is leagues better.
		// Allow this pane to accept transfers
		setOnDragOver(dragEvent -> {
			Dragboard dragboard = dragEvent.getDragboard();
			if (dragboard.hasString()
					&& TAB_DRAG_KEY.equals(dragboard.getString())
					&& draggedTab.get() != null
					&& draggedTab.get().getTabPane() != selfPane) {
				dragEvent.acceptTransferModes(TransferMode.MOVE);
				dragEvent.consume();
			}
		});
		// Setup start drag
		setOnDragDetected(mouseEvent -> {
			if (mouseEvent.getSource() instanceof TabPane) {
				Pane rootPane = (Pane) getScene().getRoot();
				rootPane.setOnDragOver(dragEvent -> {
					dragEvent.acceptTransferModes(TransferMode.MOVE);
					dragEvent.consume();
				});
				draggedTab.setValue(getSelectionModel().getSelectedItem());
				WritableImage snapshot = draggedTab.get().getContent().snapshot(SNAPSHOT_PARAMETERS, null);
				ClipboardContent clipboardContent = new ClipboardContent();
				clipboardContent.put(DataFormat.PLAIN_TEXT, TAB_DRAG_KEY);
				Dragboard db = startDragAndDrop(TransferMode.MOVE);
				db.setDragView(snapshot, 40, 40);
				db.setContent(clipboardContent);
			}
			mouseEvent.consume();
		});
		// Setup end dragging in the case where there is no tab-pane target
		setOnDragDone(dragEvent -> {
			Tab dragged = draggedTab.get();
			if (!dragEvent.isDropCompleted() && dragged != null) {
				createTabStage(dragged).show();
				setCursor(Cursor.DEFAULT);
				dragEvent.consume();
				removeStyle();
			}
		});
		// Setup end dragging in the case where this is the tab-pane target
		setOnDragDropped(dragEvent -> {
			Dragboard dragboard = dragEvent.getDragboard();
			Tab dragged = draggedTab.get();
			if (dragboard.hasString()
					&& TAB_DRAG_KEY.equals(dragboard.getString())
					&& dragged != null) {
				if (dragged.getTabPane() != selfPane) {
					SplitableTabPane owner = (SplitableTabPane) dragged.getTabPane();
					owner.closeTab(dragged);
					getTabs().add(dragged);
					getSelectionModel().select(dragged);
				}
				dragEvent.setDropCompleted(true);
				draggedTab.set(null);
				dragEvent.consume();
				removeStyle();
			}
		});
		// Highlighting with style classes
		setOnDragEntered(dragEvent -> {
			Dragboard dragboard = dragEvent.getDragboard();
			if (dragboard.hasString()
					&& TAB_DRAG_KEY.equals(dragboard.getString())
					&& draggedTab.get() != null
					&& draggedTab.get().getTabPane() != selfPane) {
				addStyle();
			}
		});
		setOnDragExited(dragEvent -> {
			Dragboard dragboard = dragEvent.getDragboard();
			if (dragboard.hasString()
					&& TAB_DRAG_KEY.equals(dragboard.getString())
					&& draggedTab.get() != null
					&& draggedTab.get().getTabPane() != selfPane) {
				removeStyle();
			}
		});*/
	}

	/**
	 * @param tab
	 * 		Tab to remove.
	 */
	public void closeTab(Tab tab) {
		getTabs().remove(tab);
	}

	/**
	 * Create a new tab with the given title and content.
	 *
	 * @param title
	 * 		Tab title.
	 * @param content
	 * 		Tab content.
	 *
	 * @return New tab.
	 */
	protected Tab createTab(String title, Node content) {
		Tab tab = new Tab(title, content);
		tab.setClosable(true);
		return tab;
	}

	/**
	 * Create a new stage for the given tab when it is dropped outside of the scope of a {@link SplitableTabPane}
	 *
	 * @param tab
	 * 		Tab to wrap in a new stage.
	 *
	 * @return New stage.
	 */
	protected Stage createTabStage(Tab tab) {
		// Validate content
		Pane content = (Pane) tab.getContent();
		if (content == null)
			throw new IllegalArgumentException("Cannot detach '" + tab.getText() + "' because content is null");
		// Remove content from tab
		closeTab(tab);
		// Create stage
		SplitableTabPane tabPaneCopy = newTabPane();
		tabPaneCopy.getTabs().add(tab);
		BorderPane root = new BorderPane(tabPaneCopy);
		Scene scene = new Scene(root, root.getPrefWidth(), root.getPrefHeight());
		Stage stage = createStage(tab.getText(), scene);
		// Set location to mouse
		Point mouseLocation = MouseInfo.getPointerInfo().getLocation();
		stage.setX(mouseLocation.getX());
		stage.setY(mouseLocation.getY());
		return stage;
	}

	/**
	 * Create a basic stage with the given title and scene.
	 *
	 * @param title
	 * 		Title for stage.
	 * @param scene
	 * 		Stage content.
	 *
	 * @return New stage.
	 */
	protected Stage createStage(String title, Scene scene) {
		Stage stage = new Stage();
		stage.setScene(scene);
		stage.setTitle(title);
		return stage;
	}

	/**
	 * @return New instance of {@link SplitableTabPane} to be used in a newly created {@link Stage}.
	 */
	protected SplitableTabPane newTabPane() {
		return new SplitableTabPane();
	}

	private void addStyle() {
		getStyleClass().add(DROP_TARGET_STYLE);
	}

	private void removeStyle() {
		getStyleClass().remove(DROP_TARGET_STYLE);
	}

	static {
		SNAPSHOT_PARAMETERS = new SnapshotParameters();
		SNAPSHOT_PARAMETERS.setTransform(Transform.scale(0.4, 0.4));
	}
}
