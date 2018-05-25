package me.coley.recaf.ui;

import java.io.IOException;

import org.objectweb.asm.tree.ClassNode;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import me.coley.event.Bus;
import me.coley.event.Listener;
import me.coley.recaf.Input;
import me.coley.recaf.Input.History;
import me.coley.recaf.Logging;
import me.coley.recaf.event.SaveStateEvent;
import me.coley.recaf.ui.component.ActionButton;
import me.coley.recaf.util.Icons;
import me.coley.recaf.util.JavaFX;
import me.coley.recaf.util.Lang;

/**
 * Window for managing save-states of specific classes.
 * 
 * @author Matt
 */
public class FxHistory extends Stage {
	private final static FxHistory INSTANCE = new FxHistory();
	/**
	 * List of history objects. One for each class with history.
	 */
	private final ListView<History> list = new ListView<>();
	/**
	 * Content wrapper used to hold currently selected history item.
	 */
	private final BorderPane bp = new BorderPane();

	private FxHistory() {
		Bus.INSTANCE.subscribe(this);
		setTitle(Lang.get("ui.history"));
		list.setCellFactory(param -> new ListCell<History>() {
			@Override
			public void updateItem(History item, boolean empty) {
				super.updateItem(item, empty);
				if (empty) {
					// Reset 'hidden' items
					setText(null);
					setGraphic(null);
				} else {
					ClassNode cn = Input.get().getClass(item.name);
					Node fxImage = Icons.getClass(cn.access);
					setText(cn.name);
					setGraphic(fxImage);
				}
			}
		});
		list.getSelectionModel().selectedItemProperty().addListener((obs, ov, nv) -> {
			bp.setCenter(getHistoryNode(nv));
		});
		updateList();
		SplitPane sp = new SplitPane(list, bp);
		sp.setDividerPosition(0, 0.5);
		setScene(JavaFX.scene(sp, 400, 400));
	}

	@Listener
	private void onSave(SaveStateEvent event) {
		Platform.runLater(() -> {
			updateList();
		});
	}

	private void updateList() {
		list.getItems().clear();
		list.getItems().addAll(Input.get().getHistory().values());
	}

	private Node getHistoryNode(History history) {
		VBox box = new VBox();
		Label lbl = new Label(getLabelText(history));
		box.getChildren().add(lbl);
		box.getChildren().add(new ActionButton(Lang.get("ui.history.pop"), () -> {
			revert(lbl, history);
		}));
		TitledPane tp = new TitledPane(history.name, box);
		tp.setCollapsible(false);
		VBox.setVgrow(tp, Priority.ALWAYS);
		return tp;
	}

	private void revert(Label lbl, History history) {
		try {
			Input.get().undo(history.name);
			lbl.setText(getLabelText(history));
		} catch (IOException e) {
			Logging.error(e, true);
		}
	}

	private String getLabelText(History history) {
		return Lang.get("ui.history.stacksize") + history.length;
	}

	/**
	 * Display history window.
	 */
	public static void open() {
		if (INSTANCE.isShowing()) {
			INSTANCE.toFront();
		} else {
			INSTANCE.show();
		}
	}
}
