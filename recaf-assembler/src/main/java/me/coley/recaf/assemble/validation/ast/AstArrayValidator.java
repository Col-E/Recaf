package me.coley.recaf.assemble.validation.ast;

import me.coley.recaf.assemble.ast.Code;
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
		if (validator.getUnit().isField())
			return;
		Code code = validator.getUnit().getDefinitionAsMethod().getCode();
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
