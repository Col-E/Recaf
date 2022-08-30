package me.coley.recaf.ui.control.code;

import me.coley.recaf.util.logging.DebuggingLogger;
import me.coley.recaf.util.logging.Logging;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generalized problem tracking for some text document. Line numbers are assumed to start at 1.
 *
 * @author Matt Coley
 */
public class ProblemTracking {
	private static final DebuggingLogger logger = Logging.get(ProblemTracking.class);
	private final Map<Integer, ProblemInfo> problemLineMap = new ConcurrentHashMap<>();
	private final List<ProblemUpdateListener> listeners = new ArrayList<>();
	private ProblemIndicatorInitializer indicatorInitializer;

	/**
	 * Update problem lines based on the insertion of a line at the given index.
	 *
	 * @param line
	 * 		Line of inserted text.
	 */
	public void lineInserted(int line) {
		linesInserted(line, line);
	}

	/**
	 * Update problem lines based on the removal of a line at the given index.
	 *
	 * @param line
	 * 		Line of removed text.
	 */
	public void lineRemoved(int line) {
		linesRemoved(line, line);
	}

	/**
	 * Update problem lines based on the insertion of one ({@code (N, N) as parameters})
	 * or more ({@code (N, M) as parameters, where M>N}) lines.
	 *
	 * @param startLine
	 * 		Line insertion index <i>(Inclusive)</i>.
	 * @param endLine
	 * 		Last line of inserted text <i>(Inclusive)</i>, used to calculate number of lines to offset items.
	 */
	public void linesInserted(int startLine, int endLine) {
		logger.debugging(l -> l.trace("Lines inserted: {}-{}", startLine, endLine));
		TreeSet<Map.Entry<Integer, ProblemInfo>> set =
				new TreeSet<>((o1, o2) -> Integer.compare(o2.getKey(), o1.getKey()));
		set.addAll(problemLineMap.entrySet());
		set.stream()
				.filter(e -> e.getKey() >= startLine)
				.forEach(e -> {
					int shift = 1 + endLine - startLine;
					// Shift all problems down by the shift amount
					int key = e.getKey();
					removeProblem(key);
					addProblem(key + shift, e.getValue());
					logger.debugging(l -> l.trace("Move problem '{}' down {} lines",
							e.getValue().getMessage(), shift));
				});
	}

	/**
	 * Update problem lines based on the removal of one ({@code (N, N) as parameters})
	 * or more ({@code (N, M) as parameters, where M>N}) lines.
	 *
	 * @param startLine
	 * 		Line removal index <i>(Inclusive)</i>.
	 * @param endLine
	 * 		Last line of removed text <i>(Inclusive)</i>, used to calculate number of lines to offset items.
	 */
	public void linesRemoved(int startLine, int endLine) {
		logger.debugging(l -> l.trace("Lines removed: {}-{}", startLine, endLine));
		// We will want to sort the order of removed lines so we
		TreeSet<Map.Entry<Integer, ProblemInfo>> set = new TreeSet<>(Comparator.comparingInt(Map.Entry::getKey));
		set.addAll(problemLineMap.entrySet());
		set.stream()
				.filter(e -> e.getKey() >= startLine)
				.forEach(e -> {
					int shift = endLine - startLine;
					// Shift all problems up by the shift amount
					int key = e.getKey();
					removeProblem(key);
					// Don't add problem back if its in the removed range
					if (key > startLine + shift) {
						logger.debugging(l -> l.trace("Move problem '{}' up {} lines",
								e.getValue().getMessage(), shift));
						addProblem(key - shift, e.getValue());
					} else {
						logger.debugging(l -> l.trace("Remove problem '{}' in deleted range",
								e.getValue().getMessage()));
					}
				});
	}

	/**
	 * @return Problem indicator initializer. Adds behavior to problem indicators when shown.
	 */
	public ProblemIndicatorInitializer getIndicatorInitializer() {
		return indicatorInitializer;
	}

	/**
	 * @param indicatorInitializer
	 * 		Problem indicator initializer. Adds behavior to problem indicators when shown.
	 */
	public void setIndicatorInitializer(ProblemIndicatorInitializer indicatorInitializer) {
		this.indicatorInitializer = indicatorInitializer;
	}

	/**
	 * @param listener
	 * 		Listener to receive updates when an problem is added or removed.
	 */
	public void addProblemListener(ProblemUpdateListener listener) {
		listeners.add(listener);
	}

	/**
	 * @param listener
	 * 		Listener to remove from receiving updates.
	 *
	 * @return {@code true} when the listener was removed.
	 */
	public boolean removeProblemListener(ProblemUpdateListener listener) {
		return listeners.remove(listener);
	}

	/**
	 * @return {@code true} if there are any problems.
	 */
	public boolean hasProblems() {
		return !problemLineMap.isEmpty();
	}

	/**
	 * @param level
	 * 		Problem serverity level.
	 *
	 * @return {@code true} if there are any problems of the given level.
	 */
	public boolean hasProblems(ProblemLevel level) {
		return problemLineMap.values().stream().anyMatch(p -> p.getLevel() == level);
	}

	/**
	 * @param line
	 * 		Document line number.
	 *
	 * @return {@code true} if a problem for the line is tracked.
	 */
	public boolean hasProblem(int line) {
		return problemLineMap.containsKey(line);
	}

	/**
	 * @param line
	 * 		Document line number.
	 *
	 * @return The tracked problem information for a line. <br>
	 * {@code null} when there is no problem on the line.
	 */
	public ProblemInfo getProblem(int line) {
		return problemLineMap.get(line);
	}

	/**
	 * Track a new problem at the given line.
	 *
	 * @param line
	 * 		Document line number.
	 * @param info
	 * 		Problem information.
	 */
	public void addProblem(int line, ProblemInfo info) {
		if (info != null) {
			ProblemInfo oldValue = problemLineMap.put(line, info);
			if (oldValue != null) {
				listeners.forEach(listener -> listener.onProblemRemoved(line, oldValue));
			}
			listeners.forEach(listener -> listener.onProblemAdded(line, info));
		}
	}

	/**
	 * Remove an problem from the given line.
	 *
	 * @param line
	 * 		Document line number.
	 */
	public void removeProblem(int line) {
		ProblemInfo info = problemLineMap.remove(line);
		if (info != null) {
			listeners.forEach(listener -> listener.onProblemRemoved(line, info));
		}
	}

	/**
	 * @return Current tracked problems.
	 */
	public Collection<ProblemInfo> getProblems() {
		return new TreeSet<>(problemLineMap.values());
	}

	/**
	 * Clears all problems of the given origin type.
	 *
	 * @param origin
	 * 		Type of problem to remove.
	 */
	public void clearOfType(ProblemOrigin origin) {
		for (Map.Entry<Integer, ProblemInfo> e : new ArrayList<>(problemLineMap.entrySet())) {
			if (e.getValue().getOrigin() == origin) {
				removeProblem(e.getKey());
			}
		}
	}
}
