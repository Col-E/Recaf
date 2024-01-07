package software.coley.recaf.ui.pane.editing.assembler.resolve;

/**
 * Common resolution type of some <i>"selected"</i> text within an {@link software.coley.recaf.ui.pane.editing.assembler.AssemblerPane}.
 * See implementations for possible resolved contents.
 *
 * @author Matt Coley
 */
public interface AssemblyResolution {
	/**
	 * Shared empty resolution instance.
	 */
	EmptyResolution EMPTY = new EmptyResolution();
}
