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

// TODO: document
public class SyntaxArea extends CodeArea implements ProblemUpdateListener {
	private static final Logger logger = Logging.get(SyntaxArea.class);
	private static final String FOLDED_STYLE = "folded";
	private final ExecutorService syntaxThreadService = Executors.newSingleThreadExecutor();
	private final ProblemTracking problemTracking;
	private final BracketSupport bracketSupport;
	private ReadOnlyStyledDocument<?, ?, ?> lastContent;

	public SyntaxArea(ProblemTracking problemTracking) {
		this.problemTracking = problemTracking;
		if (problemTracking != null) {
			problemTracking.addProblemListener(this);
		}
		bracketSupport = new BracketSupport(this);
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
		if (line > 0 && line < getParagraphs().size())
			Platform.runLater(() -> recreateParagraphGraphic(line - 1));
	}

	@Override
	public void onProblemRemoved(int line, ProblemInfo info) {
		// The parameter line is 1-indexed, but the code-area internals are 0-indexed
		if (line > 0 && line < getParagraphs().size())
			Platform.runLater(() -> recreateParagraphGraphic(line - 1));
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
	 * @param start First line containing inserted text.
	 * @param end Last line containing inserted text.
	 * @param change Text change value.
	 */
	private void onLinesInserted(int start, int end,
								 RichTextChange<Collection<String>, String, Collection<String>> change) {
		// Update problem indicators
		if (problemTracking != null) {
			problemTracking.linesInserted(start, end);
		}
	}

	/**
	 * @param start First line previously containing the removed text.
	 * @param end Last line previously containing the removed text.
	 * @param change Text change value.
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
		int changePos = ptc.getPosition();
		int insertPos = ptc.getInsertionEnd();
		int removePos = ptc.getRemovalEnd();
		// TODO: Implement this
		if (changePos == 0 && changePos == removePos && changePos < insertPos
				&& getText().equals(change.getInserted().getText())) {
			// Initial state
			logger.debug("Initial");
		} else if (changePos == removePos && changePos < insertPos) {
			// Insertion
			logger.debug("Insert");
		} else if (changePos < removePos && changePos == insertPos) {
			// Removal
			logger.debug("Remove");
		} else {
			// Replacement?
			logger.debug("Replace");
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
