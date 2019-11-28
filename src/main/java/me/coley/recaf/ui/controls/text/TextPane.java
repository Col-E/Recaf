package me.coley.recaf.ui.controls.text;

import javafx.scene.layout.BorderPane;
import me.coley.recaf.control.gui.GuiController;
import me.coley.recaf.ui.controls.CodeAreaExt;
import me.coley.recaf.ui.controls.text.model.*;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;

import java.util.function.Consumer;

/**
 * Text editor panel.
 *
 * @author Matt
 */
public class TextPane extends BorderPane {
	private final GuiController controller;
	private final CodeArea code = new CodeAreaExt();
	private final VirtualizedScrollPane<CodeArea> scroll =  new VirtualizedScrollPane<>(code);
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
		code.setEditable(false);
		code.setParagraphGraphicFactory(LineNumberFactory.get(code));
		code.richChanges()
				.filter(ch -> !ch.getInserted().equals(ch.getRemoved()))
				.filter(ch -> ch.getPosition() != 0 || ch.getNetLength() >= code.getLength() - 1)
				.subscribe(change -> {
					code.setStyleSpans(0, styler.computeStyle(code.getText()));
					if (onCodeChange != null)
						onCodeChange.accept(code.getText());
				});
	}

	private void setupSearch() {
		// TODO: Keybind for search toggles search bar in BorderPane.TOP
	}

	/**
	 * @return Text content
	 */
	public String getText() {
		return code.getText();
	}

	/**
	 * @param text
	 * 		Text content.
	 */
	public void setText(String text) {
		code.replaceText(text);
	}

	/**
	 * @param wrap
	 * 		Should text wrap lines.
	 */
	public void setWrapText(boolean wrap) {
		code.setWrapText(wrap);
	}

	/**
	 * @param editable
	 * 		Should text be editable.
	 */
	public void setEditable(boolean editable) {
		code.setEditable(editable);
	}

	/**
	 * @param onCodeChange
	 * 		Action to run when text is modified.
	 */
	public void setOnCodeChange(Consumer<String> onCodeChange) {
		this.onCodeChange = onCodeChange;
	}
}
