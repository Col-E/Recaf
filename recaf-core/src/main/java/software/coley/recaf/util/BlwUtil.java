package software.coley.recaf.util;

import dev.xdark.blw.asm.internal.Util;
import dev.xdark.blw.code.ExtensionOpcodes;
import dev.xdark.blw.code.Instruction;
import dev.xdark.blw.code.Label;
import dev.xdark.blw.code.generic.GenericLabel;
import dev.xdark.blw.code.instruction.BranchInstruction;
import dev.xdark.blw.code.instruction.ConditionalJumpInstruction;
import dev.xdark.blw.code.instruction.ImmediateJumpInstruction;
import dev.xdark.blw.code.instruction.SimpleInstruction;
import dev.xdark.blw.code.instruction.VarInstruction;
import dev.xdark.blw.code.instruction.VariableIncrementInstruction;
import dev.xdark.blw.simulation.ExecutionEngines;
import jakarta.annotation.Nonnull;
import me.darknet.assembler.helper.Variables;
import me.darknet.assembler.printer.InstructionPrinter;
import me.darknet.assembler.printer.PrintContext;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Misc blw utilities.
 *
 * @author Matt Coley
 */
public class BlwUtil {
	/**
	 * @param insn
	 * 		ASM instruction.
	 *
	 * @return BLW instruction.
	 */
	@Nonnull
	public static Instruction convert(@Nonnull AbstractInsnNode insn) {
		return switch (insn) {
			case LdcInsnNode ldc -> Util.wrapLdcInsn(ldc.cst);
			case MethodInsnNode min -> Util.wrapMethodInsn(min.getOpcode(), min.owner, min.name, min.desc, false);
			case FieldInsnNode fin -> Util.wrapFieldInsn(fin.getOpcode(), fin.owner, fin.name, fin.desc);
			case TypeInsnNode tin -> Util.wrapTypeInsn(tin.getOpcode(), tin.desc);
			case IntInsnNode iin -> Util.wrapIntInsn(iin.getOpcode(), iin.operand);
			case InsnNode in -> Util.wrapInsn(in.getOpcode());
			case InvokeDynamicInsnNode indy -> Util.wrapInvokeDynamicInsn(indy.name, indy.desc, indy.bsm, indy.bsmArgs);
			case VarInsnNode vin -> new VarInstruction(insn.getOpcode(), vin.var);
			case IincInsnNode iin -> new VariableIncrementInstruction(iin.var, iin.incr);
			case JumpInsnNode jin -> {
				int offset = AsmInsnUtil.indexOf(jin.label);
				Label label = new GenericLabel();
				label.setIndex(offset);
				yield jin.getOpcode() == Opcodes.GOTO ?
						new ImmediateJumpInstruction(insn.getOpcode(), label) :
						new ConditionalJumpInstruction(insn.getOpcode(), label);
			}
			case LabelNode ln -> new LabelInstruction(AsmInsnUtil.indexOf(ln));
			case FrameNode fr -> new SimpleInstruction(0);
			default -> new SimpleInstruction(insn.getOpcode());
		};
	}

	/**
	 * @param insn
	 * 		ASM instruction.
	 *
	 * @return JASM text representation of BLW converted instance of the instruction.
	 */
	@Nonnull
	public static String toString(@Nonnull AbstractInsnNode insn) {
		Instruction converted = convert(insn);
		return toString(converted);
	}

	/**
	 * @param insn
	 * 		BLW instruction.
	 *
	 * @return JASM text representation of BLW instruction.
	 */
	@Nonnull
	private static String toString(@Nonnull Instruction insn) {
		// Special case for our converted model
		Map<Integer, String> labelNames;
		if (insn instanceof LabelInstruction label) {
			int index = label.getIndex();
			labelNames = Map.of(index, "L" + index);
		} else if (insn instanceof BranchInstruction branch) {
			labelNames = branch.targetsStream()
					.collect(Collectors.toMap(Label::getIndex, l -> "L" + l.getIndex()));
		} else {
			labelNames = Collections.emptyMap();
		}

		PrintContext<?> ctx = new PrintContext<>("");
		Variables emptyVariables = new Variables(Collections.emptyNavigableMap(), Collections.emptyList());
		InstructionPrinter printer = new InstructionPrinter(ctx.code(), null, emptyVariables, labelNames);

		int op = insn.opcode();
		if (insn instanceof LabelInstruction label) {
			printer.label(label);
		} else if (op >= 0 && op <= ExtensionOpcodes.PRIMITIVE_CONVERSION) {
			ExecutionEngines.execute(printer, insn);
		} else {
			// The current search models shouldn't yield anything aside from the above types.
			return "<missing text mapper: " + insn.getClass().getSimpleName() + ":" + op + ">";
		}

		// Cut off first 2 chars of unused indentation then cap off the max length.
		return ctx.toString().substring(2).replace('\n', ' ');
	}

	/**
	 * Dummy instruction to facilitate label printing in {@link #toString(Instruction)}.
	 */
	private static class LabelInstruction implements Label, Instruction {
		private int index;

		public LabelInstruction(int index) {
			setIndex(index);
		}

		@Override
		public int opcode() {
			return -2;
		}

		@Override
		public int getIndex() {
			return index;
		}

		@Override
		public void setIndex(int index) {
			this.index = index;
		}

		@Override
		public int getLineNumber() {
			return -1;
		}

		@Override
		public void setLineNumber(int line) {
			// no-op
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof LabelInstruction that)) return false;
			return index == that.index;
		}

		@Override
		public int hashCode() {
			return index;
		}
	}
}
