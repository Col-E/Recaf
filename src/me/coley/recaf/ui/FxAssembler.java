package me.coley.recaf.ui;

import javafx.application.Application;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.stage.Stage;
import jregex.Matcher;
import jregex.Pattern;
import me.coley.recaf.bytecode.OpcodeUtil;
import me.coley.recaf.bytecode.insn.NamedLabelNode;
import me.coley.recaf.parse.assembly.Assembler;
import me.coley.recaf.parse.assembly.LabelLinkageException;
import me.coley.recaf.parse.assembly.impl.*;
import me.coley.recaf.util.Icons;
import org.fxmisc.richtext.LineNumberFactory;
import org.objectweb.asm.tree.*;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.function.IntFunction;

import static org.objectweb.asm.tree.AbstractInsnNode.*;

public class FxAssembler extends FxCode {
	//@formatter:off
	private static final String KEYWORD_PATTERN = "\\b(" + String.join("|", OpcodeUtil.getInsnNames()) + ")\\b";
	private static final String STRING_PATTERN = "\"([^\"\\\\]|\\\\.)*\"";
	private static final String CONST_HEX_PATTERN = "(0[xX][0-9a-fA-F]+)+";
	private static final String CONST_VAL_PATTERN = "\\b[\\d_]+[\\._]?[\\d]?[dfljf]?\\b";
	private static final String CONST_PATTERN = CONST_HEX_PATTERN + "|" + CONST_VAL_PATTERN;
	private static final Pattern PATTERN = new Pattern(
			"({KEYWORD}" + KEYWORD_PATTERN + ")" +
			"|({STRING}" + STRING_PATTERN + ")" +
			"|({CONSTPATTERN}" + CONST_PATTERN + ")");
	//@formatter:on
	private static final Pattern P_OPCODE = new Pattern("^\\w+(?=\\s*)");
	private static final Map<Integer, Function<Integer, Assembler>> assemblers = new HashMap<>();
	//
	private final SimpleListProperty<ExceptionWrapper> exceptions
			= new SimpleListProperty<>(FXCollections.observableArrayList());


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
	 *
	 * @param code
	 * 		Current updated text.
	 */
	private void parseInstructions(String code) {
		String[] lines = code.split("\n");
		// Track current line to pre-pend to exceptions so we can tell which line we failed on.
		int currentLine = 0;
		// Reset error tracking
		resetTrackedErrors();
		// Parse opcodes of each line
		Map<AbstractInsnNode, Integer> insnToLine = new HashMap<>();
		InsnList insns = new InsnList();
		for(int line = 0; line < lines.length; currentLine = ++line) {
			try {
				String lineText = lines[line];
				Matcher m = P_OPCODE.matcher(lineText);
				m.find();
				String opMatch = m.group(0);
				// Line must be empty
				if(opMatch == null)
					continue;
				String op = opMatch.toUpperCase();
				// Get opcode / opcode-type
				int opcode = -1;
				try {
					opcode = OpcodeUtil.nameToOpcode(op);
				} catch(Exception e) {
					throw new IllegalStateException("Unknown opcode: " + op);
				}
				int optype = -1;
				try {
					optype = OpcodeUtil.opcodeToType(opcode);
				} catch(Exception e) {
					throw new IllegalStateException("Unknown group for opcode: " + op);
				}
				// Get assembler for opcode and attempt to assemble the instruction
				Function<Integer, Assembler> func = assemblers.get(optype);
				if(func != null) {
					Assembler matcher = func.apply(opcode);
					String args = lineText.substring(op.length()).trim();
					if(matcher == null)
						throw new UnsupportedOperationException("Missing assembler for: " + op);
					AbstractInsnNode insn = matcher.parse(args);
					if(insn == null)
						throw new UnsupportedOperationException("Unfinished ssembler for: " + op);
					insnToLine.put(insn, currentLine);
					insns.add(insn);
				} else {
					throw new IllegalStateException("Unknown opcode type: " + optype);
				}
			} catch(Exception e) {
				addTrackedError(currentLine, e);
			}
		}
		try {
			// Create map of named labels and populate the instruction with label instances
			Map<String, LabelNode> labels = NamedLabelNode.getLabels(insns.toArray());
			NamedLabelNode.setupLabels(labels, insns.toArray());
		} catch(LabelLinkageException e) {
			int line = insnToLine.getOrDefault(e.getInsn(), -1);
			addTrackedError(line, e);
		}
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
	 * Decorator factory for building error indicators.
	 */
	class ErrorIndicatorFactory implements IntFunction<Node> {
		@Override
		public Node apply(int lineNo) {
			Polygon triangle = new Polygon(0, 5, 2.5, 0, 7.5, 0, 10, 5, 7.5, 10, 2.5, 10);
			triangle.getStyleClass().add("cursor-pointer");
			triangle.setFill(Color.RED);
			Optional<ExceptionWrapper> exx = exceptions.stream().filter(ex -> ex.line == lineNo).findFirst();
			triangle.visibleProperty().setValue(exx.isPresent());
			if(exx.isPresent()) {
				Tooltip t = new Tooltip(exx.get().exception.getMessage());
				Tooltip.install(triangle, t);
			}
			return triangle;
		}
	}
}
