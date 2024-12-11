package software.coley.recaf.ui.control.richtext.bracket;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.TwoDimensional;
import org.reactfx.Change;
import org.reactfx.EventStream;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.behavior.Closing;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.ui.control.richtext.EditorComponent;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.IntRange;
import software.coley.recaf.util.NumberUtil;
import software.coley.recaf.util.threading.ThreadPoolFactory;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

/**
 * Bracket tracking component for {@link Editor}.
 * Records matching bracket pairs the user is interacting with.
 *
 * @author Matt Coley
 * @see BracketMatchGraphicFactory Adds a line indicator to an {@link Editor} for lines covering the {@link #getRange() range}.
 * @see Editor#setSelectedBracketTracking(SelectedBracketTracking) Call to install into an {@link Editor}.
 */
public class SelectedBracketTracking implements EditorComponent, Closing, Consumer<Change<Integer>> {
	private static final Logger logger = Logging.get(SelectedBracketTracking.class);
	private final ExecutorService service = ThreadPoolFactory.newSingleThreadExecutor("brackets");
	private EventStream<Change<Integer>> lastEventStream;
	private CodeArea codeArea;
	private Editor editor;
	private IntRange range;

	@Override
	public void install(@Nonnull Editor editor) {
		this.editor = editor;
		codeArea = editor.getCodeArea();

		// Register event observer with short delay so rapid event fires only
		// consume the most recent event instance.
		lastEventStream = editor.getCaretPosEventStream()
				.successionEnds(Duration.ofMillis(Editor.SHORT_DELAY_MS));
		lastEventStream.addObserver(this);
	}

	@Override
	public void uninstall(@Nonnull Editor editor) {
		if (codeArea != null) {
			lastEventStream.removeObserver(this);
			lastEventStream = null;
			codeArea = null;
		}
	}

	@Override
	public void accept(Change<Integer> change) {
		// Submit task to check for open/close pair
		service.submit(() -> setRange(scanAround(change.getNewValue())));
	}

	/**
	 * @return Current range of bracket pair. {@code null} when no pair is selected currently.
	 */
	@Nullable
	public IntRange getRange() {
		return range;
	}

	/**
	 * Assigns the current selected bracket pair range.
	 * When the new value differs from the last value, the linked editor will redraw its paragraph graphics.
	 *
	 * @param newRange
	 * 		New range to assign. {@code null} to remove selected bracket pair range.
	 */
	private void setRange(@Nullable IntRange newRange) {
		IntRange lastRange = range;
		range = newRange;

		// Redraw paragraphs visible when the range is modified.
		if (!Objects.equals(lastRange, newRange))
			FxThreadUtil.run(() -> editor.redrawParagraphGraphics());
	}

	/**
	 * @param line
	 * 		Line number.
	 *
	 * @return {@code true} when the line belongs to the {@link #getRange() selected bracket pair range}.
	 */
	public boolean isSelectedLine(int line) {
		return isSelectedParagraph(line - 1);
	}

	/**
	 * @param paragraph
	 * 		Paragraph index, which is {@code line - 1}.
	 *
	 * @return {@code true} when the line belongs to the {@link #getRange() selected bracket pair range}.
	 */
	public boolean isSelectedParagraph(int paragraph) {
		if (range == null) return false;

		try {
			int length = editor.getTextLength();
			int start = NumberUtil.intClamp(range.start(), 0, length - 1);
			int end = NumberUtil.intClamp(range.end(), 0, length - 1);

			// Check paragraph beyond start range.
			TwoDimensional.Position startPos = codeArea.offsetToPosition(start, TwoDimensional.Bias.Backward);
			int startParagraph = startPos.getMajor();
			if (paragraph < startParagraph) return false;

			// Check paragraph before end range.
			TwoDimensional.Position endPos = codeArea.offsetToPosition(end, TwoDimensional.Bias.Forward);
			int endParagraph = endPos.getMajor();
			return paragraph <= endParagraph;
		} catch (Exception ex) {
			// Some potential edge cases in 'offsetToPosition' we'll want to look out for.
			logger.error("Failed to map selected bracket range to paragraph for line {}", paragraph + 1, ex);
			return false;
		}
	}

	protected IntRange scanAround(int pos) {
		int len = codeArea.getLength();

		// Check if character adjacent to the caret position is an open or closing bracket.
		IntRange found = null;
		if (pos > 0)
			found = scanAt(pos - 1);
		if (found == null && pos < len)
			found = scanAt(pos);
		return found;
	}

	@Nullable
	private IntRange scanAt(int pos) {
		String text = codeArea.getText();
		if (pos < 0 || pos >= text.length())
			return null;

		char c = text.charAt(pos);
		char openChar;
		char closeChar;
		boolean forward;
		if (c == '{' || c == '}') {
			openChar = '{';
			closeChar = '}';
			forward = c == '{';
		} else if (c == '[' || c == ']') {
			openChar = '[';
			closeChar = ']';
			forward = c == '[';
		} else if (c == '(' || c == ')') {
			openChar = '(';
			closeChar = ')';
			forward = c == '(';
		} else {
			// Not a supported bracket pair
			return null;
		}

		return forward ? scanForwards(text, pos, openChar, closeChar) :
				scanBackwards(text, pos, openChar, closeChar);
	}

	private IntRange scanForwards(String text, int pos, char openChar, char closeChar) {
		int start = pos;
		int end;
		int open;
		int close;
		int balance = 1;
		do {
			open = text.indexOf(openChar, pos + 1);
			close = text.indexOf(closeChar, pos + 1);

			if (open != -1 && open < close) {
				balance++;
				pos = open;
			} else if (close != -1 && close > pos) {
				balance--;
				pos = close;

				// Balance solved
				if (balance == 0) {
					end = pos;
					break;
				}
			} else {
				// Neither an open nor close character was found.
				// The balance is not resolved, no match.
				return null;
			}
		} while (true);

		// End found? Create range.
		return (end > 0) ? new IntRange(start, end) : null;
	}

	private IntRange scanBackwards(String text, int pos, char openChar, char closeChar) {
		int start = -1;
		int end = pos;
		int balance = 1;
		int open;
		int close;
		boolean remaining = true;
		do {
			open = text.lastIndexOf(openChar, pos - 1);
			close = text.lastIndexOf(closeChar, pos - 1);

			if (open > close)
				balance--;
			else if (close != -1 && close < pos)
				balance++;
			else
				return null;

			// Balance solved
			if (balance == 0) {
				start = open;
				break;
			}

			// Not solved, continue scanning by setting the cut-off for backwards scan
			// to the next closest item of the open/close character matches.
			int next = Math.max(open, close);

			// If the next position is the same, or beyond the current position
			// then that means we have exhausted all matches, and the balance is not solved.
			if (next >= pos)
				remaining = false;
			pos = next;
		} while (remaining);

		// Start found? Create range.
		return (start >= 0) ? new IntRange(start, end) : null;
	}

	@Override
	public void close() {
		if (!service.isShutdown())
			service.shutdownNow();
	}
}
