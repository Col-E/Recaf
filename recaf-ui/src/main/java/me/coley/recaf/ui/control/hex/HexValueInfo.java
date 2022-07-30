package me.coley.recaf.ui.control.hex;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import me.coley.recaf.ui.util.Icons;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * A side panel that displays values at the current offset.
 *
 * @author Matt Coley
 */
public class HexValueInfo {
	private final HexView view;
	private final Label lblOffset = new Label();
	private final Label lblInt8 = new Label();
	private final Label lblUInt8 = new Label();
	private final Label lblInt16 = new Label();
	private final Label lblUInt16 = new Label();
	private final Label lblInt32 = new Label();
	private final Label lblUInt32 = new Label();
	private final Label lblInt64 = new Label();
	private final Label lblUInt64 = new Label();
	private final Label lblFloat = new Label();
	private final Label lblDouble = new Label();
	private final Label lblBinary = new Label();
	private final Button btnEndian = new Button("Big Endian");
	private boolean useLittleEndian;
	private int lastOffset  = -1;

	/**
	 * @param view
	 * 		Parent component.
	 */
	public HexValueInfo(HexView view) {
		this.view = view;
	}

	/**
	 * @return Tab containing strings panel.
	 */
	public Tab createValuesTab() {
		// Setup tab
		int r = 0;
		GridPane grid = new GridPane();
		grid.getColumnConstraints().add(new ColumnConstraints(100));
		grid.setHgap(10);
		grid.setVgap(10);
		grid.setPadding(new Insets(10));
		grid.addRow(r++, new Label("Offset"), lblOffset);
		grid.addRow(r++, new Label("int8"), lblInt8);
		grid.addRow(r++, new Label("uint8"), lblUInt8);
		grid.addRow(r++, new Label("int16"), lblInt16);
		grid.addRow(r++, new Label("uint16"), lblUInt16);
		grid.addRow(r++, new Label("int32"), lblInt32);
		grid.addRow(r++, new Label("uint32"), lblUInt32);
		grid.addRow(r++, new Label("int64"), lblInt64);
		grid.addRow(r++, new Label("uint64"), lblUInt64);
		grid.addRow(r++, new Label("float"), lblFloat);
		grid.addRow(r++, new Label("double"), lblDouble);
		grid.addRow(r++, new Label("binary"), lblBinary);
		grid.addRow(r++, new Label("Endianness"), btnEndian);
		grid.getChildren().get(0).getStyleClass().add("b");
		grid.getChildren().get(1).getStyleClass().add("b");
		btnEndian.setOnAction(e -> {
			useLittleEndian = !useLittleEndian;
			btnEndian.setText(useLittleEndian ? "Little Endian" : "Big Endian");
			if (lastOffset >= 0)
				setOffset(lastOffset);
		});
		Tab tab = new Tab(" Values");
		tab.setGraphic(Icons.getIconView(Icons.NUMBERS));
		tab.setContent(grid);
		return tab;
	}

	/**
	 * Called when the selected index changes in the parent hex view.
	 *
	 * @param offset
	 * 		The offset selected.
	 */
	public void setOffset(int offset) {
		lastOffset = offset;
		lblOffset.setText(HexView.offsetStr(offset));
		byte[] data = view.getHex().getBackingRange(offset, 8);
		ByteBuffer bb = ByteBuffer.wrap(data);
		if (useLittleEndian)
			bb.order(ByteOrder.LITTLE_ENDIAN);

		byte int8 = bb.get(0);
		short int16 = bb.getShort(0);
		int int32 = bb.getInt(0);
		long int64 = bb.getLong(0);
		short uint8 = int8 < 0 ? (short) (int8 & 0xFF) : (short) int8;
		char uint16 = bb.getChar(0);
		long uint32 = int32 & 0xffffffffL;
		float tFloat = bb.getFloat(0);
		double tDouble = bb.getDouble(0);

		lblInt8.setText(String.valueOf(int8));
		lblUInt8.setText(String.valueOf(uint8));

		lblInt16.setText(String.valueOf(int16));
		lblUInt16.setText(String.valueOf((int)uint16));

		lblInt32.setText(String.valueOf(int32));
		lblUInt32.setText(String.valueOf(uint32));

		lblInt64.setText(String.valueOf(int32));
		lblUInt64.setText(String.valueOf(uint32));

		lblInt64.setText(String.valueOf(int64));
		lblUInt64.setText(Long.toUnsignedString(int64));

		lblFloat.setText(String.valueOf(tFloat));
		lblDouble.setText(String.valueOf(tDouble));

		lblBinary.setText(Long.toBinaryString(int64));
	}
}
