package me.coley.recaf.ui.behavior;

/**
 * Outline of interactive text component.
 *
 * @author Matt Coley
 */
public interface InteractiveText {
	/**
	 * @return Full text content.
	 */
	String getFullText();

	/**
	 * @return Selected text content.
	 */
	String getSelectionText();

	/**
	 * @return Selected text start index within the full text.
	 * {@code -1} when there is no selection.
	 */
	int getSelectionStart();

	/**
	 * @return Selected text stop index within the full text.
	 * {@code -1} when there is no selection.
	 */
	int getSelectionStop();
}
