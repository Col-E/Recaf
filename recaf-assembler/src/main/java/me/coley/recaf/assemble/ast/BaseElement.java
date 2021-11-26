package me.coley.recaf.assemble.ast;

import java.util.ArrayList;
import java.util.List;

/**
 * Base AST element implementation.
 *
 * @author Matt Coley
 */
public abstract class BaseElement implements Element {
	private final List<Element> children = new ArrayList<>();
	private Element parent;
	private int line = -1;
	private int start = -1;
	private int stop = -1;

	/**
	 * @param element
	 * 		Element to add as a child.
	 *
	 * @return Element, for chaining.
	 */
	protected <E extends Element> E child(E element) {
		children.add(element);
		if (element instanceof BaseElement)
			((BaseElement) element).parent = this;
		return element;
	}

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
	public Element getParent() {
		return parent;
	}

	@Override
	public List<Element> getChildren() {
		return children;
	}

	@Override
	public Element getChildOnLine(int line) {
		for (Element element : getChildren())
			if (element.getLine() == line)
				return element;
		return null;
	}

	@Override
	public String toString() {
		return print();
	}
}
