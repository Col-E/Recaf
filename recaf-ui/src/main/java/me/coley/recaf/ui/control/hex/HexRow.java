package me.coley.recaf.ui.control.hex;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import me.coley.recaf.config.Configs;
import me.coley.recaf.util.StringUtil;
import org.fxmisc.flowless.Cell;

/**
 * Virtual cell for {@link HexView}.
 *
 * @author Matt Coley
 */
public class HexRow implements Cell<Integer, HBox> {
	private static final int W_PADDING = 20;
	/**
	 * The hex cell size represents the width of a cell in the {@link #valuesGrid}.
	 * Each cell contains two letters of monospaced text.
	 * <br>
	 * The CSS class this maps to is {@code hex-value-editor}.
	 * The value here should match the size defined in the CSS.
	 */
	private static final int HEX_CELL_SIZE = 19;
	/**
	 * The hex cells all come with some horizontal padding <i>(Rather than using a gridview h-gap)</i> so that
	 * moving the mouse between cells has no gaps.
	 *  <br>
	 * The CSS classes this maps to are {@code hex-value} and {@code hex-value-zero}.
	 * The value here should match the combined horizontal padding defined in the CSS.
	 */
	private static final int HEX_CELL_PADDING = 6;
	private static final Insets HEAD_PADDING = new Insets(0, W_PADDING, 5, W_PADDING);
	private static final Insets ROW_PADDING = new Insets(0, W_PADDING, 5, W_PADDING);

	// These will remain constant through the cell's lifecycle.
	private final HexView view;
	private final HexAccessor hex;
	private final HBox box;
	private final Label lblOffset;
	private final GridPane valuesGrid;
	private final GridPane textGrid;
	// The offset is the only piece that will be modified when the cell is reused.
	private int offset;

	/**
	 * @param view
	 * 		Parent component.
	 */
	public HexRow(HexView view) {
		this.view = view;
		this.hex = view.getHex();
		this.lblOffset = new Label();
		this.valuesGrid = new GridPane();
		this.textGrid = new GridPane();

		lblOffset.getStyleClass().add("hex-offset");
		lblOffset.getStyleClass().add("monospace");

		lblOffset.setMinWidth(desiredOffsetLabelWidth());
		lblOffset.setMaxWidth(desiredOffsetLabelWidth());
		valuesGrid.setMinWidth(desiredValueGridWidth());
		valuesGrid.setMaxWidth(desiredValueGridWidth());
		textGrid.setMinWidth(desiredTextGridWidth());
		// The text section has no max width, so it will occupy the space.
		for (int i = 0; i < view.getHexColumns(); i++) {
			HexLabel hexVal = new HexEditLabel(this, i);
			hexVal.setOnMouseEntered(e -> onHover(hexVal.offset));
			hexVal.setOnMouseExited(e -> onLeave(hexVal.offset));
			valuesGrid.add(hexVal, i, 0);
			HexLabel hexVal2 = new HexLabel(this, i);
			hexVal2.setOnMouseEntered(e -> onHover(hexVal2.offset));
			hexVal2.setOnMouseExited(e -> onLeave(hexVal2.offset));
			textGrid.add(hexVal2, i, 0);
		}
		box = new HBox(lblOffset, valuesGrid, textGrid);
	}

	@Override
	public boolean isReusable() {
		return true;
	}

	@Override
	public void updateItem(Integer item) {
		this.offset = item;
		if (offset < 0) {
			// Special case for header row
			valuesGrid.setPadding(HEAD_PADDING);
			lblOffset.setText("Offset");
			String asciiTitle = "Ascii";
			for (int i = 0; i < view.getHexColumns(); i++) {
				// Values grid
				HexLabel label = (HexLabel) valuesGrid.getChildren().get(i);
				label.owner = this;
				label.setDisable(true);
				label.getStyleClass().clear();
				label.getStyleClass().add("monospace");
				label.getStyleClass().add("hex-value");
				label.setText(StringUtil.fillLeft(2, "0", HexView.caseHex(Integer.toHexString(i))));
				// Text grid
				label = (HexLabel) textGrid.getChildren().get(i);
				label.owner = this;
				label.setDisable(true);
				label.getStyleClass().clear();
				label.getStyleClass().add("monospace");
				label.getStyleClass().add("hex-value");
				label.setText(i == 0 ? asciiTitle : "");
			}
		} else {
			// Normal offset
			valuesGrid.setPadding(ROW_PADDING);
			lblOffset.setText(StringUtil.fillLeft(9, "0", HexView.caseHex(Integer.toHexString(offset))) + ":");
			for (int i = 0; i < view.getHexColumns(); i++) {
				updateLocalGrid(i);
			}
		}
	}

