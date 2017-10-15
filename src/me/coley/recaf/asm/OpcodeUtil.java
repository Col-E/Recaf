package me.coley.recaf.asm;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;

public class OpcodeUtil implements Opcodes {
	private static final Map<Integer, String> opcodeToName = new HashMap<>();
	private static final Map<String, Integer> nameToOpcode = new HashMap<>();
	private static final Map<Integer, String> frameToName = new HashMap<>();
	private static final Map<String, Integer> nameToFrame = new HashMap<>();
	private static final Map<Integer, String> tagToName = new HashMap<>();
	private static final Map<String, Integer> nameToTag = new HashMap<>();
	private static final Map<Integer, Set<String>> insnTypeToCodes = new HashMap<>();
	/**
	 * Opcodes of INSN type.
	 */
	public static final Set<String> OPS_INSN = Stream.of("NOP", "ACONST_NULL", "ICONST_M1", "ICONST_0", "ICONST_1", "ICONST_2",
			"ICONST_3", "ICONST_4", "ICONST_5", "LCONST_0", "LCONST_1", "FCONST_0", "FCONST_1", "FCONST_2", "DCONST_0",
			"DCONST_1", "IALOAD", "LALOAD", "FALOAD", "DALOAD", "AALOAD", "BALOAD", "CALOAD", "SALOAD", "IASTORE", "LASTORE",
			"FASTORE", "DASTORE", "AASTORE", "BASTORE", "CASTORE", "SASTORE", "POP", "POP2", "DUP", "DUP_X1", "DUP_X2", "DUP2",
			"DUP2_X1", "DUP2_X2", "SWAP", "IADD", "LADD", "FADD", "DADD", "ISUB", "LSUB", "FSUB", "DSUB", "IMUL", "LMUL", "FMUL",
			"DMUL", "IDIV", "LDIV", "FDIV", "DDIV", "IREM", "LREM", "FREM", "DREM", "INEG", "LNEG", "FNEG", "DNEG", "ISHL",
			"LSHL", "ISHR", "LSHR", "IUSHR", "LUSHR", "IAND", "LAND", "IOR", "LOR", "IXOR", "LXOR", "I2L", "I2F", "I2D", "L2I",
			"L2F", "L2D", "F2I", "F2L", "F2D", "D2I", "D2L", "D2F", "I2B", "I2C", "I2S", "LCMP", "FCMPL", "FCMPG", "DCMPL",
			"DCMPG", "IRETURN", "LRETURN", "FRETURN", "DRETURN", "ARETURN", "RETURN", "ARRAYLENGTH", "ATHROW", "MONITORENTER",
			"MONITOREXIT").collect(Collectors.toSet());
	/**
	 * Subset of {@link #OPS_INSN} for constants.
	 */
	public static final Set<String> OPS_INSN_SUB_CONSTS = Stream.of("ACONST_NULL", "ICONST_M1", "ICONST_0", "ICONST_1",
			"ICONST_2", "ICONST_3", "ICONST_4", "ICONST_5", "LCONST_0", "LCONST_1", "FCONST_0", "FCONST_1", "FCONST_2",
			"DCONST_0", "DCONST_1").collect(Collectors.toSet());
	/**
	 * Subset of {@link #OPS_INSN} for array loads/saves/etc.
	 */
	public static final Set<String> OPS_INSN_SUB_ARRAY = Stream.of("IALOAD", "LALOAD", "FALOAD", "DALOAD", "AALOAD", "BALOAD",
			"CALOAD", "SALOAD", "IASTORE", "LASTORE", "FASTORE", "DASTORE", "AASTORE", "BASTORE", "CASTORE", "SASTORE",
			"ARRAYLENGTH").collect(Collectors.toSet());
	/**
	 * Subset of {@link #OPS_INSN} for stack management.
	 */
	public static final Set<String> OPS_INSN_SUB_STACK = Stream.of("POP", "POP2", "DUP", "DUP_X1", "DUP_X2", "DUP2", "DUP2_X1",
			"DUP2_X2", "SWAP").collect(Collectors.toSet());
	/**
	 * Subset of {@link #OPS_INSN} for math handling.
	 */
	public static final Set<String> OPS_INSN_SUB_MATH = Stream.of("IADD", "LADD", "FADD", "DADD", "ISUB", "LSUB", "FSUB", "DSUB",
			"IMUL", "LMUL", "FMUL", "DMUL", "IDIV", "LDIV", "FDIV", "DDIV", "IREM", "LREM", "FREM", "DREM", "INEG", "LNEG",
			"FNEG", "DNEG", "ISHL", "LSHL", "ISHR", "LSHR", "IUSHR", "LUSHR", "IAND", "LAND", "IOR", "LOR", "IXOR").collect(
					Collectors.toSet());
	/**
	 * Subset of {@link #OPS_INSN} for type conversion.
	 */
	public static final Set<String> OPS_INSN_SUB_CONVERT = Stream.of("I2L", "I2F", "I2D", "L2I", "L2F", "L2D", "F2I", "F2L",
			"F2D", "D2I", "D2L", "D2F", "I2B", "I2C", "I2S").collect(Collectors.toSet());
	/**
	 * Subset of {@link #OPS_INSN} for primitve comparisons.
	 */
	public static final Set<String> OPS_INSN_SUB_COMPARE = Stream.of("LCMP", "FCMPL", "FCMPG", "DCMPL", "DCMPG").collect(
			Collectors.toSet());
	/**
	 * Subset of {@link #OPS_INSN} for returns.
	 */
	public static final Set<String> OPS_INSN_SUB_RETURN = Stream.of("IRETURN", "LRETURN", "FRETURN", "DRETURN", "ARETURN",
			"RETURN").collect(Collectors.toSet());
	/**
	 * Subset of {@link #OPS_INSN} for monitors.
	 */
	public static final Set<String> OPS_INSN_SUB_MONITOR = Stream.of("MONITORENTER", "MONITOREXIT").collect(Collectors.toSet());
	/**
	 * Subset of {@link #OPS_INSN} for exceptions.
	 */
	public static final Set<String> OPS_INSN_SUB_EXCEPTION = Stream.of("ATHROW").collect(Collectors.toSet());
	/**
	 * Opcodes of INT type.
	 */
	public static final Set<String> OPS_INT = Stream.of("BIPUSH", "SIPUSH", "NEWARRAY").collect(Collectors.toSet());
	/**
	 * Opcodes of INT type.
	 */
	public static final Set<String> OPS_VAR = Stream.of("ILOAD", "LLOAD", "FLOAD", "DLOAD", "ALOAD", "ISTORE", "LSTORE", "FSTORE",
			"DSTORE", "ASTORE", "RET").collect(Collectors.toSet());
	/**
	 * Opcodes of TYPE type.
	 */
	public static final Set<String> OPS_TYPE = Stream.of("NEW", "ANEWARRAY", "CHECKCAST", "INSTANCEOF").collect(Collectors
			.toSet());
	/**
	 * Opcodes of FIELD type.
	 */
	public static final Set<String> OPS_FIELD = Stream.of("GETSTATIC", "PUTSTATIC", "GETFIELD", "PUTFIELD").collect(Collectors
			.toSet());
	/**
	 * Opcodes of METHOD type.
	 */
	public static final Set<String> OPS_METHOD = Stream.of("INVOKEVIRTUAL", "INVOKESPECIAL", "INVOKESTATIC", "INVOKEINTERFACE")
			.collect(Collectors.toSet());
	/**
	 * Opcodes of INDY_METHOD type.
	 */
	public static final Set<String> OPS_INDY_METHOD = Stream.of("INVOKEDYNAMIC").collect(Collectors.toSet());
	/**
	 * Opcodes of JUMP type.
	 */
	public static final Set<String> OPS_JUMP = Stream.of("IFEQ", "IFNE", "IFLT", "IFGE", "IFGT", "IFLE", "IF_ICMPEQ", "IF_ICMPNE",
			"IF_ICMPLT", "IF_ICMPGE", "IF_ICMPGT", "IF_ICMPLE", "IF_ACMPEQ", "IF_ACMPNE", "GOTO", "JSR", "IFNULL", "IFNONNULL")
			.collect(Collectors.toSet());
	/**
	 * Opcodes of LDC type.
	 */
	public static final Set<String> OPS_LDC = Stream.of("LDC").collect(Collectors.toSet());
	/**
	 * Opcodes of IINC type.
	 */
	public static final Set<String> OPS_IINC = Stream.of("IINC").collect(Collectors.toSet());
	/**
	 * Opcodes of TABLESWITCH type.
	 */
	public static final Set<String> OPS_TABLESWITCH = Stream.of("TABLESWITCH").collect(Collectors.toSet());
	/**
	 * Opcodes of LOOKUPSWITCH type.
	 */
	public static final Set<String> OPS_LOOKUPSWITCH = Stream.of("LOOKUPSWITCH").collect(Collectors.toSet());
	/**
	 * Opcodes of LOOKUPSWITCH type.
	 */
	public static final Set<String> OPS_MULTIANEWARRAY = Stream.of("MULTIANEWARRAY").collect(Collectors.toSet());
	/**
	 * Opcodes of FRAME type.
	 */
	public static final Set<String> OPS_FRAME = Stream.of("F_NEW", "F_FULL", "F_APPEND", "F_CHOP", "F_SAME", "F_APPEND",
			"F_SAME1").collect(Collectors.toSet());
	/**
	 * Opcodes of LABEL type. Also see {@link #OPS_FRAME}[0].
	 */
	public static final Set<String> OPS_LABEL = Stream.of("F_NEW").collect(Collectors.toSet());
	/**
	 * Opcodes of LABEL type. Also see {@link #OPS_FRAME}[0].
	 */
	public static final Set<String> OPS_LINE = Stream.of("F_NEW").collect(Collectors.toSet());
	/**
	 * Empty list.
	 */
	public static final Set<String> OPS_EMPTY = Stream.<String> of().collect(Collectors.toSet());
	/**
	 * Types of InvokeDynamic handle tags.
	 */
	public static final Set<String> OPS_TAG = Stream.of("H_GETFIELD", "H_GETSTATIC", "H_PUTFIELD", "H_PUTSTATIC",
			"H_INVOKEINTERFACE", "H_INVOKESPECIAL", "H_INVOKESTATIC", "H_INVOKEVIRTUAL", "H_NEWINVOKESPECIAL").collect(Collectors
					.toSet());
	private static final Set<Set<String>> INSN_SUBS = Stream.of(OPS_INSN_SUB_ARRAY, OPS_INSN_SUB_COMPARE, OPS_INSN_SUB_CONSTS,
			OPS_INSN_SUB_CONVERT, OPS_INSN_SUB_EXCEPTION, OPS_INSN_SUB_MATH, OPS_INSN_SUB_MONITOR, OPS_INSN_SUB_RETURN,
			OPS_INSN_SUB_STACK).collect(Collectors.toSet());

