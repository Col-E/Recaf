package software.coley.recaf.util;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import me.darknet.assembler.util.BlwOpcodes;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ASM instruction utilities.
 *
 * @author Matt Coley
 */
public class AsmInsnUtil implements Opcodes {
	private static final Map<Integer, String> opcodeToName = new HashMap<>();

	static {
		BlwOpcodes.getOpcodes().forEach((name, op) -> opcodeToName.put(op, name));
	}

	/**
	 * @param opcode
	 * 		Some opcode.
	 *
	 * @return Name of opcode.
	 */
	@Nonnull
	public static String getInsnName(int opcode) {
		return opcodeToName.getOrDefault(opcode, "unknown");
	}

	/**
	 * Convert an instruction opcode to a {@link Handle} tag.
	 *
	 * @param opcode
	 * 		Some method or field opcode.
	 *
	 * @return Relevant handle tag.
	 *
	 * @throws IllegalStateException
	 * 		When the opcode does not have a respective handle tag.
	 */
	public static int opcodeToTag(int opcode) {
		return switch (opcode) {
			case INVOKEINTERFACE -> H_INVOKEINTERFACE;
			case INVOKESPECIAL -> H_INVOKESPECIAL;
			case INVOKEVIRTUAL -> H_INVOKEVIRTUAL;
			case INVOKESTATIC -> H_INVOKESTATIC;
			case GETFIELD -> H_GETFIELD;
			case GETSTATIC -> H_GETSTATIC;
			case PUTFIELD -> H_PUTFIELD;
			case PUTSTATIC -> H_PUTSTATIC;
			default -> throw new IllegalStateException("Unsupported opcode: " + opcode);
		};
	}

	/**
	 * Convert a {@link Handle} tag to an instruction opcode.
	 *
	 * @param tag
	 * 		Some method or field handle tag.
	 *
	 * @return Relevant instruction opcode.
	 *
	 * @throws IllegalStateException
	 * 		When the tag does not have a respective handle opcode.
	 */
	public static int tagToOpcode(int tag) {
		return switch (tag) {
			case H_INVOKEINTERFACE -> INVOKEINTERFACE;
			case H_INVOKESPECIAL -> INVOKESPECIAL;
			case H_INVOKEVIRTUAL -> INVOKEVIRTUAL;
			case H_INVOKESTATIC -> INVOKESTATIC;
			case H_GETFIELD -> GETFIELD;
			case H_GETSTATIC -> GETSTATIC;
			case H_PUTFIELD -> PUTFIELD;
			case H_PUTSTATIC -> PUTSTATIC;
			default -> throw new IllegalStateException("Unsupported tag: " + tag);
		};
	}

	/**
	 * @param insn
	 * 		Instruction to get index of.
	 *
	 * @return Index of instruction in containing method.
	 */
	public static int indexOf(@Nonnull AbstractInsnNode insn) {
		int i = 0;
		while ((insn = insn.getPrevious()) != null)
			i++;
		return i;
	}

	/**
	 * @param varInsn
	 * 		Variable instruction.
	 *
	 * @return Type that encompasses the variable being accessed/written to,
	 */
	@Nonnull
	public static Type getTypeForVarInsn(@Nonnull VarInsnNode varInsn) {
		return switch (varInsn.getOpcode()) {
			case Opcodes.ISTORE, Opcodes.ILOAD -> Type.INT_TYPE;
			case Opcodes.LSTORE, Opcodes.LLOAD -> Type.LONG_TYPE;
			case Opcodes.FSTORE, Opcodes.FLOAD -> Type.FLOAT_TYPE;
			case Opcodes.DSTORE, Opcodes.DLOAD -> Type.DOUBLE_TYPE;
			default -> Types.OBJECT_TYPE;
		};
	}

	/**
	 * @param op
	 * 		Instruction opcode.
	 *
	 * @return {@code true} when it is any variable storing instruction.
	 */
	public static boolean isVarStore(int op) {
		return op >= ISTORE && op <= ASTORE;
	}

	/**
	 * @param op
	 * 		Instruction opcode.
	 *
	 * @return {@code true} when it is any variable loading instruction.
	 */
	public static boolean isVarLoad(int op) {
		return op >= ILOAD && op <= ALOAD;
	}

