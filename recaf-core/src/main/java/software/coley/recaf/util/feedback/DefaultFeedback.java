package software.coley.recaf.util.feedback;

/**
 * Default implementation of {@link Feedback} that can be used for operations visiting both classes and files.
 *
 * @author Matt Coley
 */
public class DefaultFeedback implements ClassFeedback, FileFeedback {
	/** Shared instance of default feedback that does not filter anything. */
	public static final DefaultFeedback INSTANCE = new DefaultFeedback();
}
