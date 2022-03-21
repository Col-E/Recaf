package me.coley.recaf.assemble.ast;

import me.coley.recaf.assemble.IllegalAstException;
import me.coley.recaf.assemble.ast.meta.Label;

import java.util.List;
import java.util.Map;

/**
 * An element that can redirect control flow to other labels.
 *
 * @author Matt Coley
 */
public interface FlowControl extends Element {
	/**
	 * @param labelMap
	 * 		Map to pull labels from by name.
	 *
	 * @return List of branch targets of the flow control operation. This does not include fall-through.
	 *
	 * @throws IllegalAstException
	 * 		When a target could not be found in the given map.
	 */
	List<Label> getTargets(Map<String, Label> labelMap) throws IllegalAstException;

	/**
	 * @return {@code true} when the branch is always taken.
	 */
	default boolean isForced() {
		return false;
	}
}
