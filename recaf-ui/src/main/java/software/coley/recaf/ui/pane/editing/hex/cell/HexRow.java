package software.coley.recaf.ui.pane.editing.hex.cell;

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
import software.coley.recaf.ui.pane.editing.hex.HexConfig;
import software.coley.recaf.ui.pane.editing.hex.HexUtil;
import software.coley.recaf.ui.pane.editing.hex.ops.HexAccess;
import software.coley.recaf.ui.pane.editing.hex.ops.HexOperations;
import software.coley.recaf.util.CollectionUtil;
import software.coley.recaf.util.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class HexRow implements Cell<Integer, Node> {
	private final HBox layout = new HBox();
	private final HexConfig config;
	private final IntegerProperty rowCount;
	private final HexOperations ops;
	private int baseOffset;

	public HexRow(@Nonnull HexConfig config, @Nonnull IntegerProperty rowCount, @Nonnull HexOperations ops, int row) {
		this.config = config;
		this.rowCount = rowCount;
		this.ops = ops;
		this.baseOffset = row * config.getRowLength().getValue();

		layout.setMouseTransparent(true);
		layout.setAlignment(Pos.CENTER_LEFT);
		layout.setSpacing(4);
		buildLayout();
	}

	public void redraw() {
		reset();
		buildLayout();
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
		this.baseOffset = row * config.getRowLength().getValue();
		buildLayout();
	}

	@Override
	public void reset() {
		// When the cell is no longer needed we clear the layout.
		layout.getChildren().clear();
	}

	private void buildLayout() {
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
		if (baseOffset < 0) {
			// Negative used as an edge case to display the column headers
			lblAddress = new Label(StringUtil.fillLeft(addressWidth + 1, " ", "Address:"));
			for (int i = 0; i < rowLength; i++) {
				if (i % rowSplit == 0 && i < rowLength - 1)
					contentHexLabels.add(new SmallSpacer());
				Label columnOffset = new Label(HexUtil.strFormat00((byte) i));
				contentHexLabels.add(columnOffset);
			}
			contentAsciiLabels.addAll(List.of(new Label("A"), new Label("S"), new Label("C"), new Label("I"), new Label("I")));
			layout.getStyleClass().add("header");
		} else {
			lblAddress = new Label(StringUtil.fillLeft(8, " ", HexUtil.strFormat(addressWidth, baseOffset) + ":"));
			for (int i = 0; i < rowLength; i++) {
				if (i % rowSplit == 0 && i < rowLength - 1)
					contentHexLabels.add(new SmallSpacer());
				int offset = baseOffset + i;
				byte b = read.getByte(offset);
				if (read.isInBounds(offset)) {
					// Hex labels
					HexCellBase valueLabel = new EditableHexCell(ops, offset, b);
					contentHexLabels.add(valueLabel);

					// Ascii labels
					if (showAscii) {
						char c = HexUtil.charAscii(b);
						HexCellBase asciiLabel = new EditableAsciiCell(ops, offset, b);
						contentAsciiLabels.add(asciiLabel);
					}
				} else {
					contentHexLabels.add(new Label("  "));
					if (showAscii) contentAsciiLabels.add(new Label("  "));
				}
			}
			layout.getStyleClass().remove("header");
		}
		List<Node> nodes = new ArrayList<>(contentHexLabels.size() + 2);
		if (showAddress)
			nodes.add(lblAddress);
		nodes.addAll(contentHexLabels);
		if (showAscii) {
			nodes.add(new SmallSpacer());
			nodes.addAll(contentAsciiLabels);
		}
		nodes.add(new Spacer()); // Added at the end to occupy space to the right
		children.setAll(nodes);

		// Update selection so that the newly generated row will display the current selection
		// if it appears on this row.
		updateSelection(ops.navigation().selectionOffset());
	}

	public boolean isRowSelected() {
		return layout.getChildren().stream()
				.anyMatch(child -> child instanceof HexCell cell
						&& cell.offset() == ops.navigation().selectionOffset());
	}

	public void updateSelection(int offset) {
		CollectionUtil.safeForEach(layout.getChildren(), child -> {
			boolean hexColumn = ops.navigation().isHexColumnSelected();
			if (hexColumn && child instanceof EditableHexCell cell) {
				boolean match = cell.offset() == offset;
				if (match) cell.onSelectionGained();
				else cell.onSelectionLost();
			} else if (!hexColumn && child instanceof EditableAsciiCell cell) {
				boolean match = cell.offset() == offset;
				if (match) cell.onSelectionGained();
				else cell.onSelectionLost();
			}
		}, (cell, error) -> {});
	}

	public boolean hasOffset(int offset) {
		return offset >= baseOffset && offset <= baseOffset + config.getRowLength().getValue();
	}

	public void engage(int offset, boolean doEdit) {
		Consumer<HexCellBase> action = cell -> {
			boolean match = cell.offset() == offset;
			if (match) {
				if (!doEdit || cell.isEditing()) {
					cell.endEdit(doEdit);
				} else {
					cell.beginEdit();
				}
			} else {
				cell.onSelectionLost();
			}
		};
		CollectionUtil.safeForEach(layout.getChildren(), child -> {
			boolean hexColumn = ops.navigation().isHexColumnSelected();
			if (hexColumn && child instanceof EditableHexCell cell) {
				action.accept(cell);
			} else if (!hexColumn && child instanceof EditableAsciiCell cell) {
				action.accept(cell);
			}
		}, (cell, error) -> {});
	}

	public void sendKeyToCurrentEngaged(int offset, @Nonnull KeyCode code) {
		Consumer<HexCellBase> action = cell -> {
			boolean match = cell.offset() == offset;
			if (match && cell.isEditing()) {
				cell.handleKeyCode(code);
			}
		};
		CollectionUtil.safeForEach(layout.getChildren(), child -> {
			boolean hexColumn = ops.navigation().isHexColumnSelected();
			if (hexColumn && child instanceof EditableHexCell cell) {
				action.accept(cell);
			} else if (!hexColumn && child instanceof EditableAsciiCell cell) {
				action.accept(cell);
			}
		}, (cell, error) -> {});
	}

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

	private static class SmallSpacer extends Spacer {
		private SmallSpacer() {
			setMaxWidth(12);
		}
	}
}
