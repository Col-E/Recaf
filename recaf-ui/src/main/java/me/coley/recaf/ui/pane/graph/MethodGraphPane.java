package me.coley.recaf.ui.pane.graph;

import com.fxgraph.edges.DoubleCorneredEdge;
import com.fxgraph.graph.Graph;
import com.fxgraph.graph.Model;
import com.fxgraph.layout.AbegoTreeLayout;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import me.coley.recaf.assemble.AstException;
import me.coley.recaf.assemble.analysis.Analysis;
import me.coley.recaf.assemble.analysis.Analyzer;
import me.coley.recaf.assemble.analysis.Block;
import me.coley.recaf.assemble.analysis.Edge;
import me.coley.recaf.assemble.ast.Unit;
import me.coley.recaf.assemble.ast.arch.MethodDefinition;
import me.coley.recaf.assemble.ast.meta.Label;
import me.coley.recaf.assemble.transformer.BytecodeToAstTransformer;
import me.coley.recaf.code.*;
import me.coley.recaf.ui.behavior.MemberEditor;
import me.coley.recaf.ui.behavior.SaveResult;
import me.coley.recaf.util.threading.FxThreadUtil;
import me.coley.recaf.util.visitor.SingleMemberVisitor;
import org.abego.treelayout.Configuration;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Display for a control flow graph of a method.
 *
 * @author Justus Garbe
 */
public class MethodGraphPane extends BorderPane implements MemberEditor {
	private final static Logger logger = LoggerFactory.getLogger(MethodGraphPane.class);
	private MethodInfo method;
	private ClassInfo declaring;
	private final Graph graph = new Graph();



	/**
	 * @param declaring
	 * 		Class declaring the method.
	 * @param method
	 * 		The method to show the CFG of.
	 */
	public MethodGraphPane(ClassInfo declaring, MethodInfo method) {
		this.declaring = declaring;
		this.method = method;
		graph.getNodeGestures().setDragButton(MouseButton.SECONDARY);
		graph.getViewportGestures().setPanButton(MouseButton.PRIMARY);
		graph.getViewportGestures().setZoomBounds(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
		setCenter(graph.getCanvas());
		updateGraph();
	}

	@Override
	public CommonClassInfo getCurrentClassInfo() {
		return declaring;
	}

	@Override
	public boolean supportsMemberSelection() {
		return false;
	}

	@Override
	public boolean isMemberSelectionReady() {
		return false;
	}

	@Override
	public void selectMember(MemberInfo memberInfo) {
		// No-op
	}

	@Override
	public MemberInfo getTargetMember() {
		return method;
	}

	@Override
	public void setTargetMember(MemberInfo targetMember) {
		if (targetMember instanceof MethodInfo)
			method = (MethodInfo) targetMember;
	}

	private void updateGraph() {
		// Skip if not set
		if (method == null) {
			logger.error("No target method defined, cannot generate graph pane content");
			return;
		}
		// Get control flow graph
		TreeMap<Integer, Block> blocks;
		try {
			blocks = generate();
		} catch (AstException e) {
			logger.error("Error analyzing method control flow blocks", e);
			return;
		}
		// Remove last block if it is just one label instruction
		if (blocks.size() > 0) {
			Block last = blocks.get(blocks.lastKey());
			if (last.getInstructions().size() == 1 && last.getInstructions().get(0) instanceof Label)
				blocks.remove(blocks.lastKey());
		}
		// Create graph model
		Model model = graph.getModel();
		model.clear();
		graph.beginUpdate();
		// Create cells
		List<CompletableFuture<Void>> futures = new ArrayList<>();
		Map<Block, BlockCell> blockCells = new HashMap<>();
		for (var block : blocks.entrySet()) {
			BlockCell cell = new BlockCell(block.getValue());
			futures.add(cell.setCode());
			blockCells.put(block.getValue(), cell);
			model.addCell(cell);
		}
		// When all cells are generated, create the edges between them.
		CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenRun(() -> {
			Block root = blocks.get(0);
			// Create edges
			visitEdge(blockCells, new HashSet<>(), root);
			graph.endUpdate();
			BlockCell rootCell = (BlockCell) model.getAllCells().get(0);
			// Layout the cells in a tree pattern
			FxThreadUtil.run(() -> {
				graph.layout(new AbegoTreeLayout(100, 100, Configuration.Location.Bottom));
				// Looks silly, but does work to center the screen on the root cell
				FxThreadUtil.run(() -> graph.getCanvas().setPivot(
						rootCell.getGraphic(graph).getLayoutX() - graph.getCanvas().getWidth() / 2,
						rootCell.getGraphic(graph).getLayoutY() - graph.getCanvas().getHeight() / 2
				));
			});
		});
	}

	private void visitEdge(Map<Block, BlockCell> cellMap, Set<Block> visited, Block block) {
		// Skip already visited blocks
		if (!visited.add(block))
			return;
		// Get outbound edges and create graph edge
		BlockCell cell = cellMap.get(block);
		Model model = graph.getModel();
		for (Edge outboundEdge : block.getOutboundEdges()) {
			Block targetBlock = outboundEdge.getTo();
			BlockCell targetCell = cellMap.get(targetBlock);
			DoubleCorneredEdge edge = new DoubleCorneredEdge(cell, targetCell, Orientation.VERTICAL);

			model.addEdge(edge);
			visitEdge(cellMap, visited, targetBlock);
		}
	}

	private TreeMap<Integer, Block> generate() throws AstException {
		// Visit the target method
		ClassNode node = new ClassNode();
		ClassReader cr = declaring.getClassReader();
		cr.accept(new SingleMemberVisitor(node, method), ClassReader.SKIP_FRAMES);
		MethodNode methodNode = node.methods.get(0);
		// Transform the bytecode to AST
		BytecodeToAstTransformer transformer = new BytecodeToAstTransformer(methodNode);
		transformer.visit();
		Unit unit = transformer.getUnit();
		MethodDefinition definition = unit.getDefinitionAsMethod();
		// Run analysis to get block information.
		Analyzer analyzer = new Analyzer(declaring.getName(), definition);
		Analysis analysis = analyzer.analyze(true, false);
		return analysis.getBlocks();
	}

	@Override
	public SaveResult save() {
		return SaveResult.IGNORED;
	}

	@Override
	public boolean supportsEditing() {
		return false;
	}

	@Override
	public Node getNodeRepresentation() {
		return this;
	}

	@Override
	public void onUpdate(CommonClassInfo newValue) {
		if (newValue instanceof ClassInfo) {
			declaring = (ClassInfo) newValue;
			updateGraph();
		} else if (newValue instanceof DexClassInfo) {
			logger.warn("Android classes not supported yet");
		}
	}
}