	/**
	 * Converts an opcode name to its value.
	 * 
	 * @param name
	 *            Opcode name.
	 * @return Opcode value.
	 */
	public static int nameToOpcode(String name) {
		return nameToOpcode.get(name);
	}

	/**
	 * Converts an opcode value to its name.
	 * 
	 * @param op
	 *            Opcode value.
	 * @return Opcode name.
	 */
	public static String opcodeToName(int op) {
		return opcodeToName.get(op);
	}

	/**
	 * Converts an opcode <i>(Pertaining to frames)</i> name to its value.
	 * 
	 * @param name
	 *            Opcode name.
	 * @return Opcode value.
	 */
	public static int nameToFrame(String name) {
		return nameToFrame.get(name);
	}

	/**
	 * Converts an opcode <i>(Pertaining to frames)</i> value to its name.
	 * 
	 * @param op
	 *            Opcode value.
	 * @return Opcode name.
	 */
	public static String frameToName(int op) {
		return frameToName.get(op);
	}

	/**
	 * Converts a handle tag name to its value.
	 * 
	 * @param tag
	 *            Handle tag name.
	 * @return Handle tag value.
	 */
	public static int nameToTag(String tag) {
		return nameToTag.get(tag);
	}

	/**
	 * Converts a handle tag value to its name.
	 * 
	 * @param tag
	 *            Handle tag value.
	 * @return Handle tag name.
	 */
	public static String tagToName(int tag) {
		return tagToName.get(tag);
	}

