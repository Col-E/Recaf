package me.coley.recaf.ui.controls;

import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.control.cell.*;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.collections.*;
import javafx.beans.property.*;
import javafx.util.Callback;
import javafx.util.converter.DefaultStringConverter;
import me.coley.recaf.util.ThreadUtil;

import java.util.Arrays;
import java.util.function.Consumer;

/**
 * Hex editor control.
 *
 * @author Matt
 */
public class HexEditor extends BorderPane {
	private static final int INVALID = Byte.MAX_VALUE + 1;
	private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
	private static final int COLS_PER_LINE = 16;
	private final TableView<Integer> contentTable = new TableView<>();
	private final TableView<Integer> offsetTable = new TableView<>();
	private final TableView<Integer> textTable = new TableView<>();
	private final TableViewExtra<?> tveContent = new TableViewExtra<>(contentTable);
	private final TableViewExtra<?> tveOffset = new TableViewExtra<>(offsetTable);
	private final TableViewExtra<?> tveText = new TableViewExtra<>(textTable);
	private byte[] content;
	private Consumer<byte[]> contentCallback;

	/**
	 * @param array
	 * 		Content to edit.
	 */
	public HexEditor(byte[] array) {
		content = array;
		DummyList dummy = new DummyList();
		TableColumn<Integer, String> offsetColumn = new TableColumn<>("Offset");
		TableColumn<Integer, String> textColumn = new TableColumn<>("Text");
		Callback<TableColumn<Integer, String>,TableCell<Integer, String>> columnCellFactory =
				offsetColumn.getCellFactory();
		offsetColumn.setCellValueFactory(cellData -> {
			int row = cellData.getValue();
			if (row >= dummy.size() - DummyList.DUMMY_PAD_LINES)
				return new SimpleStringProperty();
			return new SimpleStringProperty(Integer.toHexString(row * COLS_PER_LINE) + ":");
		});
		textColumn.setCellValueFactory(cellData -> {
			int row = cellData.getValue();
			StringBuilder sb = new StringBuilder();
			for (int i = row * COLS_PER_LINE; i < ((row + 1) * COLS_PER_LINE); i++){
				if(i >= content.length)
					break;
				char c = (char) content[i];
				// http://www.techdictionary.com/ascii.html
				// - 0x20: space
				// - 0x7E: tilde
				// Everything in between is a standard character.
				if (c >= 0x20 && c <= 0xF1)
					sb.append(c);
				else
					sb.append('.');
			}
			return new SimpleStringProperty(sb.toString());
		});
		offsetColumn.setCellFactory(col -> {
			TableCell<Integer, String> cell = columnCellFactory.call(col);
			cell.getStyleClass().add("hex-offset-cell");
			cell.getStyleClass().add("hex-cell");
			return cell;
		});
		textColumn.setCellFactory(col -> {
			TableCell<Integer, String> cell = columnCellFactory.call(col);
			cell.getStyleClass().add("hex-text-cell");
			cell.getStyleClass().add("hex-cell");
			return cell;
		});
		for(int columnIndex = 0; columnIndex < COLS_PER_LINE; columnIndex++) {
			int columnCopy = columnIndex;
			TableColumn<Integer, String> contentColumn = new TableColumn<>(String.valueOf(HEX_ARRAY[columnIndex]));
			// Handle edit actions
			contentColumn.setOnEditCommit(e -> {
				// Verify valid hex format
				String text = e.getNewValue().toUpperCase();
				if(isHex(text)) {
					// Update array
					int index = (e.getRowValue() * COLS_PER_LINE) + columnCopy;
					byte value = unhex(text);
					updateContent(index, value);
				}
				// Update table
				refresh();
			});
			// Create property wrappers of hex strings
			contentColumn.setCellValueFactory(cellData -> {
				int row = cellData.getValue();
				int index = (row * COLS_PER_LINE) + columnCopy;
				int value = index >= content.length ? INVALID : content[index];
				if(value == INVALID)
					return new SimpleStringProperty();
				return new SimpleStringProperty(hex(value));
			});
			// Column cells
			contentColumn.setCellFactory(col -> {
				TextFieldTableCell<Integer, String> cell = new TextFieldTableCell<>();
				cell.setConverter(new DefaultStringConverter());
				cell.getStyleClass().add("hex-cell");
				// We don't have the row context, so we will set the text field to be editable if
				// it fits the basic hex patterns we're expecting to be set later.
				cell.textProperty().addListener((v, o, n) ->
					cell.setEditable(n != null && n.length() == 2)
				);
				return cell;
			});
			contentTable.getColumns().add(contentColumn);
		}
		// Setup items / selection model / sizing
		contentTable.setEditable(true);
		contentTable.setItems(dummy);
		contentTable.getSelectionModel().selectFirst();
		contentTable.sortPolicyProperty().set(t -> false);
		contentTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
		contentTable.getStyleClass().add("hex-content-table");
		offsetTable.setItems(contentTable.getItems());
		offsetTable.sortPolicyProperty().set(t -> false);
		offsetTable.getColumns().add(offsetColumn);
		offsetTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
		offsetTable.selectionModelProperty().bind(contentTable.selectionModelProperty());
		offsetTable.getStyleClass().add("hex-offset-table");
		textTable.setItems(contentTable.getItems());
		textTable.sortPolicyProperty().set(t -> false);
		textTable.getColumns().add(textColumn);
		textTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
		textTable.selectionModelProperty().bind(contentTable.selectionModelProperty());
		textTable.getStyleClass().add("hex-text-table");
		// Scroll synchronization hackery
		contentTable.addEventFilter(ScrollEvent.ANY, e -> syncFromContent());
		offsetTable.addEventFilter(ScrollEvent.ANY, e -> syncFromOffset());
		textTable.addEventFilter(ScrollEvent.ANY, e -> syncFromText());
		contentTable.getSelectionModel().selectedIndexProperty().addListener((v, o, n) -> syncFromContent());
		// Add to layout
		getStyleClass().add("hex-wrapper");
		setCenter(contentTable);
		setLeft(offsetTable);
		setRight(textTable);
		// Register a refresh so the elements are resized properly
		// - JavaFX bug, and yes that delay is needed.
		ThreadUtil.runJfxDelayed(100, () -> {
			contentTable.refresh();
			offsetTable.refresh();
			textTable.refresh();
		});
	}

