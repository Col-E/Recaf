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
import me.coley.recaf.bytecode.insn.NamedLabelNode;
import me.coley.recaf.parse.assembly.Assembler;
import me.coley.recaf.parse.assembly.LabelLinkageException;
import me.coley.recaf.parse.assembly.impl.*;
import me.coley.recaf.util.Icons;
import me.coley.recaf.util.Threads;
import org.fxmisc.flowless.Cell;
import org.fxmisc.flowless.VirtualFlow;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.ViewActions;
import org.objectweb.asm.tree.*;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

import static org.objectweb.asm.tree.AbstractInsnNode.*;
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
	private static final Pattern P_OPCODE = new Pattern("^\\w+(?=\\s*)");
	private static final Map<Integer, Function<Integer, Assembler>> assemblers = new HashMap<>();
	private static final int ROW_HEIGHT = 24;

	//
	private final SimpleListProperty<ExceptionWrapper> exceptions
			= new SimpleListProperty<>(FXCollections.observableArrayList());
	//
	private final Popup popAuto = new Popup();


	public FxAssembler() {
		super(read("temp/test.txt"));
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
		return "Assembler";
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
			if (k != PERIOD && (k.isArrowKey() || k.isModifierKey() || k.isWhitespaceKey())) {
				if (k == SPACE)
					popAuto.hide();
				return;
			}
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
		// TODO: Abstract away to allow the assembler to be its own API, rather than UI-tethered.
		String[] lines = code.split("\n");
		// Track current line to pre-pend to exceptions so we can tell which line we failed on.
		int currentLine = 1;
		// Reset error tracking
		resetTrackedErrors();
		// Parse opcodes of each line
		Map<AbstractInsnNode, Integer> insnToLine = new HashMap<>();
		InsnList insns = new InsnList();
		for(int i = 0; i < lines.length; currentLine = (++i) + 1) {
			try {
				String lineText = lines[i];
				LineData lineData = LineData.from(lineText);
				if (lineData == null)
					continue;
				String opText = lineData.optext;
				int opcode = lineData.opcode;
				int type = lineData.type;
				// Get assembler for opcode and attempt to assemble the instruction
				Function<Integer, Assembler> func = assemblers.get(type);
				if(func != null) {
					Assembler assembler = func.apply(opcode);
					if(assembler == null)
						throw new UnsupportedOperationException("Missing assembler for: " + opText);
					String args = lineText.substring(opText.length()).trim();
					AbstractInsnNode insn = assembler.parse(args);
					if(insn == null)
						throw new UnsupportedOperationException("Unfinished ssembler for: " + opText);
					insnToLine.put(insn, currentLine);
					insns.add(insn);
				} else {
					throw new IllegalStateException("Unknown opcode type: " + type);
				}
			} catch(Exception e) {
				addTrackedError(currentLine, e);
			}
		}
		try {
			// Create map of named labels and populate the instruction with label instances
			Map<String, LabelNode> labels = NamedLabelNode.getLabels(insns.toArray());
			NamedLabelNode.setupLabels(labels, insns.toArray());
			// Replace serialization-intended named instructions with standard instances
			Map<LabelNode, LabelNode> replace = new HashMap<>();
			for (LabelNode value : labels.values())
				replace.put(value, new LabelNode());
			insns = NamedLabelNode.clean(replace, insns);
			/* TODO: Make a virtual method to properly support local variables and such, for full assembled methods
			MethodNode method = null;
			String s = FormatFactory.opcodeCollectionString(Arrays.asList(insns.toArray()), method);
			System.out.println(s);
			*/
		} catch(LabelLinkageException e) {
			int line = insnToLine.getOrDefault(e.getInsn(), -1);
			addTrackedError(line, e);
		}
		// Redraw line graphics.
		// Ineffecient since ALL displayed lines are redrawn, but it works.
		// Messing with the underlying VirtualFlow is a hassle.
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
				Function<Integer, Assembler> func = assemblers.get(type);
				if(func != null) {
					Assembler assembler = func.apply(opcode);
					if(assembler != null) {
						String args = lineText.substring(opText.length()).trim();
						suggestions = assembler.suggest(args);
					}
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
	 * Update UI with last occurring error.
	 * @param line Line that the error occured on.
	 * @param e Exception that occured.
	 */
	private void addTrackedError(int line, Exception e) {
		if (exceptions != null) {
			exceptions.add(new ExceptionWrapper(line, e));
		}
	}

	/**
	 * Clears tracked errors from the UI.
	 */
	private void resetTrackedErrors() {
		if (exceptions != null) {
			exceptions.clear();
		}
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

	static {
		assemblers.put(INSN, Insn::new);
		assemblers.put(JUMP_INSN, Jump::new);
		assemblers.put(VAR_INSN, Var::new);
		assemblers.put(FIELD_INSN, Field::new);
		assemblers.put(METHOD_INSN, Method::new);
		assemblers.put(INVOKE_DYNAMIC_INSN, InvokeDynamic::new);
		assemblers.put(LABEL, Label::new);
		assemblers.put(LINE, Line::new);
		assemblers.put(TYPE_INSN, Type::new);
		assemblers.put(MULTIANEWARRAY_INSN, MultiANewArray::new);
		assemblers.put(IINC_INSN, Iinc::new);
		assemblers.put(LDC_INSN, Ldc::new);
		assemblers.put(INT_INSN, Int::new);
		assemblers.put(TABLESWITCH_INSN, TableSwitch::new);
		assemblers.put(LOOKUPSWITCH_INSN, LookupSwitch::new);
	}

	/**
	 * Wrapper for assembler exceptions with the lines that caused them.
	 */
	static class ExceptionWrapper {
		private final int line;
		private final Exception exception;

		public ExceptionWrapper(int line, Exception exception) {
			this.line = line;
			this.exception = exception;
		}
	}

	/**
	 * Wrapper for opcode information of a line of text.
	 */
	static class LineData {
		private final String optext;
		private final int opcode;
		private final int type;

		private LineData( String optext, int opcode, int type) {
			this.optext =optext;
			this.opcode = opcode;
			this.type = type;
		}

		public static LineData from(String lineText) {
			Matcher m = P_OPCODE.matcher(lineText);
			m.find();
			String opMatch = m.group(0);
			// Line must be empty
			if(opMatch == null)
				return null;
			String opText = opMatch.toUpperCase();
			// Get opcode / opcode-type
			int opcode;
			try {
				opcode = OpcodeUtil.nameToOpcode(opText);
			} catch(Exception e) {
				throw new IllegalStateException("Unknown opcode: " + opText);
			}
			int optype;
			try {
				optype = OpcodeUtil.opcodeToType(opcode);
			} catch(Exception e) {
				throw new IllegalStateException("Unknown group for opcode: " + opText);
			}
			return new LineData(opText, opcode, optype);
		}
	}

	/**
	 * Decorator factory for building error indicators.
	 */
	class ErrorIndicatorFactory implements IntFunction<Node> {
		@Override
		public Node apply(int lineNo) {
			Polygon triangle = new Polygon(0, 5, 2.5, 0, 7.5, 0, 10, 5, 7.5, 10, 2.5, 10);
			triangle.getStyleClass().add("cursor-pointer");
			triangle.setFill(Color.RED);
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
}
