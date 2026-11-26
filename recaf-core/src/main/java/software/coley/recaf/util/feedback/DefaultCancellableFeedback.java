package software.coley.recaf.util.feedback;

/**
 * Default implementation of {@link CancellableFeedback} that can be used for operations that support cancellation.
 *
 * @author Matt Coley
 */
public class DefaultCancellableFeedback implements CancellableFeedback, ClassFeedback, FileFeedback {
	private volatile boolean cancelled;

	@Override
	public void cancel() {
		cancelled = true;
	}

	@Override
	public boolean hasRequestedCancellation() {
		return cancelled;
	}
}
