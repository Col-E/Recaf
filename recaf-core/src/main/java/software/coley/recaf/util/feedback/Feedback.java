package software.coley.recaf.util.feedback;

/**
 * Base interface for feedback to operations.
 *
 * @author Matt Coley
 */
public interface Feedback {
	/**
	 * Called when the operation completes.
	 */
	default void onCompletion() {}
}
