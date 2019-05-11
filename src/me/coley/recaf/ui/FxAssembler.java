package me.coley.recaf.ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ListView;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.stage.Popup;
import javafx.stage.Stage;
import jregex.Matcher;
import jregex.Pattern;
import me.coley.recaf.bytecode.OpcodeUtil;
import me.coley.recaf.parse.assembly.AbstractAssembler;
import me.coley.recaf.parse.assembly.Assembly;
import me.coley.recaf.parse.assembly.exception.ExceptionWrapper;
import me.coley.recaf.parse.assembly.util.LineData;
import me.coley.recaf.util.Icons;
import me.coley.recaf.util.Threads;
import org.fxmisc.richtext.LineNumberFactory;
import org.objectweb.asm.Opcodes;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

import static javafx.scene.input.KeyCode.*;

public class FxAssembler extends FxCode {
	//@formatter:off
	private static final String KEYWORD_PATTERN = "\\b(" + String.join("|", OpcodeUtil.getInsnNames()) + ")\\b";
	private static final String STRING_PATTERN = "\"([^\"\\\\]|\\\\.)*\"";
	private static final String CONST_HEX_PATTERN = "(0[xX][0-9a-fA-F]+)+";
	private static final String CONST_VAL_PATTERN = "\\b[\\d_]+[\\._]?[\\d]?[dfljf]?\\b";
	private static final String CONST_PATTERN = CONST_HEX_PATTERN + "|" + CONST_VAL_PATTERN;
	private static final Pattern PATTERN = new Pattern(
			"({LABEL}\\b(LABEL[ ]+[-\\w]+)\\b)" +
			"|({KEYWORD}" + KEYWORD_PATTERN + ")" +
			"|({STRING}" + STRING_PATTERN + ")" +
			"|({CONSTPATTERN}" + CONST_PATTERN + ")");
	//@formatter:on
	private static final int ROW_HEIGHT = 24;
	private final SimpleListProperty<ExceptionWrapper> exceptions
			= new SimpleListProperty<>(FXCollections.observableArrayList());
	private final Popup popAuto = new Popup();


	public FxAssembler() {
		super();
		this.code.appendText(read("temp/test.txt"));
	}

	private static String read(String s) {
		try {
			return Files.readAllLines(Paths.get(s)).stream().reduce("", (a, b) -> a + "\n" + b);
		} catch(Exception e) {
			return "RETURN";
		}
	}

	@Override
	protected String createTitle() {
		return "AbstractAssembler";
	}

	@Override
	protected Image createIcon() {
		return Icons.LOGO;
	}

	@Override
	protected Pattern createPattern() {
		return PATTERN;
	}

	@Override
	protected String getStyleClass(Matcher matcher) {
		//@formatter:off
			return 	  matcher.group("STRING")       != null ? "string"
					: matcher.group("LABEL")        != null ? "annotation"
					: matcher.group("KEYWORD")      != null ? "keyword"
					: matcher.group("CONSTPATTERN") != null ? "const" : null;
		//@formatter:on
	}

