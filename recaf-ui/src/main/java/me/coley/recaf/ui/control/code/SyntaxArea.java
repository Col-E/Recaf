package me.coley.recaf.ui.control.code;

import com.carrotsearch.hppc.IntHashSet;
import com.google.common.base.Strings;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import me.coley.recaf.ui.behavior.Cleanable;
import me.coley.recaf.util.Threads;
import me.coley.recaf.util.logging.Logging;
import org.fxmisc.richtext.CaretSelectionBind;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.PlainTextChange;
import org.fxmisc.richtext.model.ReadOnlyStyledDocument;
import org.fxmisc.richtext.model.RichTextChange;
import org.fxmisc.richtext.model.StyledDocument;
import org.slf4j.Logger;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.IntFunction;

import static org.fxmisc.richtext.LineNumberFactory.get;

/**
 * Base editor for a syntax-highlighted language. Comes with optional support for problem indicators per-line.
 *
 * @author Matt Coley
 */
public class SyntaxArea extends CodeArea implements BracketUpdateListener, ProblemUpdateListener, Cleanable {
	private static final Logger logger = Logging.get(SyntaxArea.class);
	private static final String BRACKET_FOLD_STYLE = "collapse";
	private final IntHashSet paragraphGraphicReady = new IntHashSet(200);
	private final ExecutorService bracketThreadService = Executors.newSingleThreadExecutor();
	private final ExecutorService syntaxThreadService = Executors.newSingleThreadExecutor();
	private final ProblemTracking problemTracking;
	private final BracketTracking bracketTracking;
	private final Language language;
	private final LanguageStyler styler;
	private ReadOnlyStyledDocument<?, ?, ?> lastContent;

	/**
	 * @param language
	 * 		Language to use for syntax highlighting.
	 * @param problemTracking
	 * 		Optional problem tracking implementation to enable line problem indicators.
	 */
	public SyntaxArea(Language language, ProblemTracking problemTracking) {
		// Ensure language is not null
		language = language == null ? Languages.NONE : language;
		this.language = language;
		this.problemTracking = problemTracking;
		if (problemTracking != null) {
			problemTracking.addProblemListener(this);
		}
		bracketTracking = new BracketTracking(this);
		bracketTracking.addBracketListener(this);
		if (language.getRules().isEmpty()) {
			styler = null;
		} else {
			styler = new LanguageStyler(this, language);
		}
		setupParagraphFactory();
		setupSyntaxUpdating();
	}

	@Override
	public void cleanup() {
		dispose();
	}

	@Override
	public void dispose() {
		super.dispose();
		// Remove as listener
		if (problemTracking != null) {
			problemTracking.removeProblemListener(this);
		}
		// Clear out any remaining threads
		if (!syntaxThreadService.isShutdown()) {
			syntaxThreadService.shutdownNow();
		}
	}

	@Override
	public void onProblemAdded(int line, ProblemInfo info) {
		regenerateLineGraphic(line);
	}

	@Override
	public void onProblemRemoved(int line, ProblemInfo info) {
		regenerateLineGraphic(line);
	}

	@Override
	public void onBracketAdded(int line, BracketPair pair) {
		regenerateLineGraphic(line);
	}

	@Override
	public void onBracketRemoved(int line, BracketPair pair) {
		// The parameter line is 1-indexed, but the code-area internals are 0-indexed
		regenerateLineGraphic(line);
	}

	@Override
	public void replace(int start, int end,
						StyledDocument<Collection<String>, String, Collection<String>> replacement) {
		// This gets called early in the event chain for rich-text updates.
		// So clearing the brackets here will work since the positions of the brackets are not yet changed.
		bracketTracking.clearSelectedBrackets();
		// Parent behavior
		super.replace(start, end, replacement);
	}

	/**
	 * Setup the display on the left side of the text area displaying line numbers and other visual indicators.
	 */
	private void setupParagraphFactory() {
		IntFunction<Node> lineNumbers = get(this, this::formatLine, line -> false, this::removeFoldStyle);
		IntFunction<Node> problemIndicators = problemTracking == null ? null : new ProblemIndicatorFactory(this);
		IntFunction<Node> bracketFoldIndicators = new BracketFoldIndicatorFactory(this);
		// Combine
		IntFunction<Node> decorationFactory = paragraph -> {
			// Do not create line decoration nodes for folded lines.
			// It messes up rendering and is a waste of resources.
			if (!isParagraphFolded(paragraph)) {
				HBox hbox = new HBox();
				Node lineNo = lineNumbers.apply(paragraph);
				Node bracketFold = bracketFoldIndicators.apply(paragraph);
				hbox.getChildren().add(lineNo);
				if (bracketFold != null) {
					if (lineNo instanceof Label) {
						((Label) lineNo).setGraphic(bracketFold);
					} else {
						hbox.getChildren().add(bracketFold);
					}

				}
				if (problemIndicators != null) {
					hbox.getChildren().add(problemIndicators.apply(paragraph));
				}
				hbox.setAlignment(Pos.CENTER_LEFT);
				paragraphGraphicReady.add(paragraph);
				return hbox;
			}
			paragraphGraphicReady.remove(paragraph);
			return null;
		};
		setParagraphGraphicFactory(decorationFactory);
	}

