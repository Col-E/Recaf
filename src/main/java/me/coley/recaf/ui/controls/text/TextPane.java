package me.coley.recaf.ui.controls.text;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.ui.controls.CodeAreaExt;
import me.coley.recaf.ui.controls.text.model.*;
import me.coley.recaf.util.UnicodeUtil;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;

import java.util.function.Consumer;
import java.util.function.IntFunction;

/**
 * Text editor panel.
 *
 * @author Matt
 */
public class TextPane extends BorderPane {
	protected final GuiController controller;
	protected final CodeArea codeArea = new CodeAreaExt();
	private final VirtualizedScrollPane<CodeArea> scroll =  new VirtualizedScrollPane<>(codeArea);
	private final LanguageStyler styler;
	private Consumer<String> onCodeChange;

	/**
	 * @param controller
	 * 		Controller to act on.
	 * @param language
	 * 		Type of text content.
	 */
	public TextPane(GuiController controller, Language language) {
		this.controller = controller;
		this.styler = new LanguageStyler(language);
		getStyleClass().add("text-pane");
		setupCodeArea();
		setupSearch();
		setCenter(scroll);
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
					codeArea.setStyleSpans(0, styler.computeStyle(codeArea.getText()));
					if (onCodeChange != null)
						onCodeChange.accept(codeArea.getText());
				});
	}

	private void setupSearch() {
		// TODO: Keybind for search toggles search bar in BorderPane.TOP
	}

	/**
	 * Forgets history.
	 */
	public void forgetHistory() {
		codeArea.getUndoManager().forgetHistory();
	}

	/**
	 * @param line
	 * 		Line to check.
	 *
	 * @return {@code true} if the error has an error.
	 */
	protected boolean hasError(int line) { return false;}

	/**
	 * @param line
	 * 		Line to check.
	 *
	 * @return Error message for line. {@code null} if none.
	 */
	protected String getLineComment(int line) { return null;}

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
		codeArea.replaceText(UnicodeUtil.unescape(text));
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
	public void setOnCodeChange(Consumer<String> onCodeChange) {
		this.onCodeChange = onCodeChange;
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