	@Override
	public void updateIndex(int index) {
		Cell.super.updateIndex(index);
	}

	@Override
	public HBox getNode() {
		return box;
	}

	/**
	 * Applies the value to the given local offset.
	 * This will in turn update the backing data via {@link HexAccessor} and refresh the necessary components.
	 *
	 * @param localOffset
	 * 		Local offset in the row to modify.
	 * @param value
	 * 		Value to set.
	 */
	public void onEdit(int localOffset, int value) {
		hex.setHexAtOffset(offset + localOffset, value);
		updateLocalGrid(localOffset);
	}

	private void updateLocalGrid(int i) {
		int offsetX = offset + i;
		int value = hex.getHexAtOffset(offsetX);
		String hexStr = hex.getHexStringAtOffset(offsetX);
		HexLabel label = (HexLabel) valuesGrid.getChildren().get(i);
		label.owner = this;
		label.setDisable(false);
		label.setText(hexStr);
		label.getStyleClass().clear();
		label.getStyleClass().add("monospace");
		if (value == 0) {
			label.getStyleClass().add("hex-value-zero");
		} else {
			label.getStyleClass().add("hex-value");
		}
		String preview = hex.getPreviewAtOffset(offset);
		String previewChar = i > preview.length() ? " " : String.valueOf(preview.charAt(i));
		label = (HexLabel) textGrid.getChildren().get(i);
		label.owner = this;
		label.setDisable(false);
		label.setText(previewChar);
		label.getStyleClass().clear();
		label.getStyleClass().add("monospace");
		label.getStyleClass().add("hex-text");
	}

	private void onHover(int offset) {
		// Don't add hover if highlighting is disabled
		if (!Configs.editor().highlightCurrent)
			return;

		HexLabel label = (HexLabel) valuesGrid.getChildren().get(offset);
		// Don't highlight rows or columns of empty labels
		if (label.isEmpty())
			return;
		// Highlight the row offset.
		lblOffset.getStyleClass().add("hex-hover");
		// Highlight the current cell.
		label.getStyleClass().add("hex-hover");
		label = (HexLabel) textGrid.getChildren().get(offset);
		label.getStyleClass().add("hex-hover");
		// Update the view header's matching cell.
		label = (HexLabel) view.getHeader().valuesGrid.getChildren().get(offset);
		label.getStyleClass().add("hex-hover");
		label = (HexLabel) textGrid.getChildren().get(offset);
		label.getStyleClass().add("hex-hover");
	}

	private void onLeave(int offset) {
		// Un-highlight the row offset.
		lblOffset.getStyleClass().remove("hex-hover");
		// Un-highlight the current cell.
		HexLabel label = (HexLabel) valuesGrid.getChildren().get(offset);
		label.getStyleClass().remove("hex-hover");
		label = (HexLabel) textGrid.getChildren().get(offset);
		label.getStyleClass().remove("hex-hover");
		// Un-highlight the view header's matching cell.
		label = (HexLabel) view.getHeader().valuesGrid.getChildren().get(offset);
		label.getStyleClass().remove("hex-hover");
		label = (HexLabel) textGrid.getChildren().get(offset);
		label.getStyleClass().remove("hex-hover");
	}

	/**
	 * No fancy math here, this size just works assuming there are 9 characters in the offset.
	 *
	 * @return Desired offset label width.
	 */
	private int desiredOffsetLabelWidth() {
		return 110;
	}

	/**
	 * Since the {@link #HEX_CELL_SIZE} represents two characters of text, the text section
	 * is as wide as half the cell size times the number of characters shown.
	 *
	 * @return Desired text grid width.
	 */
	private int desiredTextGridWidth() {
		return (int) (HEX_CELL_SIZE / 2.0 * view.getHexColumns());
	}

	/**
	 * Each cell is a given size and also contains a grap between it and the next cell.
	 * This occurs for the number of cells shown. Then there's the padding on the sides.
	 *
	 * @return Desired value grid width.
	 */
	private int desiredValueGridWidth() {
		return (HEX_CELL_SIZE + HEX_CELL_PADDING) * view.getHexColumns() + (2 * W_PADDING);
	}

	/**
	 * All component widths together.
	 *
	 * @return Desired total width.
	 */
	public int desiredTotalWidth() {
		return desiredOffsetLabelWidth() + desiredValueGridWidth() + desiredTextGridWidth();
	}
}