	/**
	 * @param index
	 * 		Variable index.
	 * @param variableType
	 * 		Variable type.
	 *
	 * @return Load instruction for variable type at the given index.
	 */
	@Nonnull
	public static VarInsnNode createVarLoad(int index, @Nonnull Type variableType) {
		return switch (variableType.getSort()) {
			case Type.BOOLEAN, Type.CHAR, Type.BYTE, Type.SHORT, Type.INT -> new VarInsnNode(ILOAD, index);
			case Type.FLOAT -> new VarInsnNode(FLOAD, index);
			case Type.DOUBLE -> new VarInsnNode(DLOAD, index);
			case Type.LONG -> new VarInsnNode(LLOAD, index);
			default -> new VarInsnNode(ALOAD, index);
		};
	}

	/**
	 * @param index
	 * 		Variable index.
	 * @param variableType
	 * 		Variable type.
	 *
	 * @return Store instruction for variable type at the given index.
	 */
	@Nonnull
	public static VarInsnNode createVarStore(int index, @Nonnull Type variableType) {
		return switch (variableType.getSort()) {
			case Type.BOOLEAN, Type.CHAR, Type.BYTE, Type.SHORT, Type.INT -> new VarInsnNode(ISTORE, index);
			case Type.FLOAT -> new VarInsnNode(FSTORE, index);
			case Type.DOUBLE -> new VarInsnNode(DSTORE, index);
			case Type.LONG -> new VarInsnNode(LSTORE, index);
			default -> new VarInsnNode(ASTORE, index);
		};
	}

	/**
	 * Checks in the given method for local vars that have label references
	 * that do not exist in the method's instructions list.
	 *
	 * @param method
	 * 		Method to fix local variables of.
	 */
	public static void fixMissingVariableLabels(@Nonnull MethodNode method) {
		// Must not be abstract
		InsnList instructions = method.instructions;
		if (instructions == null || instructions.size() == 0)
			return;

		// Must have variables to fix
		List<LocalVariableNode> variables = method.localVariables;
		if (variables == null || variables.isEmpty())
			return;

		// Find or create first/last labels
		LabelNode firstLabel = null;
		LabelNode lastLabel = null;
		for (int i = 0; i < instructions.size(); i++)
			if (instructions.get(i) instanceof LabelNode label) {
				firstLabel = label;
				break;
			}
		for (int i = instructions.size() - 1; i >= 0; i--)
			if (instructions.get(i) instanceof LabelNode label) {
				lastLabel = label;
				break;
			}
		if (firstLabel == null)
			instructions.insert(firstLabel = new LabelNode());
		if (lastLabel == null || lastLabel == firstLabel)
			instructions.add(lastLabel = new LabelNode());

		// Find any variables that have invalid labels and reassign them if needed
		for (LocalVariableNode variable : variables) {
			int start = instructions.indexOf(variable.start);
			int end = instructions.indexOf(variable.end);

			// Variable start must be a valid label in the method, and occur before the end label
			if (start < 0 || start > end)
				variable.start = firstLabel;

			// End label must be a valid label in the method
			if (end < 0)
				variable.end = lastLabel;
		}
	}

	/**
	 * @param op
	 * 		Instruction opcode.
	 *
	 * @return {@code true} if the instruction pushes a constant value onto the stack.
	 */
	public static boolean isConstValue(int op) {
		return op >= ACONST_NULL && op <= LDC;
	}

	/**
	 * @param insn
	 * 		Instruction to check.
	 *
	 * @return {@code true} if the instruction pushes a constant {@code int} value onto the stack.
	 */
	public static boolean isConstIntValue(@Nonnull AbstractInsnNode insn) {
		int op = insn.getOpcode();
		if (op == LDC && ((LdcInsnNode) insn).cst instanceof Integer)
			return true;
		return (op >= ICONST_M1 && op <= ICONST_5) || op == SIPUSH || op == BIPUSH;
	}

	/**
	 * @param type
	 * 		Type to push.
	 *
	 * @return Instruction to push a default value of the given type onto the stack.
	 */
	@Nonnull
	public static AbstractInsnNode getDefaultValue(@Nonnull Type type) {
		return switch (type.getSort()) {
			case Type.BOOLEAN, Type.CHAR, Type.BYTE, Type.SHORT, Type.INT -> new InsnNode(ICONST_0);
			case Type.LONG -> new InsnNode(LCONST_0);
			case Type.FLOAT -> new InsnNode(FCONST_0);
			case Type.DOUBLE -> new InsnNode(DCONST_0);
			default -> new InsnNode(ACONST_NULL);
		};
	}

