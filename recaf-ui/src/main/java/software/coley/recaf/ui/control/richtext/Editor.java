package software.coley.recaf.ui.control.richtext;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.ScrollBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import org.fxmisc.flowless.VirtualFlow;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.GenericStyledArea;
import org.fxmisc.richtext.model.PlainTextChange;
import org.fxmisc.richtext.model.ReadOnlyStyledDocument;
import org.fxmisc.richtext.model.StyleSpans;
import org.reactfx.Change;
import org.reactfx.EventStream;
import org.reactfx.EventStreams;
import software.coley.collections.Lists;
import software.coley.recaf.ui.control.richtext.bracket.SelectedBracketTracking;
import software.coley.recaf.ui.control.richtext.linegraphics.RootLineGraphicFactory;
import software.coley.recaf.ui.control.richtext.problem.ProblemTracking;
import software.coley.recaf.ui.control.richtext.syntax.StyleResult;
import software.coley.recaf.ui.control.richtext.syntax.SyntaxHighlighter;
import software.coley.recaf.ui.control.richtext.syntax.SyntaxUtil;
import software.coley.recaf.ui.pane.editing.ProblemOverlay;
import software.coley.recaf.util.*;
import software.coley.recaf.util.threading.ThreadPoolFactory;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
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
public class Editor extends BorderPane {
	public static final int SHORT_DELAY_MS = 150;
	private final StackPane stackPane = new StackPane();
	private final CodeArea codeArea = new CodeArea();
	private final ScrollBar horizontalScrollbar;
	private final ScrollBar verticalScrollbar;
	private final VirtualFlow<?, ?> virtualFlow;
	private final ExecutorService syntaxPool = ThreadPoolFactory.newSingleThreadExecutor("syntax-highlight");
	private final RootLineGraphicFactory rootLineGraphicFactory = new RootLineGraphicFactory(this);
	private final EventStream<Change<Integer>> caretPosEventStream;
	private ReadOnlyStyledDocument<Collection<String>, String, Collection<String>> lastDocumentSnapshot;
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
		VirtualizedScrollPane<CodeArea> scrollPane = new VirtualizedScrollPane<>(codeArea);
		horizontalScrollbar = Unchecked.get(() -> ReflectUtil.quietGet(scrollPane, VirtualizedScrollPane.class.getDeclaredField("hbar")));
		verticalScrollbar = Unchecked.get(() -> ReflectUtil.quietGet(scrollPane, VirtualizedScrollPane.class.getDeclaredField("vbar")));
		virtualFlow = Unchecked.get(() -> ReflectUtil.quietGet(codeArea, GenericStyledArea.class.getDeclaredField("virtualFlow")));

		// Initial layout / style.
		getStylesheets().add("/style/code-editor.css");
		setCenter(stackPane);
		stackPane.getChildren().add(scrollPane);

		// Do not want text wrapping in a code editor.
		codeArea.setWrapText(false);

		// Set paragraph graphic factory to the user-configurable root graphics factory.
		codeArea.setParagraphGraphicFactory(rootLineGraphicFactory);

		// This property copies the style of adjacent characters when typing (instead of having no style).
		// It may not seem like much, but it makes our restyle range computation logic much simpler.
		// Consider a multi-line comment. If you had this set to use the initial style (none) it would break up
		// multi-line comment style spans. We would have to re-stitch them together based on the inserted text position
		// which would be a huge pain in the ass.
		codeArea.setUseInitialStyleForInsertion(false);

		// Register a text change listener and use the inserted/removed text content to determine what portions
		// of the document need to be restyled.
		codeArea.plainTextChanges()
				.reduceSuccessions(Collections::singletonList, Lists::add, Duration.ofMillis(SHORT_DELAY_MS))
				.addObserver(changes -> {
					// Pass to highlighter.
					if (syntaxHighlighter != null) {
						for (PlainTextChange change : changes) {
							schedule(syntaxPool, () -> {
								String text = getText();
								IntRange range = SyntaxUtil.getRangeForRestyle(text, getStyleSpans(), syntaxHighlighter, change);
								int start = range.start();
								int end = range.end();
								return new StyleResult(syntaxHighlighter.createStyleSpans(text, start, end), start);
							}, result -> codeArea.setStyleSpans(result.position(), result.spans()));
						}
					}

					// Pass to problem tracking.
					if (problemTracking != null) {
						for (PlainTextChange change : changes)
							problemTracking.accept(change);
					}

					// Record content of area.
					lastDocumentSnapshot = codeArea.getContent().snapshot();
				});

		// Create event-streams for various events.
		caretPosEventStream = EventStreams.changesOf(codeArea.caretPositionProperty());

		// Initial snapshot state.
		lastDocumentSnapshot = ReadOnlyStyledDocument.from(codeArea.getDocument());
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
			return schedule(syntaxPool, () -> {
				IntRange range = SyntaxUtil.getRangeForRestyle(getText(), getStyleSpans(),
						syntaxHighlighter, new PlainTextChange(position, "", StringUtil.repeat(".", length)));
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
		}
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
			codeArea.setStyleSpans(0, syntaxHighlighter.createStyleSpans(getText(), 0, getTextLength()));
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
		if (problemOverlay != null)
			problemOverlay = null;

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
	 * @return Backing text editor component.
	 */
	@Nonnull
	public CodeArea getCodeArea() {
		return codeArea;
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
	 * @param supplier
	 * 		Value supplier.
	 * @param consumer
	 * 		Value consumer, run on the JavaFX UI thread.
	 * @param <T>
	 * 		Value type.
	 *
	 * @return Future of consumer completion.
	 */
	public <T> CompletableFuture<Void> schedule(@Nonnull ExecutorService supplierService,
												@Nonnull Supplier<T> supplier, @Nonnull Consumer<T> consumer) {
		return CompletableFuture.supplyAsync(supplier, supplierService)
				.thenAcceptAsync(consumer, FxThreadUtil.executor());
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
