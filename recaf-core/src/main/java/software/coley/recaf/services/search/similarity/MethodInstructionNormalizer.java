package software.coley.recaf.services.search.similarity;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import me.darknet.dex.tree.definitions.OpcodeNames;
import me.darknet.dex.tree.definitions.code.Code;
import me.darknet.dex.tree.definitions.code.Handler;
import me.darknet.dex.tree.definitions.code.TryCatch;
import me.darknet.dex.tree.definitions.instructions.ArrayInstruction;
import me.darknet.dex.tree.definitions.instructions.ArrayLengthInstruction;
import me.darknet.dex.tree.definitions.instructions.Binary2AddrInstruction;
import me.darknet.dex.tree.definitions.instructions.BinaryInstruction;
import me.darknet.dex.tree.definitions.instructions.BinaryLiteralInstruction;
import me.darknet.dex.tree.definitions.instructions.BranchInstruction;
import me.darknet.dex.tree.definitions.instructions.CheckCastInstruction;
import me.darknet.dex.tree.definitions.instructions.CompareInstruction;
import me.darknet.dex.tree.definitions.instructions.ConstInstruction;
import me.darknet.dex.tree.definitions.instructions.ConstMethodHandleInstruction;
import me.darknet.dex.tree.definitions.instructions.ConstMethodTypeInstruction;
import me.darknet.dex.tree.definitions.instructions.ConstStringInstruction;
import me.darknet.dex.tree.definitions.instructions.ConstTypeInstruction;
import me.darknet.dex.tree.definitions.instructions.ConstWideInstruction;
import me.darknet.dex.tree.definitions.instructions.FillArrayDataInstruction;
import me.darknet.dex.tree.definitions.instructions.FilledNewArrayInstruction;
import me.darknet.dex.tree.definitions.instructions.GotoInstruction;
import me.darknet.dex.tree.definitions.instructions.InstanceFieldInstruction;
import me.darknet.dex.tree.definitions.instructions.InstanceOfInstruction;
import me.darknet.dex.tree.definitions.instructions.Instruction;
import me.darknet.dex.tree.definitions.instructions.InvokeCustomInstruction;
import me.darknet.dex.tree.definitions.instructions.InvokeInstruction;
import me.darknet.dex.tree.definitions.instructions.Label;
import me.darknet.dex.tree.definitions.instructions.MonitorInstruction;
import me.darknet.dex.tree.definitions.instructions.MoveExceptionInstruction;
import me.darknet.dex.tree.definitions.instructions.MoveInstruction;
import me.darknet.dex.tree.definitions.instructions.MoveObjectInstruction;
import me.darknet.dex.tree.definitions.instructions.MoveResultInstruction;
import me.darknet.dex.tree.definitions.instructions.MoveWideInstruction;
import me.darknet.dex.tree.definitions.instructions.NewArrayInstruction;
import me.darknet.dex.tree.definitions.instructions.NewInstanceInstruction;
import me.darknet.dex.tree.definitions.instructions.NopInstruction;
import me.darknet.dex.tree.definitions.instructions.PackedSwitchInstruction;
import me.darknet.dex.tree.definitions.instructions.ReturnInstruction;
import me.darknet.dex.tree.definitions.instructions.SparseSwitchInstruction;
import me.darknet.dex.tree.definitions.instructions.StaticFieldInstruction;
import me.darknet.dex.tree.definitions.instructions.ThrowInstruction;
import me.darknet.dex.tree.definitions.instructions.UnaryInstruction;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeInsnNode;
import software.coley.collections.box.LongBox;
import software.coley.recaf.util.AsmInsnUtil;
import software.coley.recaf.util.Types;
import software.coley.recaf.util.collect.primitive.Int2ObjectMap;
import software.coley.recaf.util.collect.primitive.Object2IntMap;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static software.coley.recaf.util.DexInsnUtil.*;

/**
 * Shared JVM/Dalvik instruction normalization utilities.
 *
 * @author Matt Coley
 */
