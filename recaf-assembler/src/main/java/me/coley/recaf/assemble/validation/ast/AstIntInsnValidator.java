package me.coley.recaf.assemble.validation.ast;

import me.coley.recaf.assemble.ast.Code;
import me.coley.recaf.assemble.ast.Unit;
import me.coley.recaf.assemble.ast.arch.MethodDefinition;
import me.coley.recaf.assemble.ast.insn.AbstractInstruction;
import me.coley.recaf.assemble.ast.insn.IntInstruction;
import org.objectweb.asm.Opcodes;

import static me.coley.recaf.assemble.validation.ValidationMessage.INT_VAL_TOO_BIG;
import static me.coley.recaf.assemble.validation.ValidationMessage.error;

/**
 * Validates values pushed by {@link IntInstruction} respect the type limitations of the opcode.
 * Other instructions that operate in {@code int} are validated at the parse step. This case is unique
 * because the {@link IntInstruction} may represent variable sized types such as {@code short} or {@code byte}.
 *
 * @author Matt Coley
 */
public class AstIntInsnValidator implements AstValidationVisitor, Opcodes {
	@Override
	public void visit(AstValidator validator) {
		Unit unit = validator.getUnit();
		if (unit.isClass()) {
			for (MethodDefinition method : unit.getDefinitionAsClass().getDefinedMethods()) {
				handle(validator, method);
			}
		} else if (unit.isMethod())
			handle(validator, unit.getDefinitionAsMethod());
	}

	private static void handle(AstValidator validator, MethodDefinition methodDefinition) {
		Code code = methodDefinition.getCode();
		if (code == null)
			return;
		for (AbstractInstruction instruction : code.getInstructions()) {
			// Int instructions may represent variable sized types (short/byte)
			if (instruction instanceof IntInstruction) {
				int valueInt = ((IntInstruction) instruction).getValue();
				switch (instruction.getOpcodeVal()) {
					case BIPUSH:
						if ((valueInt > Byte.MAX_VALUE || valueInt < Byte.MIN_VALUE)) {
							validator.addMessage(error(INT_VAL_TOO_BIG, instruction,
									"BIPUSH expects 'byte' but value '" + valueInt + "' is too large!"));
						}
					case SIPUSH:
						if ((valueInt > Short.MAX_VALUE || valueInt < Short.MIN_VALUE)) {
							validator.addMessage(error(INT_VAL_TOO_BIG, instruction,
									"SIPUSH expects 'short' but value '" + valueInt + "' is too large!"));
						}
					default:
						break;
				}
			}
			// TODO: Because of recent assembler changes, we need to re-address these.
			//       Can be done in the Analyzer via 'markWonky(reason)'
			// - TableSwitchInstruction range is validated in the parse step
			// - LookupSwitchInstruction keys are validated in the parse step
			// - MultiArrayInstruction value is validated in the parse step
			// - LdcInstruction value is validated in the parse step
			// - IincInstruction increment is validated in the parse step
			// - NewArrayInstruction argument is validated in the parse step
		}
	}
}
