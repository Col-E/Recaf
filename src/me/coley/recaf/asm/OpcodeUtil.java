package me.coley.recaf.asm;

import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;

public class OpcodeUtil implements Opcodes {
	private static Map<Integer, String> opcodeToName = new HashMap<>();
	private static Map<String, Integer> nameToOpcode = new HashMap<>();
	private static Map<Integer, String> frameToName = new HashMap<>();
	private static Map<String, Integer> nameToFrame = new HashMap<>();
	private static Map<Integer, String> tagToName = new HashMap<>();
	private static Map<String, Integer> nameToTag = new HashMap<>();
	private static Map<Integer, String[]> insnTypeToCodes = new HashMap<>();
	/**
	 * Opcodes of INSN type.
	 */
	public static String[] OPS_INSN = new String[] { "NOP", "ACONST_NULL", "ICONST_M1", "ICONST_0", "ICONST_1", "ICONST_2",
			"ICONST_3", "ICONST_4", "ICONST_5", "LCONST_0", "LCONST_1", "FCONST_0", "FCONST_1", "FCONST_2", "DCONST_0",
			"DCONST_1", "IALOAD", "LALOAD", "FALOAD", "DALOAD", "AALOAD", "BALOAD", "CALOAD", "SALOAD", "IASTORE", "LASTORE",
			"FASTORE", "DASTORE", "AASTORE", "BASTORE", "CASTORE", "SASTORE", "POP", "POP2", "DUP", "DUP_X1", "DUP_X2", "DUP2",
			"DUP2_X1", "DUP2_X2", "SWAP", "IADD", "LADD", "FADD", "DADD", "ISUB", "LSUB", "FSUB", "DSUB", "IMUL", "LMUL", "FMUL",
			"DMUL", "IDIV", "LDIV", "FDIV", "DDIV", "IREM", "LREM", "FREM", "DREM", "INEG", "LNEG", "FNEG", "DNEG", "ISHL",
			"LSHL", "ISHR", "LSHR", "IUSHR", "LUSHR", "IAND", "LAND", "IOR", "LOR", "IXOR", "LXOR", "I2L", "I2F", "I2D", "L2I",
			"L2F", "L2D", "F2I", "F2L", "F2D", "D2I", "D2L", "D2F", "I2B", "I2C", "I2S", "LCMP", "FCMPL", "FCMPG", "DCMPL",
			"DCMPG", "IRETURN", "LRETURN", "FRETURN", "DRETURN", "ARETURN", "RETURN", "ARRAYLENGTH", "ATHROW", "MONITORENTER",
			"MONITOREXIT"
												   };
	/**
	 * Subset of {@link #OPS_INSN} for constants.
	 */
	public static String[] OPS_INSN_SUB_CONSTS = new String[] { "ACONST_NULL", "ICONST_M1", "ICONST_0", "ICONST_1", "ICONST_2",
			"ICONST_3", "ICONST_4", "ICONST_5", "LCONST_0", "LCONST_1", "FCONST_0", "FCONST_1", "FCONST_2", "DCONST_0",
			"DCONST_1"
															  };
	/**
	 * Subset of {@link #OPS_INSN} for array loads/saves/etc.
	 */
	public static String[] OPS_INSN_SUB_ARRAY = new String[] { "IALOAD", "LALOAD", "FALOAD", "DALOAD", "AALOAD", "BALOAD",
			"CALOAD", "SALOAD", "IASTORE", "LASTORE", "FASTORE", "DASTORE", "AASTORE", "BASTORE", "CASTORE", "SASTORE",
			"ARRAYLENGTH"
															 };
	/**
	 * Subset of {@link #OPS_INSN} for stack management.
	 */
	public static String[] OPS_INSN_SUB_STACK = new String[] { "POP", "POP2", "DUP", "DUP_X1", "DUP_X2", "DUP2", "DUP2_X1",
			"DUP2_X2", "SWAP"
															 };
	/**
	 * Subset of {@link #OPS_INSN} for math handling.
	 */
	public static String[] OPS_INSN_SUB_MATH = new String[] { "IADD", "LADD", "FADD", "DADD", "ISUB", "LSUB", "FSUB", "DSUB",
			"IMUL", "LMUL", "FMUL", "DMUL", "IDIV", "LDIV", "FDIV", "DDIV", "IREM", "LREM", "FREM", "DREM", "INEG", "LNEG",
			"FNEG", "DNEG", "ISHL", "LSHL", "ISHR", "LSHR", "IUSHR", "LUSHR", "IAND", "LAND", "IOR", "LOR", "IXOR"
															};
	/**
	 * Subset of {@link #OPS_INSN} for type conversion.
	 */
	public static String[] OPS_INSN_SUB_CONVERT = new String[] { "I2L", "I2F", "I2D", "L2I", "L2F", "L2D", "F2I", "F2L", "F2D",
			"D2I", "D2L", "D2F", "I2B", "I2C", "I2S"
															   };
	/**
	 * Subset of {@link #OPS_INSN} for primitve comparisons.
	 */
	public static String[] OPS_INSN_SUB_COMPARE = new String[] { "LCMP", "FCMPL", "FCMPG", "DCMPL", "DCMPG" };
	/**
	 * Subset of {@link #OPS_INSN} for returns.
	 */
	public static String[] OPS_INSN_SUB_RETURN = new String[] { "IRETURN", "LRETURN", "FRETURN", "DRETURN", "ARETURN", "RETURN" };
	/**
	 * Subset of {@link #OPS_INSN} for monitors.
	 */
	public static String[] OPS_INSN_SUB_MONITOR = new String[] { "MONITORENTER", "MONITOREXIT" };
	/**
	 * Subset of {@link #OPS_INSN} for exceptions.
	 */
	public static String[] OPS_INSN_SUB_EXCEPTION = new String[] { "ATHROW" };
	/**
	 * Opcodes of INT type.
	 */
	public static String[] OPS_INT = new String[] { "BIPUSH", "SIPUSH", "NEWARRAY" };
	/**
	 * Opcodes of INT type.
	 */
	public static String[] OPS_VAR = new String[] { "ILOAD", "LLOAD", "FLOAD", "DLOAD", "ALOAD", "ISTORE", "LSTORE", "FSTORE",
			"DSTORE", "ASTORE", "RET"
												  };
	/**
	 * Opcodes of TYPE type.
	 */
	public static String[] OPS_TYPE = new String[] { "NEW", "ANEWARRAY", "CHECKCAST", "INSTANCEOF" };
	/**
	 * Opcodes of FIELD type.
	 */
	public static String[] OPS_FIELD = new String[] { "GETSTATIC", "PUTSTATIC", "GETFIELD", "PUTFIELD" };
	/**
	 * Opcodes of METHOD type.
	 */
	public static String[] OPS_METHOD = new String[] { "INVOKEVIRTUAL", "INVOKESPECIAL", "INVOKESTATIC", "INVOKEINTERFACE" };
	/**
	 * Opcodes of INDY_METHOD type.
	 */
	public static String[] OPS_INDY_METHOD = new String[] { "INVOKEDYNAMIC" };
	/**
	 * Opcodes of JUMP type.
	 */
	public static String[] OPS_JUMP = new String[] { "IFEQ", "IFNE", "IFLT", "IFGE", "IFGT", "IFLE", "IF_ICMPEQ", "IF_ICMPNE",
			"IF_ICMPLT", "IF_ICMPGE", "IF_ICMPGT", "IF_ICMPLE", "IF_ACMPEQ", "IF_ACMPNE", "GOTO", "JSR", "IFNULL", "IFNONNULL"
												   };
	/**
	 * Opcodes of LDC type.
	 */
	public static String[] OPS_LDC = new String[] { "LDC" };
	/**
	 * Opcodes of IINC type.
	 */
	public static String[] OPS_IINC = new String[] { "IINC" };
	/**
	 * Opcodes of TABLESWITCH type.
	 */
	public static String[] OPS_TABLESWITCH = new String[] { "TABLESWITCH" };
	/**
	 * Opcodes of LOOKUPSWITCH type.
	 */
	public static String[] OPS_LOOKUPSWITCH = new String[] { "LOOKUPSWITCH" };
	/**
	 * Opcodes of LOOKUPSWITCH type.
	 */
	public static String[] OPS_MULTIANEWARRAY = new String[] { "MULTIANEWARRAY" };
	/**
	 * Opcodes of FRAME type.
	 */
	public static String[] OPS_FRAME = new String[] { "F_NEW", "F_FULL", "F_APPEND", "F_CHOP", "F_SAME", "F_APPEND", "F_SAME1" };
	/**
	 * Empty list.
	 */
	public static String[] OPS_TAG = new String[] { "H_GETFIELD", "H_GETSTATIC", "H_PUTFIELD", "H_PUTSTATIC", "H_INVOKEINTERFACE",
			"H_INVOKESPECIAL", "H_INVOKESTATIC", "H_INVOKEVIRTUAL", "H_NEWINVOKESPECIAL"
												  };
	/**
	 * Opcodes of LABEL type. Also see {@link #OPS_FRAME}[0].
	 */
	public static String[] OPS_LABEL = new String[] { "F_NEW" };
	/**
	 * Opcodes of LABEL type. Also see {@link #OPS_FRAME}[0].
	 */
	public static String[] OPS_LINE = new String[] { "F_NEW" };
	/**
	 * Empty list.
	 */
	public static String[] OPS_NONE = new String[] {};