public final class MethodInstructionNormalizer {
	private static final long[] EMPTY_FLOW_VECTOR = {0, 0, 0, 0, 0, 0, 0};
	private static final String TOKEN_GOTO = "GOTO";
	private static final String TOKEN_BRANCH = "BRANCH";
	private static final String TOKEN_RETURN = "RETURN";
	private static final String TOKEN_THROW = "THROW";
	private static final String TOKEN_UNARY = "UNARY";
	private static final String TOKEN_ARITH = "ARITH";
	private static final String TOKEN_ARITH_INT = "ARITH:INT";
	private static final String TOKEN_COMPARE = "COMPARE";
	private static final String TOKEN_CONVERT = "CONVERT";
	private static final String TOKEN_ARRAY_GET = "ARRAY_GET";
	private static final String TOKEN_ARRAY_PUT = "ARRAY_PUT";
	private static final String TOKEN_ARRAY_LENGTH = "ARRAY_LENGTH";
	private static final String TOKEN_MONITOR_ENTER = "MONITOR_ENTER";
	private static final String TOKEN_MONITOR_EXIT = "MONITOR_EXIT";
	private static final String TOKEN_CONST = "CONST:";
	private static final String TOKEN_CONST_NULL = "CONST:NULL";
	private static final String TOKEN_CONST_INT = "CONST:INT";
	private static final String TOKEN_CONST_LONG = "CONST:LONG";
	private static final String TOKEN_CONST_FLOAT = "CONST:FLOAT";
	private static final String TOKEN_CONST_DOUBLE = "CONST:DOUBLE";
	private static final String TOKEN_CONST_STRING = "CONST:STRING";
	private static final String TOKEN_CONST_HANDLE = "CONST:HANDLE";
	private static final String TOKEN_UNKNOWN = "UNKNOWN";
	private static final String PREFIX_INVOKE = "INVOKE:";
	private static final String PREFIX_INVOKE_DYNAMIC = "INVOKE_DYNAMIC:";
	private static final String PREFIX_SWITCH = "SWITCH:";
	private static final String PREFIX_CONST_TYPE = "CONST:TYPE:";
	private static final String PREFIX_CONST_METHOD = "CONST:METHOD:";
	private static final String PREFIX_CONST_HANDLE = "CONST:HANDLE:";
	private static final String PREFIX_CAST = "CAST:";
	private static final String PREFIX_INSTANCEOF = "INSTANCEOF:";
	private static final String PREFIX_NEW = "NEW:";
	private static final String PREFIX_ARRAY_NEW = "ARRAY_NEW:";
	private static final String PREFIX_ARRAY_NEW_FILLED = "ARRAY_NEW_FILLED:";
	private static final String PREFIX_ARRAY_NEW_MULTI = "ARRAY_NEW_MULTI:";
	private static final String PREFIX_ARRAY_FILL = "ARRAY_FILL:";
	private static final String PREFIX_FIELD_GET = "FIELD_GET:";
	private static final String PREFIX_FIELD_PUT = "FIELD_PUT:";
	private static final String PREFIX_FIELD = "FIELD:";
	private static final String PREFIX_TYPE = "TYPE:";
	private static final String PREFIX_OBJECT = "OBJECT:";

	private MethodInstructionNormalizer() {}

	/**
	 * @param node
	 * 		JVM method.
	 *
	 * @return Generalized semantic tokens for executable instructions in the method.
	 */
	@Nonnull
	public static List<String> normalizeInstructions(@Nonnull MethodNode node) {
		List<String> tokens = new ArrayList<>();
		for (AbstractInsnNode insn = node.instructions.getFirst(); insn != null; insn = insn.getNext()) {
			if (AsmInsnUtil.isMetaData(insn))
				continue;
			String token = normalizeInstruction(insn);
			if (token != null)
				tokens.add(token);
		}
		return tokens;
	}

	/**
	 * @param code
	 * 		Dex method code.
	 *
	 * @return Generalized semantic tokens for executable instructions in the method.
	 */
	@Nonnull
	public static List<String> normalizeInstructions(@Nonnull Code code) {
		List<String> tokens = new ArrayList<>();
		for (Instruction instruction : code.getInstructions()) {
			String token = normalizeInstruction(instruction);
			if (token != null)
				tokens.add(token);
		}
		return tokens;
	}

	/**
	 * @param code
	 * 		Dex method code.
	 *
	 * @return {@code true} when the code contains at least one executable instruction.
	 */
	public static boolean hasExecutableInstructions(@Nullable Code code) {
		if (code == null)
			return false;
		for (Instruction instruction : code.getInstructions())
			if (isDexExecutable(instruction))
				return true;
		return false;
	}

