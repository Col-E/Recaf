package me.coley.recaf.ui.controls.text;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Bounds;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.stage.Popup;
import jregex.Matcher;
import jregex.Pattern;
import me.coley.recaf.parse.bytecode.Parse;
import me.coley.recaf.parse.bytecode.ParseResult;
import me.coley.recaf.parse.bytecode.ast.RootAST;
import me.coley.recaf.util.OpcodeUtil;
import me.coley.recaf.util.RegexUtil;
import me.coley.recaf.util.struct.*;
import org.fxmisc.richtext.CodeArea;

import java.util.*;
import java.util.stream.Collectors;

import static javafx.scene.input.KeyCode.*;

/**
 * Bytecode-focused tab-completion/suggestion handling.
 *
 * @author Matt
 */
public class BytecodeSuggestHandler  {
	private static final int ROW_HEIGHT = 24;
	private final BytecodeEditorPane bytePane;
	private final CodeArea codeArea;
	private final Popup popAuto = new Popup();

	/**
	 * @param bytePane
	 * 		Pane to handle errors for.
	 */
	public BytecodeSuggestHandler(BytecodeEditorPane bytePane) {
		this.bytePane = bytePane;
		this.codeArea = bytePane.codeArea;
	}

	/**
	 * Setup suggestions for the editor.
	 */
	public void setup() {
		codeArea.setOnKeyReleased(e -> {
			// Update auto-complete on key-release except for certain non-modifying keys.
			KeyCode k = e.getCode();
			if (k == ESCAPE){
				// Remove popup if escape is pressed.
				popAuto.hide();
				e.consume();
				return;
			}
			if (k == BACK_SPACE){
				// Don't show popup if backspacing onto some text.
				popAuto.hide();
				return;
			}
			if (k != PERIOD && (k.isArrowKey() || k.isModifierKey() || k.isWhitespaceKey())) {
				// Pressing period and most normal keyboard characters will allow auto-completion
				// to be run. Arrow keys and misc. keys will not update it.
				if (k == SPACE)
					popAuto.hide();
				return;
			}
			// Hide popup and then update auto-completion.
			// This may re-show it if there are suggestions.
			popAuto.hide();
			updateAutoComplete();
		});
		codeArea.setOnKeyTyped(e -> {
			// Ensure directional / input keys are sent to the popup.
			KeyCode k = e.getCode();
			boolean move = k == UP || k == DOWN || k == ENTER || k == TAB;
			if (popAuto.isShowing() && move) {
				popAuto.requestFocus();
			}
		});
	}

	/**
	 * Update code-completion prompt.
	 */
	private void updateAutoComplete() {
		int position = codeArea.getCaretPosition();
		int line = codeArea.getCurrentParagraph();
		String lineText = codeArea.getParagraph(line).getText();
		// Ensure that the caret is at the end of the line.
		if (codeArea.getCaretColumn() < lineText.length()) {
			return;
		}
		ThreadAction.<List<String>>create().supplier(() -> {
			List<String> suggestions = null;
			try {
				suggestions = suggest(bytePane.getLastParse(), lineText);
			} catch(Exception e) {
				// If we fail, don't suggest anything
				return null;
			}
			// Limit capacity
			suggestions = suggestions.stream().limit(7).collect(Collectors.toList());
			return suggestions;
		}).consumer(suggestions -> {
			// No suggestions? Do nothing
			if (suggestions == null || suggestions.isEmpty()) {
				return;
			}
			ListView<String> listSuggestions = new ListView<>(FXCollections.observableArrayList(suggestions));
			listSuggestions.getStyleClass().add("tab-complete");
			listSuggestions.getSelectionModel().select(0);
			listSuggestions.setPrefHeight(suggestions.size() * ROW_HEIGHT + 2);
			// Get current word
			Matcher m = new Pattern("([\\/\\w]+|(?!\\.))$").matcher(lineText);
			if(!m.find()) {
				return;
			}
			String curWord = m.group(0);
			// Action to replace the word with some given replacement word.
			Runnable replace = () -> {
				String selected = listSuggestions.getSelectionModel().getSelectedItem();
				Platform.runLater(() -> {
					codeArea.replaceText(position - curWord.length(), position, selected);
					codeArea.moveTo(position + selected.length() - curWord.length());
				});
				popAuto.hide();
			};
			Optional<Bounds> val = codeArea.caretBoundsProperty().getValue();
			if (!val.isPresent())
				return;
			Bounds pointer = val.get();
			popAuto.getContent().clear();
			popAuto.getContent().add(listSuggestions);
			listSuggestions.setOnMouseClicked(e -> replace.run());
			listSuggestions.setOnKeyPressed(e -> {
				if(e.getCode() == ENTER || e.getCode() == TAB) {
					replace.run();
				}
			});
			popAuto.show(codeArea, pointer.getMaxX(), pointer.getMinY());
		}).onUi().run();
	}

	private static List<String> suggest(ParseResult<RootAST> ast, String line) {
		String firstToken = Objects.requireNonNull(RegexUtil.getFirstWord(line));
		// Suggest opcodes
		if (!line.contains(" "))
			return OpcodeUtil.getInsnNames().stream()
					.filter(n -> n.startsWith(firstToken) && !firstToken.equals(n))
					.collect(Collectors.toList());
		// Create dummy AST if needed
		if (ast == null)
			ast = Parse.parse("");
		try {
			String lastToken = Objects.requireNonNull(RegexUtil.getLastWord(line));
			return Parse.getParser(-1, firstToken).suggest(ast, line).stream()
						.filter(option -> !lastToken.equals(option))
						.collect(Collectors.toList());
		} catch(Exception ex) {
			return Collections.emptyList();
		}
	}
}
