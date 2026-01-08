package software.coley.recaf.ui.control.richtext.problem;

import jakarta.annotation.Nonnull;
import software.coley.collections.Unchecked;
import software.coley.recaf.ui.control.richtext.AbstractLineItemTracking;
import software.coley.recaf.ui.control.richtext.Editor;

import java.util.List;

/**
 * Tracking for problems to display in an {@link Editor}.
 *
 * @author Matt Coley
 */
public class ProblemTracking extends AbstractLineItemTracking<Problem, ProblemInvalidationListener> {
	/**
	 * @param level
	 * 		Problem level to filter problems by.
	 *
	 * @return List of problems matching the given level.
	 */
	@Nonnull
	public List<Problem> getProblemsByLevel(@Nonnull ProblemLevel level) {
		return getItems(p -> p.level() == level);
	}

	/**
	 * @param phase
	 * 		Problem phase to filter problems by.
	 *
	 * @return List of problems matching the given phase.
	 */
	@Nonnull
	public List<Problem> getProblemsByPhase(@Nonnull ProblemPhase phase) {
		return getItems(p -> p.phase() == phase);
	}

	/**
	 * @param problem
	 * 		Exact instance of a problem to remove.
	 *
	 * @return {@code true} when the problem was removed.
	 * {@code false} when the problem instance was not contained in the problems map.
	 */
	public boolean removeByInstance(@Nonnull Problem problem) {
		List<Problem> list;
		synchronized (items) {list = items.get(problem.line());}
		if (list != null) {
			boolean updated = list.removeIf(p -> p == problem);
			if (updated)
				notifyListeners("Exception thrown when removing problem from tracking");
			// if list empty, remove the entry
			if (list.isEmpty())
				items.remove(problem.line());
			return updated;
		}
		return false;
	}

	/**
	 * @param phase
	 * 		The phase to remove problems of.
	 *
	 * @return {@code true} when one or more problems matching the phase were removed.
	 */
	public boolean removeByPhase(@Nonnull ProblemPhase phase) {
		boolean updated;
		synchronized (items) {updated = items.values().removeIf(list -> list.removeIf(p -> p.phase() == phase));}
		if (updated)
			notifyListeners("Exception thrown when removing problems from tracking");
		return updated;
	}

	@Override
	protected void notifyListeners(@Nonnull String failureMessage) {
		Unchecked.checkedForEach(listeners, ProblemInvalidationListener::onProblemInvalidation,
				(listener, t) -> logger.error(failureMessage, t));

	}

	@Override
	protected int getLine(@Nonnull Problem item) {
		return item.line();
	}

	@Override
	protected Problem withLine(@Nonnull Problem item, int newLine) {
		return item.withLine(newLine);
	}
}
