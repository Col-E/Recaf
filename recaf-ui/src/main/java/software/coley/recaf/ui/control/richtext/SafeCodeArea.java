package software.coley.recaf.ui.control.richtext;

import jakarta.annotation.Nonnull;
import org.fxmisc.richtext.CharacterHit;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.util.UndoUtils;
import org.reactfx.SuspendableYes;

/**
 * Code area with additional parameter checks for some common edge cases.
 *
 * @author Matt Coley
 */
public class SafeCodeArea extends CodeArea {
	private final SuspendableYes undoAvailability = new SuspendableYes();
	private boolean foldOperationInProgress;

	public SafeCodeArea() {
		// Suspendable undo manager to keep fold/unfold out of the history
		setUndoManager(UndoUtils.richTextSuspendableUndoManager(this, undoAvailability));
	}

	/**
	 * Runs an operation without recording any changes it makes in the undo history.
	 *
	 * @param operation
	 * 		Operation to run.
	 */
	public void runWithoutUndo(@Nonnull Runnable operation) {
		undoAvailability.suspendWhile(operation);
	}

	/**
	 * Collapses the given paragraph range without polluting the undo history, hiding {@code startParagraph + 1} to
	 * {@code endParagraph}.
	 *
	 * @param startParagraph
	 * 		Header paragraph of the fold, exclusive.
	 * @param endParagraph
	 * 		Last paragraph of the fold, inclusive.
	 */
	public void foldParagraphsSafely(int startParagraph, int endParagraph) {
		if (startParagraph < 0 || endParagraph >= getParagraphs().size() || endParagraph <= startParagraph)
			return;
		if (isFolded(startParagraph + 1))
			return;

		int caret = getCaretPosition();
		int anchor = getAnchor();
		int hiddenStart = getAbsolutePosition(startParagraph + 1, 0);
		int hiddenEnd = getAbsolutePosition(endParagraph, getParagraphLength(endParagraph));

		foldOperationInProgress = true;
		try {
			undoAvailability.suspendWhile(() -> foldParagraphs(startParagraph, endParagraph));

			// Restore caret/selection if it sat outside the hidden range.
			if ((caret < hiddenStart || caret > hiddenEnd) && (anchor < hiddenStart || anchor > hiddenEnd))
				selectRange(anchor, caret);
		} finally {
			foldOperationInProgress = false;
		}
	}

	/**
	 * Expands the folded block following the given paragraph without polluting the undo history.
	 *
	 * @param startParagraph
	 * 		Header paragraph of the fold.
	 */
	public void unfoldParagraphsSafely(int startParagraph) {
		if (startParagraph < 0 || startParagraph + 1 >= getParagraphs().size())
			return;
		if (!isFolded(startParagraph + 1))
			return;

		int caret = getCaretPosition();
		int anchor = getAnchor();

		boolean wasInProgress = foldOperationInProgress;
		foldOperationInProgress = true;
		try {
			undoAvailability.suspendWhile(() -> unfoldParagraphs(startParagraph));

			selectRange(anchor, caret);
		} finally {
			foldOperationInProgress = wasInProgress;
		}
	}

	@Override
	public void foldParagraphs(int startPar, int endPar) {
		boolean wasInProgress = foldOperationInProgress;
		foldOperationInProgress = true;
		try {
			super.foldParagraphs(startPar, endPar);
		} finally {
			foldOperationInProgress = wasInProgress;
		}
	}

	@Override
	public void unfoldParagraphs(int startingFromPar) {
		boolean wasInProgress = foldOperationInProgress;
		foldOperationInProgress = true;
		try {
			super.unfoldParagraphs(startingFromPar);
		} finally {
			foldOperationInProgress = wasInProgress;
		}
	}

	/**
	 * Expands any folds hiding the given paragraph.
	 *
	 * @param paragraph
	 * 		Paragraph index to make visible.
	 */
	public void expandFoldsContaining(int paragraph) {
		if (paragraph <= 0 || paragraph >= getParagraphs().size())
			return;

		int sanity = 128;
		while (isFolded(paragraph) && sanity-- > 0) {
			int start = paragraph;
			while (start > 0 && isFolded(start))
				start--;
			unfoldParagraphsSafely(start);
		}
	}

	@Override
	public Position offsetToPosition(int charOffset, Bias bias) {
		// Covering out-of-bounds offset cases, which otherwise crash in the base impl.
		if (charOffset <= 0)
			return position(0, 0);
		else if (charOffset >= getLength()) {
			int paragraph = getParagraphs().size() - 1;
			return position(paragraph, getParagraphLength(paragraph));
		}

		return super.offsetToPosition(charOffset, bias);
	}

	@Override
	public CharacterHit hit(double x, double y) {
		try {
			// Possible race condition of a virtual cell not being made 'present' yet
			// so the virtual-flow hit-test throws a NoSuchElementException.
			return super.hit(x, y);
		} catch (Throwable t) {
			// There's not a great way to recover, but 0 is always a safe value.
			return CharacterHit.insertionAt(0);
		}
	}

	@Override
	public void moveTo(int pos) {
		if (pos >= 0 && pos <= getLength())
			super.moveTo(pos);
	}

	@Override
	public void moveTo(int paragraphIndex, int columnIndex) {
		if (paragraphIndex >= 0 && paragraphIndex < getParagraphs().size())
			super.moveTo(paragraphIndex, columnIndex);
	}

	@Override
	public void moveTo(int pos, SelectionPolicy selectionPolicy) {
		if (pos >= 0 && pos <= getLength())
			super.moveTo(pos, selectionPolicy);
	}

	@Override
	public void moveTo(int paragraphIndex, int columnIndex, SelectionPolicy selectionPolicy) {
		if (paragraphIndex >= 0 && paragraphIndex < getParagraphs().size())
			super.moveTo(paragraphIndex, columnIndex, selectionPolicy);
	}

	@Override
	public void selectRange(int anchor, int caretPosition) {
		if (anchor >= 0 && anchor <= getLength() && caretPosition >= 0 && caretPosition <= getLength()) {
			// Expand folds over the selection so the caret isn't pushed out
			if (!foldOperationInProgress) {
				expandFoldsContaining(offsetToPosition(anchor, Bias.Forward).getMajor());
				expandFoldsContaining(offsetToPosition(caretPosition, Bias.Forward).getMajor());
			}
			super.selectRange(anchor, caretPosition);
		}
	}

	@Override
	public void selectRange(int anchorParagraph, int anchorColumn, int caretPositionParagraph, int caretPositionColumn) {
		if (anchorParagraph >= 0 && anchorParagraph < getParagraphs().size() &&
				caretPositionParagraph >= 0 && caretPositionParagraph < getParagraphs().size()) {
			if (!foldOperationInProgress) {
				expandFoldsContaining(anchorParagraph);
				expandFoldsContaining(caretPositionParagraph);
			}
			super.selectRange(anchorParagraph, anchorColumn, caretPositionParagraph, caretPositionColumn);
		}
	}
}
