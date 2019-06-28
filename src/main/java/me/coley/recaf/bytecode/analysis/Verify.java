package me.coley.recaf.bytecode.analysis;

import javafx.scene.image.Image;
import jregex.Matcher;
import jregex.Pattern;
import me.coley.recaf.Input;
import me.coley.recaf.Logging;
import me.coley.recaf.ui.FxCode;
import me.coley.recaf.util.Icons;
import me.coley.recaf.util.Lang;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.PrintWriter;
import java.io.StringWriter;

public class Verify {
	//@formatter:off
	private static final String[] KEYWORDS = new String[] { "R", "I", "J", "F", "D"};
	private static final String[] OPCODES = new String[] { "AALOAD", "AASTORE", "ACONST_NULL", "ARETURN", "ARRAYLENGTH", "ATHROW",
			"BALOAD", "BASTORE", "CALOAD", "CASTORE", "D2F", "D2I", "D2L", "DADD", "DALOAD", "DASTORE", "DCMPG", "DCMPL",
			"DCONST_0", "DCONST_1", "DDIV", "DMUL", "DNEG", "DREM", "DRETURN", "DSUB", "DUP", "DUP2", "DUP2_X1", "DUP2_X2",
			"DUP_X1", "DUP_X2", "F2D", "F2I", "F2L", "FADD", "FALOAD", "FASTORE", "FCMPG", "FCMPL", "FCONST_0", "FCONST_1",
			"FCONST_2", "FDIV", "FMUL", "FNEG", "FREM", "FRETURN", "FSUB", "I2B", "I2C", "I2D", "I2F", "I2L", "I2S", "IADD",
			"IALOAD", "IAND", "IASTORE", "ICONST_0", "ICONST_1", "ICONST_2", "ICONST_3", "ICONST_4", "ICONST_5", "ICONST_M1",
			"IDIV", "IMUL", "INEG", "IOR", "IREM", "IRETURN", "ISHL", "ISHR", "ISUB", "IUSHR", "IXOR", "L2D", "L2F", "L2I",
			"LADD", "LALOAD", "LAND", "LASTORE", "LCMP", "LCONST_0", "LCONST_1", "LDIV", "LMUL", "LNEG", "LOR", "LREM", "LRETURN",
			"LSHL", "LSHR", "LSUB", "LUSHR", "LXOR", "MONITORENTER", "MONITOREXIT", "NOP", "POP", "POP2", "RETURN", "SALOAD",
			"SASTORE", "SWAP", "BIPUSH", "SIPUSH", "NEWARRAY", "ALOAD", "ASTORE", "DLOAD", "DSTORE", "FLOAD", "FSTORE", "ILOAD",
			"ISTORE", "LLOAD", "LSTORE", "RET", "ANEWARRAY", "CHECKCAST", "INSTANCEOF", "NEW", "GETSTATIC", "PUTSTATIC",
			"GETFIELD", "PUTFIELD", "INVOKEVIRTUAL", "INVOKESPECIAL", "INVOKESTATIC", "INVOKEINTERFACE", "INVOKEDYNAMIC" ,
			"GOTO", "IF_ACMPEQ", "IF_ACMPNE", "IF_ICMPEQ", "IF_ICMPGE", "IF_ICMPGT", "IF_ICMPLE", "IF_ICMPLT", "IF_ICMPNE",
			"IFEQ", "IFGE", "IFGT", "IFLE", "IFLT", "IFNE", "IFNONNULL", "IFNULL", "JSR", "LDC", "IINC", "TABLESWITCH",
			"LOOKUPSWITCH", "F_NEW", "F_FULL", "F_APPEND", "F_CHOP", "F_SAME", "F_APPEND", "F_SAME1", "LINENUMBER" };
	//@formatter:on
	private static final String KEYWORD_PATTERN = "\\b(" + String.join("|", KEYWORDS) + ")\\b";
	private static final String OPCODE_PATTERN = "\\b(" + String.join("|", OPCODES) + ")\\b";
	private static final String LINE_PREFIX_GOOD_PATTERN = "([0-9]{5})(?= \\w)";
	private static final String LINE_PREFIX_ERRR_PATTERN = "([0-9]{5})(?= \\?)";
	private static final String CONST_VAL_PATTERN = "(\\b([\\d._]*[\\d])\\b)";
	private static final String LINENUM_PATTERN = "L[0-9]+";
	//@formatter:off
	private static final Pattern PATTERN = new Pattern(
			 "({OPCODE}" + OPCODE_PATTERN + ")" +
			"|({KEYWORD}" + KEYWORD_PATTERN + ")" +
			"|({GOODPREFIX}" + LINE_PREFIX_GOOD_PATTERN + ")" +
			"|({ERRRPREFIX}" + LINE_PREFIX_ERRR_PATTERN + ")" +
			"|({LINENUM}" + LINENUM_PATTERN + ")" +
			"|({CONST}" + CONST_VAL_PATTERN + ")");
	//@formatter:on

