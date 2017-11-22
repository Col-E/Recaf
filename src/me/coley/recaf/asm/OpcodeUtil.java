package me.coley.recaf.asm;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;

public class OpcodeUtil implements Opcodes {
	private static final Map<Integer, String> opcodeToName = new LinkedHashMap<>();
	private static final Map<String, Integer> nameToOpcode = new LinkedHashMap<>();
	private static final Map<Integer, String> frameToName = new LinkedHashMap<>();
	private static final Map<String, Integer> nameToFrame = new LinkedHashMap<>();
	private static final Map<Integer, String> tagToName = new LinkedHashMap<>();
	private static final Map<String, Integer> nameToTag = new LinkedHashMap<>();
	private static final Map<Integer, Set<String>> insnTypeToCodes = new LinkedHashMap<>();
	/**
	 * Opcodes of INSN type.
	 */
	public static final Set<String> OPS_INSN = Stream.of("AALOAD", "AASTORE", "ACONST_NULL", "ARETURN", "ARRAYLENGTH",
			"ATHROW", "BALOAD", "BASTORE", "CALOAD", "CASTORE", "D2F", "D2I", "D2L", "DADD", "DALOAD", "DASTORE", "DCMPG",
			"DCMPL", "DCONST_0", "DCONST_1", "DDIV", "DMUL", "DNEG", "DREM", "DRETURN", "DSUB", "DUP", "DUP2", "DUP2_X1",
			"DUP2_X2", "DUP_X1", "DUP_X2", "F2D", "F2I", "F2L", "FADD", "FALOAD", "FASTORE", "FCMPG", "FCMPL", "FCONST_0",
			"FCONST_1", "FCONST_2", "FDIV", "FMUL", "FNEG", "FREM", "FRETURN", "FSUB", "I2B", "I2C", "I2D", "I2F", "I2L", "I2S",
			"IADD", "IALOAD", "IAND", "IASTORE", "ICONST_0", "ICONST_1", "ICONST_2", "ICONST_3", "ICONST_4", "ICONST_5",
			"ICONST_M1", "IDIV", "IMUL", "INEG", "IOR", "IREM", "IRETURN", "ISHL", "ISHR", "ISUB", "IUSHR", "IXOR", "L2D", "L2F",
			"L2I", "LADD", "LALOAD", "LAND", "LASTORE", "LCMP", "LCONST_0", "LCONST_1", "LDIV", "LMUL", "LNEG", "LOR", "LREM",
			"LRETURN", "LSHL", "LSHR", "LSUB", "LUSHR", "LXOR", "MONITORENTER", "MONITOREXIT", "NOP", "POP", "POP2", "RETURN",
			"SALOAD", "SASTORE", "SWAP").collect(Collectors.toCollection(LinkedHashSet::new));
	/**
	 * Subset of {@link #OPS_INSN} for constants.
	 */
	public static final Set<String> OPS_INSN_SUB_CONSTS = Stream.of("ACONST_NULL", "DCONST_0", "DCONST_1", "FCONST_0",
			"FCONST_1", "FCONST_2", "ICONST_0", "ICONST_1", "ICONST_2", "ICONST_3", "ICONST_4", "ICONST_5", "ICONST_M1",
			"LCONST_0", "LCONST_1").collect(Collectors.toCollection(LinkedHashSet::new));
	/**
	 * Subset of {@link #OPS_INSN} for array loads/saves/etc.
	 */
	public static final Set<String> OPS_INSN_SUB_ARRAY = Stream.of("AALOAD", "AASTORE", "ARRAYLENGTH", "BALOAD", "BASTORE",
			"CALOAD", "CASTORE", "DALOAD", "DASTORE", "FALOAD", "FASTORE", "IALOAD", "IASTORE", "LALOAD", "LASTORE", "SALOAD",
			"SASTORE").collect(Collectors.toCollection(LinkedHashSet::new));
	/**
	 * Subset of {@link #OPS_INSN} for stack management.
	 */
	public static final Set<String> OPS_INSN_SUB_STACK = Stream.of("DUP", "DUP2", "DUP2_X1", "DUP2_X2", "DUP_X1", "DUP_X2",
			"POP", "POP2", "SWAP").collect(Collectors.toCollection(LinkedHashSet::new));
	/**
	 * Subset of {@link #OPS_INSN} for math handling.
	 */
	public static final Set<String> OPS_INSN_SUB_MATH = Stream.of("DADD", "DDIV", "DMUL", "DNEG", "DREM", "DSUB", "FADD",
			"FDIV", "FMUL", "FNEG", "FREM", "FSUB", "IADD", "IAND", "IDIV", "IMUL", "INEG", "IOR", "IREM", "ISHL", "ISHR", "ISUB",
			"IUSHR", "IXOR", "LADD", "LAND", "LDIV", "LMUL", "LNEG", "LOR", "LREM", "LSHL", "LSHR", "LSUB", "LUSHR").collect(Collectors.toCollection(LinkedHashSet::new));
	/**
	 * Subset of {@link #OPS_INSN} for type conversion.
	 */
	public static final Set<String> OPS_INSN_SUB_CONVERT = Stream.of("D2F", "D2I", "D2L", "F2D", "F2I", "F2L", "I2B", "I2C",
			"I2D", "I2F", "I2L", "I2S", "L2D", "L2F", "L2I").collect(Collectors.toCollection(LinkedHashSet::new));
	/**
	 * Subset of {@link #OPS_INSN} for primitve comparisons.
	 */
	public static final Set<String> OPS_INSN_SUB_COMPARE = Stream.of("DCMPG", "DCMPL", "FCMPG", "FCMPL", "LCMP").collect(Collectors.toCollection(LinkedHashSet::new));
	/**
	 * Subset of {@link #OPS_INSN} for returns.
	 */
	public static final Set<String> OPS_INSN_SUB_RETURN = Stream.of("ARETURN", "DRETURN", "FRETURN", "IRETURN", "LRETURN",
			"RETURN").collect(Collectors.toCollection(LinkedHashSet::new));
	/**
	 * Subset of {@link #OPS_INSN} for monitors.
	 */
	public static final Set<String> OPS_INSN_SUB_MONITOR = Stream.of("MONITORENTER", "MONITOREXIT").collect(Collectors.toCollection(LinkedHashSet::new));
	/**
	 * Subset of {@link #OPS_INSN} for exceptions.
	 */
	public static final Set<String> OPS_INSN_SUB_EXCEPTION = Stream.of("ATHROW").collect(Collectors.toCollection(LinkedHashSet::new));
	/**
	 * Opcodes of INT type.
	 */
	public static final Set<String> OPS_INT = Stream.of("BIPUSH", "SIPUSH", "NEWARRAY").collect(Collectors.toCollection(LinkedHashSet::new));
	/**
	 * Opcodes of INT type.
	 */
	public static final Set<String> OPS_VAR = Stream.of("ALOAD", "ASTORE", "DLOAD", "DSTORE", "FLOAD", "FSTORE", "ILOAD",
			"ISTORE", "LLOAD", "LSTORE", "RET").collect(Collectors.toCollection(LinkedHashSet::new));
	/**
	 * Opcodes of TYPE type.
	 */
	public static final Set<String> OPS_TYPE = Stream.of("ANEWARRAY", "CHECKCAST", "INSTANCEOF", "NEW").collect(Collectors.toCollection(LinkedHashSet::new));
	/**
	 * Opcodes of FIELD type.
	 */
	public static final Set<String> OPS_FIELD = Stream.of("GETSTATIC", "PUTSTATIC", "GETFIELD", "PUTFIELD").collect(Collectors.toCollection(LinkedHashSet::new));
	/**
	 * Opcodes of METHOD type.
	 */
	public static final Set<String> OPS_METHOD = Stream.of("INVOKEVIRTUAL", "INVOKESPECIAL", "INVOKESTATIC",
			"INVOKEINTERFACE").collect(Collectors.toCollection(LinkedHashSet::new));
	/**
	 * Opcodes of INDY_METHOD type.
	 */
	public static final Set<String> OPS_INDY_METHOD = Stream.of("INVOKEDYNAMIC").collect(Collectors.toCollection(LinkedHashSet::new));
	/**
	 * Opcodes of JUMP type.
	 */
	public static final Set<String> OPS_JUMP = Stream.of("GOTO", "IF_ACMPEQ", "IF_ACMPNE", "IF_ICMPEQ", "IF_ICMPGE",
			"IF_ICMPGT", "IF_ICMPLE", "IF_ICMPLT", "IF_ICMPNE", "IFEQ", "IFGE", "IFGT", "IFLE", "IFLT", "IFNE", "IFNONNULL",
			"IFNULL", "JSR").collect(Collectors.toCollection(LinkedHashSet::new));
	/**
	 * Opcodes of LDC type.
	 */
	public static final Set<String> OPS_LDC = Stream.of("LDC").collect(Collectors.toCollection(LinkedHashSet::new));
	/**
	 * Opcodes of IINC type.
	 */
	public static final Set<String> OPS_IINC = Stream.of("IINC").collect(Collectors.toCollection(LinkedHashSet::new));
	/**
	 * Opcodes of TABLESWITCH type.
	 */
	public static final Set<String> OPS_TABLESWITCH = Stream.of("TABLESWITCH").collect(Collectors.toCollection(LinkedHashSet::new));
	/**
	 * Opcodes of LOOKUPSWITCH type.
	 */
	public static final Set<String> OPS_LOOKUPSWITCH = Stream.of("LOOKUPSWITCH").collect(Collectors.toCollection(LinkedHashSet::new));
	/**
	 * Opcodes of LOOKUPSWITCH type.
	 */
	public static final Set<String> OPS_MULTIANEWARRAY = Stream.of("MULTIANEWARRAY").collect(Collectors.toCollection(LinkedHashSet::new));
	/**
	 * Opcodes of FRAME type.
	 */
	public static final Set<String> OPS_FRAME = Stream.of("F_NEW", "F_FULL", "F_APPEND", "F_CHOP", "F_SAME", "F_APPEND",
			"F_SAME1").collect(Collectors.toCollection(LinkedHashSet::new));
	/**
	 * Opcodes of LABEL type. Also see {@link #OPS_FRAME}[0].
	 */
	public static final Set<String> OPS_LABEL = Stream.of("F_NEW").collect(Collectors.toCollection(LinkedHashSet::new));
	/**
	 * Opcodes of LABEL type. Also see {@link #OPS_FRAME}[0].
	 */
	public static final Set<String> OPS_LINE = Stream.of("F_NEW").collect(Collectors.toCollection(LinkedHashSet::new));
	/**
	 * Empty list.
	 */
	public static final Set<String> OPS_EMPTY = Stream.of().collect(Collectors.toCollection(LinkedHashSet<String>::new));
	/**
	 * Types of InvokeDynamic handle tags.
	 */
	public static final Set<String> OPS_TAG = Stream.of("H_GETFIELD", "H_GETSTATIC", "H_PUTFIELD", "H_PUTSTATIC",
			"H_INVOKEINTERFACE", "H_INVOKESPECIAL", "H_INVOKESTATIC", "H_INVOKEVIRTUAL", "H_NEWINVOKESPECIAL").collect(Collectors.toCollection(LinkedHashSet::new));
	private static final Set<Set<String>> INSN_SUBS = Stream.of(OPS_INSN_SUB_ARRAY, OPS_INSN_SUB_COMPARE, OPS_INSN_SUB_CONSTS,
			OPS_INSN_SUB_CONVERT, OPS_INSN_SUB_EXCEPTION, OPS_INSN_SUB_MATH, OPS_INSN_SUB_MONITOR, OPS_INSN_SUB_RETURN,
			OPS_INSN_SUB_STACK).collect(Collectors.toCollection(LinkedHashSet::new));;

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
		put(AALOAD, "AALOAD");
		put(AASTORE, "AASTORE");
		put(ACONST_NULL, "ACONST_NULL");
		put(ALOAD, "ALOAD");
		put(ANEWARRAY, "ANEWARRAY");
		put(ARETURN, "ARETURN");
		put(ARRAYLENGTH, "ARRAYLENGTH");
		put(ASTORE, "ASTORE");
		put(ATHROW, "ATHROW");
		put(BALOAD, "BALOAD");
		put(BASTORE, "BASTORE");
		put(BIPUSH, "BIPUSH");
		put(CALOAD, "CALOAD");
		put(CASTORE, "CASTORE");
		put(CHECKCAST, "CHECKCAST");
		put(D2F, "D2F");
		put(D2I, "D2I");
		put(D2L, "D2L");
		put(DADD, "DADD");
		put(DALOAD, "DALOAD");
		put(DASTORE, "DASTORE");
		put(DCMPG, "DCMPG");
		put(DCMPL, "DCMPL");
		put(DCONST_0, "DCONST_0");
		put(DCONST_1, "DCONST_1");
		put(DDIV, "DDIV");
		put(DLOAD, "DLOAD");
		put(DMUL, "DMUL");
		put(DNEG, "DNEG");
		put(DREM, "DREM");
		put(DRETURN, "DRETURN");
		put(DSTORE, "DSTORE");
		put(DSUB, "DSUB");
		put(DUP, "DUP");
		put(DUP2, "DUP2");
		put(DUP2_X1, "DUP2_X1");
		put(DUP2_X2, "DUP2_X2");
		put(DUP_X1, "DUP_X1");
		put(DUP_X2, "DUP_X2");
		put(F2D, "F2D");
		put(F2I, "F2I");
		put(F2L, "F2L");
		put(F_NEW, "F_NEW");
		put(FADD, "FADD");
		put(FALOAD, "FALOAD");
		put(FASTORE, "FASTORE");
		put(FCMPG, "FCMPG");
		put(FCMPL, "FCMPL");
		put(FCONST_0, "FCONST_0");
		put(FCONST_1, "FCONST_1");
		put(FCONST_2, "FCONST_2");
		put(FDIV, "FDIV");
		put(FLOAD, "FLOAD");
		put(FMUL, "FMUL");
		put(FNEG, "FNEG");
		put(FREM, "FREM");
		put(FRETURN, "FRETURN");
		put(FSTORE, "FSTORE");
		put(FSUB, "FSUB");
		put(GETFIELD, "GETFIELD");
		put(GETSTATIC, "GETSTATIC");
		put(GOTO, "GOTO");
		put(I2B, "I2B");
		put(I2C, "I2C");
		put(I2D, "I2D");
		put(I2F, "I2F");
		put(I2L, "I2L");
		put(I2S, "I2S");
		put(IADD, "IADD");
		put(IALOAD, "IALOAD");
		put(IAND, "IAND");
		put(IASTORE, "IASTORE");
		put(ICONST_0, "ICONST_0");
		put(ICONST_1, "ICONST_1");
		put(ICONST_2, "ICONST_2");
		put(ICONST_3, "ICONST_3");
		put(ICONST_4, "ICONST_4");
		put(ICONST_5, "ICONST_5");
		put(ICONST_M1, "ICONST_M1");
		put(IDIV, "IDIV");
		put(IF_ACMPEQ, "IF_ACMPEQ");
		put(IF_ACMPNE, "IF_ACMPNE");
		put(IF_ICMPEQ, "IF_ICMPEQ");
		put(IF_ICMPGE, "IF_ICMPGE");
		put(IF_ICMPGT, "IF_ICMPGT");
		put(IF_ICMPLE, "IF_ICMPLE");
		put(IF_ICMPLT, "IF_ICMPLT");
		put(IF_ICMPNE, "IF_ICMPNE");
		put(IFEQ, "IFEQ");
		put(IFGE, "IFGE");
		put(IFGT, "IFGT");
		put(IFLE, "IFLE");
		put(IFLT, "IFLT");
		put(IFNE, "IFNE");
		put(IFNONNULL, "IFNONNULL");
		put(IFNULL, "IFNULL");
		put(IINC, "IINC");
		put(ILOAD, "ILOAD");
		put(IMUL, "IMUL");
		put(INEG, "INEG");
		put(INSTANCEOF, "INSTANCEOF");
		put(INVOKEDYNAMIC, "INVOKEDYNAMIC");
		put(INVOKEINTERFACE, "INVOKEINTERFACE");
		put(INVOKESPECIAL, "INVOKESPECIAL");
		put(INVOKESTATIC, "INVOKESTATIC");
		put(INVOKEVIRTUAL, "INVOKEVIRTUAL");
		put(IOR, "IOR");
		put(IREM, "IREM");
		put(IRETURN, "IRETURN");
		put(ISHL, "ISHL");
		put(ISHR, "ISHR");
		put(ISTORE, "ISTORE");
		put(ISUB, "ISUB");
		put(IUSHR, "IUSHR");
		put(IXOR, "IXOR");
		put(JSR, "JSR");
		put(L2D, "L2D");
		put(L2F, "L2F");
		put(L2I, "L2I");
		put(LADD, "LADD");
		put(LALOAD, "LALOAD");
		put(LAND, "LAND");
		put(LASTORE, "LASTORE");
		put(LCMP, "LCMP");
		put(LCONST_0, "LCONST_0");
		put(LCONST_1, "LCONST_1");
		put(LDC, "LDC");
		put(LDIV, "LDIV");
		put(LLOAD, "LLOAD");
		put(LMUL, "LMUL");
		put(LNEG, "LNEG");
		put(LOOKUPSWITCH, "LOOKUPSWITCH");
		put(LOR, "LOR");
		put(LREM, "LREM");
		put(LRETURN, "LRETURN");
		put(LSHL, "LSHL");
		put(LSHR, "LSHR");
		put(LSTORE, "LSTORE");
		put(LSUB, "LSUB");
		put(LUSHR, "LUSHR");
		put(LXOR, "LXOR");
		put(MONITORENTER, "MONITORENTER");
		put(MONITOREXIT, "MONITOREXIT");
		put(MULTIANEWARRAY, "MULTIANEWARRAY");
		put(NEW, "NEW");
		put(NEWARRAY, "NEWARRAY");
		put(NOP, "NOP");
		put(POP, "POP");
		put(POP2, "POP2");
		put(PUTFIELD, "PUTFIELD");
		put(PUTSTATIC, "PUTSTATIC");
		put(RET, "RET");
		put(RETURN, "RETURN");
		put(SALOAD, "SALOAD");
		put(SASTORE, "SASTORE");
		put(SIPUSH, "SIPUSH");
		put(SWAP, "SWAP");
		put(TABLESWITCH, "TABLESWITCH");
		putFrame(F_APPEND, "F_APPEND");
		putFrame(F_APPEND, "F_APPEND");
		putFrame(F_CHOP, "F_CHOP");
		putFrame(F_FULL, "F_FULL");
		putFrame(F_NEW, "F_NEW");
		putFrame(F_SAME, "F_SAME");
		putFrame(F_SAME1, "F_SAME1");
		putTag(Opcodes.H_GETFIELD, "H_GETFIELD");
		putTag(Opcodes.H_GETSTATIC, "H_GETSTATIC");
		putTag(Opcodes.H_INVOKEINTERFACE, "H_INVOKEINTERFACE");
		putTag(Opcodes.H_INVOKESPECIAL, "H_INVOKESPECIAL");
		putTag(Opcodes.H_INVOKESTATIC, "H_INVOKESTATIC");
		putTag(Opcodes.H_INVOKEVIRTUAL, "H_INVOKEVIRTUAL");
		putTag(Opcodes.H_NEWINVOKESPECIAL, "H_NEWINVOKESPECIAL");
		putTag(Opcodes.H_PUTFIELD, "H_PUTFIELD");
		putTag(Opcodes.H_PUTSTATIC, "H_PUTSTATIC");
	}
}
