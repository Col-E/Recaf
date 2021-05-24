package me.coley.recaf.ui.control.code;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
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
public class SyntaxArea extends CodeArea implements ProblemUpdateListener {
	private static final Logger logger = Logging.get(SyntaxArea.class);
	private static final String FOLDED_STYLE = "folded";
	private final ExecutorService syntaxThreadService = Executors.newSingleThreadExecutor();
	private final ProblemTracking problemTracking;
	private final BracketSupport bracketSupport;
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
		bracketSupport = new BracketSupport(this);
		if (language.getRules().isEmpty()) {
			styler = null;
		} else {
			styler = new LanguageStyler(this, language);
		}
		setupParagraphFactory();
		setupSyntaxUpdating();
	}

	@Override
	public void dispose() {
		// TODO: This should be called when anything containing a syntax-area is closed
		//  - Most importantly for shutting down the thread service
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
		// The parameter line is 1-indexed, but the code-area internals are 0-indexed
		Platform.runLater(() -> {
			if (line > 1 && line <= getParagraphs().size())
				recreateParagraphGraphic(line - 1);
		});
	}

	@Override
	public void onProblemRemoved(int line, ProblemInfo info) {
		// The parameter line is 1-indexed, but the code-area internals are 0-indexed
		Platform.runLater(() -> {
			if (line > 1 && line <= getParagraphs().size())
				recreateParagraphGraphic(line - 1);
		});
	}

	@Override
	public void replace(int start, int end,
						StyledDocument<Collection<String>, String, Collection<String>> replacement) {
		// This gets called early in the event chain for rich-text updates.
		// So clearing the brackets here will work since the positions of the brackets are not yet changed.
		bracketSupport.clearBrackets();
		// Parent behavior
		super.replace(start, end, replacement);
	}

	/**
	 * Setup the display on the left side of the text area displaying line numbers and other visual indicators.
	 */
	private void setupParagraphFactory() {
		// The default factory has support for lines/folding in the same provider
		IntFunction<Node> lineNumbersAndFolding =
				get(this, this::formatLine, this::isStyleFolded, this::removeFoldStyle);
		if (problemTracking != null) {
			// Our problem indicators will be separately handled
			IntFunction<Node> problemIndicators = new ProblemIndicatorFactory();
			// And we will bundle them on the same line with horizontal boxes
			IntFunction<Node> decorationFactory = line -> {
				// TODO: I think folding only shows the "open dis back up"
				//  - need to make ANOTHER item to create the "hide dis piece"
				//  - Also dont know how flexible folding-in-folding is, may just do top-level folding
				HBox hbox = new HBox(lineNumbersAndFolding.apply(line), problemIndicators.apply(line));
				hbox.setAlignment(Pos.CENTER_LEFT);
				return hbox;
			};
			setParagraphGraphicFactory(decorationFactory);
		} else {
			setParagraphGraphicFactory(lineNumbersAndFolding);
		}
	}

	/**
	 * Setup text changes firing syntax style computations.
	 */
	private void setupSyntaxUpdating() {
		richChanges()
				.filter(ch -> !ch.isPlainTextIdentity())
				.filter(ch -> !ch.getInserted().equals(ch.getRemoved()))
				.subscribe(this::onTextChanged);
	}

	/**
	 * Called when the text has been changed.
	 * Queues up a syntax highlight runnable.
	 *
	 * @param change
	 * 		Text changed.
	 */
	protected void onTextChanged(RichTextChange<Collection<String>, String, Collection<String>> change) {
		try {
			// Some thoughts, you may ask why we do an "if" block for each, but not with else if.
			// Well, copy pasting text does both. So we remove then insert for replacement.
			//
			// TODO: There are some edge cases where the tracking of problem indicators will fail, and we just
			//  delete them. Deleting an empty line before a line with an error will void it.
			//  I'm not really sure how to make a clean fix for that, but because the rest of
			//  it works relatively well I'm not gonna touch it for now.
			String removedText = change.getRemoved().getText();
			boolean insert = change.getInserted().getText().contains("\n");
			boolean remove = removedText.contains("\n");
			if (remove) {
				// Get line number and add +1 to make it 1-indexed
				int start = lastContent.offsetToPosition(change.getPosition(), Bias.Backward).getMajor() + 1;
				// End line number needs +1 since it will include the next line due to inclusion of "\n"
				int end = lastContent.offsetToPosition(change.getRemovalEnd(), Bias.Backward).getMajor() + 1;
				onLinesRemoved(start, end, change);
			}
			if (insert) {
				// Get line number and add +1 to make it 1-indexed
				int start = offsetToPosition(change.getPosition(), Bias.Backward).getMajor() + 1;
				// End line number doesn't need +1 since it will include the next line due to inclusion of "\n"
				int end = offsetToPosition(change.getInsertionEnd(), Bias.Backward).getMajor();
				onLinesInserted(start, end, change);
			}
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
	 * @param change
	 * 		Text change value.
	 */
	private void onLinesInserted(int start, int end,
								 RichTextChange<Collection<String>, String, Collection<String>> change) {
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
	 * @param change
	 * 		Text change value.
	 */
	private void onLinesRemoved(int start, int end,
								RichTextChange<Collection<String>, String, Collection<String>> change) {
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
	private void syntax(RichTextChange<Collection<String>, String, Collection<String>> change) {
		PlainTextChange ptc = change.toPlainTextChange();
		// Change positions. If the change is removal of some text,
		// then the insert position will be equal to the start position.
		int insertEndPos = ptc.getInsertionEnd();
		int removeEndPos = ptc.getRemovalEnd();
		int start = ptc.getPosition();
		int end = Math.max(insertEndPos, removeEndPos);
		// Change type checks
		boolean insert = start == removeEndPos && start < insertEndPos;
		boolean removal = start < removeEndPos && start == insertEndPos;
		// Special case check for the initial text of the document.
		// Do a full style calculation for this case.
		if (start == 0 && insert && getText().equals(change.getInserted().getText())) {
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
	private Collection<String> removeFoldStyle(Collection<String> ps) {
		List<String> copy = new ArrayList<>(ps);
		copy.remove(FOLDED_STYLE);
		return copy;
	}

	/**
	 * @param ps
	 * 		Collection of paragraph styles.
	 *
	 * @return {@code true} when the paragraph styles indicate the paragraph should be folded.
	 */
	private boolean isStyleFolded(Collection<String> ps) {
		return ps.contains(FOLDED_STYLE);
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
	 * @return Language used for syntax highlighting.
	 */
	public Language getLanguage() {
		return language;
	}

	/**
	 * Decorator factory for building problem indicators.
	 */
	class ProblemIndicatorFactory implements IntFunction<Node> {
		private final double[] shape = new double[]{0, 0, 10, 5, 0, 10};

		@Override
		public Node apply(int lineNo) {
			// Lines are addressed as how they visually appear (based on 1 being the beginning)
			lineNo++;
			// Create the problem/error shape
			Polygon poly = new Polygon(shape);
			poly.getStyleClass().add("cursor-pointer");
			// Populate or hide it.
			if (problemTracking == null)
				throw new IllegalStateException("Should not have generated problem indicator," +
						" no problem tracking is associated with the control!");
			ProblemInfo problemInfo = problemTracking.getProblem(lineNo);
			if (problemInfo != null) {
				// Change color based on severity
				switch (problemInfo.getLevel()) {
					case ERROR:
						poly.setFill(Color.RED);
						break;
					case WARNING:
						poly.setFill(Color.YELLOW);
						break;
					case INFO:
					default:
						poly.setFill(Color.LIGHTBLUE);
						break;
				}
				// Populate behavior
				ProblemIndicatorInitializer indicatorInitializer = problemTracking.getIndicatorInitializer();
				if (indicatorInitializer != null) {
					indicatorInitializer.setupErrorIndicatorBehavior(lineNo, poly);
				}
			} else {
				poly.setVisible(false);
			}
			return poly;
		}
	}
}