	/**
	 * @param method
	 * 		JVM method to analyze.
	 *
	 * @return Vector of control-flow metrics for the method.
	 */
	@Nonnull
	public static long[] computeControlFlowVector(@Nonnull MethodNode method) {
		int size = method.instructions.size();
		if (size == 0)
			return EMPTY_FLOW_VECTOR.clone();

		// Build successor and predecessor maps modeling control flow.
		Int2ObjectMap<List<Integer>> successorMap = new Int2ObjectMap<>();
		Int2ObjectMap<List<Integer>> predecessorMap = new Int2ObjectMap<>();
		AsmInsnUtil.populateFlowMaps(method, successorMap, predecessorMap);
		BitSet reachable = AsmInsnUtil.computeReachable(size, successorMap);

		// Normalize the flow maps by skipping metadata instructions and remapping indices to the next real instruction.
		Int2ObjectMap<Set<Integer>> normalizedSuccessors = new Int2ObjectMap<>();
		Int2ObjectMap<Set<Integer>> normalizedPredecessors = new Int2ObjectMap<>();
		int entry = normalizeInsnIndex(method, 0);
		if (entry >= 0)
			normalizedSuccessors.put(entry, new HashSet<>());
		for (int rawIndex = reachable.nextSetBit(0); rawIndex >= 0; rawIndex = reachable.nextSetBit(rawIndex + 1)) {
			int from = normalizeInsnIndex(method, rawIndex);
			if (from < 0)
				continue;
			Set<Integer> successors = normalizedSuccessors.computeIfAbsent(from, ignored -> new HashSet<>());
			for (int rawSuccessor : successorMap.getOrDefault(rawIndex, Collections.emptyList())) {
				int to = normalizeInsnIndex(method, rawSuccessor);
				if (to < 0)
					continue;
				successors.add(to);
				normalizedPredecessors.computeIfAbsent(to, ignored -> new HashSet<>()).add(from);
			}
		}

		// Identify leaders as the first instruction and any instruction that is a target of a jump or has multiple predecessors.
		Set<Integer> leaders = collectLeaders(entry, normalizedSuccessors, normalizedPredecessors);

		// Count branches, switches, and exits by iterating over reachable instructions and checking their types.
		int branchCount = 0;
		int switchCount = 0;
		int exitCount = 0;
		for (int rawIndex = reachable.nextSetBit(0); rawIndex >= 0; rawIndex = reachable.nextSetBit(rawIndex + 1)) {
			AbstractInsnNode insn = method.instructions.get(rawIndex);
			if (AsmInsnUtil.isMetaData(insn))
				continue;
			int opcode = insn.getOpcode();
			if (insn instanceof JumpInsnNode && opcode != Opcodes.GOTO && opcode != Opcodes.JSR)
				branchCount++;
			else if (insn instanceof TableSwitchInsnNode || insn instanceof LookupSwitchInsnNode)
				switchCount++;
			if (AsmInsnUtil.isReturn(opcode) || opcode == Opcodes.ATHROW)
				exitCount++;
		}

		// Count exception handlers by checking try-catch blocks and counting unique handler targets.
		Set<Integer> handlerTargets = new HashSet<>();
		for (TryCatchBlockNode block : method.tryCatchBlocks) {
			int handlerIndex = normalizeInsnIndex(method, method.instructions.indexOf(block.handler));
			if (handlerIndex >= 0)
				handlerTargets.add(handlerIndex);
		}

		// Build vector from components.
		return buildControlFlowVector(leaders, normalizedSuccessors, branchCount, switchCount, handlerTargets.size(), exitCount);
	}

