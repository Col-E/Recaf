package software.coley.recaf.ui.control.richtext;

import org.fxmisc.richtext.CodeArea;

/**
 * Code area with additional parameter checks for some common edge cases.
 *
 * @author Matt Coley
 */
public class SafeCodeArea extends CodeArea {
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
		if (anchor >= 0 && anchor <= getLength() && caretPosition >= 0 && caretPosition <= getLength())
			super.selectRange(anchor, caretPosition);
	}

	@Override
	public void selectRange(int anchorParagraph, int anchorColumn, int caretPositionParagraph, int caretPositionColumn) {
		if (anchorParagraph >= 0 && anchorParagraph < getParagraphs().size() &&
				caretPositionParagraph >= 0 && caretPositionParagraph < getParagraphs().size())
			super.selectRange(anchorParagraph, anchorColumn, caretPositionParagraph, caretPositionColumn);
	}
}
