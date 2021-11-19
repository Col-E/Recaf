package me.coley.recaf.ui.control.hex;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.layout.BorderPane;
import me.coley.recaf.ui.util.Icons;

import java.util.ArrayList;
import java.util.List;

/**
 * A side panel that displays an interactive string dump.
 *
 * @author Matt Coley
 */
public class HexStringsInfo {
	private final ObservableList<Info> strings = FXCollections.observableArrayList();
	private final HexView view;

	/**
	 * @param view
	 * 		Parent component.
	 */
	public HexStringsInfo(HexView view) {
		this.view = view;
	}

	/**
	 * @return Tab containing strings panel.
	 */
	public Tab createStringsTab() {
		ListView<Info> list = new ListView<>(strings);
		list.setCellFactory(c -> new ListCell<Info>() {
			@Override
			protected void updateItem(Info item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || item == null) {
					setText(null);
					setGraphic(null);
				} else {
					String offset = HexView.offsetStr(item.offset) + ":";
					Label lblOffset = new Label();
					lblOffset.setText(offset);
					lblOffset.getStyleClass().add("hex-offset");
					lblOffset.getStyleClass().add("monospace");
					lblOffset.getStyleClass().add("faint");
					setGraphic(lblOffset);
					setText(item.text);
					// Make it so they're clickable
					int start = item.offset;
					int end = start + item.text.length() - 1;
					setOnMousePressed(e -> view.selectRange(EditableHexLocation.ASCII, start, end));
				}
			}
		});
		// Setup tab
		BorderPane wrapper = new BorderPane();
		wrapper.setCenter(list);
		Tab tab = new Tab("Strings");
		tab.setGraphic(Icons.getIconView(Icons.QUOTE));
		tab.setContent(wrapper);
		return tab;
	}

	/**
	 * Updates the displayed values.
	 */
	public void populateStrings() {
		byte[] data = view.getHex().getBacking();
		List<Info> strings = new ArrayList<>();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < data.length; i++) {
			char c = (char) data[i];
			if (Character.isISOControl(c)) {
				int len = sb.length();
				if (len > 0) {
					strings.add(new Info(i - len, sb.toString()));
					sb.setLength(0);
				}
			} else {
				sb.append(c);
			}
		}
		this.strings.clear();
		this.strings.addAll(strings);
	}

	private static class Info {
		private final int offset;
		private final String text;

		public Info(int offset, String text) {
			this.offset = offset;
			this.text = text;
		}
	}
}
