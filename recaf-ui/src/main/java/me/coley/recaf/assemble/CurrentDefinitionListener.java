package me.coley.recaf.assemble;

import me.coley.recaf.assemble.ast.arch.AbstractDefinition;

/**
 * Listener to handle new selections of fields and methods in classes.
 *
 * @author Matt Coley
 */
public interface CurrentDefinitionListener {
	/**
	 * @param unit
	 * 		Containing unit.
	 * @param selection
	 * 		New selection.
	 */
	void onCurrentDefinitionUpdate(ContextualUnit unit, AbstractDefinition selection);
}
