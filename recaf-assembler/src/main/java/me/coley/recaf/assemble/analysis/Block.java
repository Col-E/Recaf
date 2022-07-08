package me.coley.recaf.assemble.analysis;

import me.coley.recaf.assemble.ast.insn.AbstractInstruction;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Basic CFG block.
 *
 * @author Matt Coley
 */
public class Block {
	private final List<AbstractInstruction> instructions = new ArrayList<>();
	private final List<Frame> frames = new ArrayList<>();
	private final List<Edge> edges = new ArrayList<>();

	/**
	 * @param instruction
	 * 		Instruction to add to the block.
	 * @param frame
	 * 		Frame of the instruction.
	 */
	public void add(AbstractInstruction instruction, Frame frame) {
		if (!instructions.contains(instruction)) {
			instructions.add(instruction);
			frames.add(frame);
		}
	}

	/**
	 * @return Instructions of the block.
	 * Aligns with {@link #getFrames()}.
	 */
	public List<AbstractInstruction> getInstructions() {
		return instructions;
	}

	/**
	 * @return Frames of the block.
	 * Aligns with {@link #getInstructions()}.
	 */
	public List<Frame> getFrames() {
		return frames;
	}

	/**
	 * Adds an edge between the current block and the given target block.
	 *
	 * @param targetBlock
	 * 		Targeted block to flow into.
	 */
	public void addJumpEdge(Block targetBlock) {
		Edge edge = new Edge(this, targetBlock, EdgeType.JUMP);
		if (!edges.contains(edge))
			edges.add(edge);
		if (!targetBlock.edges.contains(edge))
			targetBlock.edges.add(edge);
	}

	/**
	 * Adds an edge between the current block to a given handler block.
	 *
	 * @param handlerBlock
	 * 		Target handler block.
	 */
	public void addHandlerEdge(Block handlerBlock) {
		if (edges.stream().noneMatch(e -> e.getTo() == handlerBlock && e.getType() == EdgeType.EXCEPTION_HANDLER)) {
			Edge edge = new Edge(this, handlerBlock, EdgeType.EXCEPTION_HANDLER);
			edges.add(edge);
			handlerBlock.edges.add(edge);
		}
	}

	/**
	 * @return All edges to and from this block.
	 */
	public List<Edge> getEdges() {
		return edges;
	}

	/**
	 * @return Edges where the flow source is this block.
	 */
	public List<Edge> getOutboundEdges() {
		return getEdges().stream()
				.filter(e -> e.getFrom() == this)
				.collect(Collectors.toList());
	}

	/**
	 * @return Edges where the flow source is another block.
	 */
	public List<Edge> getInboundEdges() {
		return getEdges().stream()
				.filter(e -> e.getTo() == this)
				.collect(Collectors.toList());
	}

	@Override
	public boolean equals(Object o) {
		// See comment in 'hashCode'
		return super.equals(o);
	}

	@Override
	public int hashCode() {
		// This is explicitly defined just so it is clear that this is intentional.
		// We want to use the default hashing for this block class so that the hashcode
		// for edges work correctly.
		return super.hashCode();
	}
}