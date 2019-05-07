package me.coley.recaf.ui;

import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import jregex.Matcher;
import jregex.Pattern;
import me.coley.recaf.bytecode.OpcodeUtil;
import me.coley.recaf.bytecode.insn.NamedLabelNode;
import me.coley.recaf.parse.assembly.Assembler;
import me.coley.recaf.parse.assembly.AssemblyParseException;
import me.coley.recaf.parse.assembly.impl.*;
import me.coley.recaf.util.Icons;
import org.fxmisc.richtext.LineNumberFactory;
import org.objectweb.asm.tree.*;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;

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
		code.setParagraphGraphicFactory(LineNumberFactory.get(code));
	}

	@Override
	protected void setupSearch() {
		// Don't setup search bar
	}

	@Override
	protected void onCodeChange(String code) {
		String[] lines = code.split("\n");
		// Track current line to pre-pend to exceptions so we can tell which line we failed on.
		int currentLine = 0;
		try {
			// Parse opcodes of each line
			InsnList insns = new InsnList();
			for(int line = 0; line < lines.length; currentLine = ++line) {
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
					insns.add(insn);
				} else {
					throw new IllegalStateException("Unknown opcode type: " + optype);
				}
			}
			// Create map of named labels and populate the instruction with label instances
			Map<String, LabelNode> labels = NamedLabelNode.getLabels(insns.toArray());
			NamedLabelNode.setupLabels(labels, insns.toArray());
		} catch(IllegalStateException e) {
			System.err.println(currentLine + ": " + e.getMessage());
		} catch(UnsupportedOperationException e) {
			System.err.println(currentLine + ": " + e.getMessage());
		} catch(AssemblyParseException e) {
			System.err.println(currentLine + ": " + e.getMessage());
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
		assemblers.put(AbstractInsnNode.INSN, (op) -> new Insn(op));
		assemblers.put(AbstractInsnNode.JUMP_INSN, (op) -> new Jump(op));
		assemblers.put(AbstractInsnNode.VAR_INSN, (op) -> new Var(op));
		assemblers.put(AbstractInsnNode.FIELD_INSN, (op) -> new Field(op));
		assemblers.put(AbstractInsnNode.METHOD_INSN, (op) -> new Method(op));
		assemblers.put(AbstractInsnNode.INVOKE_DYNAMIC_INSN, (op) -> new InvokeDynamic(op));
		assemblers.put(AbstractInsnNode.LABEL, (op) -> new Label(op));
		assemblers.put(AbstractInsnNode.LINE, (op) -> new Line(op));
		assemblers.put(AbstractInsnNode.TYPE_INSN, (op) -> new Type(op));
		assemblers.put(AbstractInsnNode.MULTIANEWARRAY_INSN, (op) -> new MultiANewArray(op));
		assemblers.put(AbstractInsnNode.IINC_INSN, (op) -> new Iinc(op));
		assemblers.put(AbstractInsnNode.LDC_INSN, (op) -> new Ldc(op));
		assemblers.put(AbstractInsnNode.INT_INSN, (op) -> new Int(op));
		assemblers.put(AbstractInsnNode.TABLESWITCH_INSN, (op) -> new TableSwitch(op));
		assemblers.put(AbstractInsnNode.LOOKUPSWITCH_INSN, (op) -> new LookupSwitch(op));
	}
}
