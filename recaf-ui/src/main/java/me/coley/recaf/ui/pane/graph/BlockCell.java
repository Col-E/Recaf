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
import me.coley.recaf.util.threading.FxThreadUtil;
import org.fxmisc.richtext.CodeArea;

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
	final SyntaxArea area;

	public BlockCell(Block block, int blockIndex) {
		this.code = getCode(block);
		this.block = block;
		this.blockIndex = blockIndex;
		this.area = new BlockSyntaxArea();
		this.area.setText(code);
		this.area.setEditable(false);
	}

	public String getCode(Block block) {
		StringBuilder sb = new StringBuilder();
		for (AbstractInstruction instruction : block.getInstructions()) {
			sb.append(instruction.toString()).append("\n");
		}
		return sb.toString();
	}

	@Override
	public Region getGraphic(Graph graph) {
		// if region null, cache it
		if (graphic == null) {

			GridPane grid = new GridPane();

			grid.setVgap(5);
			grid.getStyleClass().add("graph-node");

			Text text = new Text(code);
			text.getStyleClass().add("graph-node-text-virt");

			System.out.println("Text layout:" + text.getLayoutBounds().getHeight());

			area.setEditable(false);
			area.getStyleClass().add("graph-node-text");

			area.setWrapText(true);
			area.setPrefHeight(text.getLayoutBounds().getHeight() + 40);

			area.setPrefWidth(text.getLayoutBounds().getWidth() + 40);

			grid.addRow(0, area);

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

	public SyntaxArea getArea() {
		return area;
	}
}
