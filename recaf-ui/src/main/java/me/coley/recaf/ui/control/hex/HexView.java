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
	 * Exposed so that when a {@link HexRow} gains hover-access it can notify the header to update
	 * its highlighted column to match.
	 *
	 * @return Header row.
	 */
	public HexRow getHeader() {
		return header;
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
}
