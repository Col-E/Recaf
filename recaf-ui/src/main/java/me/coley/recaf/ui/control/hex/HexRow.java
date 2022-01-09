package me.coley.recaf.ui.control.hex;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import me.coley.recaf.config.Configs;
import me.coley.recaf.ui.util.NodeUtil;
import me.coley.recaf.util.StringUtil;
import org.fxmisc.flowless.Cell;

/**
 * Virtual cell for {@link HexView}.
 *
 * @author Matt Coley
 */
public class HexRow implements Cell<Integer, HBox> {
	/**
	 * Number of characters to include in offset labels.
	 */
	public static final int OFFSET_LEN = 8;
	/**
	 * Padding on sides <i>(left/right)</i> of the row.
	 */
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
	 * <br>
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
			// Value labels
			HexLabel hexVal = new HexEditLabel(this, EditableHexLocation.RAW, i);
			hexVal.setOnMouseEntered(e -> addHoverEffect(hexVal.offset, true, true));
			hexVal.setOnMouseExited(e -> removeHoverEffect(hexVal.offset, true, true));
			// Dragging
			hexVal.setOnDragDetected(e -> {
				if (e.getButton() == MouseButton.PRIMARY) getNode().getScene().startFullDrag();
			});
			hexVal.setOnMousePressed(e -> {
				if (e.getButton() == MouseButton.PRIMARY) onDragStart(EditableHexLocation.RAW, hexVal.offset);
			});
			hexVal.setOnMouseDragEntered(e -> {
				if (e.getButton() == MouseButton.PRIMARY) onDragUpdate(hexVal.offset);
			});
			hexVal.setOnMouseReleased(e -> {
				if (e.getButton() == MouseButton.PRIMARY) onDragEnd();
			});
			valuesGrid.add(hexVal, i, 0);
			// Text labels
			HexLabel hexAscii = new HexEditLabel(this, EditableHexLocation.ASCII, i);
			hexAscii.setOnMouseEntered(e -> addHoverEffect(hexAscii.offset, true, true));
			hexAscii.setOnMouseExited(e -> removeHoverEffect(hexAscii.offset, true, true));
			// Dragging
			hexAscii.setOnDragDetected(e -> {
				if (e.getButton() == MouseButton.PRIMARY) getNode().getScene().startFullDrag();
			});
			hexAscii.setOnMousePressed(e -> {
				if (e.getButton() == MouseButton.PRIMARY) onDragStart(EditableHexLocation.ASCII, hexVal.offset);
			});
			hexAscii.setOnMouseDragEntered(e -> {
				if (e.getButton() == MouseButton.PRIMARY) onDragUpdate(hexVal.offset);
			});
			hexAscii.setOnMouseReleased(e -> {
				if (e.getButton() == MouseButton.PRIMARY) onDragEnd();
			});
			textGrid.add(hexAscii, i, 0);
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
			lblOffset.setText(HexView.offsetStr(offset) + ":");
			for (int i = 0; i < view.getHexColumns(); i++) {
				// Update displayed values
				updateLocalGrid(i);
				// Update selection
				if (view.getRange().isInRange(offset + i)) {
					addHoverEffect(i, false, false);
				}
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

	private void updateLocalGrid(int localOffset) {
		int offsetX = offset + localOffset;
		int value = hex.getHexAtOffset(offsetX);
		String hexStr = hex.getHexStringAtOffset(offsetX);
		HexLabel label = (HexLabel) valuesGrid.getChildren().get(localOffset);
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
		String preview = hex.getPreviewAtOffset(offset, view.getHexColumns());
		String previewChar = localOffset > preview.length() ? " " : String.valueOf(preview.charAt(localOffset));
		label = (HexLabel) textGrid.getChildren().get(localOffset);
		label.owner = this;
		label.setDisable(false);
		label.setText(previewChar);
		label.getStyleClass().clear();
		label.getStyleClass().add("monospace");
		label.getStyleClass().add("hex-text");
	}

	/**
	 * Highlight the given item based on the index.
	 *
	 * @param localOffset
	 * 		Local offset from the row's base offset.
	 * @param header
	 *        {@code true} if the header section should add a highlight for this column.
	 * @param offsetLabel
	 *        {@code true} if the offset section should add a highlight for this row.
	 */
	public void addHoverEffect(int localOffset, boolean header, boolean offsetLabel) {
		// Bounds check
		if (offset + localOffset >= hex.getLength())
			return;
		// Don't add hover if highlighting is disabled
		if (!Configs.editor().highlightCurrent && (header || offsetLabel))
			return;

		HexLabel label = (HexLabel) valuesGrid.getChildren().get(localOffset);
		// Don't highlight rows or columns of empty labels
		if (label.isEmpty())
			return;
		// Highlight the row offset.
		if (offsetLabel)
			NodeUtil.addStyleClass(lblOffset, "hex-hover");
		// Update the view header's matching cell.
		if (header) {
			label = (HexLabel) view.getHeader().valuesGrid.getChildren().get(localOffset);
			NodeUtil.addStyleClass(label, "hex-hover");
		}
		// Highlight the current cell.
		label = (HexLabel) valuesGrid.getChildren().get(localOffset);
		NodeUtil.addStyleClass(label, "hex-hover");
		label = (HexLabel) textGrid.getChildren().get(localOffset);
		NodeUtil.addStyleClass(label, "hex-hover");
	}

	/**
	 * Un-highlight the given item based on the index.
	 *
	 * @param localOffset
	 * 		Local offset from the row's base offset.
	 * @param header
	 *        {@code true} if the header section should have its highlight for this column removed.
	 * @param offsetLabel
	 *        {@code true} if the offset section should have its highlight removed for this row.
	 */
	public void removeHoverEffect(int localOffset, boolean header, boolean offsetLabel) {
		// Un-highlight the row offset.
		if (offsetLabel)
			NodeUtil.removeStyleClass(lblOffset, "hex-hover");
		HexLabel label;
		// Un-highlight the view header's matching cell.
		if (header) {
			label = (HexLabel) view.getHeader().valuesGrid.getChildren().get(localOffset);
			NodeUtil.removeStyleClass(label, "hex-hover");
		}
		// Un-highlight the current cell if it is not within a selection range.
		if (view.getRange().isInRange(offset + localOffset)) {
			return;
		}
		label = (HexLabel) valuesGrid.getChildren().get(localOffset);
		NodeUtil.removeStyleClass(label, "hex-hover");
		label = (HexLabel) textGrid.getChildren().get(localOffset);
		NodeUtil.removeStyleClass(label, "hex-hover");
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

	/**
	 * Called when the mouse over a {@link HexLabel} of this row is pressed.
	 *
	 * @param location
	 * 		Location where the drag originated from.
	 * @param localOffset
	 * 		Local hex label offset.
	 */
	public void onDragStart(EditableHexLocation location, int localOffset) {
		view.onDragStart(location, offset + localOffset);
	}

	/**
	 * Called when the mouse over a {@link HexLabel} of this row is moved while the mouse is pressed.
	 *
	 * @param localOffset
	 * 		Local hex label offset.
	 */
	public void onDragUpdate(int localOffset) {
		view.onDragUpdate(offset + localOffset);
	}

	/**
	 * Called when the mouse over a {@link HexLabel} of this row is released.
	 */
	public void onDragEnd() {
		view.onDragEnd();
	}
}
