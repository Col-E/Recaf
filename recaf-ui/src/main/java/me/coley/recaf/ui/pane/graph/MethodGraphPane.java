package me.coley.recaf.ui.pane.graph;

import com.fxgraph.edges.DoubleCorneredEdge;
import com.fxgraph.graph.Graph;
import com.fxgraph.graph.ICell;
import com.fxgraph.graph.Model;
import com.fxgraph.layout.AbegoTreeLayout;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import me.coley.recaf.assemble.AstException;
import me.coley.recaf.assemble.analysis.Block;
import me.coley.recaf.assemble.analysis.Edge;
import me.coley.recaf.code.*;
import me.coley.recaf.graph.MethodGraph;
import me.coley.recaf.ui.behavior.MemberEditor;
import me.coley.recaf.ui.behavior.SaveResult;
import me.coley.recaf.util.threading.FxThreadUtil;
import org.abego.treelayout.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class MethodGraphPane extends BorderPane implements MemberEditor {

	private final static Logger logger = LoggerFactory.getLogger(MethodGraphPane.class);

	private MemberInfo targetMember;
	private ClassInfo classInfo;
	private final Graph graph = new Graph();

	public MethodGraphPane(MemberInfo targetMember, ClassInfo classInfo) {
		graph.getNodeGestures().setDragButton(MouseButton.NONE);
		graph.getViewportGestures().setPanButton(MouseButton.PRIMARY);
		graph.getViewportGestures().setZoomBounds(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
		setCenter(graph.getCanvas());
		this.targetMember = targetMember;
		this.classInfo = classInfo;
		updateGraph();
	}

	@Override
	public CommonClassInfo getCurrentClassInfo() {
		return classInfo;
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
		return targetMember;
	}

	@Override
	public void setTargetMember(MemberInfo targetMember) {
		this.targetMember = targetMember;
	}

	public void visitEdge(Map<Block, BlockCell> cellMap, Set<Block> visited, Block block) {

		if(!visited.add(block))
			return;

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

	private void updateGraph() {

		if(!targetMember.isMethod()) {
			logger.error("Target member is not a method, cannot generate graph.");
			return;
		}

		MethodGraph methodGraph = new MethodGraph((MethodInfo) targetMember, classInfo);

		TreeMap<Integer, Block> blocks;
		try {
			blocks = methodGraph.generate();
		} catch(AstException e) {
			logger.error("Error generating graph", e);
			return;
		}

		Map<Block, BlockCell> blockCells = new HashMap<>();

		Model model = graph.getModel();
		model.clear();
		graph.beginUpdate();

		List<CompletableFuture<Void>> futures = new ArrayList<>();

		// Create cells
		for (var block : blocks.entrySet()) {
			BlockCell cell = new BlockCell(block.getValue(), block.getKey());
			futures.add(cell.setCode());
			blockCells.put(block.getValue(), cell);
			model.addCell(cell);
		}

		CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenRun(() -> {

			Block root = blocks.get(0);

			// Create edges
			visitEdge(blockCells, new HashSet<>(), root);

			graph.endUpdate();

			BlockCell rootCell = (BlockCell) model.getAllCells().get(0);

			FxThreadUtil.run(() -> {

				graph.layout(new AbegoTreeLayout(100, 100, Configuration.Location.Bottom));

				FxThreadUtil.run(() -> graph.getCanvas().setPivot(
						rootCell.getGraphic(graph).getLayoutX() - graph.getCanvas().getWidth() / 2,
						rootCell.getGraphic(graph).getLayoutY() - graph.getCanvas().getHeight() / 2
				));

			});
		});

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
		if(newValue instanceof ClassInfo) {
			classInfo = (ClassInfo) newValue;
			updateGraph();
		} else if (newValue instanceof DexClassInfo){
			logger.warn("Android classes not supported yet");
		}
	}
}
