package me.coley.recaf.ui.behavior;

import me.coley.recaf.ui.control.CollapsibleTabPane;

/**
 * Outline of a component with {@link CollapsibleTabPane} on the side.
 *
 * @author Matt Coley
 */
public interface ToolSideTabbed {
	/**
	 * Register the given tab pane.
	 *
	 * @param tabPane
	 * 		Tab pane to add to the current component.
	 */
	void installSideTabs(CollapsibleTabPane tabPane);

	/**
	 * Populate the given tab pane with content.
	 *
	 * @param tabPane
	 * 		Tab pane to add to.
	 */
	void populateSideTabs(CollapsibleTabPane tabPane);
}
