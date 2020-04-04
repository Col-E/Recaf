package me.coley.recaf.ui.controls.text;

import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.ui.controls.CodeAreaExt;
import me.coley.recaf.ui.controls.SearchBar;
import me.coley.recaf.ui.controls.text.model.*;
import me.coley.recaf.util.ThreadUtil;
import me.coley.recaf.util.struct.Pair;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;

import java.awt.Toolkit;
import java.util.function.*;

/**
 * Text editor panel.
 *
 * @param <E>
 * 		Error handler type.
 * @param <C>
 *     	Context handler type.
 *
 * @author Matt
 */
public class TextPane<E extends ErrorHandling, C extends ContextHandling> extends BorderPane {
	protected final GuiController controller;
	protected final CodeArea codeArea = new CodeAreaExt();
	protected final C contextHandler;
	private final VirtualizedScrollPane<CodeArea> scroll =  new VirtualizedScrollPane<>(codeArea);
	private final LanguageStyler styler;
	private final SplitPane split;
	private final SearchBar searchBar = new SearchBar(codeArea::getText);
	private E errHandler;
	private Consumer<String> onCodeChange;
	private ListView<Pair<Integer, String>> errorList = new ListView<>();

	/**
	 * @param controller
	 * 		Controller to act on.
	 * @param language
	 * 		Type of text content.
	 * @param handlerFunc
	 * 		Function to supply the context handler.
	 */
	public TextPane(GuiController controller, Language language, BiFunction<GuiController, CodeArea, C> handlerFunc) {
		this.controller = controller;
		this.styler = new LanguageStyler(language);
		this.contextHandler = handlerFunc.apply(controller, codeArea);
		getStyleClass().add("text-pane");
		setupCodeArea();
		setupSearch();
		setupErrors();
		split = new SplitPane(scroll, errorList);
		split.setOrientation(Orientation.VERTICAL);
		split.setDividerPositions(1);
		split.getStyleClass().add("no-border");
		SplitPane.setResizableWithParent(errorList, Boolean.FALSE);
		setCenter(split);
	}

	private void setupCodeArea() {
		codeArea.setEditable(false);
		IntFunction<Node> lineFactory = LineNumberFactory.get(codeArea);
		IntFunction<Node> errorFactory = new ErrorIndicatorFactory();
		IntFunction<Node> decorationFactory = line -> {
			HBox hbox = new HBox(
					lineFactory.apply(line),
					errorFactory.apply(line));
			hbox.setAlignment(Pos.CENTER_LEFT);
			return hbox;
		};
		Platform.runLater(() -> codeArea.setParagraphGraphicFactory(decorationFactory));
		codeArea.richChanges()
				.filter(ch -> !ch.isPlainTextIdentity())
				.filter(ch -> !ch.getInserted().equals(ch.getRemoved()))
				.subscribe(change -> {
					ThreadUtil.runJfx(() -> {
						if(onCodeChange != null)
							onCodeChange.accept(codeArea.getText());
						return styler.computeStyle(codeArea.getText());
					}, computedStyle -> codeArea.setStyleSpans(0, computedStyle));
				});
	}

	private void setupSearch() {
		setOnKeyPressed(e -> {
			if(controller.config().keys().find.match(e)) {
				// On search bind:
				//  - open search field if necessary
				//  - select search field text
				boolean open = getTop() != null && getTop().equals(searchBar);
				if(!open)
					setTop(searchBar);
				searchBar.focus();
			}
		});
		searchBar.setOnEscape(() -> {
			// Escape -> Hide field
			searchBar.clear();
			codeArea.requestFocus();
			setTop(null);
		});
		searchBar.setOnSearch(results -> {
			// On search, goto next result
			int caret = codeArea.getCaretPosition();
			Pair<Integer, Integer> range = results.next(caret);
			if (range == null) {
				// No results
				Toolkit.getDefaultToolkit().beep();
			} else {
				// Move caret to result range
				codeArea.selectRange(range.getKey(), range.getValue());
				codeArea.requestFollowCaret();
			}
		});
	}

	private void setupErrors() {
		errorList.setCellFactory(e -> new ErrorCell(codeArea));
		errorList.getStyleClass().add("error-list");
	}

	/**
	 * Forgets history.
	 */
	public void forgetHistory() {
		codeArea.getUndoManager().forgetHistory();
	}

	/**
	 * @return Text content
	 */
	public String getText() {
		return codeArea.getText();
	}

	/**
	 * @param text
	 * 		Text content.
	 */
	public void setText(String text) {
		codeArea.replaceText(text);
	}

	/**
	 * @param wrap
	 * 		Should text wrap lines.
	 */
	public void setWrapText(boolean wrap) {
		codeArea.setWrapText(wrap);
	}

	/**
	 * @param editable
	 * 		Should text be editable.
	 */
	public void setEditable(boolean editable) {
		codeArea.setEditable(editable);
	}

	/**
	 * @param onCodeChange
	 * 		Action to run when text is modified.
	 */
	protected void setOnCodeChange(Consumer<String> onCodeChange) {
		this.onCodeChange = onCodeChange;
	}

	/**
	 * @param errHandler
	 * 		Error handler.
	 */
	protected void setErrorHandler(E errHandler) {
		if (this.errHandler != null)
			this.errHandler.unbind();
		this.errHandler = errHandler;
		this.errHandler.bind(errorList);
	}

	public E getErrorHandler() {
		return errHandler;
	}

	protected boolean hasNoErrors() {
		if (errHandler == null)
			return true;
		return !errHandler.hasErrors();
	}

	private boolean hasError(int line) {
		if (hasNoErrors())
			return false;
		return errHandler.hasError(line);
	}

	private String getLineComment(int line) {
		if (hasNoErrors())
			return null;
		return errHandler.getLineComment(line);
	}

	/**
	 * Decorator factory for building error indicators.
	 */
	class ErrorIndicatorFactory implements IntFunction<Node> {
		private final double[] shape = new double[]{0, 0, 10, 5, 0, 10};

		@Override
		public Node apply(int lineNo) {
			Polygon poly = new Polygon(shape);
			poly.getStyleClass().add("cursor-pointer");
			poly.setFill(Color.RED);
			if(hasError(lineNo)) {
				String msg = getLineComment(lineNo);
				if(msg != null) {
					Tooltip.install(poly, new Tooltip(msg));
				}
			} else {
				poly.setVisible(false);
			}
			return poly;
		}
	}
}
