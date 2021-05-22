package me.coley.recaf.ui.control.code;

import java.util.Objects;

/**
 * Pair of positions for a pair of brackets in a {@link SyntaxArea} document.
 *
 * @author Matt Coley
 * @see BracketSupport
 */
public class BracketPair {
	private final int start;
	private final int end;

	/**
	 * @param start
	 * 		Position of opening bracket.
	 * @param end
	 * 		Position of closing bracket.
	 */
	public BracketPair(int start, int end) {
		this.start = start;
		this.end = end;
	}

	/**
	 * @return Position of opening bracket.
	 */
	public int getStart() {
		return start;
	}

	/**
	 * @return Position of closing bracket.
	 */
	public int getEnd() {
		return end;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		BracketPair that = (BracketPair) o;
		return start == that.start &&
				end == that.end;
	}

	@Override
	public int hashCode() {
		return Objects.hash(start, end);
	}

	@Override
	public String toString() {
		return "[" + start + " - " + end + ']';
	}
}