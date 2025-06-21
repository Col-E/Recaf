package software.coley.recaf.ui.pane.editing.binary.hex.cell;

import atlantafx.base.controls.Spacer;
import jakarta.annotation.Nonnull;
import javafx.beans.property.IntegerProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import org.fxmisc.flowless.Cell;
import org.fxmisc.flowless.VirtualFlow;
import software.coley.collections.Unchecked;
import software.coley.recaf.ui.pane.editing.binary.hex.HexConfig;
import software.coley.recaf.ui.pane.editing.binary.hex.HexUtil;
import software.coley.recaf.ui.pane.editing.binary.hex.ops.HexAccess;
import software.coley.recaf.ui.pane.editing.binary.hex.ops.HexOperations;
import software.coley.recaf.util.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Cell for {@link VirtualFlow} to draw a row of bytes.
 *
 * @author Matt Coley
 */
public class HexRow implements Cell<Integer, Node> {
	protected final HBox layout = new HBox();
	protected final HexConfig config;
	protected final IntegerProperty rowCount;
	protected final HexOperations ops;
	protected int baseOffset;
	protected int lastConfigHash;

	public HexRow(@Nonnull HexConfig config, @Nonnull IntegerProperty rowCount, @Nonnull HexOperations ops, int row) {
		this.config = config;
		this.rowCount = rowCount;
		this.ops = ops;
		this.baseOffset = row * config.getRowLength().getValue();

		layout.setMouseTransparent(true);
		layout.setAlignment(Pos.CENTER_LEFT);
		layout.setSpacing(4);

		buildFreshLayout();
	}

	/**
	 * Re-populate the layout.
	 */
	public void redraw() {
		int configHash = config.hashCode();
		if (lastConfigHash != configHash) {
			reset();
		} else {
			refreshLayout();
		}
	}

	@Override
	public Node getNode() {
		return layout;
	}

	@Override
	public boolean isReusable() {
		return true;
	}

	@Override
	public void updateItem(Integer row) {
		int rowLength = config.getRowLength().getValue();
		int oldBaseOffset = this.baseOffset;
		int newBaseOffset = row * rowLength;
		if (oldBaseOffset != newBaseOffset) {
			this.baseOffset = newBaseOffset;
			refreshLayout();
		}
	}

	@Override
	public void reset() {
		// We only need to clear the layout when the hash changes.
		int configHash = config.hashCode();
		if (lastConfigHash != configHash) {
			layout.getChildren().clear();
			buildFreshLayout();
		}
	}

	/**
	 * Updates the existing {@link HexCellBase} items in this row.
	 * <br>
	 * This assumes the {@link #baseOffset} does not represent the header, or the last row in the file
	 * which are special cases and not supported.
	 */
	private void refreshLayout() {
		if (baseOffset < 0)
			throw new IllegalStateException("Hex row refreshed in unsupported context");

		ObservableList<Node> children = layout.getChildren();
		int rowLength = config.getRowLength().getValue();
		int rowSplit = config.getRowSplitInterval().getValue();
		int addressWidth = Integer.toHexString(rowCount.get() * rowLength).length();
		boolean showAddress = config.getShowAddress().getValue();
		boolean showAscii = config.getShowAscii().getValue();

		// Update the label value.
		int spaces = 0;
		if (showAddress) {
			Label lblAddress = (Label) children.getFirst();
			lblAddress.setText(StringUtil.fillLeft(8, " ", HexUtil.strFormat(addressWidth, baseOffset) + ":"));
			spaces++;
		}

		// Update the hex values
		HexAccess read = ops.currentAccess();
		int asciiChildOffset = 0;
		for (int i = 0; i < rowLength; i++) {
			// Account for spacer padding
			if (i % rowSplit == 0 && i < rowLength - 1)
				spaces++;
			int childIndex = i + spaces;
			int offset = baseOffset + i;
			boolean outOfBounds = !read.isInBounds(offset);

			HexCellBase valueLabel = (HexCellBase) children.get(childIndex);
			valueLabel.setDisable(outOfBounds);
			valueLabel.setOpacity(outOfBounds ? 0.1 : 1);
			valueLabel.updateOffset(offset);

			// Record the last child index as the beginning of where to look for ascii display controls.
			asciiChildOffset = childIndex;
		}
		if (showAscii) {
			// Offset by the last row-split padding (not counted above), and the first padding before the ascii begins.
			asciiChildOffset += 2;

			// Update ascii values.
			for (int i = 0; i < rowLength; i++) {
				int childIndex = asciiChildOffset + i;
				int offset = baseOffset + i;
				boolean outOfBounds = !read.isInBounds(offset);

				HexCellBase asciiLabel = (HexCellBase) children.get(childIndex);
				asciiLabel.setDisable(outOfBounds);
				asciiLabel.setOpacity(outOfBounds ? 0.1 : 1);
				asciiLabel.updateOffset(offset);
			}
		}

		// Update selection so that the newly generated row will display the current selection
		// if it appears on this row.
		updateSelection(ops.navigation().selectionOffset());
	}

