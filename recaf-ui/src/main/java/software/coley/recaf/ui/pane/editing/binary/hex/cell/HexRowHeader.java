package software.coley.recaf.ui.pane.editing.binary.hex.cell;

import jakarta.annotation.Nonnull;
import javafx.beans.property.IntegerProperty;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import software.coley.recaf.ui.pane.editing.binary.hex.HexConfig;
import software.coley.recaf.ui.pane.editing.binary.hex.HexUtil;
import software.coley.recaf.ui.pane.editing.binary.hex.ops.HexOperations;
import software.coley.recaf.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Extended header to display the hex editor header row.
 *
 * @author Matt Coley
 */
public class HexRowHeader extends HexRow {
	public HexRowHeader(@Nonnull HexConfig config, @Nonnull IntegerProperty rowCount, @Nonnull HexOperations ops) {
		super(config, rowCount, ops, -1);
		layout.getStyleClass().add("header");
	}

	@Override
	protected void buildFreshLayout() {
		int rowLength = config.getRowLength().getValue();
		int rowSplit = config.getRowSplitInterval().getValue();
		int addressWidth = Integer.toHexString(rowCount.get() * rowLength).length();
		boolean showAddress = config.getShowAddress().getValue();
		boolean showAscii = config.getShowAscii().getValue();
		List<Node> contentHexLabels = new ArrayList<>();
		Label lblAddress = new Label(StringUtil.fillLeft(addressWidth + 1, " ", "Address:"));
		for (int i = 0; i < rowLength; i++) {
			if (i % rowSplit == 0 && i < rowLength - 1)
				contentHexLabels.add(new SmallSpacer());
			Label columnOffset = new Label(HexUtil.strFormat00((byte) i));
			contentHexLabels.add(columnOffset);
		}
		List<Node> contentAsciiLabels = new ArrayList<>(Arrays.asList(new Label("A"), new Label("S"), new Label("C"), new Label("I"), new Label("I")));
		for (int i = 5; i < rowLength; i++) {
			Label filler = new Label(".");
			filler.setOpacity(0.2);
			contentAsciiLabels.add(filler);
		}
		setChildren(lblAddress, contentHexLabels, contentAsciiLabels);
	}

	@Override
	public boolean isReusable() {
		return false;
	}

	@Override
	public boolean isRowSelected() {
		return false;
	}

	@Override
	public void updateSelection(int offset) {
		// no-op
	}

	@Override
	public boolean hasOffset(int offset) {
		return false;
	}

	@Override
	public void engage(int offset, boolean initiateEdit) {
		// no-op
	}

	@Override
	public void sendKeyToCurrentEngaged(int offset, @Nonnull KeyCode code) {
		// no-op
	}

	@Override
	public int pickOffsetAtPosition(double x, double y) {
		return -1;
	}
}
