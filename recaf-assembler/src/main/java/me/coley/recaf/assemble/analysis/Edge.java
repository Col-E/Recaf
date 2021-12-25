package me.coley.recaf.assemble.analysis;

import java.util.Objects;

/**
 * Edge between frames.
 *
 * @author Matt Coley
 * @see Frame
 */
public class Edge {
	private final Frame from;
	private final Frame to;
	private final EdgeType type;

	/**
	 * @param from
	 * 		Edge source frame.
	 * @param to
	 * 		Edge destination frame.
	 * 		May be {@code null} depending on the {@link #getType() type}.
	 * @param type
	 * 		Edge type.
	 */
	public Edge(Frame from, Frame to, EdgeType type) {
		this.from = from;
		this.to = to;
		this.type = type;
	}

	/**
	 * @return Edge source frame.
	 */
	public Frame getFrom() {
		return from;
	}

	/**
	 * @return Edge destination frame.
	 * May be {@code null} depending on the {@link #getType() type}.
	 */
	public Frame getTo() {
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