	private void updateContent(int index, byte value) {
		content[index] = value;
		if (contentCallback != null)
			contentCallback.accept(content);
	}

	private void refresh() {
		contentTable.refresh();
		textTable.refresh();
	}

	/**
	 * @param contentCallback
	 * 		Called when the content is modified.
	 */
	public void setContentCallback(Consumer<byte[]> contentCallback) {
		this.contentCallback = contentCallback;
	}

	/**
	 * @return Array being modified.
	 */
	public byte[] getContent() {
		return content;
	}

	/**
	 * @param editable
	 *        {@code true} to allow editing of the content table, {@code false} to disable editing.
	 */
	public void setEditable(boolean editable) {
		contentTable.setEditable(editable);
	}

	// ================= SCROLL SYNCHRONIZATION ================== //

	private void syncFromContent() {
		Platform.runLater(() -> {
			int top = tveContent.getFirstVisibleIndex();
			contentTable.scrollTo(top);
			offsetTable.scrollTo(top);
			textTable.scrollTo(top);
		});
	}

	private void syncFromOffset() {
		Platform.runLater(() -> {
			int top = tveOffset.getFirstVisibleIndex();
			contentTable.scrollTo(top);
			textTable.scrollTo(top);
		});
	}

	private void syncFromText() {
		Platform.runLater(() -> {
			int top = tveText.getFirstVisibleIndex();
			offsetTable.scrollTo(top);
			contentTable.scrollTo(top);
		});
	}

	// ======================== HEX UTILS ======================== //

	private static boolean isHex(String text) {
		return  text.length() == 2 &&
				Arrays.binarySearch(HEX_ARRAY, text.charAt(0)) >= 0 &&
				Arrays.binarySearch(HEX_ARRAY, text.charAt(1)) >= 0;
	}

	private static String hex(int value) {
		return new String(new char[]{
				HEX_ARRAY[(value & 0xFF) >>> 4],
				HEX_ARRAY[(value & 0xFF) & 0x0F]
		});
	}

	private static byte unhex(String value) {
		return (byte) Integer.parseInt(value, 16);
	}

	// ====================== INNER CLASSES ====================== //

	/**
	 * Dummy observable list. Truly a hack, but it works.
	 */
	private class DummyList extends ObservableListBase<Integer> {
		private static final int DUMMY_PAD_LINES = 1;

		@Override
		public int size() {
			// We add DUMMY_PAD_LINES to the actual length so the table has a final "dummy" line.
			// This allows the entire table to be visible with out scroll sync hack.
			return DUMMY_PAD_LINES + (int) Math.ceil(content.length / (double) COLS_PER_LINE);
		}

		@Override
		public Integer get(int index) {
			// The arg is the row. We don't get a per-column index so we will just return the
			// arg and handle fetching the actual content locally instead of here.
			// This is just semantics to get the table to display the byte array.
			return index;
		}
	}
}
