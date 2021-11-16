package me.coley.recaf.assemble.ast;

/**
 * Base AST element implementation.
 *
 * @author Matt Coley
 */
public abstract class BaseElement implements Element {
	private int line = -1;
	private int start = -1;
	private int stop = -1;

	/**
	 * @param line
	 * 		Line number to apply.
	 * @param <E>
	 * 		Current element type.
	 *
	 * @return Self.
	 */
	@SuppressWarnings("unchecked")
	public <E extends BaseElement> E setLine(int line) {
		this.line = line;
		return (E) this;
	}

	/**
	 * @param start
	 * 		Element start position.
	 * @param stop
	 * 		Element end position.
	 * @param <E>
	 * 		Current element type.
	 *
	 * @return Self.
	 */
	@SuppressWarnings("unchecked")
	public <E extends BaseElement> E setRange(int start, int stop) {
		this.start = start;
		this.stop = stop;
		return (E) this;
	}

	@Override
	public int getLine() {
		return line;
	}

	@Override
	public int getStart() {
		return start;
	}

	@Override
	public int getStop() {
		return stop;
	}

	@Override
	public String toString() {
		return print();
	}
}
