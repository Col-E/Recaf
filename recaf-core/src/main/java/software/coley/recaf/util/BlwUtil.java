package software.coley.recaf.util;

import dev.xdark.blw.asm.internal.Util;
import dev.xdark.blw.code.Instruction;
import dev.xdark.blw.code.instruction.*;
import jakarta.annotation.Nonnull;
import me.darknet.assembler.helper.Variables;
import me.darknet.assembler.printer.InstructionPrinter;
import me.darknet.assembler.printer.PrintContext;
import org.objectweb.asm.tree.*;

import java.util.Collections;

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
		if (insn instanceof LdcInsnNode ldc) {
			return Util.wrapLdcInsn(ldc.cst);
		} else if (insn instanceof MethodInsnNode min) {
			return Util.wrapMethodInsn(min.getOpcode(), min.owner, min.name, min.desc, false);
		} else if (insn instanceof FieldInsnNode fin) {
			return Util.wrapFieldInsn(fin.getOpcode(), fin.owner, fin.name, fin.desc);
		} else if (insn instanceof TypeInsnNode tin) {
			return Util.wrapTypeInsn(tin.getOpcode(), tin.desc);
		} else if (insn instanceof IntInsnNode iin) {
			return Util.wrapIntInsn(iin.getOpcode(), iin.operand);
		} else if (insn instanceof InsnNode in) {
			return Util.wrapInsn(in.getOpcode());
		} else if (insn instanceof InvokeDynamicInsnNode indy) {
			return Util.wrapInvokeDynamicInsn(indy.name, indy.desc, indy.bsm, indy.bsmArgs);
		}

		// Unhandled
		return new SimpleInstruction(-1);
	}

	/**
	 * @param insn
	 * 		ASM instruction.
	 *
	 * @return JASM text representation of BLW converted instance of the instruction.
	 */
	@Nonnull
	public static String toString(@Nonnull AbstractInsnNode insn) {
		PrintContext<?> ctx = new PrintContext<>("");
		InstructionPrinter printer = new InstructionPrinter(ctx.code(),
				null, new Variables(Collections.emptyNavigableMap(), Collections.emptyList()),
				Collections.emptyMap()
		);

		// Map ASM insn model to BLW which is used by JASM
		if (insn instanceof LdcInsnNode ldc) {
			printer.execute(Util.wrapLdcInsn(ldc.cst));
		} else if (insn instanceof MethodInsnNode min) {
			printer.execute(Util.wrapMethodInsn(min.getOpcode(), min.owner, min.name, min.desc, false));
		} else if (insn instanceof FieldInsnNode fin) {
			printer.execute(Util.wrapFieldInsn(fin.getOpcode(), fin.owner, fin.name, fin.desc));
		} else if (insn instanceof TypeInsnNode tin) {
			Instruction wrapped = Util.wrapTypeInsn(tin.getOpcode(), tin.desc);
			if (wrapped instanceof AllocateInstruction allocateWrapped)
				printer.execute(allocateWrapped);
			else if (wrapped instanceof CheckCastInstruction castWrapped)
				printer.execute(castWrapped);
			else if (wrapped instanceof InstanceofInstruction instanceofWrapped)
				printer.execute(instanceofWrapped);
			else
				printer.execute(wrapped);
		} else if (insn instanceof IntInsnNode iin) {
			Instruction wrapped = Util.wrapIntInsn(iin.getOpcode(), iin.operand);
			if (wrapped instanceof ConstantInstruction<?> constWrapped)
				printer.execute(constWrapped);
			else if (wrapped instanceof AllocateInstruction allocateWrapped)
				printer.execute(allocateWrapped);
			else
				printer.execute(wrapped);
		} else if (insn instanceof InsnNode in) {
			Instruction wrapped = Util.wrapInsn(in.getOpcode());
			if (wrapped instanceof ConstantInstruction<?> constWrapped)
				printer.execute(constWrapped);
			else if (wrapped instanceof PrimitiveConversionInstruction convWrapped)
				printer.execute(convWrapped);
			else if (wrapped instanceof SimpleInstruction simpleWrapped)
				printer.execute(simpleWrapped);
			else
				printer.execute(wrapped);
		} else if (insn instanceof InvokeDynamicInsnNode indy) {
			printer.execute(Util.wrapInvokeDynamicInsn(indy.name, indy.desc, indy.bsm, indy.bsmArgs));
		} else {
			// The current search models shouldn't yield anything aside from the above types.
			return "<missing text mapper: " + insn.getClass().getSimpleName() + ">";
		}

		// Cut off first 2 chars of unused indentation then cap off the max length.
		return ctx.toString().substring(2).replace('\n', ' ');
	}
}
