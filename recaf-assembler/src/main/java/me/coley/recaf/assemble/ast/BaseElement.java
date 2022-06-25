package me.coley.recaf.assemble.ast;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Base AST element implementation.
 *
 * @author Matt Coley
 */
public abstract class BaseElement implements Element {
	private final List<Element> children = new ArrayList<>();
	private Element parent;
	private int line = -1;
	private int columnStart = -1;
	private int columnEnd = -1;
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
	 * @param columnStart
	 * 		Start column of the element.
	 * @param columnEnd
	 * 		End column of the element.
	 * @param <E>
	 * 		Current element type.
	 *
	 * @return Self.
	 */
	@SuppressWarnings("unchecked")
	public <E extends BaseElement> E setColumnRange(int columnStart, int columnEnd) {
		this.columnStart = columnStart;
		this.columnEnd = columnEnd;
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
	public int getColumnStart() {
		return columnStart;
	}

	@Override
	public int getColumnEnd() {
		return columnEnd;
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
	public List<Element> getChildrenAt(int line) {
		return getChildren().stream()
				.filter(e -> e.getLine() == line)
				.collect(Collectors.toList());
	}

	@Override
	public Element getChildAt(int line, int column) {
		List<Element> elementsOnLine = getChildrenAt(line);
		if (elementsOnLine.isEmpty()) {
			return null;
		} else if (elementsOnLine.size() == 1) {
			return elementsOnLine.get(0);
		}
		// Get the element closest to the column
		int minElementDist = Integer.MAX_VALUE;
		Element closestElement = null;
		for (Element element : elementsOnLine) {
			int start = element.getColumnStart();
			int end = element.getColumnEnd();
			if (column >= start && column <= end)
				return element;
			int distToStart = Math.abs(column - start);
			int distToEnd = Math.abs(column - end);
			int min = Math.min(distToStart, distToEnd);
			if (min < minElementDist) {
				closestElement = element;
				minElementDist = min;
			}
		}
		return closestElement;

	}

	@Override
	public Element getChildAt(int position) {
		Element[] elements = getChildren().toArray(new Element[0]);
		if (elements.length == 0) { // base case
			return null;
		}
		if (elements.length == 1) { // base case
			return elements[0];
		}

		// perform binary search
		int start = 0;
		int end = elements.length - 1;

		while (start <= end) {
			int mid = (start + end) / 2;
			if (position < elements[mid].getStart()) {
				end = mid - 1;
			} else if (position > elements[mid].getStop()) {
				start = mid + 1;
			} else {
				return elements[mid];
			}
		}
		return null;
	}

	@Override
	public String toString() {
		return print(PrintContext.DEFAULT_CTX);
	}
}
