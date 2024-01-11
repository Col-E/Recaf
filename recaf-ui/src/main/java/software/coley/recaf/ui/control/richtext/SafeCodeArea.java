package software.coley.recaf.ui.control.richtext;

import org.fxmisc.richtext.CodeArea;

/**
 * Code area with additional parameter checks for some common edge cases.
 *
 * @author Matt Coley
 */
public class SafeCodeArea extends CodeArea {
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
}