	public static int nameToOpcode(String name) {
		return nameToOpcode.get(name);
	}

	public static String opcodeToName(int op) {
		return opcodeToName.get(op);
	}

	public static int nameToFrame(String name) {
		return nameToFrame.get(name);
	}

	public static String frameToName(int op) {
		return frameToName.get(op);
	}

	public static int nameToTag(String tag) {
		return nameToTag.get(tag);
	}

	public static String tagToName(int tag) {
		return tagToName.get(tag);
	}

	public static String[] typeToCodes(int type) {
		return insnTypeToCodes.get(type);
	}

	/**
	 * Return smaller subset of the {@link #OPS_INSN} string array.
	 *
	 * @param name The name of the target array.
	 * @return The desired subset.
	 *
	 * TODO: I have no idea what this is, just filling in some stubs
	 * so JavaDoc is happy.
	 *
	 *	- Charles
	 */
	public static String[] getInsnSubset(String name) {
		//@formatter:off
		String[][] arrays = new String[][] {
			OpcodeUtil.OPS_INSN_SUB_ARRAY,
			OpcodeUtil.OPS_INSN_SUB_COMPARE,
			OpcodeUtil.OPS_INSN_SUB_CONSTS,
			OpcodeUtil.OPS_INSN_SUB_CONVERT,
			OpcodeUtil.OPS_INSN_SUB_EXCEPTION,
			OpcodeUtil.OPS_INSN_SUB_MATH,
			OpcodeUtil.OPS_INSN_SUB_MONITOR,
			OpcodeUtil.OPS_INSN_SUB_RETURN,
			OpcodeUtil.OPS_INSN_SUB_STACK
		};
		//@formatter:on
		for (String[] array : arrays) {
			if (contains(array, name)) {
				return array;
			}
		}
		// Just return an empty array
		return OPS_NONE;
	}