	/**
	 * Setup text changes firing syntax style computations.
	 */
	private void setupSyntaxUpdating() {
		richChanges()
				.filter(ch -> !ch.isPlainTextIdentity())
				.filter(ch -> !ch.getInserted().equals(ch.getRemoved()))
				.map(RichTextChange::toPlainTextChange)
				.subscribe(this::onTextChanged);
	}

	/**
	 * Called when the text has been changed.
	 * Queues up a syntax highlight runnable.
	 *
	 * @param change
	 * 		Text changed.
	 */
	protected void onTextChanged(PlainTextChange change) {
		try {
			// TODO: There are some edge cases where the tracking of problem indicators will fail, and we just
			//  delete them. Deleting an empty line before a line with an error will void it.
			//  I'm not really sure how to make a clean fix for that, but because the rest of
			//  it works relatively well I'm not gonna touch it for now.
			String insertedText = change.getInserted();
			String removedText = change.getRemoved();
			boolean lineInserted = insertedText.contains("\n");
			boolean lineRemoved = removedText.contains("\n");
			// Handle line removal/insertion.
			//
			// Some thoughts, you may ask why we do an "if" block for each, but not with else if.
			// Well, copy pasting text does both. So we remove then insert for replacement.
			if (lineRemoved) {
				// Get line number and add +1 to make it 1-indexed
				int start = lastContent.offsetToPosition(change.getPosition(), Bias.Backward).getMajor() + 1;
				// End line number needs +1 since it will include the next line due to inclusion of "\n"
				int end = lastContent.offsetToPosition(change.getRemovalEnd(), Bias.Backward).getMajor() + 1;
				onLinesRemoved(start, end);
			}
			if (lineInserted) {
				// Get line number and add +1 to make it 1-indexed
				int start = offsetToPosition(change.getPosition(), Bias.Backward).getMajor() + 1;
				// End line number doesn't need +1 since it will include the next line due to inclusion of "\n"
				int end = offsetToPosition(change.getInsertionEnd(), Bias.Backward).getMajor();
				onLinesInserted(start, end);
			}
			// Handle any removal/insertion for bracket completion
			bracketThreadService.execute(() -> {
				if (!Strings.isNullOrEmpty(removedText))
					bracketTracking.textRemoved(change);
				if (!Strings.isNullOrEmpty(insertedText))
					bracketTracking.textInserted(change);
			});
			// The way service is set up, tasks should just sorta queue up one by one.
			// Each will wait for the prior to finish.
			syntaxThreadService.execute(() -> syntax(change));
		} catch (RejectedExecutionException ex) {
			// ignore
		}
		lastContent = getContent().snapshot();
	}

	/**
	 * @param start
	 * 		First line containing inserted text.
	 * @param end
	 * 		Last line containing inserted text.
	 */
	protected void onLinesInserted(int start, int end) {
		// Update problem indicators
		if (problemTracking != null) {
			problemTracking.linesInserted(start, end);
		}
	}

	/**
	 * @param start
	 * 		First line previously containing the removed text.
	 * @param end
	 * 		Last line previously containing the removed text.
	 */
	protected void onLinesRemoved(int start, int end) {
		// Remove tracked state for paragraph graphics
		int range = end - start;
		int max = getParagraphs().size() - 1;
		for (int i = max; i > max - range; i--) {
			paragraphGraphicReady.remove(i);
		}
		// Update problem indicators
		if (problemTracking != null) {
			problemTracking.linesRemoved(start, end);
		}
	}

	/**
	 * Update the syntax of the document.
	 *
	 * @param change
	 * 		The changes applied to the document.
	 */
	private void syntax(PlainTextChange change) {
		// Change positions. If the change is removal of some text,
		// then the insert position will be equal to the start position.
		int insertEndPos = change.getInsertionEnd();
		int removeEndPos = change.getRemovalEnd();
		int start = change.getPosition();
		int end = Math.max(insertEndPos, removeEndPos);
		// Change type checks
		boolean insert = start == removeEndPos && start < insertEndPos;
		boolean removal = start < removeEndPos && start == insertEndPos;
		// Special case check for the initial text of the document.
		// Do a full style calculation for this case.
		if (start == 0 && insert && getText().equals(change.getInserted())) {
			// Initial state
			logger.trace("Style update for initial document text");
			// Update style
			if (styler != null) {
				styler.styleCompleteDocument();
			}
		} else {
			// Allow for insertion and removal style recalculations
			if (insert) {
				logger.trace("Style update for inserted text, range=[{}, {}]", start, end);
			} else if (removal) {
				logger.trace("Style update for removed text, range=[{}, {}]", start, end);
			} else {
				logger.trace("Style update for replaced text, range=[{}, {}]", start, end);
			}
			// Update style
			if (styler != null) {
				styler.styleRange(start, end);
			}
		}
	}

