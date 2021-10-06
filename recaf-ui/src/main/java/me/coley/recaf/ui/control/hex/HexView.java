package me.coley.recaf.ui.control.hex;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import me.coley.recaf.config.Configs;
import me.coley.recaf.ui.behavior.Cleanable;
import me.coley.recaf.ui.behavior.Representation;
import me.coley.recaf.ui.behavior.SaveResult;
import org.fxmisc.flowless.VirtualFlow;
import org.fxmisc.flowless.Virtualized;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.reactfx.value.Val;
import org.reactfx.value.Var;

import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * A hex viewer and editor component that utilizes virtual/recyclable cells.
 * <br>
 * To populate the UI, see {@link #onUpdate(byte[])}.
 *
 * @author Matt Coley
 */
public class HexView extends BorderPane implements Cleanable, Representation, Virtualized {
	private final int hexColumns;
	private final HexAccessor hex = new HexAccessor(this);
	private final HexRange range = new HexRange(hex);
	private final ObservableList<Integer> offsets = FXCollections.observableArrayList();
	private final HexRow header;
	private VirtualFlow<Integer, HexRow> hexFlow;
	// TODO: Multi-select (drag over range)
	//  - Copy hex to clipboard
	//  - Copy as byte array (export options for different languages
	//     - C:        unsigned char [file name]_arr = { 0xFF, 0xAB };
	//     - Java:     short[]                   arr = { 0xFF, 0xAB };
	//     - Python 3:                           arr = bytes([ 0xFF, 0xAB ])
	//  - Copy as string
	//  - Copy as value (use standards like int8/uint8, but add note for which map to java primitives)

	// TODO: Local control for:
	//  - toggle endian format
	//  - search for patterns (hex/values/string)

	// TODO: Syntax matching for known formats (just class to begin with, see HexClassView for details)

	// TODO: Search bar
	//  - Perhaps similar to workspace filter, highlighting matches

	/**
	 * Initialize the components.
	 */
	public HexView() {
		hexColumns = Configs.editor().hexColumns;
		header = new HexRow(this);
		header.updateItem(-1);
		Node headerNode = header.getNode();
		headerNode.getStyleClass().add("hex-header");
		setTop(headerNode);
		setMinWidth(header.desiredTotalWidth());
		range.addListener(new SelectionHighlighter());
	}

	@Override
	public Val<Double> totalWidthEstimateProperty() {
		return hexFlow.totalWidthEstimateProperty();
	}

	@Override
	public Val<Double> totalHeightEstimateProperty() {
		return hexFlow.totalHeightEstimateProperty();
	}

	@Override
	public Var<Double> estimatedScrollXProperty() {
		return hexFlow.estimatedScrollXProperty();
	}

	@Override
	public Var<Double> estimatedScrollYProperty() {
		return hexFlow.estimatedScrollYProperty();
	}

	@Override
	public void scrollXBy(double deltaX) {
		hexFlow.scrollXBy(deltaX);
	}

	@Override
	public void scrollYBy(double deltaY) {
		hexFlow.scrollYBy(deltaY);
	}

	@Override
	public void scrollXToPixel(double pixel) {
		hexFlow.scrollXToPixel(pixel);
	}

	@Override
	public void scrollYToPixel(double pixel) {
		hexFlow.scrollYToPixel(pixel);
	}

	@Override
	public SaveResult save() {
		return SaveResult.IGNORED;
	}

	@Override
	public boolean supportsEditing() {
		return true;
	}

	@Override
	public Node getNodeRepresentation() {
		return this;
	}

	@Override
	public void cleanup() {
		if (hexFlow != null) {
			hexFlow.dispose();
		}
	}

	/**
	 * Populate the UI with new data.
	 *
	 * @param data
	 * 		Data to use.
	 */
	public void onUpdate(byte[] data) {
		hex.setBacking(data);
		List<Integer> newOffsets = hex.computeOffsetsInRange();
		offsets.clear();
		offsets.addAll(newOffsets);
		if (hexFlow != null) {
			hexFlow.dispose();
		}
		hexFlow = VirtualFlow.createVertical(offsets, i -> {
			HexRow row = new HexRow(this);
			row.updateItem(i);
			return row;
		});
		setCenter(new VirtualizedScrollPane<>(hexFlow));
	}

	/**
	 * Called when a {@link HexRow} is pressed.
	 *
	 * @param offset
	 * 		Offset pressed on.
	 */
	public void onDragStart(int offset) {
		range.createSelectionBound(offset);
	}

	/**
	 * Called when a {@link HexRow} is released.
	 *
	 * @param offset
	 * 		Offset released on.
	 */
	public void onDragUpdate(int offset) {
		range.updateSelectionBound(offset);
	}

	/**
	 * Called when a {@link HexRow} is released.
	 * Unlike {@link #onDragStart(int)} and {@link #onDragUpdate(int)} there is no parameter.
	 * The assumption is the last value from {@link #onDragUpdate(int)} is the end value.
	 */
	public void onDragEnd() {
		range.endSelectionBound();
	}

	/**
	 * Exposed so that when a {@link HexRow} gains hover-access it can notify the header to update
	 * its highlighted column to match.
	 *
	 * @return Header row.
	 */
	public HexRow getHeader() {
		return header;
	}

	/**
	 * @return Selection range.
	 */
	public HexRange getRange() {
		return range;
	}

	/**
	 * @return Backing hex data.
	 */
	public HexAccessor getHex() {
		return hex;
	}

	/**
	 * @return Number of columns.
	 */
	public int getHexColumns() {
		return hexColumns;
	}

	/**
	 * @param text
	 * 		Text to format.
	 *
	 * @return Case formatted text.
	 */
	public static String caseHex(String text) {
		return text.toUpperCase();
	}

	/**
	 * Ensures that the current selection <i>({@link #range})</i> is shown to the user.
	 */
	private class SelectionHighlighter implements HexRangeListener {
		private int oldStart = -1;
		private int oldStop = -1;

		@Override
		public void onSelectionUpdate(int start, int stop) {
			if (oldStart != -1) {
				int min;
				int max;
				if (start > oldStart) {
					// Two scenarios:
					// - User is retracting start but selection is still backwards
					// - User is now making a forwards selection, prior backwards selection must be cleared
					min = oldStart;
					max = start - 1;
					handle(min, max, (row, localOffset) -> row.removeHoverEffect(localOffset, false, false));
				} else if (stop < oldStop) {
					// Two scenarios:
					// - User is retracting stop but selection is still forwards
					// - User is now making a backwards selection, prior forwards selection must be cleared
					min = stop + 1;
					max = oldStop;
					handle(min, max, (row, localOffset) -> row.removeHoverEffect(localOffset, false, false));
				}
			}
			handle(start, stop, (row, localOffset) -> row.addHoverEffect(localOffset, false, false));
			oldStart = start;
			oldStop = stop;
		}

		@Override
		public void onSelectionComplete(int start, int stop) {
			oldStart = -1;
			oldStop = -1;
		}

		@Override
		public void onSelectionClear(int start, int stop) {
			handle(start, stop, (row, localOffset) -> row.removeHoverEffect(localOffset, false, false));
			oldStart = -1;
			oldStop = -1;
		}

		private void handle(int start, int stop, BiConsumer<HexRow, Integer> action) {
			int incr = getHexColumns();
			int rowOffsetStart = start - (start % incr);
			int rowOffsetStop = Math.max(stop - (stop % incr), rowOffsetStart + incr);
			for (int rowOffset = rowOffsetStart; rowOffset <= rowOffsetStop; rowOffset += incr) {
				int itemIndex = rowOffset / incr;
				if (itemIndex >= offsets.size())
					break;
				Optional<HexRow> rowAtIndex = hexFlow.getCellIfVisible(itemIndex);
				if (rowAtIndex.isPresent()) {
					HexRow row = rowAtIndex.get();
					for (int localOffset = 0; localOffset < incr; localOffset++) {
						int offset = rowOffset + localOffset;
						if (offset < start)
							continue;
						else if (offset > stop)
							break;
						action.accept(row, localOffset);
					}
				}
			}
		}
	}
}
