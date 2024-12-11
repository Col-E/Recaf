package software.coley.recaf.ui.control.richtext;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.IndexRange;
import javafx.scene.control.ScrollBar;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import org.fxmisc.flowless.Cell;
import org.fxmisc.flowless.VirtualFlow;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.GenericStyledArea;
import org.fxmisc.richtext.model.*;
import org.reactfx.Change;
import org.reactfx.EventStream;
import org.reactfx.EventStreams;
import org.reactfx.collection.MemoizationList;
import org.slf4j.Logger;
import software.coley.collections.Lists;
import software.coley.collections.Unchecked;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.behavior.Closing;
import software.coley.recaf.ui.control.VirtualizedScrollPaneWrapper;
import software.coley.recaf.ui.control.richtext.bracket.SelectedBracketTracking;
import software.coley.recaf.ui.control.richtext.linegraphics.RootLineGraphicFactory;
import software.coley.recaf.ui.control.richtext.problem.ProblemTracking;
import software.coley.recaf.ui.control.richtext.suggest.TabCompleter;
import software.coley.recaf.ui.control.richtext.syntax.StyleResult;
import software.coley.recaf.ui.control.richtext.syntax.SyntaxHighlighter;
import software.coley.recaf.ui.control.richtext.syntax.SyntaxUtil;
import software.coley.recaf.ui.pane.editing.ProblemOverlay;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.IntRange;
import software.coley.recaf.util.ReflectUtil;
import software.coley.recaf.util.StringUtil;
import software.coley.recaf.util.threading.ThreadPoolFactory;
import software.coley.recaf.util.threading.ThreadUtil;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Modular text editor control.
 * <ul>
 *     <li>Configure syntax with {@link #setSyntaxHighlighter(SyntaxHighlighter)}</li>
 *     <li>Configure selected bracket tracking with {@link #setSelectedBracketTracking(SelectedBracketTracking)}</li>
 *     <li>Configure line graphics via {@link #getRootLineGraphicFactory()}</li>
 * </ul>
 *
 * @author Matt Coley
 */
public class Editor extends BorderPane implements Closing {
	private static final Logger logger = Logging.get(Editor.class);
	public static final int SHORTER_DELAY_MS = 25;
	public static final int SHORT_DELAY_MS = 150;
	public static final int MEDIUM_DELAY_MS = 400;
	private static final StyleResult FALLBACK_STYLE_RESULT = new StyleResult(StyleSpans.singleton(Collections.emptyList(), 0), 0);
	private final StackPane stackPane = new StackPane();
	private final CodeArea codeArea = new SafeCodeArea();
	private final ScrollBar horizontalScrollbar;
	private final ScrollBar verticalScrollbar;
	private final VirtualFlow<?, ?> virtualFlow;
	private final MemoizationList<Cell<?, ?>> virtualCellList;
	private final ExecutorService syntaxPool = ThreadPoolFactory.newSingleThreadExecutor("syntax-highlight");
	private final RootLineGraphicFactory rootLineGraphicFactory = new RootLineGraphicFactory(this);
	private final EventStream<Change<Integer>> caretPosEventStream;
	private ReadOnlyStyledDocument<Collection<String>, String, Collection<String>> lastDocumentSnapshot;
	private TabCompleter<?> tabCompleter;
	private SyntaxHighlighter syntaxHighlighter;
	private SelectedBracketTracking selectedBracketTracking;
	private ProblemTracking problemTracking;
	private ProblemOverlay problemOverlay;

	/**
	 * New editor instance.
	 */
	public Editor() {
		// Get the reflection hacks out of the way first.
		//  - Want to have access to scrollbars & the internal 'virtualFlow'
		VirtualizedScrollPaneWrapper<CodeArea> scrollPane = new VirtualizedScrollPaneWrapper<>(codeArea);
		horizontalScrollbar = Unchecked.get(() -> ReflectUtil.quietGet(scrollPane, VirtualizedScrollPane.class.getDeclaredField("hbar")));
		verticalScrollbar = Unchecked.get(() -> ReflectUtil.quietGet(scrollPane, VirtualizedScrollPane.class.getDeclaredField("vbar")));
		virtualFlow = Unchecked.get(() -> ReflectUtil.quietGet(codeArea, GenericStyledArea.class.getDeclaredField("virtualFlow")));
		Object virtualCellManager = Unchecked.get(() -> ReflectUtil.quietGet(virtualFlow, VirtualFlow.class.getDeclaredField("cellListManager")));
		virtualCellList = ReflectUtil.quietInvoke(virtualCellManager.getClass(), virtualCellManager, "getLazyCellList", new Class[0], new Object[0]);

		// Initial layout / style.
		getStylesheets().add("/style/code-editor.css");
		setCenter(stackPane);
		stackPane.getChildren().add(scrollPane);

		// Do not want text wrapping in a code editor.
		codeArea.setWrapText(false);

		// Add event filter to hook tab usage.
		codeArea.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
			try {
				if (e.getCode() == KeyCode.TAB)
					handleTab(e);
				else if (e.getCode() == KeyCode.ENTER)
					handleNewline(e);
			} catch (Throwable t) {
				logger.error("Error handling tab/newline interception in editor", t);
			}
		});

		// Set paragraph graphic factory to the user-configurable root graphics factory.
		codeArea.setParagraphGraphicFactory(rootLineGraphicFactory);

		// This property copies the style of adjacent characters when typing (instead of having no style).
		// It may not seem like much, but it makes our restyle range computation logic much simpler.
		// Consider a multi-line comment. If you had this set to use the initial style (none) it would break up
		// multi-line comment style spans. We would have to re-stitch them together based on the inserted text position
		// which would be a huge pain in the ass.
		codeArea.setUseInitialStyleForInsertion(false);

		// Register a text change listener for recording state used for tab completion and updating problem locations.
		codeArea.plainTextChanges().addObserver(change -> {
			// Do fine completion updates.
			try {
				if (tabCompleter != null)
					tabCompleter.onFineTextUpdate(change);
			} catch (Throwable t) {
				logger.error("Error handling tab-completion update in editor", t);
			}
		});

		// Register a text change listener that operates on reduces calls (limit calls to when user stops typing).
		// Used for:
		//  - Restyling text near inserted/removed text
		//  - Taking document snapshots
		codeArea.plainTextChanges()
				.reduceSuccessions(Collections::singletonList, Lists::add, Duration.ofMillis(SHORT_DELAY_MS))
				.addObserver(changes -> {
					try {
						// Pass to highlighter.
						if (syntaxHighlighter != null) {
							for (PlainTextChange change : changes) {
								schedule(syntaxPool, FALLBACK_STYLE_RESULT, () -> {
									String text = getText();
									IntRange range = SyntaxUtil.getRangeForRestyle(text, getStyleSpans(), syntaxHighlighter, change);
									int start = range.start();
									int end = range.end();
									return new StyleResult(syntaxHighlighter.createStyleSpans(text, start, end), start);
								}, result -> codeArea.setStyleSpans(result.position(), result.spans()));
							}
						}

						// Do rough completion updates.
						if (tabCompleter != null)
							tabCompleter.onRoughTextUpdate(changes);

						// Record content of area.
						lastDocumentSnapshot = codeArea.getContent().snapshot();
					} catch (Throwable t) {
						logger.error("Uncaught error on editor reduced-succession update", t);
					}
				});

		// Create event-streams for various events.
		caretPosEventStream = EventStreams.changesOf(codeArea.caretPositionProperty());

		// Initial snapshot state.
		lastDocumentSnapshot = ReadOnlyStyledDocument.from(codeArea.getDocument());
	}

	@Override
	public void close() {
		if (selectedBracketTracking != null)
			selectedBracketTracking.close();
		if (!syntaxPool.isShutdown())
			syntaxPool.shutdownNow();
	}

	/**
	 * @return {@code true} to indicate the contents of this editor are editable. {@code false} for read-only content.
	 */
	public boolean isEditable() {
		return codeArea.isEditable();
	}

	/**
	 * The passed inputs for the position will be modified as per
	 * {@link SyntaxUtil#getRangeForRestyle(String, StyleSpans, SyntaxHighlighter, PlainTextChange)}.
	 * For this reason, you do not have to be super exact with the given values.
	 *
	 * @param position
	 * 		Start position to begin the restyling at.
	 * @param length
	 * 		Length of the restyled range.
	 *
	 * @return Restyle future.
	 */
	public CompletableFuture<Void> restyleAtPosition(int position, int length) {
		if (syntaxHighlighter != null) {
			return schedule(syntaxPool, FALLBACK_STYLE_RESULT, () -> {
				IntRange range = SyntaxUtil.getRangeForRestyle(getText(), getStyleSpans(),
						syntaxHighlighter, new PlainTextChange(position, "", ".".repeat(length)));
				int start = range.start();
				int end = range.end();
				return new StyleResult(syntaxHighlighter.createStyleSpans(getText(), start, end), start);
			}, result -> codeArea.setStyleSpans(result.position(), result.spans()));
		}
		return CompletableFuture.completedFuture(null);
	}

	/**
	 * The editor is a {@link BorderPane} layout. The sides can be used to toggle "drawers" of sorts.
	 * The center is home to the primary component, the {@link #getCodeArea() code-area}.
	 *
	 * @return The {@link StackPane} present in the {@link #getCenter() center} of the editor.
	 */
	@Nonnull
	public StackPane getPrimaryStack() {
		return stackPane;
	}

	/**
	 * Redraw visible paragraph graphics.
	 * <br>
	 * <b>Must be called on FX thread.</b>
	 */
	public void redrawParagraphGraphics() {
		int startParagraphIndex = Math.max(0, codeArea.firstVisibleParToAllParIndex() - 1);
		int endParagraphIndex = Math.min(codeArea.getParagraphs().size() - 1, codeArea.lastVisibleParToAllParIndex());
		for (int i = startParagraphIndex; i <= endParagraphIndex; i++)
			codeArea.recreateParagraphGraphic(i);
	}

	/**
	 * @return Current style spans for the entire document.
	 */
	@Nonnull
	public StyleSpans<Collection<String>> getStyleSpans() {
		return codeArea.getStyleSpans(0, getTextLength());
	}

	/**
	 * @return Current length of document text.
	 */
	public int getTextLength() {
		return codeArea.getLength();
	}

	/**
	 * @return Current document text.
	 */
	@Nonnull
	public String getText() {
		return Objects.requireNonNullElse(codeArea.getText(), "");
	}

	/**
	 * @return The prior document state, from the last {@link #getTextChangeEventStream() text change event}.
	 */
	@Nonnull
	public ReadOnlyStyledDocument<Collection<String>, String, Collection<String>> getLastDocumentSnapshot() {
		return lastDocumentSnapshot;
	}

	/**
	 * @param text
	 * 		Text to set.
	 */
	public void setText(@Nullable String text) {
		// Filter input
		if (text == null)
			text = "";

		// Prepare reset of caret/scroll position
		codeArea.textProperty().addListener(new CaretReset(codeArea.getCaretPosition()));
		codeArea.textProperty().addListener(new ScrollReset(virtualFlow.getFirstVisibleIndex()));

		// Replace the full text document
		if (getTextLength() == 0) {
			codeArea.appendText(text);
		} else {
			codeArea.replaceText(text);

			// Whole text replacement often results in the text not applying a restyle, resulting in all default text.
			// Scheduling a restyle at the start ensures if this happens it gets fixed in the next FX update cycle.
			if (!text.isBlank())
				restyleAtPosition(0, 0);
		}
	}

	/**
	 * Adds a tab indentation into the given paragraph.
	 *
	 * @param paragraph
	 * 		Paragraph to indent.
	 */
	public void indent(int paragraph) {
		String paragraphContents = codeArea.getParagraph(paragraph).getText();
		int column = 0;
		for (; column < paragraphContents.length(); column++) {
			char c = paragraphContents.charAt(column);
			if (c != ' ' && c != '\t') break;
		}
		codeArea.insert(paragraph, column, newText("\t"));
	}

	/**
	 * Removes indentation from the given paragraph.
	 *
	 * @param paragraph
	 * 		Paragraph to unindent.
	 *
	 * @return {@code true} when there was text removed (successful unindentation).
	 */
	public boolean unindent(int paragraph) {
		String paragraphContents = codeArea.getParagraph(paragraph).getText();
		int column = 0;
		for (; column < paragraphContents.length(); column++) {
			char c = paragraphContents.charAt(column);
			if (c != ' ' && c != '\t') break;
		}
		if (column > 0) {
			codeArea.deleteText(paragraph, column - 1, paragraph, column);
			return true;
		}
		return false;
	}

	/**
	 * Delegates to {@link CodeArea#textProperty()}.
	 * <br>
	 * Do not use this to set text. Instead, use {@link #setText(String)}.
	 *
	 * @return Property representation of {@link #getText()}.
	 */
	@Nonnull
	public ObservableValue<String> textProperty() {
		return codeArea.textProperty();
	}

	/**
	 * Delegates to {@link CodeArea#plainTextChanges()}.
	 *
	 * @return Event stream for changes to {@link #textProperty()}.
	 */
	@Nonnull
	public EventStream<PlainTextChange> getTextChangeEventStream() {
		return codeArea.plainTextChanges();
	}

	/**
	 * @return Event stream wrapper for {@link CodeArea#caretPositionProperty()}.
	 */
	@Nonnull
	public EventStream<Change<Integer>> getCaretPosEventStream() {
		return caretPosEventStream;
	}

	/**
	 * @return The root line graphics factory.
	 */
	@Nonnull
	public RootLineGraphicFactory getRootLineGraphicFactory() {
		return rootLineGraphicFactory;
	}

	/**
	 * @return Current highlighter.
	 */
	@Nullable
	public SyntaxHighlighter getSyntaxHighlighter() {
		return syntaxHighlighter;
	}

	/**
	 * @param syntaxHighlighter
	 * 		Highlighter to use.
	 */
	public void setSyntaxHighlighter(@Nullable SyntaxHighlighter syntaxHighlighter) {
		// Uninstall prior.
		SyntaxHighlighter previousSyntaxHighlighter = this.syntaxHighlighter;
		if (previousSyntaxHighlighter != null)
			previousSyntaxHighlighter.uninstall(this);

		// Set and install new instance.
		this.syntaxHighlighter = syntaxHighlighter;
		if (syntaxHighlighter != null) {
			syntaxHighlighter.install(this);
			String text = getText();
			if (!text.isBlank())
				codeArea.setStyleSpans(0, syntaxHighlighter.createStyleSpans(text, 0, getTextLength()));
		}
	}

	/**
	 * @param selectedBracketTracking
	 * 		New selected bracket tracking implementation, or {@code null} to disable selected bracket tracking.
	 */
	public void setSelectedBracketTracking(@Nullable SelectedBracketTracking selectedBracketTracking) {
		// Uninstall prior.
		SelectedBracketTracking previousSelectedBracketTracking = this.selectedBracketTracking;
		if (previousSelectedBracketTracking != null)
			previousSelectedBracketTracking.uninstall(this);

		// Set and install new instance.
		this.selectedBracketTracking = selectedBracketTracking;
		if (selectedBracketTracking != null)
			selectedBracketTracking.install(this);
	}

	/**
	 * @return Selected bracket tracking implementation.
	 */
	@Nullable
	public SelectedBracketTracking getSelectedBracketTracking() {
		return selectedBracketTracking;
	}

	/**
	 * @param problemTracking
	 * 		Problem tracking implementation.
	 */
	public void setProblemTracking(@Nullable ProblemTracking problemTracking) {
		// Uninstall prior.
		ProblemTracking previousProblemTracking = this.problemTracking;
		if (previousProblemTracking != null)
			previousProblemTracking.uninstall(this);
		if (problemOverlay != null) {
			problemOverlay.uninstall(this);
			problemOverlay = null;
		}

		// Set and install new instance.
		this.problemTracking = problemTracking;
		if (problemTracking != null) {
			problemTracking.install(this);
			problemOverlay = new ProblemOverlay();
			problemOverlay.install(this);
		}
	}

	/**
	 * @return Problem tracking implementation.
	 */
	@Nullable
	public ProblemTracking getProblemTracking() {
		return problemTracking;
	}

	/**
	 * @return Tab completion implementation.
	 */
	@Nullable
	public TabCompleter<?> getTabCompleter() {
		return tabCompleter;
	}

	/**
	 * @param tabCompleter
	 * 		Tab completion implementation.
	 */
	public void setTabCompleter(@Nullable TabCompleter<?> tabCompleter) {
		// Uninstall prior.
		TabCompleter<?> previousTabCompleter = this.tabCompleter;
		if (previousTabCompleter != null)
			previousTabCompleter.uninstall(this);

		// Set and install new instance.
		this.tabCompleter = tabCompleter;
		if (tabCompleter != null)
			tabCompleter.install(this);
	}

	/**
	 * @return Backing text editor component.
	 */
	@Nonnull
	public CodeArea getCodeArea() {
		return codeArea;
	}

	/**
	 * @return Virtual flow backing the {@link #getCodeArea() code area}.
	 */
	@Nonnull
	public VirtualFlow<?, ?> getVirtualFlow() {
		return virtualFlow;
	}

	/**
	 * @return Virtualized cell list within the {@link #getVirtualFlow() virtual flow}.
	 */
	@Nonnull
	public MemoizationList<Cell<?, ?>> getVirtualCellList() {
		return virtualCellList;
	}

	/**
	 * Get text nodes on a paragraph
	 * <p>
	 * Be <b>very aware</b> of when you call this. You may encounter unexpected values if invoked during early
	 * layout of your node / scene.
	 * <p/>
	 * This method is why we have to do the {@link FxThreadUtil#delayedRun(long, Runnable)} call above.
	 * Normally when you use {@code virtualFlow.getCellIfVisible(paragraph)} it lays out the nodes for
	 * you so that you don't run into this problem. The problem is it lays out the whole {@code ParagraphBox}
	 * class, which includes the graphic factory we're currently populating the content of.
	 * This means using that method will cause a {@link StackOverflowError}.
	 * Thus, we have the hacky delayed run instead.
	 *
	 * @param paragraph
	 * 		Paragraph index to get the text nodes of.
	 *
	 * @return List of text nodes in the paragraph.
	 */
	public List<Text> getTextNodes(int paragraph) {
		// Get the cell from the given paragraph. It should exist since we're
		// initializing a paragraph graphic for it.
		Cell<?, ?> cell = virtualCellList.get(paragraph);
		if (cell == null) return Collections.emptyList();

		// ParagraphBox is private in RichTextFX, but we just need to get the children so
		// casting to region suffices.
		Region paragraphBox = (Region) cell.getNode();
		ObservableList<Node> paragraphBoxChildren = paragraphBox.getChildrenUnmodifiable();

		if (paragraphBoxChildren.isEmpty())
			return Collections.emptyList();

		// The text flow is always the first child of the box.
		Region textFlow = (Region) paragraphBoxChildren.getFirst();

		// In the text flow, we want the first 'Text' child. This should be the first one with empty spaces.
		ObservableList<Node> flowChildren = textFlow.getChildrenUnmodifiable();
		return Unchecked.cast(flowChildren.stream()
				.filter(c -> c instanceof Text)
				.toList());
	}

	/**
	 * Compute the width of blank text before non-blank text.
	 *
	 * @param paragraph
	 * 		Paragraph index to compute empty space (in pixels) to the first non-whitespace character.
	 *
	 * @return Pixels to first non-whitespace character.
	 *
	 * @see #getTextNodes(int)
	 */
	public double computeWhitespacePrefixWidth(int paragraph) {
		List<Text> textNodes = getTextNodes(paragraph);
		double width = 0;

		for (Text textNode : textNodes) {
			String text = textNode.getText();
			double boundWidth = textNode.getBoundsInLocal().getWidth();
			if (text.isBlank()) {
				// Texts that are blank are all whitespace, add it up.
				width += boundWidth;
			} else {
				// Some texts have leading whitespace that we want to consider.
				int whitespacePrefix = StringUtil.getWhitespacePrefixLength(text);
				if (whitespacePrefix > 0) {
					double charWidth = boundWidth / StringUtil.getTabAdjustedLength(text);
					width += charWidth * whitespacePrefix;
				}
				break;
			}
		}

		return width;
	}

	/**
	 * Compute the width of text until a specific character.
	 *
	 * @param paragraph
	 * 		Paragraph index to compute the width of.
	 * @param character
	 * 		Character index to compute the width until.
	 *
	 * @return Width of text until the character.
	 */
	public double computeWidthUntilCharacter(int paragraph, int character) {
		List<Text> textNodes = getTextNodes(paragraph);
		double width = 0;
		int index = 0;
		Text lastNode = null;

		for (Text textNode : textNodes) {
			String text = textNode.getText();
			double boundWidth = textNode.getBoundsInLocal().getWidth();

			if (index + text.length() < character) {
				index += text.length();
				width += boundWidth;
			} else {
				double charWidth = boundWidth / StringUtil.getTabAdjustedLength(text);
				width += charWidth * (character - index);
				return width;
			}

			lastNode = textNode;
		}

		// we never reached the character
		if (index < character) {
			double charWidth = 0L;
			if (lastNode == null)
				charWidth = 1.7;
			else
				charWidth = lastNode.getBoundsInLocal().getWidth() / StringUtil.getTabAdjustedLength(lastNode.getText());

			width += charWidth * (character - index);
		}

		return width;
	}

	/**
	 * @param line
	 * 		Paragraph index, 0-based.
	 *
	 * @return {@code true} when the paragraph is visible.
	 */
	public boolean isParagraphVisible(int line) {
		// TODO: If we ever add paragraph folding back, we need to check those cases here and return false

		// We use the internal virtual flow because the provided methods call 'layout()' unnecessarily
		//  - firstVisibleParToAllParIndex()
		//  - lastVisibleParToAllParIndex()
		// It is very likely by the time of calling this that our text is already populated and laid out.
		// This gets called rather frequently so the constant layout requests contribute a massive waste of time.
		// If we use these methods from the internal 'VirtualFlow' we skip all that and the result is almost instant.
		return line >= virtualFlow.getFirstVisibleIndex() && line <= virtualFlow.getLastVisibleIndex();
	}

	/**
	 * @return {@link #getCodeArea() Code area's} horizontal scrollbar.
	 */
	@Nonnull
	public ScrollBar getHorizontalScrollbar() {
		return horizontalScrollbar;
	}

	/**
	 * @return {@link #getCodeArea() Code area's} vertical scrollbar.
	 */
	@Nonnull
	public ScrollBar getVerticalScrollbar() {
		return verticalScrollbar;
	}

	/**
	 * @param supplierService
	 * 		Executor service to run the supplier on.
	 * @param fallback
	 * 		Fallback value when the supplier action encounters an exception.
	 * @param supplier
	 * 		Value supplier.
	 * @param consumer
	 * 		Value consumer, run on the JavaFX UI thread.
	 * @param <T>
	 * 		Value type.
	 *
	 * @return Future of consumer completion.
	 */
	@Nonnull
	public <T> CompletableFuture<Void> schedule(@Nonnull ExecutorService supplierService, @Nullable T fallback,
	                                            @Nonnull Supplier<T> supplier, @Nonnull Consumer<T> consumer) {
		return CompletableFuture.supplyAsync(ThreadUtil.wrap(supplier, fallback), supplierService)
				.thenAcceptAsync(ThreadUtil.wrap(consumer), FxThreadUtil.executor());
	}

	/**
	 * Some RichTextFX methods require styled documents, hence this helper for converting simple strings into those.
	 *
	 * @param text
	 * 		Text to wrap in a styled document with the default values.
	 *
	 * @return Text document of the requested text.
	 */
	@Nonnull
	public StyledDocument<Collection<String>, String, Collection<String>> newText(@Nonnull String text) {
		return ReadOnlyStyledDocument.fromString(text, codeArea.getInitialParagraphStyle(),
				codeArea.getInitialTextStyle(), codeArea.getSegOps());
	}

	/**
	 * Handles newline events. Inserts a new line, but matches the indentation level of the previous line.
	 *
	 * @param event
	 * 		Key event where {@link KeyCode#ENTER} was pressed.
	 */
	private void handleNewline(@Nonnull KeyEvent event) {
		// Consume the event so the normal 'enter' behavior is skipped.
		event.consume();

		// Abort if handling tab completion.
		// Holding shift bypasses tab completion.
		if (!event.isShiftDown() && handleTabCompletion(event))
			return;

		// If there is selected contents, newline should replace it.
		IndexRange selection = codeArea.getSelection();
		if (selection.getLength() > 0) {
			codeArea.replace(selection.getStart(), selection.getEnd(), newText("\n"));
			return;
		}

		// Compute matching indentation level.
		int paragraph = codeArea.getCurrentParagraph();
		String paragraphContents = codeArea.getParagraph(paragraph).getText();
		int indent = 0;
		for (; indent < paragraphContents.length(); indent++) {
			char c = paragraphContents.charAt(indent);
			if (c != ' ' && c != '\t') break;
		}

		// Padding to insert in into the next line.
		String padding = paragraphContents.substring(0, indent);

		// For open brackets we'd like to do an extra level of indentation
		// and also complete the brace for the user.
		char closingBrace = '\0';
		for (int i = paragraphContents.length() - 1; i >= indent; i--) {
			char c = paragraphContents.charAt(i);
			if (c == '{') {
				closingBrace = '}';
				padding += '\t';
				break;
			} else if (c == '[') {
				closingBrace = ']';
				padding += '\t';
				break;
			} else if (c != ' ' && c != '\t') {
				break;
			}
		}

		// Insert the indented newline + closing brace (if needed)
		int caret = codeArea.getCaretPosition();
		if (closingBrace == '\0') {
			codeArea.insertText(caret, '\n' + padding);
		} else {
			// Insert the '}' or ']' and move the caret between
			codeArea.insertText(caret, '\n' + padding + '\n' + StringUtil.limit(padding, padding.length() - 1) + closingBrace);
			codeArea.moveTo(caret + padding.length() + 1);
		}
	}

	/**
	 * Handles tab events.
	 *
	 * @param event
	 * 		Key event where {@link KeyCode#TAB} was pressed.
	 */
	private void handleTab(@Nonnull KeyEvent event) {
		IndexRange selection = codeArea.getSelection();

		// Skip if selection is empty (unless doing shift-tab for unindentation)
		if (selection.getLength() == 0 && !event.isShiftDown()) {
			// Abort if handling tab completion.
			if (handleTabCompletion(event))
				event.consume();

			// Skip tab handling below.
			return;
		}

		// Consume the event so the tab key does not get handled (no insertion of \t)
		event.consume();

		// Detailed selection info.
		TwoDimensional.Position selectionStart2D = codeArea.offsetToPosition(selection.getStart(), TwoDimensional.Bias.Forward);
		TwoDimensional.Position selectionEnd2D = codeArea.offsetToPosition(selection.getEnd(), TwoDimensional.Bias.Forward);
		boolean isMultiLine = selectionStart2D.getMajor() != selectionEnd2D.getMajor();

		// Check if text selection is multi-line
		boolean doShift = false;
		if (isMultiLine) {
			// Insert a '\t' before the first non-whitespace character of all selected paragaphs.
			int start = selection.getStart();
			int end = selection.getEnd();
			int startParagraph = codeArea.offsetToPosition(start, TwoDimensional.Bias.Forward).getMajor();
			int endParagraph = codeArea.offsetToPosition(end, TwoDimensional.Bias.Backward).getMajor();
			for (int i = startParagraph; i <= endParagraph; i++) {
				// Not ideal as it counts as multiple actions (not undo-able in one go) but good enough for now.
				if (event.isShiftDown()) {
					doShift = unindent(i);
				} else {
					doShift = true;
					indent(i);
				}
			}
		} else {
			// Insert a '\t' before the first non-whitespace character of the paragraph.
			int paragraph = codeArea.getCurrentParagraph();
			if (event.isShiftDown()) {
				doShift = unindent(paragraph);
			} else {
				doShift = true;
				indent(paragraph);
			}
		}

		// Re-select the original text.
		if (doShift)
			codeArea.selectRange(selectionStart2D.getMajor(), selectionStart2D.getMinor() + 1,
					selectionEnd2D.getMajor(), selectionEnd2D.getMinor() + 1);
	}

	/**
	 * Handle tab completion <i>(completion also works with enter key)</i>
	 *
	 * @param event
	 * 		Key event where {@link KeyCode#TAB} or {@link KeyCode#ENTER} was pressed.
	 *
	 * @return {@code true} when a completion was made.
	 */
	private boolean handleTabCompletion(@Nonnull KeyEvent event) {
		return tabCompleter != null && tabCompleter.requestCompletion(event);
	}

	/**
	 * Handles re-scrolling to the same location after the text document has been updated.
	 *
	 * @see #setText(String)
	 */
	private class ScrollReset implements ChangeListener<String> {
		private final int firstIndex;

		ScrollReset(int firstIndex) {
			this.firstIndex = firstIndex;
		}

		@Override
		public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
			// Of the multiple ways to reset scroll position, this seems to be the most reliable.
			// It's not pixel perfect, but it shouldn't be too jarring since resetting content should
			// not happen often.
			virtualFlow.showAsFirst(firstIndex);
			observable.removeListener(this);
		}
	}

	/**
	 * Handles re-positioning the caret to the same location after the text document has been updated.
	 *
	 * @see #setText(String)
	 */
	private class CaretReset implements ChangeListener<String> {
		private final int pos;

		CaretReset(int pos) {
			this.pos = pos;
		}

		@Override
		public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
			codeArea.moveTo(Math.min(codeArea.getLength(), pos));
			observable.removeListener(this);
		}
	}
}
