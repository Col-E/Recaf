package software.coley.recaf.ui.control.richtext.problem;

import org.junit.jupiter.api.Test;
import static  software.coley.recaf.ui.control.richtext.problem.ProblemLevel.*;
import static  software.coley.recaf.ui.control.richtext.problem.ProblemPhase.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ProblemTracking}
 */
class ProblemTrackingTest {
	@Test
	void removeByPhase() {
		ProblemTracking tracking = new ProblemTracking();
		tracking.add(new Problem(1, 0, ERROR, LINT, "message-err-lint"));
		tracking.add(new Problem(2, 0, WARN, LINT, "message-warn-lint"));
		tracking.add(new Problem(3, 0, ERROR, BUILD, "message-err-build"));
		tracking.add(new Problem(4, 0, WARN, BUILD, "message-warn-build"));
		tracking.add(new Problem(5, 0, ERROR, POST_PROCESS, "message-err-proc"));
		tracking.add(new Problem(6, 0, WARN, POST_PROCESS, "message-warn-proc"));

		// Base truth
		assertEquals(6, tracking.getProblems().size());

		// Remove 2 BUILD problems
		assertTrue(tracking.removeByPhase(BUILD));
		assertEquals(4, tracking.getProblems().size());

		// Duplicate calls yield no change
		assertFalse(tracking.removeByPhase(BUILD));
		assertEquals(4, tracking.getProblems().size());
	}

	@Test
	void removeByInstance() {
		ProblemTracking tracking = new ProblemTracking();
		Problem problem = new Problem(0, 0, ERROR, LINT, "message");
		Problem copy = new Problem(0, 0, ERROR, LINT, "message");
		tracking.add(problem);

		// Base truth
		assertEquals(1, tracking.getProblems().size());

		// Remove by instance must be THE instance, equality does not cut it
		assertFalse(tracking.removeByInstance(copy));
		assertTrue(tracking.removeByInstance(problem));

		// Should be empty now
		assertEquals(0, tracking.getProblems().size());
	}

	@Test
	void removeByLine() {
		ProblemTracking tracking = new ProblemTracking();
		tracking.add(new Problem(0, 0, ERROR, LINT, "message"));
		tracking.add(new Problem(1, 0, ERROR, LINT, "message"));

		assertEquals(2, tracking.getProblems().size());
		assertTrue(tracking.removeByLine(1));
		assertEquals(1, tracking.getProblems().size());
	}

	@Test
	void foo() {
		ProblemTracking tracking = new ProblemTracking();
		tracking.onLinesInserted(1, 2);
		tracking.add(new Problem(5, 0, ERROR, LINT, "message"));
	}
}