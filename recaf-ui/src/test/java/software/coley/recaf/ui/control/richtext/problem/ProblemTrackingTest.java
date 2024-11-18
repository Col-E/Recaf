package software.coley.recaf.ui.control.richtext.problem;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static software.coley.recaf.ui.control.richtext.problem.ProblemLevel.ERROR;
import static software.coley.recaf.ui.control.richtext.problem.ProblemLevel.WARN;
import static software.coley.recaf.ui.control.richtext.problem.ProblemPhase.*;

/**
 * Tests for {@link ProblemTracking}
 */
class ProblemTrackingTest {
	@Test
	void removeByPhase() {
		ProblemTracking tracking = new ProblemTracking();
		tracking.add(new Problem(1, 0, 0, ERROR, LINT, "message-err-lint"));
		tracking.add(new Problem(2, 0, 0, WARN, LINT, "message-warn-lint"));
		tracking.add(new Problem(3, 0, 0, ERROR, BUILD, "message-err-build"));
		tracking.add(new Problem(4, 0, 0, WARN, BUILD, "message-warn-build"));
		tracking.add(new Problem(5, 0, 0, ERROR, POST_PROCESS, "message-err-proc"));
		tracking.add(new Problem(6, 0, 0, WARN, POST_PROCESS, "message-warn-proc"));

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
		Problem problem = new Problem(0, 0, 0, ERROR, LINT, "message");
		Problem copy = new Problem(0, 0, 0, ERROR, LINT, "message");
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
		tracking.add(new Problem(0, 0, 0, ERROR, LINT, "message"));
		tracking.add(new Problem(1, 0, 0, ERROR, LINT, "message"));

		assertEquals(2, tracking.getProblems().size());
		assertTrue(tracking.removeByLine(1));
		assertEquals(1, tracking.getProblems().size());
	}

	@Test
	void onLinesRemoved() {
		Problem problem0 = new Problem(0, 0, 0, ERROR, LINT, "message");
		Problem problem10 = new Problem(10, 0, 0, ERROR, LINT, "message");
		ProblemTracking tracking = new ProblemTracking();
		tracking.add(problem0);
		tracking.add(problem10);

		// Initial state
		assertSame(problem0, tracking.getFirstProblemOnLine(0), "Invalid initial state");
		assertSame(problem10, tracking.getFirstProblemOnLine(10), "Invalid initial state");

		// Remove line 3
		//  - End range is exclusive so this is just removing one line
		tracking.onLinesRemoved(3, 4);

		// Validate line0 problem not moved, problem10 moved to line 9
		assertEquals(2, tracking.getAllProblems().size());
		assertSame(problem0, tracking.getFirstProblemOnLine(0), "Line 0 should not have moved");
		assertNull(tracking.getFirstProblemOnLine(10), "Line 10 should have moved");
		Problem problem9 = tracking.getFirstProblemOnLine(9);
		assertNotEquals(problem10, problem9, "Moved problem should be different reference + have different line number");
		assertEquals(9, problem9.line(), "Line 10 problem should have moved to line 9");
	}

	@Test
	void onLinesInserted() {
		Problem problem0 = new Problem(0, 0, 0, ERROR, LINT, "message");
		Problem problem10 = new Problem(10, 0, 0, ERROR, LINT, "message");
		ProblemTracking tracking = new ProblemTracking();
		tracking.add(problem0);
		tracking.add(problem10);

		// Initial state
		assertSame(problem0, tracking.getFirstProblemOnLine(0), "Invalid initial state");
		assertSame(problem10, tracking.getFirstProblemOnLine(10), "Invalid initial state");

		// Insert new line at line 3
		//  - End range is inclusive
		tracking.onLinesInserted(3, 3);

		// Validate line0 problem not moved, problem10 moved to line 11
		assertEquals(2, tracking.getAllProblems().size());
		assertSame(problem0, tracking.getFirstProblemOnLine(0), "Line 0 should not have moved");
		assertNull(tracking.getFirstProblemOnLine(10), "Line 10 should have moved");
		Problem problem11 = tracking.getFirstProblemOnLine(11);
		assertNotEquals(problem10, problem11, "Moved problem should be different reference + have different line number");
		assertEquals(11, problem11.line(), "Line 10 problem should have moved to line 11");
	}

	@Test
	void multipleOnLine() {
		ProblemTracking tracking = new ProblemTracking();
		tracking.add(new Problem(10, 1, 0, ERROR, LINT, "foo"));
		tracking.add(new Problem(10, 0, 0, ERROR, LINT, "fizz"));
		tracking.add(new Problem(10, 1, 0, ERROR, LINT, "buzz"));

		// 3 total
		assertEquals(3, tracking.getAllProblems().size());
		assertEquals(3, tracking.getProblemsByLevel(ERROR).size());
		assertEquals(3, tracking.getProblemsByPhase(LINT).size());

		// 1 map entry since they all are on a single line
		assertEquals(1, tracking.getProblems().size());

		// 3 on line 10
		List<Problem> problemsOnLine = tracking.getProblemsOnLine(10);
		assertEquals(3, problemsOnLine.size());
	}
}