	/**
	 * Create an instruction to hold a given {@code int} value.
	 *
	 * @param value
	 * 		Value to hold.
	 *
	 * @return Insn with const value.
	 */
	@Nonnull
	public static AbstractInsnNode intToInsn(int value) {
		switch (value) {
			case -1:
				return new InsnNode(ICONST_M1);
			case 0:
				return new InsnNode(ICONST_0);
			case 1:
				return new InsnNode(ICONST_1);
			case 2:
				return new InsnNode(ICONST_2);
			case 3:
				return new InsnNode(ICONST_3);
			case 4:
				return new InsnNode(ICONST_4);
			case 5:
				return new InsnNode(ICONST_5);
			default:
				if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
					return new IntInsnNode(BIPUSH, value);
				} else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
					return new IntInsnNode(SIPUSH, value);
				} else {
					return new LdcInsnNode(value);
				}
		}
	}

	/**
	 * Create an instruction to hold a given {@code float} value.
	 *
	 * @param value
	 * 		Value to hold.
	 *
	 * @return Insn with const value.
	 */
	@Nonnull
	public static AbstractInsnNode floatToInsn(float value) {
		if (value == 0)
			return new InsnNode(FCONST_0);
		if (value == 1)
			return new InsnNode(FCONST_1);
		if (value == 2)
			return new InsnNode(FCONST_2);
		return new LdcInsnNode(value);
	}

	/**
	 * Create an instruction to hold a given {@code double} value.
	 *
	 * @param value
	 * 		Value to hold.
	 *
	 * @return Insn with const value.
	 */
	@Nonnull
	public static AbstractInsnNode doubleToInsn(double value) {
		if (value == 0)
			return new InsnNode(DCONST_0);
		if (value == 1)
			return new InsnNode(DCONST_1);
		return new LdcInsnNode(value);
	}

	/**
	 * Create an instruction to hold a given {@code long} value.
	 *
	 * @param value
	 * 		Value to hold.
	 *
	 * @return Insn with const value.
	 */
	@Nonnull
	public static AbstractInsnNode longToInsn(long value) {
		if (value == 0)
			return new InsnNode(LCONST_0);
		if (value == 1)
			return new InsnNode(LCONST_1);
		return new LdcInsnNode(value);
	}

	/**
	 * @param type
	 * 		Some type.
	 *
	 * @return Method return instruction opcode for the given type.
	 */
	public static int getReturnOpcode(@Nonnull Type type) {
		return switch (type.getSort()) {
			case Type.VOID -> RETURN;
			case Type.BOOLEAN, Type.CHAR, Type.BYTE, Type.SHORT, Type.INT -> IRETURN;
			case Type.FLOAT -> FRETURN;
			case Type.LONG -> LRETURN;
			case Type.DOUBLE -> DRETURN;
			default -> ARETURN;
		};
	}

	/**
	 * @param insn
	 * 		Instruction to check.
	 *
	 * @return {@code true} when it is a return operation.
	 */
	public static boolean isReturn(@Nullable AbstractInsnNode insn) {
		if (insn == null)
			return false;
		return isReturn(insn.getOpcode());
	}

	/**
	 * @param op
	 * 		Instruction opcode.
	 *
	 * @return {@code true} when it is a return operation.
	 */
	public static boolean isReturn(int op) {
		return switch (op) {
			case IRETURN, LRETURN, FRETURN, DRETURN, ARETURN, RETURN -> true;
			default -> false;
		};
	}

	/**
	 * @param insn
	 * 		Instruction to check.
	 *
	 * @return {@code true} when it is a label.
	 */
	public static boolean isLabel(@Nullable AbstractInsnNode insn) {
		if (insn == null)
			return false;
		return insn.getType() == AbstractInsnNode.LABEL;
	}

	/**
	 * @param insn
	 * 		Instruction to check.
	 *
	 * @return {@code true} if the instruction modifies the control flow.
	 */
	public static boolean isFlowControl(@Nullable AbstractInsnNode insn) {
		if (insn == null)
			return false;
		int type = insn.getType();
		return type == AbstractInsnNode.JUMP_INSN ||
				type == AbstractInsnNode.TABLESWITCH_INSN ||
				type == AbstractInsnNode.LOOKUPSWITCH_INSN ||
				insn.getOpcode() == ATHROW || insn.getOpcode() == RET;
	}

	/**
	 * Any instruction that is matched by this should be safe to use as the last instruction in a method.
	 * If the last instruction in a method yields {@code false} then there is dangling control flow and
	 * the code is not verifier compatible.
	 *
	 * @param op
	 * 		Instruction opcode.
	 *
	 * @return {@code true} when the opcode represents an instruction that
	 * terminates the method flow, or consistently takes a branch.
	 */
	public static boolean isTerminalOrAlwaysTakeFlowControl(int op) {
		return switch (op) {
			case IRETURN, LRETURN, FRETURN, DRETURN, ARETURN, RETURN, ATHROW, TABLESWITCH, LOOKUPSWITCH, GOTO, JSR ->
					true;
			default -> false;
		};
	}

	/**
	 * @param switchInsn
	 * 		Switch instruction.
	 *
	 * @return {@code true} when all destinations are identical.
	 */
	public static boolean isSwitchEffectiveGoto(@Nonnull TableSwitchInsnNode switchInsn) {
		LabelNode target = switchInsn.dflt;
		for (LabelNode label : switchInsn.labels)
			if (label != target)
				return false;
		return true;
	}

	/**
	 * @param switchInsn
	 * 		Switch instruction.
	 *
	 * @return {@code true} when all destinations are identical.
	 */
	public static boolean isSwitchEffectiveGoto(@Nonnull LookupSwitchInsnNode switchInsn) {
		LabelNode target = switchInsn.dflt;
		for (LabelNode label : switchInsn.labels)
			if (label != target)
				return false;
		return true;
	}

	/**
	 * @param insn
	 * 		Instruction to check.
	 *
	 * @return {@code true} if the instruction represents metadata such as line numbers, stack frames, or a label/offset.
	 */
	public static boolean isMetaData(@Nonnull AbstractInsnNode insn) {
		// The following instruction types set their opcode as '-1'
		// - FrameNode
		// - LabelNode
		// - LineNumberNode
		return insn.getOpcode() == -1;
	}

	/**
	 * @param insn
	 * 		Instruction to begin from.
	 *
	 * @return Next non-metadata instruction.
	 * Can be {@code null} for no next instruction at the end of a method.
	 */
	@Nullable
	public static AbstractInsnNode getNextInsn(@Nonnull AbstractInsnNode insn) {
		AbstractInsnNode next = insn.getNext();
		while (next != null && isMetaData(next))
			next = next.getNext();
		return next;
	}

	/**
	 * @param insn
	 * 		Instruction to begin from.
	 *
	 * @return Previous non-metadata instruction.
	 * Can be {@code null} for no previous instruction at the start of a method.
	 */
	@Nullable
	public static AbstractInsnNode getPreviousInsn(@Nonnull AbstractInsnNode insn) {
		AbstractInsnNode prev = insn.getPrevious();
		while (prev != null && isMetaData(prev))
			prev = prev.getPrevious();
		return prev;
	}

	/**
	 * @param insn
	 * 		Instruction to begin from.
	 *
	 * @return Next non-metadata instruction, following {@link Opcodes#GOTO} if found.
	 * Can be {@code null} for no next instruction at the end of a method.
	 */
	@Nullable
	public static AbstractInsnNode getNextFollowGoto(@Nonnull AbstractInsnNode insn) {
		AbstractInsnNode next = getNextInsn(insn);
		while (next != null && next.getOpcode() == GOTO) {
			JumpInsnNode jin = (JumpInsnNode) next;
			next = getNextInsn(jin.label);
		}
		return next;
	}

	/**
	 * Primarily used for debugging and passing to {@link BlwUtil#toString(Iterable)}.
	 *
	 * @param insn
	 * 		Midpoint instruction.
	 * @param back
	 * 		Steps backwards to take and include in the output.
	 * @param forward
	 * 		Steps forward to take and include in the output.
	 *
	 * @return List of instructions surrounding the given instruction.
	 */
	@Nonnull
	public static List<AbstractInsnNode> getSurrounding(@Nonnull AbstractInsnNode insn, int back, int forward) {
		List<AbstractInsnNode> list = new ArrayList<>(back + forward + 1);
		AbstractInsnNode t = insn;
		for (int i = 0; i < back; i++) {
			t = getPreviousInsn(t);
			if (t == null)
				break;
			list.addFirst(t);
		}
		t = insn;
		list.add(t);
		for (int i = 0; i < forward; i++) {
			t = getNextInsn(t);
			if (t == null)
				break;
			list.add(t);
		}
		return list;
	}

	/**
	 * @param method
	 * 		Containing method of the instruction.
	 * @param insn
	 * 		Instruction to search for.
	 *
	 * @return The {@link TryCatchBlockNode} of a try start-end range containing the given instruction.
	 */
	@Nullable
	public static TryCatchBlockNode getContainingTryBlock(@Nonnull MethodNode method, @Nonnull AbstractInsnNode insn) {
		for (TryCatchBlockNode block : method.tryCatchBlocks) {
			AbstractInsnNode i = block.start;
			while (i != null && i != block.end && i != insn)
				i = i.getNext();
			if (i == insn)
				return block;
		}
		return null;
	}

	/**
	 * Check if the given block of instructions has a catch block handler target.
	 * <p/>
	 * When {@code includeFirstInsn=true} this will include match the first instruction of the block if it is
	 * the label outlined by {@link TryCatchBlockNode#handler}. Otherwise, if {@code false} is passed, then the handler
	 * is somewhere in the middle of the block.
	 *
	 * @param method
	 * 		Containing method to analyze control flow of.
	 * @param block
	 * 		Some arbitrary list of instructions representing a block of code.
	 * @param includeFirstInsn
	 *        {@code true} to count the first instruction of the {@code block} which is assumed to be a
	 *        {@link LabelNode} that is a candidate for being a value of {@link TryCatchBlockNode#handler}.
	 *
	 * @return {@code true} when the block contains a label that is a catch block handler.
	 */
	public static boolean hasHandlerFlowIntoBlock(@Nonnull MethodNode method,
	                                              @Nonnull List<AbstractInsnNode> block,
	                                              boolean includeFirstInsn) {
		int start = includeFirstInsn ? 0 : 1;
		for (TryCatchBlockNode tryCatchBlock : method.tryCatchBlocks)
			if (block.indexOf(tryCatchBlock.handler) >= start)
				return true;
		return false;
	}

	/**
	 * Check if the given block of instructions is referenced by explicit control flow instructions.
	 * <p/>
	 * This does not cover the following cases:
	 * <ul>
	 *     <li>Linear control flow where the previous instruction continues normally to the next instruction.</li>
	 * </ul>
	 * This is checks for explicit control flow references such as:
	 * <ul>
	 *     <li>{@link JumpInsnNode}</li>
	 *     <li>{@link TableSwitchInsnNode}</li>
	 *     <li>{@link LookupSwitchInsnNode}</li>
	 * </ul>
	 *
	 * @param method
	 * 		Containing method to analyze control flow of.
	 * @param block
	 * 		Some arbitrary list of instructions representing a block of code.
	 *
	 * @return {@code true} when the method has control flow outside the given block that flows into the given block.
	 * {@code false} when the given block is never explicitly flowed into via control flow instructions.
	 */
	public static boolean hasInboundFlowReferences(@Nonnull MethodNode method, @Nonnull List<AbstractInsnNode> block) {
		Set<LabelNode> labels = Collections.newSetFromMap(new IdentityHashMap<>());
		for (AbstractInsnNode insn : block)
			if (insn.getType() == AbstractInsnNode.LABEL)
				labels.add((LabelNode) insn);

		// If the block has no labels, then there cannot be any inbound references.
		if (labels.isEmpty())
			return false;

		// No control flow instruction should point to this block *at all*.
		for (AbstractInsnNode insn : method.instructions) {
			// Skip instructions in the given block.
			if (block.contains(insn))
				continue;

			// Check for control-flow instructions pointing to a location in the given block.
			if (insn instanceof JumpInsnNode jump && block.contains(jump.label))
				return true;
			if (insn instanceof TableSwitchInsnNode tswitch) {
				if (labels.contains(tswitch.dflt))
					return true;
				for (LabelNode label : tswitch.labels)
					if (labels.contains(label))
						return true;
			}
			if (insn instanceof LookupSwitchInsnNode lswitch) {
				if (labels.contains(lswitch.dflt))
					return true;
				for (LabelNode label : lswitch.labels)
					if (labels.contains(label))
						return true;
			}
		}

		return false;
	}

	/**
	 * Computes the size of stack items consumed for the given operation of the instruction.
	 * <ul>
	 *     <li>This considers {@code long} and {@code double} types taking two spaces.</li>
	 *     <li>This considers {@code dup} like instructions to not <i>"consume"</i> values.</li>
	 * </ul>
	 *
	 * @param insn
	 * 		Instruction to compute for.
	 *
	 * @return Size of stack consumed. Never negative.
	 *
	 * @see #getSizeProduced(AbstractInsnNode)
	 * @see <a href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5">JVMS 6.5</a>
	 */
	public static int getSizeConsumed(@Nonnull AbstractInsnNode insn) {
		// Just a note about this consumed and the other produced method, ASM has a giant
		// array in MethodWriter that has the total delta, but in some cases you want to know
		// both components that combine into the final delta. It also skips entries that are
		// not constant, so we would still need some edge-case handling anyways.
		int op = insn.getOpcode();
		int type = insn.getType();
		if (type == AbstractInsnNode.MULTIANEWARRAY_INSN) {
			return ((MultiANewArrayInsnNode) insn).dims;
		} else if (type == AbstractInsnNode.METHOD_INSN) {
			MethodInsnNode min = (MethodInsnNode) insn;
			Type methodType = Type.getMethodType(min.desc);
			int count = op == INVOKESTATIC ? 0 : 1;
			for (Type argType : methodType.getArgumentTypes()) {
				count += argType.getSize();
			}
			return count;
		} else if (type == AbstractInsnNode.INVOKE_DYNAMIC_INSN) {
			Type methodType = Type.getMethodType(((InvokeDynamicInsnNode) insn).desc);
			int count = 0;
			for (Type argType : methodType.getArgumentTypes()) {
				count += argType.getSize();
			}
			return count;
		} else if (type == AbstractInsnNode.FIELD_INSN) {
			FieldInsnNode fin = (FieldInsnNode) insn;
			if (op == GETSTATIC)
				return 0;
			if (op == GETFIELD)
				return 1; // owner-value

			if (op == PUTSTATIC)
				return Type.getType(fin.desc).getSize(); // value (can be wide)
			if (op == PUTFIELD)
				return 1 + Type.getType(fin.desc).getSize(); // owner, value (can be wide)
		} else if (type == AbstractInsnNode.FRAME ||
				type == AbstractInsnNode.LABEL ||
				type == AbstractInsnNode.LINE) {
			return 0;
		}
		// noinspection EnhancedSwitchMigration
		switch (op) {
			// visitInsn
			case NOP:
			case ACONST_NULL:
			case ICONST_M1:
			case ICONST_0:
			case ICONST_1:
			case ICONST_2:
			case ICONST_3:
			case ICONST_4:
			case ICONST_5:
			case LCONST_0:
			case LCONST_1:
			case FCONST_0:
			case FCONST_1:
			case FCONST_2:
			case DCONST_0:
			case DCONST_1:
				return 0;
			// visitIntInsn
			case BIPUSH:
			case SIPUSH:
				return 0;
			// visitLdcInsn
			case LDC:
				return 0;
			// visitVarInsn
			case ILOAD:
			case LLOAD:
			case FLOAD:
			case DLOAD:
			case ALOAD:
				return 0;
			// visitInsn
			case IALOAD:
			case LALOAD:
			case FALOAD:
			case DALOAD:
			case AALOAD:
			case BALOAD:
			case CALOAD:
			case SALOAD:
				return 2; // arrayref, index
			// visitVarInsn
			case ISTORE:
			case FSTORE:
			case ASTORE:
				return 1; // value
			case DSTORE:
			case LSTORE:
				return 2; // wide-value
			// visitInsn
			case IASTORE:
			case FASTORE:
			case AASTORE:
			case BASTORE:
			case CASTORE:
			case SASTORE:
				return 3; // arrayref, index, value
			case DASTORE:
			case LASTORE:
				return 4; // arrayref, index, wide-value
			case POP:
				return 1; // value
			case POP2:
				return 2; // value x2 or wide-value
			case DUP:
			case DUP_X1:
			case DUP_X2:
			case DUP2:
			case DUP2_X1:
			case DUP2_X2:
			case SWAP:
				return 0; // Does not "consume" technically
			case IADD:
			case FADD:
			case ISUB:
			case FSUB:
			case IMUL:
			case FMUL:
			case IDIV:
			case FDIV:
			case IREM:
			case FREM:
			case ISHL:
			case ISHR:
			case IUSHR:
			case IAND:
			case IXOR:
			case IOR:
				return 2; // value1, value2
			case LUSHR:
			case LSHR:
			case LSHL:
				return 3; // wide-value1, value2
			case DREM:
			case DDIV:
			case DMUL:
			case DSUB:
			case DADD:
			case LREM:
			case LDIV:
			case LMUL:
			case LSUB:
			case LADD:
			case LAND:
			case LOR:
			case LXOR:
				return 4; // wide-value1, wide-value2
			case INEG:
			case FNEG:
				return 1; // value
			case DNEG:
			case LNEG:
				return 2; // wide-value
			// visitIincInsn
			case IINC:
				return 0;
			// visitInsn
			case I2L:
			case I2F:
			case I2D:
			case F2I:
			case F2L:
			case F2D:
			case I2B:
			case I2C:
			case I2S:
				return 1; // value
			case D2I:
			case D2L:
			case D2F:
			case L2I:
			case L2F:
			case L2D:
				return 2; // wide-value
			case FCMPL:
			case FCMPG:
				return 2; // value1, value2
			case LCMP:
			case DCMPL:
			case DCMPG:
				return 4; // wide-value1, wide-value2
			// visitJumpInsn
			case IFEQ:
			case IFNE:
			case IFLT:
			case IFGE:
			case IFGT:
			case IFLE:
			case IFNULL:
			case IFNONNULL:
				return 1; // value
			case IF_ICMPEQ:
			case IF_ICMPNE:
			case IF_ICMPLT:
			case IF_ICMPGE:
			case IF_ICMPGT:
			case IF_ICMPLE:
			case IF_ACMPEQ:
			case IF_ACMPNE:
				return 2; // value1, value2
			case GOTO:
				return 0;
			case JSR:
				return 0;
			// visitVarInsn
			case RET:
				return 0;
			// visiTableSwitchInsn/visitLookupSwitch
			case TABLESWITCH:
			case LOOKUPSWITCH:
				return 1; // value
			// visitInsn
			case IRETURN:
			case FRETURN:
			case ARETURN:
				return 1; // value
			case LRETURN:
			case DRETURN:
				return 2; // wide-value
			case RETURN:
				return 0;
			// visitTypeInsn
			case NEW:
				return 0;
			// visitIntInsn
			case NEWARRAY:
				return 1; // count
			// visitTypeInsn
			case ANEWARRAY:
				return 1; // count
			// visitInsn
			case ARRAYLENGTH:
				return 1; // array
			case ATHROW:
				return 1; // exception, but it technically should clear the stack
			// visitTypeInsn
			case CHECKCAST:
				return 0; // instance to verify, not technically consumed but referenced
			case INSTANCEOF:
				return 1; // value
			// visitInsn
			case MONITORENTER:
			case MONITOREXIT:
				return 1; // monitor
			default:
				throw new IllegalArgumentException("Unhandled instruction: " + op);
		}
	}

	/**
	 * Computes the size of stack items produced for the given operation of the instruction.
	 * This considers {@code long} and {@code double} types taking two spaces.
	 *
	 * @param insn
	 * 		Instruction to compute for.
	 *
	 * @return Size of stack produced. Never negative.
	 *
	 * @see #getSizeConsumed(AbstractInsnNode)
	 * @see <a href="https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html#jvms-6.5">JVMS 6.5</a>
	 */
	public static int getSizeProduced(AbstractInsnNode insn) {
		int op = insn.getOpcode();
		int type = insn.getType();
		if (type == AbstractInsnNode.METHOD_INSN) {
			Type methodType = Type.getMethodType(((MethodInsnNode) insn).desc);
			return methodType.getReturnType().getSize();
		} else if (type == AbstractInsnNode.INVOKE_DYNAMIC_INSN) {
			Type methodType = Type.getMethodType(((InvokeDynamicInsnNode) insn).desc);
			return methodType.getReturnType().getSize();
		} else if (type == AbstractInsnNode.FIELD_INSN) {
			FieldInsnNode fin = (FieldInsnNode) insn;
			Type fieldtype = Type.getType(fin.desc);
			if (op == GETSTATIC)
				return fieldtype.getSize(); // field type can be wide
			if (op == GETFIELD)
				return fieldtype.getSize(); // field type can be wide
			if (op == PUTSTATIC || op == PUTFIELD)
				return 0;
		} else if (type == AbstractInsnNode.LDC_INSN) {
			Object cst = ((LdcInsnNode) insn).cst;
			return (cst instanceof Double || cst instanceof Long) ? 2 : 1;
		} else if (type == AbstractInsnNode.FRAME ||
				type == AbstractInsnNode.LABEL ||
				type == AbstractInsnNode.LINE) {
			return 0;
		} else if (type == AbstractInsnNode.JUMP_INSN) {
			return op == JSR ? 1 : 0;
		}
		// noinspection EnhancedSwitchMigration
		switch (op) {
			// visitInsn
			case NOP:
				return 0;
			case ACONST_NULL:
			case ICONST_M1:
			case ICONST_0:
			case ICONST_1:
			case ICONST_2:
			case ICONST_3:
			case ICONST_4:
			case ICONST_5:
			case FCONST_0:
			case FCONST_1:
			case FCONST_2:
				return 1; // value
			case LCONST_0:
			case LCONST_1:
			case DCONST_0:
			case DCONST_1:
				return 2; // wide-value
			// visitIntInsn
			case BIPUSH:
			case SIPUSH:
				return 1; // value
			// visitVarInsn
			case ILOAD:
			case FLOAD:
			case ALOAD:
				return 1; // value
			case LLOAD:
			case DLOAD:
				return 2; // wide-value
			// visitInsn
			case IALOAD:
			case FALOAD:
			case AALOAD:
			case BALOAD:
			case CALOAD:
			case SALOAD:
				return 1; // value
			case LALOAD:
			case DALOAD:
				return 2; // wide-value
			// visitVarInsn
			case ISTORE:
			case LSTORE:
			case FSTORE:
			case DSTORE:
			case ASTORE:
				return 0;
			// visitInsn
			case IASTORE:
			case LASTORE:
			case FASTORE:
			case DASTORE:
			case AASTORE:
			case BASTORE:
			case CASTORE:
			case SASTORE:
				return 0;
			case POP:
			case POP2:
				return 0;
			case DUP:
			case DUP_X1:
			case DUP_X2:
				return 1; // stack stuff
			case DUP2:
			case DUP2_X1:
			case DUP2_X2:
				return 2; // stack stuff
			case SWAP:
				return 0; // technically does not introduce
			case IADD:
			case FADD:
			case ISUB:
			case FSUB:
			case IMUL:
			case FMUL:
			case IDIV:
			case FDIV:
			case IREM:
			case FREM:
			case INEG:
			case FNEG:
			case ISHL:
			case ISHR:
			case IUSHR:
			case IAND:
			case IOR:
			case IXOR:
				return 1; // result
			case LDIV:
			case LMUL:
			case LSUB:
			case LADD:
			case LREM:
			case LNEG:
			case LSHL:
			case LSHR:
			case LUSHR:
			case LAND:
			case LOR:
			case LXOR:
			case DADD:
			case DSUB:
			case DMUL:
			case DDIV:
			case DREM:
			case DNEG:
				return 2; // wide-result
			// visitIincInsn
			case IINC:
				return 0;
			// visitInsn
			case I2F:
			case L2I:
			case L2F:
			case F2I:
			case D2I:
			case D2F:
			case I2B:
			case I2C:
			case I2S:
				return 1; // result
			case I2D:
			case L2D:
			case F2D:
			case F2L:
			case D2L:
			case I2L:
				return 2; // wide-result
			case LCMP:
			case FCMPL:
			case FCMPG:
			case DCMPL:
			case DCMPG:
				return 1; // result
			// visitVarInsn
			case RET:
				return 0;
			// visiTableSwitchInsn/visitLookupSwitch
			case TABLESWITCH:
			case LOOKUPSWITCH:
				return 0;
			// visitInsn
			case IRETURN:
			case LRETURN:
			case FRETURN:
			case DRETURN:
			case ARETURN:
			case RETURN:
				return 0;
			// visitTypeInsn
			case NEW:
				return 1; // uninitialized value
			// visitIntInsn
			case NEWARRAY:
				return 1;
			// visitTypeInsn
			case ANEWARRAY:
				return 1;
			// visitInsn
			case ARRAYLENGTH:
				return 1;
			case ATHROW:
				return 0;
			// visitTypeInsn
			case CHECKCAST:
				return 0; // technically does not introduce
			case INSTANCEOF:
				return 1; // result
			// visitInsn
			case MONITORENTER:
				return 0;
			case MONITOREXIT:
				return 0;
			// visitMultiANewArrayInsn
			case MULTIANEWARRAY:
				return 1; // array
			default:
				throw new IllegalArgumentException("Unhandled instruction: " + op);
		}
	}
}
