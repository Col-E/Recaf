package me.coley.recaf.ui.control.code;

/**
 * Listener to receive updates when a {@link BracketTracking} adds or removes a {@link BracketPair}.
 *
 * @author Matt Coley
 */
public interface BracketUpdateListener {
	/**
	 * @param line
	 * 		Document line number.
	 * @param pair
	 * 		Bracket associated with the line.
	 */
	void onBracketAdded(int line, BracketPair pair);

	/**
	 * @param line
	 * 		Document line number.
	 * @param pair
	 * 		Bracket removed from the line.
	 */
	void onBracketRemoved(int line, BracketPair pair);
}
