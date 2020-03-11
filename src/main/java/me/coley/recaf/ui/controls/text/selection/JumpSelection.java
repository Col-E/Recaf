package me.coley.recaf.ui.controls.text.selection;

/**
 * Wrapper for selected jump instruction.
 *
 * @author Matt
 */
public class JumpSelection {
	public final String destination;

	/**
	 * @param destination
	 * 		Destination label name.
	 */
	public JumpSelection(String destination) {
		this.destination = destination;
	}
}