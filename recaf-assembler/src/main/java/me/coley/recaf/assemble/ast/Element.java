package me.coley.recaf.assemble.ast;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Base AST element which exposes position information of the element within the document text
 * and a dissassemble of the element via {@link #print()}.
 *
 * @author Matt Coley
 */
public interface Element extends Printable {
	/**
	 * @return Line number the element appears on.
	 */
	int getLine();

	/**
	 * @return Position in the line the element appears at.
	 */
	int getStart();

	/**
	 * @return Position in the line the element ends at.
	 */
	int getStop();

	/**
	 * @return Parent element.
	 */
	Element getParent();

	/**
	 * @return Child elements.
	 */
	List<Element> getChildren();

	/**
	 * @param type
	 * 		Type to filter children by.
	 *
	 * @return Child elements of the given type.
	 */
	@SuppressWarnings("unchecked")
	default <E extends Element> List<E> getChildrenOfType(Class<E> type) {
		List<E> list = new ArrayList<>();
		for (Element c : getChildren())
			if (type.isAssignableFrom(c.getClass()))
				list.add((E) c);
		return list;
	}

	/**
	 * @param line
	 * 		Line to check.
	 *
	 * @return First child element on the given line.
	 */
	Element getChildOnLine(int line);
}