	protected void buildFreshLayout() {
		if (baseOffset < 0)
			throw new IllegalStateException("Hex row refreshed in unsupported context");

		lastConfigHash = config.hashCode();
		int rowLength = config.getRowLength().getValue();
		int rowSplit = config.getRowSplitInterval().getValue();
		int addressWidth = Integer.toHexString(rowCount.get() * rowLength).length();
		boolean showAddress = config.getShowAddress().getValue();
		boolean showAscii = config.getShowAscii().getValue();

		ObservableList<Node> children = layout.getChildren();

		Label lblAddress;
		List<Node> contentHexLabels = new ArrayList<>();
		List<Node> contentAsciiLabels = new ArrayList<>();
		HexAccess read = ops.currentAccess();

		lblAddress = new Label(StringUtil.fillLeft(8, " ", HexUtil.strFormat(addressWidth, baseOffset) + ":"));
		for (int i = 0; i < rowLength; i++) {
			if (i % rowSplit == 0 && i < rowLength - 1)
				contentHexLabels.add(new SmallSpacer());
			int offset = baseOffset + i;
			byte b = read.getByte(offset);
			boolean outOfBounds = !read.isInBounds(offset);

			// Hex labels
			HexCellBase valueLabel = new EditableHexCell(ops, offset, b);
			valueLabel.setDisable(outOfBounds);
			valueLabel.setOpacity(outOfBounds ? 0.1 : 1);
			contentHexLabels.add(valueLabel);

			// Ascii labels
			if (showAscii) {
				char c = HexUtil.charAscii(b);
				HexCellBase asciiLabel = new EditableAsciiCell(ops, offset, b);
				asciiLabel.setDisable(outOfBounds);
				asciiLabel.setOpacity(outOfBounds ? 0.1 : 1);
				contentAsciiLabels.add(asciiLabel);
			}
		}
		setChildren(lblAddress, contentHexLabels, contentAsciiLabels);

		// Update selection so that the newly generated row will display the current selection
		// if it appears on this row.
		updateSelection(ops.navigation().selectionOffset());
	}

	protected void setChildren(@Nonnull Label lblAddress,
	                           @Nonnull List<Node> contentHexLabels,
	                           @Nonnull List<Node> contentAsciiLabels) {
		boolean showAddress = config.getShowAddress().getValue();
		boolean showAscii = config.getShowAscii().getValue();

		List<Node> nodes = new ArrayList<>(contentHexLabels.size() + 2);
		if (showAddress)
			nodes.add(lblAddress);
		nodes.addAll(contentHexLabels);
		if (showAscii) {
			nodes.add(new SmallSpacer());
			nodes.addAll(contentAsciiLabels);
		}
		nodes.add(new Spacer()); // Added at the end to occupy space to the right
		layout.getChildren().setAll(nodes);
	}

	/**
	 * @return {@code true} when this row contains the current selected offset.
	 */
	public boolean isRowSelected() {
		return layout.getChildren().stream()
				.anyMatch(child -> child instanceof HexCell cell
						&& cell.offset() == ops.navigation().selectionOffset());
	}

