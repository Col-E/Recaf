package me.coley.recaf.ui.pane.graph;

import com.fxgraph.cells.AbstractCell;
import com.fxgraph.graph.Graph;
import javafx.scene.control.*;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import me.coley.recaf.assemble.analysis.Block;
import me.coley.recaf.assemble.ast.insn.AbstractInstruction;
import me.coley.recaf.ui.control.code.Languages;
import me.coley.recaf.ui.control.code.ProblemTracking;
import me.coley.recaf.ui.control.code.SyntaxArea;
import me.coley.recaf.ui.control.code.SyntaxFlow;
import me.coley.recaf.util.threading.FxThreadUtil;
import org.fxmisc.richtext.CodeArea;

import java.util.concurrent.CompletableFuture;

/**
 * {@link AbstractCell} for a block of bytecode, usually a flow block.
 *
 * @author Justus Garbe
 *
 */
public class BlockCell extends AbstractCell {

	private final String code;
	private final Block block;
	private final int blockIndex;
	private Region graphic;
	final SyntaxFlow area;

	public BlockCell(Block block, int blockIndex) {
		this.code = getCode(block);
		this.block = block;
		this.blockIndex = blockIndex;
		this.area = new SyntaxFlow(Languages.JAVA_BYTECODE);
	}

	public String getCode(Block block) {
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

	public CompletableFuture<Void> setCode() {
		return area.setCode(code);
	}

	@Override
	public Region getGraphic(Graph graph) {
		// if region null, cache it
		if (graphic == null) {

			GridPane grid = new GridPane();

			grid.setVgap(5);
			grid.getStyleClass().add("graph-node");

			BorderPane wrapper = new BorderPane();
			wrapper.setCenter(area);
			wrapper.getStyleClass().add("graph-node-text");

			grid.addRow(0, wrapper);

			graphic = grid;
		}
		return graphic;
	}

	public Region getGraphic() {
		return graphic;
	}

	public String getCode() {
		return code;
	}

	public Block getBlock() {
		return block;
	}

	public int getBlockIndex() {
		return blockIndex;
	}

	public SyntaxFlow getArea() {
		return area;
	}
}
