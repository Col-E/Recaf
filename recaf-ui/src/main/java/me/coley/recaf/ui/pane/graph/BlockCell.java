package me.coley.recaf.ui.pane.graph;

import com.fxgraph.cells.AbstractCell;
import com.fxgraph.graph.Graph;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import me.coley.recaf.assemble.analysis.Block;
import me.coley.recaf.assemble.ast.insn.AbstractInstruction;
import me.coley.recaf.ui.control.code.Languages;
import me.coley.recaf.ui.control.code.SyntaxFlow;

import java.util.concurrent.CompletableFuture;

/**
 * {@link AbstractCell} for a block of bytecode, usually a flow block.
 *
 * @author Justus Garbe
 */
public class BlockCell extends AbstractCell {
	private final String code;
	private Region graphic;
	final SyntaxFlow area;

	/**
	 * @param block
	 * 		Block to represent.
	 */
	public BlockCell(Block block) {
		this.code = generateCode(block);
		this.area = new SyntaxFlow(Languages.JAVA_BYTECODE);
	}

	/**
	 * @param block
	 * 		Block to generate representation of.
	 *
	 * @return Generated text.
	 */
	private static String generateCode(Block block) {
		StringBuilder sb = new StringBuilder();
		int i = 0;
		int size = block.getInstructions().size() - 1;
		for (AbstractInstruction instruction : block.getInstructions()) {
			sb.append(instruction.toString());
			if (i < size)
				sb.append("\n");
			i++;
		}
		return sb.toString();
	}

	/**
	 * @return Delegate to {@link SyntaxFlow#setCode(String)} with {@link #code}.
	 */
	public CompletableFuture<Void> setCode() {
		return area.setCode(code);
	}

	@Override
	public Region getGraphic(Graph graph) {
		if (graphic == null) {
			GridPane grid = new GridPane();
			grid.setVgap(5);
			grid.getStyleClass().add("graph-node");
			// Wrap in borderpane for background
			BorderPane wrapper = new BorderPane();
			wrapper.setCenter(area);
			wrapper.getStyleClass().add("graph-node-text");
			// Add to layout
			grid.addRow(0, wrapper);
			graphic = grid;
		}
		return graphic;
	}
}