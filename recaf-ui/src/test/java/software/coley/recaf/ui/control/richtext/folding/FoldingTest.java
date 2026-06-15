package software.coley.recaf.ui.control.richtext.folding;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.coley.recaf.ui.BaseFxTest;
import software.coley.recaf.ui.control.richtext.Editor;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Tests for {@link Editor} paragraph folding.
 */
class FoldingTest extends BaseFxTest {
	private static final String TEXT = "line1\nline2\nline3\nline4\nline5\nline6\nline7\nline8";
	private static boolean fxAvailable;

	@BeforeAll
	static void startFx() {
		try {
			var started = new CountDownLatch(1);
			Platform.startup(started::countDown);
			fxAvailable = started.await(10, TimeUnit.SECONDS);
		} catch (IllegalStateException e) {
			fxAvailable = true; //already running
		} catch (Throwable ignored) {
			fxAvailable = false;
		}
	}

	@Test
	void testFoldHidesParagraphs() {
		onEditor(editor -> {
			editor.foldParagraphs(1, 4);

			assertFalse(editor.isParagraphFolded(0));
			assertFalse(editor.isParagraphFolded(1), "Fold header paragraph shouldn't be folded");
			assertTrue(editor.isParagraphFolded(2));
			assertTrue(editor.isParagraphFolded(3));
			assertTrue(editor.isParagraphFolded(4));
			assertFalse(editor.isParagraphFolded(5));
		});
	}

	@Test
	void testUnfoldRestoresParagraphs() {
		onEditor(editor -> {
			editor.foldParagraphs(1, 4);
			assertTrue(editor.isParagraphFolded(2));

			editor.unfoldParagraphs(1);
			for (var i = 0; i <= 5; i++)
				assertFalse(editor.isParagraphFolded(i));
		});
	}

	@Test
	void testFoldDoesNotAffectUndoHistory() {
		onEditor(editor -> {
			editor.getCodeArea().getUndoManager().forgetHistory();

			editor.foldParagraphs(1, 4);
			assertFalse(editor.getCodeArea().getUndoManager().isUndoAvailable(), "Fold operations should not be undoable");

			editor.unfoldParagraphs(1);
			assertFalse(editor.getCodeArea().getUndoManager().isUndoAvailable(), "Unfold operations should not be undoable");
		});
	}

	@Test
	void testFoldKeepsCaretInPlace() {
		onEditor(editor -> {
			editor.getCodeArea().moveTo(2);
			editor.foldParagraphs(1, 4);
			assertEquals(2, editor.getCodeArea().getCaretPosition(), "Caret outside the folded range should not move when folding");
		});
	}

	@Test
	void testSelectionIntoFoldedRangeExpandsFold() {
		onEditor(editor -> {
			editor.foldParagraphs(1, 4);
			assertTrue(editor.isParagraphFolded(2));

			var idx = TEXT.indexOf("line3"); //line3 is folded away
			editor.getCodeArea().selectRange(idx, idx + 5);

			assertFalse(editor.isParagraphFolded(2), "Selecting hidden text should expand the fold");
			assertEquals("line3", editor.getCodeArea().getSelectedText());
		});
	}

	@Test
	void testExpandFoldsHandlesNestedFolds() {
		onEditor(editor -> {
			// Inner fold, then outer fold around it
			editor.foldParagraphs(2, 3);
			editor.foldParagraphs(1, 5);
			assertTrue(editor.isParagraphFolded(3));

			editor.expandFoldsContaining(3);
			assertFalse(editor.isParagraphFolded(3), "Nested folds covering the paragraph should be expanded");
		});
	}

	@Test
	void testArrowNavigationSkipsFoldedParagraphs() {
		onEditor(editor -> {
			editor.foldParagraphs(1, 4);

			// Down from the header skips the fold to the next visible paragraph, keeping the column
			editor.getCodeArea().moveTo(1, 3);
			fireKey(editor, KeyCode.DOWN, false);
			assertEquals(5, editor.getCodeArea().getCurrentParagraph());
			assertEquals(3, editor.getCodeArea().getCaretColumn());

			// Ip from below lands back on the header
			fireKey(editor, KeyCode.UP, false);
			assertEquals(1, editor.getCodeArea().getCurrentParagraph());
			assertEquals(3, editor.getCodeArea().getCaretColumn());
		});
	}

	@Test
	void testArrowNavigationOverFoldKeepsTargetColumn() {
		// Down over a fold onto a shorter line lands at its end; continuing onto a longer line snaps back
		// to the remembered x-offset, just like regular vertical navigation.
		var text = "line1\nlong line number 2\nline3\nline4\nline5\nx\nline7 is much longer still\nline8";
		onEditor(text, editor -> {
			editor.foldParagraphs(1, 4);

			// Caret at the end of the long fold header.
			var headerLength = "long line number 2".length();
			editor.getCodeArea().moveTo(1, headerLength);
			fireKey(editor, KeyCode.DOWN, false);
			assertEquals(5, editor.getCodeArea().getCurrentParagraph());
			assertEquals(1, editor.getCodeArea().getCaretColumn(), "Navigating onto a shorter line should land at its end");

			// Plain navigation, the original x-offset should still apply
			fireKey(editor, KeyCode.DOWN, false);
			assertEquals(6, editor.getCodeArea().getCurrentParagraph());
			assertEquals(headerLength, editor.getCodeArea().getCaretColumn(), "Navigating onto a longer line should snap back to the remembered column");
		});
	}

	private void fireKey(Editor editor, KeyCode code, boolean shift) {
		editor.getCodeArea().fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code, shift, false, false, false));
	}

	private void onEditor(Consumer<Editor> action) {
		onEditor(TEXT, action);
	}

	private void onEditor(String text, Consumer<Editor> action) {
		assumeTrue(fxAvailable, "FX platform unavailable");

		var error = new AtomicReference<Throwable>();
		var done = new CountDownLatch(1);
		Platform.runLater(() -> {
			var stage = new Stage();
			try {
				var editor = new Editor();
				stage.setScene(new Scene(editor, 400, 300));
				stage.show();
				editor.setText(text);
				action.accept(editor);
			} catch (Throwable t) {
				error.set(t);
			} finally {
				stage.hide();
				done.countDown();
			}
		});

		try {
			assertTrue(done.await(30, TimeUnit.SECONDS), "FX task timed out");
		} catch (InterruptedException e) {
			throw new IllegalStateException(e);
		}
		if (error.get() != null) {
			if (error.get() instanceof RuntimeException re) throw re;
			if (error.get() instanceof Error err) throw err;
			throw new IllegalStateException(error.get());
		}
	}
}