	/**
	 * @param code
	 * 		Dex method code to analyze.
	 *
	 * @return Vector of control-flow metrics for the method.
	 */
	@Nonnull
	public static long[] computeControlFlowVector(@Nonnull Code code) {
		List<Instruction> instructions = code.getInstructions();
		if (instructions.isEmpty())
			return EMPTY_FLOW_VECTOR.clone();

		// Build successor and predecessor maps modeling control flow.
		Int2ObjectMap<List<Integer>> successorMap = new Int2ObjectMap<>();
		Int2ObjectMap<List<Integer>> predecessorMap = new Int2ObjectMap<>();
		populateFlowMaps(code, successorMap, predecessorMap);
		BitSet reachable = computeReachable(instructions.size(), successorMap);

		// Normalize the flow maps by skipping metadata instructions and remapping indices to the next real instruction.
		Int2ObjectMap<Set<Integer>> normalizedSuccessors = new Int2ObjectMap<>();
		Int2ObjectMap<Set<Integer>> normalizedPredecessors = new Int2ObjectMap<>();
		int entry = normalizeInsnIndex(instructions, 0);
		if (entry >= 0)
			normalizedSuccessors.put(entry, new HashSet<>());
		for (int rawIndex = reachable.nextSetBit(0); rawIndex >= 0; rawIndex = reachable.nextSetBit(rawIndex + 1)) {
			int from = normalizeInsnIndex(instructions, rawIndex);
			if (from < 0)
				continue;
			Set<Integer> successors = normalizedSuccessors.computeIfAbsent(from, ignored -> new HashSet<>());
			for (int rawSuccessor : successorMap.getOrDefault(rawIndex, Collections.emptyList())) {
				int to = normalizeInsnIndex(instructions, rawSuccessor);
				if (to < 0)
					continue;
				successors.add(to);
				normalizedPredecessors.computeIfAbsent(to, ignored -> new HashSet<>()).add(from);
			}
		}

		// Identify leaders as the first instruction and any instruction that is a target of a jump or has multiple predecessors.
		Set<Integer> leaders = collectLeaders(entry, normalizedSuccessors, normalizedPredecessors);

		// Count branches, switches, and exits by iterating over reachable instructions and checking their types.
		int branchCount = 0;
		int switchCount = 0;
		int exitCount = 0;
		for (int rawIndex = reachable.nextSetBit(0); rawIndex >= 0; rawIndex = reachable.nextSetBit(rawIndex + 1)) {
			Instruction instruction = instructions.get(rawIndex);
			if (!isDexExecutable(instruction))
				continue;
			if (instruction instanceof BranchInstruction)
				branchCount++;
			else if (instruction instanceof PackedSwitchInstruction || instruction instanceof SparseSwitchInstruction)
				switchCount++;
			if (instruction instanceof ReturnInstruction || instruction instanceof ThrowInstruction)
				exitCount++;
		}

		// Count exception handlers by checking try-catch ranges and counting unique handler targets.
		Set<Integer> handlerTargets = new HashSet<>();
		Map<Label, Integer> labelIndices = buildDexLabelIndex(instructions);
		for (TryCatch tryCatch : code.tryCatch()) {
			for (Handler handler : tryCatch.handlers()) {
				Integer handlerIndex = labelIndices.get(handler.handler());
				if (handlerIndex == null)
					continue;
				int normalized = normalizeInsnIndex(instructions, handlerIndex);
				if (normalized >= 0)
					handlerTargets.add(normalized);
			}
		}

		// Build vector from components.
		return buildControlFlowVector(leaders, normalizedSuccessors, branchCount, switchCount, handlerTargets.size(), exitCount);
	}

	/**
	 * @param tokens
	 * 		Instruction tokens.
	 *
	 * @return Trigrams of the instruction tokens.
	 */
	@Nonnull
	public static List<String> toTrigrams(@Nonnull List<String> tokens) {
		// Skip if there are too few tokens to form trigrams.
		if (tokens.isEmpty())
			return List.of();
		if (tokens.size() < 3)
			return List.of(String.join("|", tokens));

		// Create triples of consecutive tokens.
		List<String> trigrams = new ArrayList<>(tokens.size() - 2);
		for (int i = 0; i < tokens.size() - 2; i++)
			trigrams.add(tokens.get(i) + "|" + tokens.get(i + 1) + "|" + tokens.get(i + 2));
		return trigrams;
	}

	/**
	 * Count the occurrences of each value in the given collection.
	 *
	 * @param values
	 * 		Collection of values to create a multiset from.
	 *
	 * @return Multiset of the values.
	 */
	@Nonnull
	public static Object2IntMap<String> multiset(@Nonnull Collection<String> values) {
		Object2IntMap<String> counts = new Object2IntMap<>();
		for (String value : values)
			counts.increment(value, 1);
		return counts;
	}