	@Override
	protected void setupCodePane(String initialText) {
		super.setupCodePane(initialText);
		code.setEditable(true);
		// Add line numbers.
		IntFunction<Node> lineFactory = LineNumberFactory.get(code);
		IntFunction<Node> errorFactory = new ErrorIndicatorFactory();
		IntFunction<Node> decorationFactory = line -> {
			HBox hbox = new HBox(
					lineFactory.apply(line),
					errorFactory.apply(line));
			hbox.setAlignment(Pos.CENTER_LEFT);
			return hbox;
		};
		code.setParagraphGraphicFactory(decorationFactory);
		// Setup auto-complete
		code.setOnKeyReleased(e -> {
			// Update auto-complete on key-release except for certain non-modifying keys.
			KeyCode k = e.getCode();
			if (k == ESCAPE){
				// Remove popup if escape is pressed.
				popAuto.hide();
				return;
			}
			if (!popAuto.isShowing() && k == BACK_SPACE){
				// Don't show popup if backspacing onto some text.
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
			Threads.run(() -> updateAutoComplete());
		});
		code.setOnKeyTyped(e -> {
			// Ensure directional / input keys are sent to the popup.
			KeyCode k = e.getCode();
			if (popAuto.isShowing() && (k == UP || k == DOWN || k == ENTER || k == TAB)) {
				popAuto.requestFocus();
			}
		});
	}

	@Override
	protected void setupSearch() {
		super.setupSearch();
		// Disable the auto-display by mouse proximity
		pane.setTriggerDistance(-1);
	}

	@Override
	protected void onCodeChange(String code) {
		parseInstructions(code);
	}

	/**
	 * Updates the interpreted instructions.
	 * <br>
	 * <b>Note:</b> RichTextFX is conservative in redrawing paragraph graphics. It will only
	 * update lines have their content modified. For example, writing an instruction. This means
	 * writing bad exceptions will always trigger the graphic redraw, so no extra work is needed.
	 * However, for verification & other non-direct items errors may not be associated with edits
	 * to the line of the error... so some extra work is needed, thus we use
	 * {@link #forceUpdate()}.
	 *
	 * @param code
	 * 		Current updated text.
	 */
	private void parseInstructions(String code) {
		// Reset exceptions
		this.exceptions.clear();
		// Attempt to assemble instructions
		String[] lines = code.split("\n");
		Assembly asm = new Assembly();
		asm.setMethodDeclaration(Opcodes.ACC_PUBLIC, "temp", "()V");
		if (asm.parseInstructions(lines)) {
			// Success
		} else {
			// Failure
			this.exceptions.addAll(asm.getExceptions());
		}
		// Update exceptions to reset displayed exception icons
		forceUpdate();
	}

	/**
	 * Update code-completion prompt.
	 */
	private void updateAutoComplete() {
		if(popAuto == null)
			return;
		int position = code.getCaretPosition();
		int line = code.getCurrentParagraph();
		String lineText = code.getParagraph(line).getText();
		// Ensure that the caret is at the end of the line.
		if (code.getCaretColumn() < lineText.length()) {
			return;
		}
		List<String> suggestions = null;
		// Check for opcode matching
		if (lineText.matches("^\\w+$")) {
			// Populate by matching opcode names
			suggestions = OpcodeUtil.getInsnNames().stream()
					.filter(op -> op.toUpperCase().startsWith(lineText.toUpperCase()) && !op.equalsIgnoreCase(lineText))
					.sorted(Comparator.naturalOrder())
					.collect(Collectors.toList());
		} else {
			// Instruction arg matching
			try {
				LineData lineData = LineData.from(lineText);
				if (lineData == null)
					return;
				String opText = lineData.optext;
				int opcode = lineData.opcode;
				int type = lineData.type;
				AbstractAssembler assembler = Assembly.getAssembler(type, opcode);
				if(assembler != null) {
					String args = lineText.substring(opText.length()).trim();
					suggestions = assembler.suggest(args);
				}
			} catch(Exception e) {
				// If we fail, don't suggest anything
				return;
			}
		}
		// No suggestions? Do nothing
		if (suggestions == null || suggestions.isEmpty()) {
			return;
		}
		// Limit capacity
		suggestions = suggestions.stream().limit(7).collect(Collectors.toList());
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
		Runnable r = () -> {
			String selected = listSuggestions.getSelectionModel().getSelectedItem();
			Platform.runLater(() -> {
				code.replaceText(position - curWord.length(), position, selected);
				code.moveTo(position + selected.length() - curWord.length());
			});
			popAuto.hide();
		};
		Bounds pointer = code.caretBoundsProperty().getValue().get();
		Threads.runFx(() -> {
			popAuto.getContent().clear();
			popAuto.getContent().add(listSuggestions);
			listSuggestions.setOnMouseClicked(e -> r.run());
			listSuggestions.setOnKeyPressed(e -> {
				if(e.getCode() == ENTER || e.getCode() == TAB) {
					r.run();
				}
			});
			popAuto.show(code, pointer.getMaxX(), pointer.getMinY());
		});
	}

	/**
	 * RichTextFX hack to redraw line graphics.
	 */
	private void forceUpdate() {
		IntFunction<Node> va = (IntFunction<Node>) code.getParagraphGraphicFactory();
		code.setParagraphGraphicFactory(null);
		code.setParagraphGraphicFactory(va);
	}

	/**
	 * Decorator factory for building error indicators.
	 */
	class ErrorIndicatorFactory implements IntFunction<Node> {
		private final double[] shape = new double[] {0, 5, 2.5, 0, 7.5, 0, 10, 5, 7.5, 10, 2.5, 10};

		@Override
		public Node apply(int lineNo) {
			Polygon triangle = new Polygon(shape);
			triangle.getStyleClass().add("cursor-pointer");
			triangle.setFill(Color.RED);
			if(exceptions == null || exceptions.isEmpty()) {
				triangle.setVisible(false);
				return triangle;
			}
			Optional<ExceptionWrapper> exx = exceptions.stream()
					.filter(ex -> ex.line == (lineNo + 1)).findFirst();
			triangle.visibleProperty().setValue(exx.isPresent());
			if(exx.isPresent()) {
				Tooltip t = new Tooltip(exx.get().exception.getMessage());
				Tooltip.install(triangle, t);
			}
			return triangle;
		}
	}

	/**
	 * Wrapper so I can quickly launch/test this.
	 */
	public static class TestWrapper extends Application {
		public static void main(String[] a) {
			Application.launch(a);
		}

		@Override
		public void start(Stage s) throws Exception {
			s = new FxAssembler();
			s.setMinWidth(300);
			s.setMinHeight(300);
			s.show();
		}
	}
}