	/**
	 * Notifies cells of selection updates, primarily to refresh visual cues.
	 *
	 * @param offset
	 * 		Target selection offset.
	 */
	public void updateSelection(int offset) {
		Unchecked.checkedForEach(layout.getChildren(), child -> {
			boolean hexColumn = ops.navigation().isHexColumnSelected();
			if (child instanceof EditableHexCell cell) {
				boolean match = hexColumn && cell.offset() == offset;
				if (match) cell.onSelectionGained();
				else cell.onSelectionLost();
			} else if (child instanceof EditableAsciiCell cell) {
				boolean match = !hexColumn && cell.offset() == offset;
				if (match) cell.onSelectionGained();
				else cell.onSelectionLost();
			}
		}, (cell, error) -> {});
	}

	/**
	 * @param offset
	 * 		Offset in the data to check.
	 *
	 * @return {@code true} if this row contains the offset.
	 */
	public boolean hasOffset(int offset) {
		return offset >= baseOffset && offset <= baseOffset + config.getRowLength().getValue();
	}

	/**
	 * Toggles editing in the cell at the given offset.
	 *
	 * @param offset
	 * 		Target offset to toggle editing on.
	 * @param initiateEdit
	 *        {@code true} to trigger the target cell at the offset to begin editing or complete a current edit.
	 *        {@code false} to cancel editing.
	 */
	public void engage(int offset, boolean initiateEdit) {
		Consumer<HexCellBase> action = cell -> {
			boolean match = cell.offset() == offset;
			if (match) {
				if (initiateEdit && cell.isEditing()) {
					// Complete the current edit
					cell.endEdit(true);
				} else if (initiateEdit) {
					// Initiate a new edit
					cell.beginEdit();
				} else {
					// Cancel the current edit
					cell.endEdit(false);
				}
			} else {
				cell.onSelectionLost();
			}
		};
		Unchecked.checkedForEach(layout.getChildren(), child -> {
			boolean hexColumn = ops.navigation().isHexColumnSelected();
			if (hexColumn && child instanceof EditableHexCell cell) {
				action.accept(cell);
			} else if (!hexColumn && child instanceof EditableAsciiCell cell) {
				action.accept(cell);
			}
		}, (cell, error) -> {});
	}

	/**
	 * Finds the cell matching the given offset and delegates key handling for the given key-code to it.
	 *
	 * @param offset
	 * 		Target offset to send key to.
	 * @param code
	 * 		Key to send.
	 */
	public void sendKeyToCurrentEngaged(int offset, @Nonnull KeyCode code) {
		Consumer<HexCellBase> action = cell -> {
			boolean match = cell.offset() == offset;
			if (match && cell.isEditing()) {
				cell.handleKeyCode(code);
			}
		};
		Unchecked.checkedForEach(layout.getChildren(), child -> {
			boolean hexColumn = ops.navigation().isHexColumnSelected();
			if (hexColumn && child instanceof EditableHexCell cell) {
				action.accept(cell);
			} else if (!hexColumn && child instanceof EditableAsciiCell cell) {
				action.accept(cell);
			}
		}, (cell, error) -> {});
	}

	/**
	 * @param x
	 * 		Layout x position.
	 * @param y
	 * 		Layout y position.
	 *
	 * @return Offset into the data for the closest cell to the given coordinates.
	 */
	public int pickOffsetAtPosition(double x, double y) {
		HexCell closestChild = null;
		double closestDistance = Integer.MAX_VALUE;
		int offset = -1;
		for (Node child : layout.getChildren()) {
			if (child instanceof HexCell cell) {
				int bufferZone = 2;
				double centerX = cell.node().getBoundsInParent().getCenterX();
				double distance = Math.abs(centerX - x);
				if (distance < closestDistance) {
					closestDistance = distance;
					closestChild = cell;
					offset = cell.offset();
				}
			}
		}

		// Toggle navigation columns if the clicked column is not the current column.
		if (ops.navigation().isHexColumnSelected() != closestChild instanceof EditableHexCell)
			ops.navigation().switchColumns();

		return offset;
	}

	/**
	 * Fixed size spacer to put between columns.
	 */
	protected static class SmallSpacer extends Spacer {
		protected SmallSpacer() {
			setMaxWidth(12);
		}
	}
}