	@Nullable
	private static String normalizeInstruction(@Nonnull AbstractInsnNode insn) {
		return switch (insn.getType()) {
			case AbstractInsnNode.VAR_INSN -> null;
			case AbstractInsnNode.INSN -> normalizeJvmInsn(insn.getOpcode());
			case AbstractInsnNode.INT_INSN -> normalizeJvmIntInstruction((IntInsnNode) insn);
			case AbstractInsnNode.JUMP_INSN ->
					insn.getOpcode() == Opcodes.GOTO || insn.getOpcode() == Opcodes.JSR ? TOKEN_GOTO : TOKEN_BRANCH;
			case AbstractInsnNode.TYPE_INSN -> normalizeJvmTypeInstruction((TypeInsnNode) insn);
			case AbstractInsnNode.FIELD_INSN -> normalizeJvmFieldInstruction((FieldInsnNode) insn);
			case AbstractInsnNode.METHOD_INSN -> PREFIX_INVOKE + methodDescriptorToken(((MethodInsnNode) insn).desc);
			case AbstractInsnNode.INVOKE_DYNAMIC_INSN ->
					PREFIX_INVOKE_DYNAMIC + methodDescriptorToken(((InvokeDynamicInsnNode) insn).desc);
			case AbstractInsnNode.LDC_INSN -> normalizeJvmLdc((LdcInsnNode) insn);
			case AbstractInsnNode.IINC_INSN -> TOKEN_ARITH_INT;
			case AbstractInsnNode.TABLESWITCH_INSN ->
					PREFIX_SWITCH + sizeBucket(((TableSwitchInsnNode) insn).labels.size());
			case AbstractInsnNode.LOOKUPSWITCH_INSN ->
					PREFIX_SWITCH + sizeBucket(((LookupSwitchInsnNode) insn).labels.size());
			case AbstractInsnNode.MULTIANEWARRAY_INSN -> normalizeJvmMultiArray((MultiANewArrayInsnNode) insn);
			default -> null;
		};
	}

	@Nullable
	private static String normalizeInstruction(@Nonnull Instruction instruction) {
		if (instruction instanceof Label || instruction instanceof NopInstruction ||
				instruction instanceof MoveInstruction || instruction instanceof MoveObjectInstruction ||
				instruction instanceof MoveWideInstruction || instruction instanceof MoveResultInstruction ||
				instruction instanceof MoveExceptionInstruction)
			return null;
		return switch (instruction) {
			case ConstInstruction constant -> TOKEN_CONST_INT;
			case ConstWideInstruction constant -> TOKEN_CONST_LONG;
			case ConstStringInstruction constant -> TOKEN_CONST_STRING;
			case ConstTypeInstruction constant ->
					PREFIX_CONST_TYPE + typeToken(Type.getType(constant.type().descriptor()));
			case ConstMethodTypeInstruction constant ->
					PREFIX_CONST_METHOD + methodDescriptorToken(constant.type().descriptor());
			case ConstMethodHandleInstruction constant -> normalizeDexHandleConstant(constant.handle());
			case InvokeInstruction invoke -> PREFIX_INVOKE + methodDescriptorToken(invoke.type().descriptor());
			case InvokeCustomInstruction invoke ->
					PREFIX_INVOKE_DYNAMIC + methodDescriptorToken(invoke.type().descriptor());
			case InstanceFieldInstruction field ->
					dexFieldAccessToken(OpcodeNames.name(field.opcode()), Type.getType(field.type().descriptor()));
			case StaticFieldInstruction field ->
					dexFieldAccessToken(OpcodeNames.name(field.opcode()), Type.getType(field.type().descriptor()));
			case CheckCastInstruction cast -> PREFIX_CAST + typeToken(Type.getType(cast.type().descriptor()));
			case InstanceOfInstruction cast -> PREFIX_INSTANCEOF + typeToken(Type.getType(cast.type().descriptor()));
			case NewInstanceInstruction instance -> PREFIX_NEW + typeToken(Type.getType(instance.type().descriptor()));
			case NewArrayInstruction instance ->
					PREFIX_ARRAY_NEW + typeToken(Type.getType(instance.componentType().descriptor()));
			case FilledNewArrayInstruction array ->
					PREFIX_ARRAY_NEW_FILLED + typeToken(Type.getType(array.componentType().descriptor()));
			case FillArrayDataInstruction array ->
					PREFIX_ARRAY_FILL + sizeBucket(Math.max(1, array.data().length / Math.max(array.elementSize(), 1)));
			case ArrayInstruction array ->
					OpcodeNames.name(array.opcode()).startsWith("aput") ? TOKEN_ARRAY_PUT : TOKEN_ARRAY_GET;
			case ArrayLengthInstruction array -> TOKEN_ARRAY_LENGTH;
			case GotoInstruction jump -> TOKEN_GOTO;
			case BranchInstruction jump -> TOKEN_BRANCH;
			case PackedSwitchInstruction table -> PREFIX_SWITCH + sizeBucket(table.targets().size());
			case SparseSwitchInstruction table -> PREFIX_SWITCH + sizeBucket(table.targets().size());
			case ReturnInstruction ret -> TOKEN_RETURN;
			case ThrowInstruction throwing -> TOKEN_THROW;
			case UnaryInstruction op -> TOKEN_UNARY;
			case BinaryInstruction op -> TOKEN_ARITH;
			case Binary2AddrInstruction op -> TOKEN_ARITH;
			case BinaryLiteralInstruction op -> TOKEN_ARITH;
			case CompareInstruction op -> TOKEN_COMPARE;
			case MonitorInstruction monitor -> monitor.exit() ? TOKEN_MONITOR_EXIT : TOKEN_MONITOR_ENTER;
			default -> null;
		};
	}

