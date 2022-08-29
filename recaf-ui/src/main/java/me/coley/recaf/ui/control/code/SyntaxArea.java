package me.coley.recaf.ui.control.code;

import com.carrotsearch.hppc.IntHashSet;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.IntegerProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import me.coley.recaf.config.Configs;
import me.coley.recaf.ui.behavior.*;
import me.coley.recaf.ui.util.SearchHelper;
import me.coley.recaf.util.ReflectUtil;
import me.coley.recaf.util.StringUtil;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.util.threading.FxThreadUtil;
import me.coley.recaf.util.threading.ThreadUtil;
import org.fxmisc.flowless.VirtualFlow;
import org.fxmisc.richtext.CaretSelectionBind;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.GenericStyledArea;
import org.fxmisc.richtext.model.*;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

import static org.fxmisc.richtext.LineNumberFactory.get;

/**
 * Base editor for a syntax-highlighted language. Comes with optional support for problem indicators per-line.
 *
 * @author Matt Coley
 */
public class SyntaxArea extends CodeArea implements BracketUpdateListener, ProblemUpdateListener,
		InteractiveText, Styleable, Searchable, Cleanable, Scrollable, FontSizeChangeable {
	private static final Logger logger = Logging.get(SyntaxArea.class);
	private static final String BRACKET_FOLD_STYLE = "collapse";
	private final List<StringBinding> styles = new ArrayList<>();
	private final IntHashSet paragraphGraphicReady = new IntHashSet(200);
	private final IndicatorFactory indicatorFactory = new IndicatorFactory(this);
	private final SearchHelper searchHelper = new SearchHelper(this::newSearchResult);
	private final ProblemTracking problemTracking;
	private final BracketTracking bracketTracking;
	private final Language language;
	private final LanguageStyler styler;
	private final VirtualFlow<?, ?> internalVirtualFlow;
	private ReadOnlyStyledDocument<?, ?, ?> lastContent;
	private Future<?> bracketUpdate;
	private Future<?> syntaxUpdate;

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
		this.internalVirtualFlow = getInternalVirtualFlow();
		if (problemTracking != null) {
			problemTracking.addProblemListener(this);
		}
		if (Configs.editor().showBracketFolds) {
			bracketTracking = new BracketTracking(this);
			bracketTracking.addBracketListener(this);
		} else {
			bracketTracking = new BracketTracking.NoOp(this);
		}
		if (language.getRules().isEmpty()) {
			styler = new LanguageStyler(Languages.NONE, this);
		} else {
			styler = new LanguageStyler(language, this);
		}
		setupParagraphFactory();
		setupSyntaxUpdating();
		addEventFilter(KeyEvent.KEY_PRESSED, this::handleTabForIndent);
	}

	@Override
	public void cleanup() {
		dispose();
	}

	@Override
	public void dispose() {
		// RichTextFX disposal needs to be on the FX thread
		FxThreadUtil.run(super::dispose);
		// Remove as listener
		if (problemTracking != null) {
			problemTracking.removeProblemListener(this);
		}
		// Cleanup threading
		if (syntaxUpdate != null) {
			syntaxUpdate.cancel(true);
		}
		if (bracketUpdate != null) {
			bracketUpdate.cancel(true);
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

	@Override
	public String getFullText() {
		return getText();
	}

	@Override
	public String getSelectionText() {
		return getSelectedText();
	}

	@Override
	public int getSelectionStart() {
		if (StringUtil.isNullOrEmpty(getSelectionText()))
			return -1;
		return getSelection().getStart();
	}

	@Override
	public int getSelectionStop() {
		if (StringUtil.isNullOrEmpty(getSelectionText()))
			return -1;
		return getSelection().getEnd();
	}

	@Override
	public SearchResults next(EnumSet<SearchModifier> modifiers, String search) {
		searchHelper.setText(getText());
		return searchHelper.next(modifiers, search);
	}

	@Override
	public SearchResults previous(EnumSet<SearchModifier> modifiers, String search) {
		searchHelper.setText(getText());
		return searchHelper.previous(modifiers, search);
	}

	private SearchResult newSearchResult(Integer start, Integer stop) {
		int startFiltered = Math.max(start, 0);
		int stopFiltered = Math.min(stop, getLength());
		return new SearchResult() {
			@Override
			public int getStart() {
				return startFiltered;
			}

			@Override
			public int getStop() {
				return stopFiltered;
			}

			@Override
			public void select() {
				int paragraph = offsetToPosition(getStart(), Bias.Backward).getMajor();
				selectRange(getStart(), getStop());
				showParagraphAtCenter(paragraph);
			}
		};
	}

	/**
	 * Setup the display on the left side of the text area displaying line numbers and other visual indicators.
	 */
	protected void setupParagraphFactory() {
		IntFunction<Node> lineNumbers = get(this, this::formatLine, line -> false, this::removeFoldStyle);
		IntFunction<Node> bracketFoldIndicators = new BracketFoldIndicatorFactory(this);
		if (problemTracking != null) {
			indicatorFactory.addIndicatorApplier(new ProblemIndicatorApplier(this));
		}
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
				hbox.getChildren().add(indicatorFactory.createGraphic(paragraph));
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
	 * @param text
	 * 		Text to set.
	 */
	public void setText(String text) {
		setText(text, true);
	}

	/**
	 * @param text
	 * 		Text to set.
	 * @param keepPosition
	 * 		Attempt to re-orient caret/scroll positions to where they were.
	 */
	public void setText(String text, boolean keepPosition) {
		if (text == null) {
			clear();
			return;
		} else if (getText().equals(text)) {
			// Text is the same, no need to trigger changes
			return;
		}
		boolean isInitialSet = lastContent == null;
		if (keepPosition) {
			ScrollSnapshot snapshot = makeScrollSnapshot();
			// Update the text
			replaceText(text);
			// Restore scroll and caret position
			snapshot.restore();
		} else {
			int pos = getCaretPosition();
			replaceText(text);
			// Common case for initial text placement.
			// Using 'replaceText' pushes the caret to the end of the document, which we do not want to do.
			if (pos == 0) {
				moveTo(pos);
				requestFollowCaret();
			}
		}
		// Wipe history if initial set call
		if (isInitialSet) {
			getUndoManager().forgetHistory();
		}
	}

	/**
	 * Called when the text has been changed.
	 * Queues up a syntax highlight runnable.
	 *
	 * @param change
	 * 		Text changed.
	 */
	protected void onTextChanged(PlainTextChange change) {
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

		// Cancel any previous updates
		if (bracketUpdate != null) {
			bracketUpdate.cancel(true);
			bracketUpdate = null;
		}
		if (syntaxUpdate != null) {
			syntaxUpdate.cancel(true);
			syntaxUpdate = null;
		}

		// Handle any removal/insertion for bracket completion
		boolean hasRemovedText = !StringUtil.isNullOrEmpty(removedText);
		boolean hasInsertedText = !StringUtil.isNullOrEmpty(insertedText);
		if (hasRemovedText || hasInsertedText) {
			bracketUpdate = ThreadUtil.run(() -> {
				if (hasRemovedText)
					bracketTracking.textRemoved(change);
				if (Thread.interrupted())
					return;
				if (hasInsertedText)
					bracketTracking.textInserted(change);
			});
		}
		syntaxUpdate = ThreadUtil.run(() -> syntax(change));
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
			// Update initial style
			if (styler != null) {
				styler.styleCompleteDocument(getText());
			}
		} else {
			// Allow for insertion and removal style recalculations limited in scope to the affected text.
			if (styler != null) {
				styler.styleFlexibleRange(this, getText(), start, end);
			}
		}
		onPostStyle(change);
	}

	/**
	 * Called after {@link #syntax(PlainTextChange)}.
	 * Allows adding additional styles on top of the defaults.
	 *
	 * @param change
	 * 		Text change applied.
	 */
	protected void onPostStyle(PlainTextChange change) {
		// no-op by default
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
	 * @param line
	 * 		Paragraph index, 0-based.
	 *
	 * @return {@code true} when the paragraph is visible.
	 */
	public boolean isParagraphVisible(int line) {
		if (isParagraphFolded(line))
			return false;
		// We use the internal virtual flow because the provided methods call 'layout()' unnecessarily
		//  - firstVisibleParToAllParIndex()
		//  - lastVisibleParToAllParIndex()
		// It is very likely by the time of calling this that our text is already populated and laid out.
		// This gets called rather frequently so the constant layout requests contribute a massive waste of time.
		// If we use these methods from the internal 'VirtualFlow' we skip all that and the result is almost instant.
		return line >= internalVirtualFlow.getFirstVisibleIndex() && line <= internalVirtualFlow.getLastVisibleIndex();
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
	public void regenerateLineGraphic(int line) {
		int paragraph = line - 1;
		FxThreadUtil.run(() -> {
			if (isParagraphVisible(paragraph)) {
				internalRegenLineGraphic(line);
			}
		});
	}

	/**
	 * Request the graphic for the given lines be regenerated.
	 * Effectively a proxy call to {@link #recreateParagraphGraphic(int)} with some handling.
	 *
	 * @param lines
	 * 		Lines to update.
	 * 		Do note these are <i>lines</i> and not the internal 0-indexed <i>paragraphs</i>.
	 */
	public void regenerateLineGraphics(Collection<Integer> lines) {
		FxThreadUtil.run(() -> {
			Collection<Integer> filtered = lines.stream().filter(line -> {
				int paragraph = line - 1;
				return isParagraphVisible(paragraph);
			}).collect(Collectors.toList());
			for (int line : filtered)
				internalRegenLineGraphic(line);
		});
	}

	private void internalRegenLineGraphic(int line) {
		// The parameter line is 1-indexed, but the code-area internals are 0-indexed
		int paragraph = line - 1;
		// Don't recreate an item that hasn't been initialized yet.
		// There is wonky logic about "duplicate children" that this prevents in a multi-threaded context.
		if (paragraphGraphicReady.contains(paragraph))
			// It is assumed 'paragraph' has already been validated by a calling method
			recreateParagraphGraphic(paragraph);
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
	 * Select the text at the given offset.
	 *
	 * @param offset
	 * 		Raw offset in the document to select
	 */
	public void selectPosition(int offset) {
		selectPosition(1, offset);
	}

	/**
	 * Select the text and {@link #showParagraphAtCenter(int) center it on the screen}.
	 *
	 * @param line
	 * 		Line to select.
	 * @param column
	 * 		Column to select.
	 */
	public void selectPosition(int line, int column) {
		FxThreadUtil.run(() -> {
			int currentLine = getCurrentParagraph();
			int targetLine = line - 1;
			moveTo(targetLine, column);
			requestFocus();
			selectWord();
			if (currentLine != targetLine) {
				showParagraphAtCenter(targetLine);
			}
		});
	}

	/**
	 * @return Caret's line number <i>(First line being 1)</i>.
	 */
	public int getCaretLine() {
		return offsetToPosition(getCaretPosition(), Bias.Forward).getMajor() + 1;
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

	/**
	 * @return Indicator factory.
	 */
	protected IndicatorFactory getIndicatorFactory() {
		return indicatorFactory;
	}

	@Override
	public ScrollSnapshot makeScrollSnapshot() {
		// Record which paragraph was in the middle of the screen
		int middleScreenParagraph = -1;
		if (lastContent != null) {
			int vis = getVisibleParagraphs().size();
			if (vis > 0) {
				// Get middle index (minor decimal used to prevent ties)
				int midVis = Math.round(vis / 2.01F);
				// Find that paragraph among all to get the raw index
				middleScreenParagraph = getParagraphs().indexOf(getVisibleParagraphs().get(midVis));
			}
		}
		// Record prior caret position
		int caret = getCaretPosition();
		return new TextScrollSnapshot(caret, middleScreenParagraph);
	}

	/**
	 * Restyles the text with the new language.
	 *
	 * @param language
	 * 		The language to apply to the styler.
	 */
	public void applyLanguage(Language language) {
		styler.setLanguage(language);
		styler.styleCompleteDocument(getText());
	}

	@Override
	public CompletableFuture<Void> onClearStyle() {
		return CompletableFuture.runAsync(() -> clearStyle(0, getText().length()), FxThreadUtil.executor());
	}

	@Override
	public CompletableFuture<Void> onApplyStyle(int start, List<LanguageStyler.Section> sections) {
		StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
		for (LanguageStyler.Section section : sections)
			spansBuilder.add(section.classes, section.length);
		StyleSpans<Collection<String>> spans = spansBuilder.create();
		// Update editor at position
		if (Thread.currentThread().isInterrupted())
			return ThreadUtil.failedFuture(new InterruptedException());
		return CompletableFuture.runAsync(() -> setStyleSpans(start, spans), FxThreadUtil.executor());
	}

	public void addStyle(StringBinding style) {
		styles.add(style);
		style.addListener(observable -> reapplyStyles());
		reapplyStyles();
	}

	@Override
	public void applyEventsForFontSizeChange(Consumer<Node> consumer) {
		consumer.accept(this);
	}

	@Override
	public void bindFontSize(IntegerProperty property) {
		addStyle(Bindings.createStringBinding(() -> "-fx-font-size: " + property.intValue() + "px;", property));
	}

	/**
	 * See {@link #isParagraphVisible(int)} for why we need this.
	 *
	 * @return Internal virtual flow of the code area.
	 */
	private VirtualFlow<?, ?> getInternalVirtualFlow() {
		String fieldName = "virtualFlow";
		try {
			Field field = ReflectUtil.getDeclaredField(GenericStyledArea.class, fieldName);
			field.setAccessible(true);
			return ReflectUtil.quietGet(this, field);
		} catch (ReflectiveOperationException ex) {
			// Make it obvious what went wrong so that if the internals do change we notice quickly and can update this.
			logger.error("RichTextFX internals changed, cannot locate '{}'", fieldName);
			throw new IllegalStateException("RichTextFX internals changed!", ex);
		}
	}

	private void reapplyStyles() {
		styleProperty().set(styles.stream().map(StringBinding::getValue)
				.collect(Collectors.joining("\n")));
	}

	private void handleTabForIndent(KeyEvent e) {
		if (e.getCode() != KeyCode.TAB) return;
		var selection = getCaretSelectionBind();
		// if there are 2 or more lines involved
		if (selection.getStartParagraphIndex() == selection.getEndParagraphIndex() || getSelectionText().isBlank())
			return;
		e.consume();
		int startPar = selection.getStartParagraphIndex();
		int endPar = selection.getEndParagraphIndex();
		int endCol = selection.getEndColumnPosition();
		// selecting stating from the first column, to avoid adding \t in the middle of a word
		selection.selectRange(startPar, 0, endPar, endCol);
		boolean shift = e.isShiftDown();
		replaceText(getSelectionStart(), getSelectionStop(),
			shift ?
				(getSelectionText().startsWith("\t") ? getSelectionText().substring(1) : getSelectionText())
					.replace("\n\t", "\n") :
				"\t" + getSelectionText().replace("\n", "\n\t")
		);
		// setting the selection anew, for further indentation
		selection.selectRange(startPar, 0, endPar, shift ? endCol : endCol + 1);
	}

	/**
	 * Snapshot of scroll / caret position.
	 */
	public class TextScrollSnapshot implements ScrollSnapshot {
		private final int caret;
		private final int middleScreenParagraph;

		/**
		 * @param caret
		 * 		Caret position in text at snapshot time.
		 * @param middleScreenParagraph
		 * 		Paragraph at middle of screen at snapshot time.
		 */
		public TextScrollSnapshot(int caret, int middleScreenParagraph) {
			this.caret = caret;
			this.middleScreenParagraph = middleScreenParagraph;
		}

		@Override
		public void restore() {
			if (caret >= 0 && caret < getText().length()) {
				moveTo(caret);
			}
			// Set to prior scroll position
			if (middleScreenParagraph > 0) {
				showParagraphAtCenter(middleScreenParagraph);
			} else {
				requestFollowCaret();
			}
		}
	}
}
