package me.coley.recaf.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import me.coley.event.Bus;
import me.coley.event.Listener;
import me.coley.recaf.Input;
import me.coley.recaf.event.ClassRenameEvent;
import me.coley.recaf.event.SaveStateEvent;
import me.coley.recaf.ui.component.ActionButton;
import me.coley.recaf.util.*;
import me.coley.recaf.workspace.History;
import org.objectweb.asm.tree.ClassNode;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;

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
	private final ListView<History> histories = new ListView<>();
	/**
	 * Content wrapper used to hold currently selected history item.
	 */
	private final BorderPane bp = new BorderPane();
	/**
	 * Current selected history.
	 */
	private History currentSelection;
	/**
	 * List of history values stored in the {@link #currentSelection current
	 * history}.
	 */
	private ListView<Integer> historyEntries;
	/**
	 * Button to revert to the last value in the {@link #currentSelection
	 * current history}.
	 */
	private ActionButton currentRevert;
	/**
	 * Current timestamps of values in the {@link #currentSelection
	 * current history}.
	 */
	private Instant[] times;

	private FxHistory() {
		Bus.subscribe(this);
		setTitle(Lang.get("ui.history"));
		getIcons().add(Icons.LOGO);
		histories.setCellFactory(param -> new ListCell<History>() {
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
		histories.getSelectionModel().selectedItemProperty().addListener((obs, ov, nv) -> {
			if (nv != null) {
				bp.setCenter(getHistoryNode(currentSelection = nv));
			}
		});
		updateList();
		SplitPane sp = new SplitPane(histories, bp);
		sp.setDividerPosition(0, 0.35);
		setScene(JavaFX.scene(sp, 600, 400));
	}

	@Listener
	private void onSave(SaveStateEvent event) {
		Threads.runFx(() -> {
			updateList();
			updateSelected(event.getClasses());
		});
	}

	@Listener()
	private void onClassRename(ClassRenameEvent event) {
		Threads.runFx(() -> {
			updateList();
			updateSelected(Collections.singleton(event.getOriginalName()));

		});
	}

	private void updateList() {
		histories.getItems().clear();
		histories.getItems().addAll(Input.get().getHistory().values());
	}

	private void updateSelected(Collection<String> classes) {
		if (currentSelection != null && !classes.contains(currentSelection.name)) {
			bp.setCenter(null);
		} else if (currentSelection != null) {
			regen(historyEntries, currentSelection);
		}
	}

	private Node getHistoryNode(History history) {
		historyEntries = new ListView<>();
		historyEntries.setEditable(false);
			historyEntries.setCellFactory(cell -> new ListCell<Integer>() {
				final DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(
						getLocale()).withZone(ZoneId.systemDefault());

				@Override
				protected void updateItem(Integer index, boolean empty) {
					super.updateItem(index, empty);
					if (empty || index == null) {
						setText(null);
					} else {
						int i = index.intValue();
						String fmt = formatter.format(times[i]);
						setText(index.toString() + ": " + fmt);
					}
				}

				private Locale getLocale() {
					try {
						return new Locale(System.getProperty("user.language"), System.getProperty("user.country"));
					} catch (Exception e) {}
					return Locale.US;
				}
			});
		
		VBox wraper = new VBox();
		currentRevert = new ActionButton(Lang.get("ui.history.pop"), () -> {
			revert(historyEntries, history);
		});
		wraper.getChildren().add(currentRevert);
		wraper.setPadding(new Insets(9, 9, 9, 9));
		VBox box = new VBox();
		box.getChildren().add(historyEntries);
		box.getChildren().add(wraper);
		box.setAlignment(Pos.TOP_LEFT);
		regen(historyEntries, history);
		return box;
	}

	private void revert(ListView<Integer> histories, History history) {
		Input.get().undo(history.name);
		regen(histories, history);
		currentRevert.setDisable(history.size() == 0);
	}

	private void regen(ListView<Integer> histories, History history) {
		times = history.getFileTimes();
		histories.getItems().clear();
		for (int i = 0; i < history.size(); i++) {
			histories.getItems().add(i);
		}
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