	@Nullable
	private static String normalizeJvmInsn(int opcode) {
		if (opcode < 0)
			return null;
		return switch (opcode) {
			case Opcodes.NOP -> null;
			case Opcodes.ACONST_NULL -> TOKEN_CONST_NULL;
			case Opcodes.ICONST_M1, Opcodes.ICONST_0, Opcodes.ICONST_1, Opcodes.ICONST_2,
			     Opcodes.ICONST_3, Opcodes.ICONST_4, Opcodes.ICONST_5 -> TOKEN_CONST_INT;
			case Opcodes.LCONST_0, Opcodes.LCONST_1 -> TOKEN_CONST_LONG;
			case Opcodes.FCONST_0, Opcodes.FCONST_1, Opcodes.FCONST_2 -> TOKEN_CONST_FLOAT;
			case Opcodes.DCONST_0, Opcodes.DCONST_1 -> TOKEN_CONST_DOUBLE;
			case Opcodes.IALOAD, Opcodes.LALOAD, Opcodes.FALOAD, Opcodes.DALOAD,
			     Opcodes.AALOAD, Opcodes.BALOAD, Opcodes.CALOAD, Opcodes.SALOAD -> TOKEN_ARRAY_GET;
			case Opcodes.IASTORE, Opcodes.LASTORE, Opcodes.FASTORE, Opcodes.DASTORE,
			     Opcodes.AASTORE, Opcodes.BASTORE, Opcodes.CASTORE, Opcodes.SASTORE -> TOKEN_ARRAY_PUT;
			case Opcodes.POP, Opcodes.POP2, Opcodes.DUP, Opcodes.DUP_X1, Opcodes.DUP_X2,
			     Opcodes.DUP2, Opcodes.DUP2_X1, Opcodes.DUP2_X2, Opcodes.SWAP -> null;
			case Opcodes.IADD, Opcodes.LADD, Opcodes.FADD, Opcodes.DADD,
			     Opcodes.ISUB, Opcodes.LSUB, Opcodes.FSUB, Opcodes.DSUB,
			     Opcodes.IMUL, Opcodes.LMUL, Opcodes.FMUL, Opcodes.DMUL,
			     Opcodes.IDIV, Opcodes.LDIV, Opcodes.FDIV, Opcodes.DDIV,
			     Opcodes.IREM, Opcodes.LREM, Opcodes.FREM, Opcodes.DREM,
			     Opcodes.ISHL, Opcodes.LSHL, Opcodes.ISHR, Opcodes.LSHR,
			     Opcodes.IUSHR, Opcodes.LUSHR, Opcodes.IAND, Opcodes.LAND,
			     Opcodes.IOR, Opcodes.LOR, Opcodes.IXOR, Opcodes.LXOR -> TOKEN_ARITH;
			case Opcodes.INEG, Opcodes.LNEG, Opcodes.FNEG, Opcodes.DNEG -> TOKEN_UNARY;
			case Opcodes.I2L, Opcodes.I2F, Opcodes.I2D, Opcodes.L2I, Opcodes.L2F, Opcodes.L2D,
			     Opcodes.F2I, Opcodes.F2L, Opcodes.F2D, Opcodes.D2I, Opcodes.D2L, Opcodes.D2F,
			     Opcodes.I2B, Opcodes.I2C, Opcodes.I2S -> TOKEN_CONVERT;
			case Opcodes.LCMP, Opcodes.FCMPL, Opcodes.FCMPG, Opcodes.DCMPL, Opcodes.DCMPG -> TOKEN_COMPARE;
			case Opcodes.IRETURN, Opcodes.LRETURN, Opcodes.FRETURN, Opcodes.DRETURN, Opcodes.ARETURN,
			     Opcodes.RETURN -> TOKEN_RETURN;
			case Opcodes.ARRAYLENGTH -> TOKEN_ARRAY_LENGTH;
			case Opcodes.ATHROW -> TOKEN_THROW;
			case Opcodes.MONITORENTER -> TOKEN_MONITOR_ENTER;
			case Opcodes.MONITOREXIT -> TOKEN_MONITOR_EXIT;
			default -> AsmInsnUtil.getInsnName(opcode);
		};
	}

