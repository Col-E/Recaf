package me.coley.recaf.assemble.validation.ast;

import me.coley.recaf.assemble.ast.Code;
import me.coley.recaf.assemble.ast.Unit;
import me.coley.recaf.assemble.ast.arch.MethodDefinition;
import me.coley.recaf.assemble.ast.insn.AbstractInstruction;
import me.coley.recaf.assemble.ast.insn.NewArrayInstruction;

import static me.coley.recaf.assemble.validation.ValidationMessage.ILLEGAL_FMT;
import static me.coley.recaf.assemble.validation.ValidationMessage.error;

/**
 * Validates array instructions are formatted correctly.
 *
 * @author Matt Coley
 */
public class AstArrayValidator implements AstValidationVisitor {
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
			if (instruction instanceof NewArrayInstruction) {
				NewArrayInstruction newArrayInstruction = (NewArrayInstruction) instruction;
				String t = newArrayInstruction.getArrayType();
				try {
					newArrayInstruction.getArrayTypeInt();
				} catch (Exception ex) {
					validator.addMessage(error(ILLEGAL_FMT, instruction,
							instruction.getOpcode() + " type '" + t + "' is not a valid primitive"));
				}
			}
		}
	}
}
