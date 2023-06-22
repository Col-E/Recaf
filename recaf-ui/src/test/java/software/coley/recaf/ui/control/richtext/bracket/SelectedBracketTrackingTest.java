package software.coley.recaf.ui.control.richtext.bracket;

import org.fxmisc.richtext.CodeArea;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import software.coley.recaf.ui.BaseFxTest;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.util.IntRange;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link SelectedBracketTracking}.
 */
class SelectedBracketTrackingTest extends BaseFxTest {
	@Nested
	class Simple {
		@Test
		void testImmediateAdjacent() {
			SelectedBracketTracking tracking = setup(" {} ");
			IntRange target = new IntRange(1, 2);

			// Caret not adjacent to bracket
			assertNull(tracking.scanAround(0));

			// { to right
			assertEquals(target, tracking.scanAround(1));

			// { to right
			assertEquals(target, tracking.scanAround(2));

			// } to left
			assertEquals(target, tracking.scanAround(3));

			// Caret not adjacent to bracket
			assertNull(tracking.scanAround(4));
		}

		@Test
		void testWithContent() {
			SelectedBracketTracking tracking = setup(" { ... } ");
			IntRange target = new IntRange(1, 7);

			// Caret not adjacent to bracket
			assertNull(tracking.scanAround(0));

			// { to right
			assertEquals(target, tracking.scanAround(1));

			// { to left
			assertEquals(target, tracking.scanAround(2));

			// Caret not adjacent to bracket
			assertNull(tracking.scanAround(3));
			assertNull(tracking.scanAround(4));
			assertNull(tracking.scanAround(5));
			assertNull(tracking.scanAround(6));

			// } to right
			assertEquals(target, tracking.scanAround(7));

			// } to left
			assertEquals(target, tracking.scanAround(8));

			// Caret not adjacent to bracket
			assertNull(tracking.scanAround(9));
		}

		@Test
		void testAtEdges() {
			SelectedBracketTracking tracking = setup("{...}");
			IntRange target = new IntRange(0, 4);

			// { to right
			assertEquals(target, tracking.scanAround(0));

			// { to left
			assertEquals(target, tracking.scanAround(1));

			// Caret not adjacent to bracket
			assertNull(tracking.scanAround(2));
			assertNull(tracking.scanAround(3));

			// { to right
			assertEquals(target, tracking.scanAround(4));

			// } to left
			assertEquals(target, tracking.scanAround(5));
		}
	}

	@Nested
	class Balance {
		@Test
		void testTwoLevels() {
			String text = """
					{
						{
						}
					}
					""";
			SelectedBracketTracking tracking = setup(text);
			IntRange outer = new IntRange(0, 8);
			IntRange inner = new IntRange(3, 6);

			// First open bracket
			IntRange fromOuterStart0 = tracking.scanAround(0);
			assertEquals(outer, fromOuterStart0);
			IntRange fromOuterStart1 = tracking.scanAround(1);
			assertEquals(outer, fromOuterStart1);

			// No adjacent brackets
			assertNull(tracking.scanAround(2));

			// Last open bracket
			IntRange fromInnerStart3 = tracking.scanAround(3);
			assertEquals(inner, fromInnerStart3);
			IntRange fromInnerStart4 = tracking.scanAround(4);
			assertEquals(inner, fromInnerStart4);

			// No adjacent brackets
			assertNull(tracking.scanAround(5));

			// First close bracket
			IntRange fromInnerEnd6 = tracking.scanAround(6);
			assertEquals(inner, fromInnerEnd6);
			IntRange fromInnerEnd7 = tracking.scanAround(7);
			assertEquals(inner, fromInnerEnd7);

			// Last close bracket (there is no unmatched region here, as there is no '\t' unlike before)
			IntRange fromOuterEnd8 = tracking.scanAround(8);
			assertEquals(outer, fromOuterEnd8);
			IntRange fromOuterEnd9 = tracking.scanAround(9);
			assertEquals(outer, fromOuterEnd9);
		}
	}

	private SelectedBracketTracking setup(String text) {
		// Setup UI mocks
		CodeArea codeArea = mock(CodeArea.class, Answers.RETURNS_MOCKS);
		Editor editor = mock(Editor.class, Answers.RETURNS_MOCKS);
		when(editor.getCodeArea()).thenReturn(codeArea);
		when(codeArea.getText()).thenReturn(text);
		when(codeArea.getLength()).thenReturn(text.length());

		// Create bracket impl
		SelectedBracketTracking tracking = new SelectedBracketTracking();
		tracking.install(editor);
		return tracking;
	}
}