	@Nonnull
	private static String normalizeJvmIntInstruction(@Nonnull IntInsnNode insn) {
		return insn.getOpcode() == Opcodes.NEWARRAY ?
				PREFIX_ARRAY_NEW + typeToken(Types.newArrayElementType(insn.operand)) :
				TOKEN_CONST_INT;
	}

	@Nonnull
	private static String normalizeJvmTypeInstruction(@Nonnull TypeInsnNode insn) {
		Type type = Type.getObjectType(insn.desc);
		return switch (insn.getOpcode()) {
			case Opcodes.NEW -> PREFIX_NEW + typeToken(type);
			case Opcodes.CHECKCAST -> PREFIX_CAST + typeToken(type);
			case Opcodes.INSTANCEOF -> PREFIX_INSTANCEOF + typeToken(type);
			case Opcodes.ANEWARRAY -> PREFIX_ARRAY_NEW + typeToken(type);
			default -> PREFIX_TYPE + typeToken(type);
		};
	}

	@Nonnull
	private static String normalizeJvmFieldInstruction(@Nonnull FieldInsnNode insn) {
		String prefix = switch (insn.getOpcode()) {
			case Opcodes.GETSTATIC, Opcodes.GETFIELD -> PREFIX_FIELD_GET;
			case Opcodes.PUTSTATIC, Opcodes.PUTFIELD -> PREFIX_FIELD_PUT;
			default -> PREFIX_FIELD;
		};
		return prefix + typeToken(Type.getType(insn.desc));
	}

	@Nonnull
	private static String normalizeJvmLdc(@Nonnull LdcInsnNode insn) {
		Object value = insn.cst;
		return switch (value) {
			case null -> TOKEN_CONST_NULL;
			case String ignored -> TOKEN_CONST_STRING;
			case Short ignored -> TOKEN_CONST_INT;
			case Byte ignored -> TOKEN_CONST_INT;
			case Boolean ignored -> TOKEN_CONST_INT;
			case Character ignored -> TOKEN_CONST_INT;
			case Integer ignored -> TOKEN_CONST_INT;
			case Long ignored -> TOKEN_CONST_LONG;
			case Float ignored -> TOKEN_CONST_FLOAT;
			case Double ignored -> TOKEN_CONST_DOUBLE;
			case Type type -> PREFIX_CONST_TYPE + typeToken(type);
			case org.objectweb.asm.Handle handle -> PREFIX_CONST_HANDLE + methodDescriptorToken(handle.getDesc());
			default -> TOKEN_CONST + value.getClass().getSimpleName().toUpperCase();
		};
	}

	@Nonnull
	private static String normalizeJvmMultiArray(@Nonnull MultiANewArrayInsnNode insn) {
		return PREFIX_ARRAY_NEW_MULTI + insn.dims + ':' + typeToken(Type.getType(insn.desc));
	}

	@Nonnull
	private static String normalizeDexHandleConstant(@Nonnull me.darknet.dex.tree.definitions.constant.Handle handle) {
		if (handle.type() instanceof me.darknet.dex.tree.type.MethodType methodType)
			return PREFIX_CONST_HANDLE + methodDescriptorToken(methodType.descriptor());
		return TOKEN_CONST_HANDLE;
	}