	/**
	 * Retrieves the set of opcode names by the given opcode type.
	 * 
	 * @param type
	 *            Type of opcode
	 * @return Set of opcode names.
	 * 
	 * @see {@link org.objectweb.asm.tree.AbstractInsnNode AbstractInsnNode}:
	 *      <ul>
	 *      <li>{@link org.objectweb.asm.tree.AbstractInsnNode#FIELD_INSN
	 *      FIELD_INSN}</li>
	 *      <li>{@link org.objectweb.asm.tree.AbstractInsnNode#IINC_INSN
	 *      IINC_INSN}</li>
	 *      <li>{@link org.objectweb.asm.tree.AbstractInsnNode#INSN INSN}</li>
	 *      <li>{@link org.objectweb.asm.tree.AbstractInsnNode#INT_INSN
	 *      INT_INSN}</li>
	 *      <li>{@link org.objectweb.asm.tree.AbstractInsnNode#INVOKE_DYNAMIC_INSN
	 *      INVOKE_DYNAMIC_INSN}</li>
	 *      <li>{@link org.objectweb.asm.tree.AbstractInsnNode#JUMP_INSN
	 *      JUMP_INSN}</li>
	 *      <li>{@link org.objectweb.asm.tree.AbstractInsnNode#LDC_INSN
	 *      LDC_INSN}</li>
	 *      <li>{@link org.objectweb.asm.tree.AbstractInsnNode#LOOKUPSWITCH_INSN
	 *      LOOKUPSWITCH_INSN}</li>
	 *      <li>{@link org.objectweb.asm.tree.AbstractInsnNode#METHOD_INSN
	 *      METHOD_INSN}</li>
	 *      <li>{@link org.objectweb.asm.tree.AbstractInsnNode#MULTIANEWARRAY_INSN
	 *      MULTIANEWARRAY_INSN}</li>
	 *      <li>{@link org.objectweb.asm.tree.AbstractInsnNode#TABLESWITCH_INSN
	 *      TABLESWITCH_INSN}</li>
	 *      <li>{@link org.objectweb.asm.tree.AbstractInsnNode#TYPE_INSN
	 *      TYPE_INSN}</li>
	 *      <li>{@link org.objectweb.asm.tree.AbstractInsnNode#VAR_INSN
	 *      VAR_INSN}</li>
	 *      </ul>
	 */
	public static Set<String> typeToCodes(int type) {
		return insnTypeToCodes.get(type);
	}

	/**
	 * Return smaller subset of the {@link #OPS_INSN} set containing the given
	 * opcodes and others related to it.
	 * 
	 *
	 * @param insnOpName
	 *            The name of opcode
	 * @return The desired subset.
	 */
	public static Set<String> getInsnSubset(String insnOpName) {
		for (Set<String> set : INSN_SUBS) {
			if (set.contains(insnOpName)) {
				return set;
			}
		}
		// If not found, return empty set
		return OPS_EMPTY;
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