	/**
	 * Verify correctness of a MethodNode.
	 * 
	 * @param method
	 *            The MethodNode to check.
	 * @return Check if this method has passed verification.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static VerifyResults checkValid(String owner, MethodNode method) {
		Exception ex = null;
		try {
			new Analyzer(new BasicVerifier()).analyze(owner, method);
		} catch(AnalyzerException e) {
			// Thrown on failure.
			ex = e;
		} catch(IndexOutOfBoundsException e) {
			// Thrown when local variables are messed up.
			ex = e;
		} catch(Exception e) {
			// Unknown origin
			Logging.error(e);
		}
		return new VerifyResults(ex);
	}

	/**
	 * Verify correctness of a ClassNode. Since this is expected to be used in a
	 * wide-scope, this does not gather extra information about errors that
	 * {@link #checkValid(String,MethodNode)} would.
	 * 
	 * @param clazz
	 *            The ClassNode to check.
	 * @return Check if this class has passed verification.
	 */
	public static VerifyResults checkValid(ClassNode clazz) {
		StringWriter sw = new StringWriter();
		Exception ex = null;
		try {
			PrintWriter printer = new PrintWriter(sw);
			TraceClassVisitor trace = new TraceClassVisitor(printer);
			CheckClassAdapter check = new CheckClassAdapter(trace);
			clazz.accept(check);
		} catch (IllegalArgumentException e) {
			// Thrown by CheckClassAdapter
			ex = e;
		} catch (Exception e) {
			// Unknown origin
			Logging.error(e);
		}
		return new VerifyResults(ex, sw.toString());
	}

	/**
	 * @return Check if the current Input instance is valid.
	 */
	public static boolean isValid() {
		try {
			Input in = Input.get();
			for (String name : in.getModifiedClasses()) {
				VerifyResults res = checkValid(in.getClass(name));
				if (!res.valid()) {
					res.showWindow();
					return false;
				}
			}
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	public static class VerifyResults {
		/**
		 * Exception thrown by verifier. {@code null} if verification was a
		 * success <i>(see {@link #valid()})</i>.
		 */
		public final Exception ex;

		private final String content;

		public VerifyResults(Exception ex, String content) {
			this.ex = ex;
			this.content = content;
		}

		public VerifyResults(Exception ex) {
			this(ex, ex == null ? "" : ex.toString());
		}

		public void showWindow() {
			new FxVerify(ex.getMessage()).show();
		}

		/**
		 * @return Cause of verification failure.
		 */
		public AbstractInsnNode getCause() {
			if (ex instanceof AnalyzerException) {
				return ((AnalyzerException) ex).node;
			}
			return null;
		}

		/**
		 * @return {@code true} if no exception was raised by
		 */
		public boolean valid() {
			return ex == null;
		}

		@Override
		public String toString() {
			return content;
		}
	}

	public static class FxVerify extends FxCode {
		protected FxVerify(String initialText) {
			super();
			setInitialText(initialText);
		}

		@Override
		protected String createTitle() {
			return Lang.get("ui.verify");
		}

		@Override
		protected Image createIcon() {
			return Icons.L_ERROR;
		}

		@Override
		protected Pattern createPattern() {
			return PATTERN;
		}

		@Override
		protected String getStyleClass(Matcher matcher) {
			//@formatter:off
			return  matcher.group("OPCODE")     != null ? "ver-op"
					: matcher.group("KEYWORD")    != null ? "ver-key"
					: matcher.group("GOODPREFIX") != null ? "ver-pre-good"
					: matcher.group("ERRRPREFIX") != null ? "ver-pre-err"
					: matcher.group("LINENUM")    != null ? "ver-line"
					: matcher.group("CONST")      != null ? "ver-const" : null;
			//@formatter:on
		}

		@Override
		protected void onCodeChange(String code) {}
	}
}
