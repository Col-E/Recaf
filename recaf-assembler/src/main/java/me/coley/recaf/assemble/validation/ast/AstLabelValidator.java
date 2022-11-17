package me.coley.recaf.assemble.validation.ast;

import me.coley.recaf.assemble.ast.Code;
import me.coley.recaf.assemble.ast.Unit;
import me.coley.recaf.assemble.ast.arch.MethodDefinition;
import me.coley.recaf.assemble.ast.arch.TryCatch;
import me.coley.recaf.assemble.ast.insn.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static me.coley.recaf.assemble.validation.ValidationMessage.LBL_UNDEFINED;
import static me.coley.recaf.assemble.validation.ValidationMessage.error;

/**
 * Validates references to labels are valid. For example, jumps must point to a label that exists.
 *
 * @author Matt Coley
 */
public class AstLabelValidator implements AstValidationVisitor {
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
		Predicate<String> existence = name -> code.getLabels().containsKey(name);
		for (TryCatch tryCatch : code.getTryCatches()) {
			if (!existence.test(tryCatch.getStartLabel())) {
				validator.addMessage(error(LBL_UNDEFINED, tryCatch,
						"Try-catch start label '" + tryCatch.getStartLabel() + "' does not exist"));
			}
			if (!existence.test(tryCatch.getEndLabel())) {
				validator.addMessage(error(LBL_UNDEFINED, tryCatch,
						"Try-catch end label '" + tryCatch.getEndLabel() + "' does not exist"));
			}
			if (!existence.test(tryCatch.getHandlerLabel())) {
				validator.addMessage(error(LBL_UNDEFINED, tryCatch,
						"Try-catch handler label '" + tryCatch.getHandlerLabel() + "' does not exist"));
			}
		}
		for (AbstractInstruction instruction : code.getInstructions()) {
			if (instruction instanceof JumpInstruction) {
				String label = ((JumpInstruction) instruction).getLabel();
				if (!existence.test(label)) {
					validator.addMessage(error(LBL_UNDEFINED, instruction,
							instruction.getOpcode() + " label '" + label + "' does not exist"));
				}
			}
			if (instruction instanceof LineInstruction) {
				String label = ((LineInstruction) instruction).getLabel();
				if (!existence.test(label)) {
					validator.addMessage(error(LBL_UNDEFINED, instruction,
							instruction.getOpcode() + " label '" + label + "' does not exist"));
				}
			} else if (instruction instanceof TableSwitchInstruction) {
				TableSwitchInstruction table = (TableSwitchInstruction) instruction;
				List<String> labels = new ArrayList<>();
				labels.add(table.getDefaultIdentifier());
				labels.addAll(table.getLabels());
				for (String label : labels) {
					if (!existence.test(label)) {
						validator.addMessage(error(LBL_UNDEFINED, instruction,
								instruction.getOpcode() + " label '" + label + "' does not exist"));
					}
				}
			} else if (instruction instanceof LookupSwitchInstruction) {
				LookupSwitchInstruction lookup = (LookupSwitchInstruction) instruction;
				List<String> labels = new ArrayList<>();
				labels.add(lookup.getDefaultIdentifier());
				labels.addAll(lookup.getEntries().stream()
						.map(LookupSwitchInstruction.Entry::getName)
						.collect(Collectors.toList()));
				for (String label : labels) {
					if (!existence.test(label)) {
						validator.addMessage(error(LBL_UNDEFINED, instruction,
								instruction.getOpcode() + " label '" + label + "' does not exist"));
					}
				}
			}
		}
	}
}