	@Nonnull
	private static String dexFieldAccessToken(@Nonnull String fieldOp, @Nonnull Type type) {
		String prefix = fieldOp.startsWith("iput") || fieldOp.startsWith("sput") ? PREFIX_FIELD_PUT : PREFIX_FIELD_GET;
		return prefix + typeToken(type);
	}

	@Nonnull
	private static Set<Integer> collectLeaders(int entry,
	                                           @Nonnull Int2ObjectMap<Set<Integer>> normalizedSuccessors,
	                                           @Nonnull Int2ObjectMap<Set<Integer>> normalizedPredecessors) {
		Set<Integer> leaders = new HashSet<>();
		if (entry >= 0)
			leaders.add(entry);
		normalizedSuccessors.forEach((k, successors) -> {
			if (successors.size() > 1)
				leaders.addAll(successors);
			for (int successor : successors) {
				if (normalizedPredecessors.getOrDefault(successor, Collections.emptySet()).size() != 1)
					leaders.add(successor);
			}
		});
		return leaders;
	}

	@Nonnull
	private static long[] buildControlFlowVector(@Nonnull Set<Integer> leaders,
	                                             @Nonnull Int2ObjectMap<Set<Integer>> normalizedSuccessors,
	                                             int branchCount,
	                                             int switchCount,
	                                             int handlerCount,
	                                             int exitCount) {
		LongBox edgeCountBox = new LongBox();
		normalizedSuccessors.forEach((k, successors) -> edgeCountBox.increment(successors.size()));
		int edgeCount = Math.toIntExact(edgeCountBox.get());
		int blockCount = leaders.size();
		int cyclomaticComplexity = Math.max(1, edgeCount - blockCount + 2);
		return new long[]{blockCount, edgeCount, branchCount, switchCount, handlerCount, exitCount, cyclomaticComplexity};
	}

	private static int normalizeInsnIndex(@Nonnull MethodNode method, int rawIndex) {
		if (rawIndex < 0 || rawIndex >= method.instructions.size())
			return -1;

		AbstractInsnNode insn = method.instructions.get(rawIndex);
		while (insn != null && AsmInsnUtil.isMetaData(insn))
			insn = insn.getNext();
		return insn == null ? -1 : method.instructions.indexOf(insn);
	}

	private static int normalizeInsnIndex(@Nonnull List<Instruction> instructions, int rawIndex) {
		if (rawIndex < 0 || rawIndex >= instructions.size())
			return -1;
		for (int i = rawIndex; i < instructions.size(); i++)
			if (isDexExecutable(instructions.get(i)))
				return i;
		return -1;
	}

	@Nonnull
	static String methodDescriptorToken(@Nonnull String descriptor) {
		Type methodType = Type.getMethodType(descriptor);
		StringBuilder builder = new StringBuilder();
		builder.append("ARGS:").append(sizeBucket(methodType.getArgumentTypes().length)).append(':');
		for (Type argumentType : methodType.getArgumentTypes())
			builder.append(typeToken(argumentType)).append(',');
		builder.append("RET:").append(typeToken(methodType.getReturnType()));
		return builder.toString();
	}

	@Nonnull
	static String typeToken(@Nonnull Type type) {
		int sort = type.getSort();
		return switch (sort) {
			case Type.VOID,
			     Type.BOOLEAN,
			     Type.CHAR,
			     Type.BYTE,
			     Type.SHORT,
			     Type.INT,
			     Type.FLOAT,
			     Type.LONG,
			     Type.DOUBLE -> Types.getSortName(sort).toUpperCase();
			case Type.ARRAY -> "ARRAY[" + type.getDimensions() + "]:" + typeToken(type.getElementType());
			case Type.METHOD -> methodDescriptorToken(type.getDescriptor());
			case Type.OBJECT -> PREFIX_OBJECT + PackagePurpose.objectBucket(type.getInternalName());
			default -> TOKEN_UNKNOWN;
		};
	}

	@Nonnull
	private static String sizeBucket(int value) {
		if (value <= 0)
			return "0";
		if (value == 1)
			return "1";
		if (value <= 3)
			return "2-3";
		if (value <= 7)
			return "4-7";
		if (value <= 15)
			return "8-15";
		if (value <= 31)
			return "16-31";
		return "32+";
	}
}