	public static boolean contains(String[] array, String key) {
		for (String value : array) {
			if (value.equals(key)) {
				return true;
			}
		}
		return false;
	}

	private static void put(int op, String text) {
		nameToOpcode.put(text, op);
		opcodeToName.put(op, text);
	}

	private static void putFrame(int op, String text) {
		nameToFrame.put(text, op);
		frameToName.put(op, text);
	}

	private static void putTag(int op, String text) {
		nameToTag.put(text, op);
		tagToName.put(op, text);
	}

	static {
		put(F_NEW, "F_NEW");
		put(NOP, "NOP");
		put(ACONST_NULL, "ACONST_NULL");
		put(ICONST_M1, "ICONST_M1");
		put(ICONST_0, "ICONST_0");
		put(ICONST_1, "ICONST_1");
		put(ICONST_2, "ICONST_2");
		put(ICONST_3, "ICONST_3");
		put(ICONST_4, "ICONST_4");
		put(ICONST_5, "ICONST_5");
		put(LCONST_0, "LCONST_0");
		put(LCONST_1, "LCONST_1");
		put(FCONST_0, "FCONST_0");
		put(FCONST_1, "FCONST_1");
		put(FCONST_2, "FCONST_2");
		put(DCONST_0, "DCONST_0");
		put(DCONST_1, "DCONST_1");
		put(BIPUSH, "BIPUSH");
		put(SIPUSH, "SIPUSH");
		put(LDC, "LDC");
		put(ILOAD, "ILOAD");
		put(LLOAD, "LLOAD");
		put(FLOAD, "FLOAD");
		put(DLOAD, "DLOAD");
		put(ALOAD, "ALOAD");
		put(IALOAD, "IALOAD");
		put(LALOAD, "LALOAD");
		put(FALOAD, "FALOAD");
		put(DALOAD, "DALOAD");
		put(AALOAD, "AALOAD");
		put(BALOAD, "BALOAD");
		put(CALOAD, "CALOAD");
		put(SALOAD, "SALOAD");
		put(ISTORE, "ISTORE");
		put(LSTORE, "LSTORE");
		put(FSTORE, "FSTORE");
		put(DSTORE, "DSTORE");
		put(ASTORE, "ASTORE");
		put(IASTORE, "IASTORE");
		put(LASTORE, "LASTORE");
		put(FASTORE, "FASTORE");
		put(DASTORE, "DASTORE");
		put(AASTORE, "AASTORE");
		put(BASTORE, "BASTORE");
		put(CASTORE, "CASTORE");
		put(SASTORE, "SASTORE");
		put(POP, "POP");
		put(POP2, "POP2");
		put(DUP, "DUP");
		put(DUP_X1, "DUP_X1");
		put(DUP_X2, "DUP_X2");
		put(DUP2, "DUP2");
		put(DUP2_X1, "DUP2_X1");
		put(DUP2_X2, "DUP2_X2");
		put(SWAP, "SWAP");
		put(IADD, "IADD");
		put(LADD, "LADD");
		put(FADD, "FADD");
		put(DADD, "DADD");
		put(ISUB, "ISUB");
		put(LSUB, "LSUB");
		put(FSUB, "FSUB");
		put(DSUB, "DSUB");
		put(IMUL, "IMUL");
		put(LMUL, "LMUL");
		put(FMUL, "FMUL");
		put(DMUL, "DMUL");
		put(IDIV, "IDIV");
		put(LDIV, "LDIV");
		put(FDIV, "FDIV");
		put(DDIV, "DDIV");
		put(IREM, "IREM");
		put(LREM, "LREM");
		put(FREM, "FREM");
		put(DREM, "DREM");
		put(INEG, "INEG");
		put(LNEG, "LNEG");
		put(FNEG, "FNEG");
		put(DNEG, "DNEG");
		put(ISHL, "ISHL");
		put(LSHL, "LSHL");
		put(ISHR, "ISHR");
		put(LSHR, "LSHR");
		put(IUSHR, "IUSHR");
		put(LUSHR, "LUSHR");
		put(IAND, "IAND");
		put(LAND, "LAND");
		put(IOR, "IOR");
		put(LOR, "LOR");
		put(IXOR, "IXOR");
		put(LXOR, "LXOR");
		put(IINC, "IINC");
		put(I2L, "I2L");
		put(I2F, "I2F");
		put(I2D, "I2D");
		put(L2I, "L2I");
		put(L2F, "L2F");
		put(L2D, "L2D");
		put(F2I, "F2I");
		put(F2L, "F2L");
		put(F2D, "F2D");
		put(D2I, "D2I");
		put(D2L, "D2L");
		put(D2F, "D2F");
		put(I2B, "I2B");
		put(I2C, "I2C");
		put(I2S, "I2S");
		put(LCMP, "LCMP");
		put(FCMPL, "FCMPL");
		put(FCMPG, "FCMPG");
		put(DCMPL, "DCMPL");
		put(DCMPG, "DCMPG");
		put(IFEQ, "IFEQ");
		put(IFNE, "IFNE");
		put(IFLT, "IFLT");
		put(IFGE, "IFGE");
		put(IFGT, "IFGT");
		put(IFLE, "IFLE");
		put(IF_ICMPEQ, "IF_ICMPEQ");
		put(IF_ICMPNE, "IF_ICMPNE");
		put(IF_ICMPLT, "IF_ICMPLT");
		put(IF_ICMPGE, "IF_ICMPGE");
		put(IF_ICMPGT, "IF_ICMPGT");
		put(IF_ICMPLE, "IF_ICMPLE");
		put(IF_ACMPEQ, "IF_ACMPEQ");
		put(IF_ACMPNE, "IF_ACMPNE");
		put(GOTO, "GOTO");
		put(JSR, "JSR");
		put(RET, "RET");
		put(TABLESWITCH, "TABLESWITCH");
		put(LOOKUPSWITCH, "LOOKUPSWITCH");
		put(IRETURN, "IRETURN");
		put(LRETURN, "LRETURN");
		put(FRETURN, "FRETURN");
		put(DRETURN, "DRETURN");
		put(ARETURN, "ARETURN");
		put(RETURN, "RETURN");
		put(GETSTATIC, "GETSTATIC");
		put(PUTSTATIC, "PUTSTATIC");
		put(GETFIELD, "GETFIELD");
		put(PUTFIELD, "PUTFIELD");
		put(INVOKEVIRTUAL, "INVOKEVIRTUAL");
		put(INVOKESPECIAL, "INVOKESPECIAL");
		put(INVOKESTATIC, "INVOKESTATIC");
		put(INVOKEINTERFACE, "INVOKEINTERFACE");
		put(INVOKEDYNAMIC, "INVOKEDYNAMIC");
		put(NEW, "NEW");
		put(NEWARRAY, "NEWARRAY");
		put(ANEWARRAY, "ANEWARRAY");
		put(ARRAYLENGTH, "ARRAYLENGTH");
		put(ATHROW, "ATHROW");
		put(CHECKCAST, "CHECKCAST");
		put(INSTANCEOF, "INSTANCEOF");
		put(MONITORENTER, "MONITORENTER");
		put(MONITOREXIT, "MONITOREXIT");
		put(MULTIANEWARRAY, "MULTIANEWARRAY");
		put(IFNULL, "IFNULL");
		put(IFNONNULL, "IFNONNULL");
		putFrame(F_NEW, "F_NEW");
		putFrame(F_FULL, "F_FULL");
		putFrame(F_APPEND, "F_APPEND");
		putFrame(F_CHOP, "F_CHOP");
		putFrame(F_SAME, "F_SAME");
		putFrame(F_APPEND, "F_APPEND");
		putFrame(F_SAME1, "F_SAME1");
		putTag(Opcodes.H_GETFIELD, "H_GETFIELD");
		putTag(Opcodes.H_GETSTATIC, "H_GETSTATIC");
		putTag(Opcodes.H_PUTFIELD, "H_PUTFIELD");
		putTag(Opcodes.H_PUTSTATIC, "H_PUTSTATIC");
		putTag(Opcodes.H_INVOKEINTERFACE, "H_INVOKEINTERFACE");
		putTag(Opcodes.H_INVOKESPECIAL, "H_INVOKESPECIAL");
		putTag(Opcodes.H_INVOKESTATIC, "H_INVOKESTATIC");
		putTag(Opcodes.H_INVOKEVIRTUAL, "H_INVOKEVIRTUAL");
		putTag(Opcodes.H_NEWINVOKESPECIAL, "H_NEWINVOKESPECIAL");
		insnTypeToCodes.put(AbstractInsnNode.FIELD_INSN, OpcodeUtil.OPS_FIELD);
		insnTypeToCodes.put(AbstractInsnNode.FRAME, OpcodeUtil.OPS_FRAME);
		insnTypeToCodes.put(AbstractInsnNode.IINC_INSN, OpcodeUtil.OPS_IINC);
		insnTypeToCodes.put(AbstractInsnNode.INSN, OpcodeUtil.OPS_INSN);
		insnTypeToCodes.put(AbstractInsnNode.INT_INSN, OpcodeUtil.OPS_INT);
		insnTypeToCodes.put(AbstractInsnNode.INVOKE_DYNAMIC_INSN, OpcodeUtil.OPS_INDY_METHOD);
		insnTypeToCodes.put(AbstractInsnNode.JUMP_INSN, OpcodeUtil.OPS_JUMP);
		insnTypeToCodes.put(AbstractInsnNode.LABEL, OpcodeUtil.OPS_LABEL);
		insnTypeToCodes.put(AbstractInsnNode.LDC_INSN, OpcodeUtil.OPS_LDC);
		insnTypeToCodes.put(AbstractInsnNode.LINE, OpcodeUtil.OPS_LINE);
		insnTypeToCodes.put(AbstractInsnNode.LOOKUPSWITCH_INSN, OpcodeUtil.OPS_LOOKUPSWITCH);
		insnTypeToCodes.put(AbstractInsnNode.METHOD_INSN, OpcodeUtil.OPS_METHOD);
		insnTypeToCodes.put(AbstractInsnNode.MULTIANEWARRAY_INSN, OpcodeUtil.OPS_MULTIANEWARRAY);
		insnTypeToCodes.put(AbstractInsnNode.TABLESWITCH_INSN, OpcodeUtil.OPS_TABLESWITCH);
		insnTypeToCodes.put(AbstractInsnNode.TYPE_INSN, OpcodeUtil.OPS_TYPE);
		insnTypeToCodes.put(AbstractInsnNode.VAR_INSN, OpcodeUtil.OPS_VAR);
	}
}
