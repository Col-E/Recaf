package me.coley.recaf.assemble.analysis;

import java.util.Objects;

/**
 * Edge between blocks.
 *
 * @author Matt Coley
 * @see Block
 */
public class Edge {
	private final Block from;
	private final Block to;
	private final EdgeType type;

	/**
	 * @param from
	 * 		Edge source block.
	 * @param to
	 * 		Edge destination block.
	 * 		May be {@code null} depending on the {@link #getType() type}.
	 * @param type
	 * 		Edge type.
	 */
	public Edge(Block from, Block to, EdgeType type) {
		this.from = from;
		this.to = to;
		this.type = type;
	}

	/**
	 * @return Edge source block.
	 */
	public Block getFrom() {
		return from;
	}

	/**
	 * @return Edge destination block.
	 * May be {@code null} depending on the {@link #getType() type}.
	 */
	public Block getTo() {
		return to;
	}

	/**
	 * @return Edge type.
	 */
	public EdgeType getType() {
		return type;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Edge edge = (Edge) o;
		return from.equals(edge.from) &&
				Objects.equals(to, edge.to) &&
				type.equals(edge.type);
	}

	@Override
	public int hashCode() {
		return Objects.hash(from, to, type);
	}
}
