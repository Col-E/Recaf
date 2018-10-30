package me.coley.recaf.ui.component;

import javafx.event.*;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import me.coley.event.*;
import me.coley.recaf.config.impl.ConfDisplay;
import me.coley.recaf.event.*;
import me.coley.recaf.util.*;
import com.sun.javafx.scene.control.skin.ListViewSkin;

/**
 * Pane displaying logging information.
 * 
 * @author Matt
 */
public class LoggingPane extends BorderPane {
	private final ListView<LogEvent> list = new ListView<>();

	public LoggingPane() {
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
			Threads.runFx(() -> {
				list.scrollTo(list.getItems().size() - 1);
			});

		}
	}
	
	public ListView<LogEvent> getLoggingView() {
		return list;
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