	/**
	 * @param ps
	 * 		Collection of paragraph styles.
	 *
	 * @return Collection without the fold style.
	 */
	public Collection<String> removeFoldStyle(Collection<String> ps) {
		List<String> copy = new ArrayList<>(ps);
		copy.remove(BRACKET_FOLD_STYLE);
		return copy;
	}

	/**
	 * @param paragraph
	 * 		Paragraph index, 0-based.
	 *
	 * @return {@code true} when the paragraph is currently folded.
	 */
	public boolean isParagraphFolded(int paragraph) {
		// Skip out of bounds paragraphs
		if (paragraph < 0 || paragraph >= getParagraphs().size())
			return false;
		return isStyleFolded(getParagraph(paragraph).getParagraphStyle());
	}

	/**
	 * @param ps
	 * 		Collection of paragraph styles.
	 *
	 * @return {@code true} when the paragraph styles indicate the paragraph should be folded.
	 */
	private boolean isStyleFolded(Collection<String> ps) {
		return ps.contains(BRACKET_FOLD_STYLE);
	}

	/**
	 * Request the graphic for the given line be regenerated.
	 * Effectively a proxy call to {@link #recreateParagraphGraphic(int)} with some handling.
	 *
	 * @param line
	 * 		Line to update.
	 */
	private void regenerateLineGraphic(int line) {
		Threads.runFx(() -> {
			if (line <= getParagraphs().size()) {
				// The parameter line is 1-indexed, but the code-area internals are 0-indexed
				int paragraph = line - 1;
				// Don't recreate an item that hasn't been initialized yet.
				// There is wonky logic about "duplicate children" that this prevents in a multi-threaded context.
				if (paragraphGraphicReady.contains(paragraph))
					recreateParagraphGraphic(paragraph);
			}
		});
	}

	/**
	 * @param digits
	 * 		Number of digits in the line.
	 *
	 * @return Magical format string that puts out the line number.
	 */
	private String formatLine(int digits) {
		return "%1$" + digits + "s";
	}

	@Override
	public void selectWord() {
		// Pasted from parent implementation with minor adjustments
		if (getLength() == 0) return;

		CaretSelectionBind<?, ?, ?> csb = getCaretSelectionBind();
		int paragraph = csb.getParagraphIndex();
		int position = csb.getColumnPosition();

		String paragraphText = getText(paragraph);
		BreakIterator breakIterator = BreakIterator.getWordInstance(getLocale());
		// Replace the text so that it doesn't consider non-word characters.
		// This is faster than re-implementing the existing 'BreakIterator' logic
		breakIterator.setText(paragraphText.replace('.', ' '));

		breakIterator.preceding(position);
		int start = breakIterator.current();

		while (start > 0 && paragraphText.charAt(start - 1) == '_' && paragraphText.charAt(start) != '.') {
			if (--start > 0 && !breakIterator.isBoundary(start - 1)) {
				breakIterator.preceding(start);
				start = breakIterator.current();
			}
		}

		breakIterator.following(position);
		int end = breakIterator.current();
		int len = paragraphText.length();
		while (end < len && paragraphText.charAt(end) == '_' && paragraphText.charAt(end) != '.') {
			if (++end < len && !breakIterator.isBoundary(end + 1)) {
				breakIterator.following(end);
				end = breakIterator.current();
			}
			// For some reason single digits aren't picked up so ....
			else if (Character.isDigit(paragraphText.charAt(end))) {
				end++;
			}
		}
		csb.selectRange(paragraph, start, paragraph, end);
	}

	/**
	 * Select the position of an AST element and {@link #centerParagraph(int) center it on the screen}.
	 *
	 * @param line
	 * 		Line to select.
	 * @param column
	 * 		Column to select.
	 */
	public void selectPosition(int line, int column) {
		Threads.runFx(() -> {
			int currentLine = getCurrentParagraph();
			int targetLine = line - 1;
			moveTo(targetLine, column);
			requestFocus();
			selectWord();
			if (currentLine != targetLine) {
				centerParagraph(targetLine);
			}
		});
	}

	/**
	 * @param paragraph
	 * 		Paragraph to center in the viewport.
	 */
	public void centerParagraph(int paragraph) {
		// Normally a full bounds will show the paragraph at the top of the viewport.
		// If we offset the position by half the height upwards, it centers it.
		Bounds bounds = new BoundingBox(0, -getHeight() / 2, getWidth(), getHeight());
		showParagraphRegion(paragraph, bounds);
	}

	/**
	 * @return Bracket matching handling.
	 */
	public BracketTracking getBracketTracking() {
		return bracketTracking;
	}

	/**
	 * @return Optional problem tracking implementation which enables line problem indicators.
	 * May be {@code null}.
	 */
	public ProblemTracking getProblemTracking() {
		return problemTracking;
	}

	/**
	 * @return Language used for syntax highlighting.
	 */
	public Language getLanguage() {
		return language;
	}
}
