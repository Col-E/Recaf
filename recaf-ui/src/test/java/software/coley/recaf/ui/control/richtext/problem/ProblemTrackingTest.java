package software.coley.recaf.ui.control.richtext.problem;

import org.junit.jupiter.api.Test;
import software.coley.recaf.ui.control.richtext.inheritance.InheritanceTracking;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static software.coley.recaf.ui.control.richtext.problem.ProblemLevel.ERROR;
import static software.coley.recaf.ui.control.richtext.problem.ProblemLevel.WARN;
import static software.coley.recaf.ui.control.richtext.problem.ProblemPhase.*;

/**
 * Tests for {@link ProblemTracking}.
 * <br>
 * Since the base class is shared with {@link InheritanceTracking} as well, its largely covering that too.
 */
class ProblemTrackingTest {
	@Test
	void removeByPhase() {
		ProblemTracking tracking = new ProblemTracking();
		tracking.addItem(new Problem(1, 0, 0, ERROR, LINT, "message-err-lint"));
		tracking.addItem(new Problem(2, 0, 0, WARN, LINT, "message-warn-lint"));
		tracking.addItem(new Problem(3, 0, 0, ERROR, BUILD, "message-err-build"));
		tracking.addItem(new Problem(4, 0, 0, WARN, BUILD, "message-warn-build"));
		tracking.addItem(new Problem(5, 0, 0, ERROR, POST_PROCESS, "message-err-proc"));
		tracking.addItem(new Problem(6, 0, 0, WARN, POST_PROCESS, "message-warn-proc"));

		// Base truth
		assertEquals(6, tracking.getItems().size());

		// Remove 2 BUILD problems
		assertTrue(tracking.removeByPhase(BUILD));
		assertEquals(4, tracking.getItems().size());

		// Duplicate calls yield no change
		assertFalse(tracking.removeByPhase(BUILD));
		assertEquals(4, tracking.getItems().size());
	}

	@Test
	void removeByInstance() {
		ProblemTracking tracking = new ProblemTracking();
		Problem problem = new Problem(0, 0, 0, ERROR, LINT, "message");
		Problem copy = new Problem(0, 0, 0, ERROR, LINT, "message");
		tracking.addItem(problem);

		// Base truth
		assertEquals(1, tracking.getItems().size());

		// Remove by instance must be THE instance, equality does not cut it
		assertFalse(tracking.removeByInstance(copy));
		assertTrue(tracking.removeByInstance(problem));

		// Should be empty now
		assertEquals(0, tracking.getItems().size());
	}

	@Test
	void removeByLine() {
		ProblemTracking tracking = new ProblemTracking();
		tracking.addItem(new Problem(0, 0, 0, ERROR, LINT, "message"));
		tracking.addItem(new Problem(1, 0, 0, ERROR, LINT, "message"));

		assertEquals(2, tracking.getItems().size());
		assertTrue(tracking.removeByLine(1));
		assertEquals(1, tracking.getItems().size());
	}

	@Test
	void onLinesRemoved() {
		Problem problem0 = new Problem(0, 0, 0, ERROR, LINT, "message");
		Problem problem10 = new Problem(10, 0, 0, ERROR, LINT, "message");
		TestProblemTracking tracking = new TestProblemTracking();
		tracking.addItem(problem0);
		tracking.addItem(problem10);

		// Initial state
		assertSame(problem0, tracking.getFirstItemOnLine(0), "Invalid initial state");
		assertSame(problem10, tracking.getFirstItemOnLine(10), "Invalid initial state");

		// Remove line 3
		//  - End range is exclusive so this is just removing one line
		tracking.onLinesRemoved(3, 4);

		// Validate line0 problem not moved, problem10 moved to line 9
		assertEquals(2, tracking.getAllItems().size());
		assertSame(problem0, tracking.getFirstItemOnLine(0), "Line 0 should not have moved");
		assertNull(tracking.getFirstItemOnLine(10), "Line 10 should have moved");
		Problem problem9 = tracking.getFirstItemOnLine(9);
		assertNotEquals(problem10, problem9, "Moved problem should be different reference + have different line number");
		assertEquals(9, problem9.line(), "Line 10 problem should have moved to line 9");
	}

	@Test
	void onLinesInserted() {
		Problem problem0 = new Problem(0, 0, 0, ERROR, LINT, "message");
		Problem problem10 = new Problem(10, 0, 0, ERROR, LINT, "message");
		TestProblemTracking tracking = new TestProblemTracking();
		tracking.addItem(problem0);
		tracking.addItem(problem10);

		// Initial state
		assertSame(problem0, tracking.getFirstItemOnLine(0), "Invalid initial state");
		assertSame(problem10, tracking.getFirstItemOnLine(10), "Invalid initial state");

		// Insert new line at line 3
		//  - End range is inclusive
		tracking.onLinesInserted(3, 3);

		// Validate line0 problem not moved, problem10 moved to line 11
		assertEquals(2, tracking.getAllItems().size());
		assertSame(problem0, tracking.getFirstItemOnLine(0), "Line 0 should not have moved");
		assertNull(tracking.getFirstItemOnLine(10), "Line 10 should have moved");
		Problem problem11 = tracking.getFirstItemOnLine(11);
		assertNotEquals(problem10, problem11, "Moved problem should be different reference + have different line number");
		assertEquals(11, problem11.line(), "Line 10 problem should have moved to line 11");
	}

	@Test
	void multipleOnLine() {
		ProblemTracking tracking = new ProblemTracking();
		tracking.addItem(new Problem(10, 1, 0, ERROR, LINT, "foo"));
		tracking.addItem(new Problem(10, 0, 0, ERROR, LINT, "fizz"));
		tracking.addItem(new Problem(10, 1, 0, ERROR, LINT, "buzz"));

		// 3 total
		assertEquals(3, tracking.getAllItems().size());
		assertEquals(3, tracking.getProblemsByLevel(ERROR).size());
		assertEquals(3, tracking.getProblemsByPhase(LINT).size());

		// 1 map entry since they all are on a single line
		assertEquals(1, tracking.getItems().size());

		// 3 on line 10
		List<Problem> problemsOnLine = tracking.getItemsOnLine(10);
		assertEquals(3, problemsOnLine.size());
	}

	/**
	 * Exists to retain visibility of protected methods for testing.
	 */
	private static class TestProblemTracking extends ProblemTracking {
		protected void onLinesInserted(int startLine, int endLine) {
			super.onLinesInserted(startLine, endLine);
		}

		protected void onLinesRemoved(int startLine, int endLine) {
			super.onLinesRemoved(startLine, endLine);
		}
	}
}