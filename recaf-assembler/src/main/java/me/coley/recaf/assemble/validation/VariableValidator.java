package me.coley.recaf.assemble.validation;

import me.coley.recaf.assemble.ast.Code;
import me.coley.recaf.assemble.ast.Unit;
import me.coley.recaf.assemble.ast.arch.MethodDefinition;
import me.coley.recaf.assemble.ast.arch.MethodParameter;
import me.coley.recaf.assemble.ast.insn.AbstractInstruction;
import me.coley.recaf.assemble.ast.insn.IincInstruction;
import me.coley.recaf.assemble.ast.insn.VarInstruction;
import me.coley.recaf.util.Types;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.HashMap;
import java.util.Map;

import static me.coley.recaf.assemble.validation.ValidationMessage.*;

/**
 * Basic variable usage validation. Stuff like:
 * <ul>
 *     <li>Not allowing a LOAD operation before a variable is defined</li>
 *     <li>Not allowing a ISTORE then using it later with ALOAD</li>
 * </ul>
 *
 * @author Matt Coley
 */
public class VariableValidator implements ValidationVisitor, Opcodes {
	private static final int DEFAULT_SORT = -1;

	@Override
	public void visit(Validator validator) {
		Map<String, VarInfo> variables = fromUnit(validator, validator.getUnit());
		for (VarInfo info : variables.values()) {
			// Skip if the variable is used incorrectly anyways
			if (info.isUsedBeforeDefined())
				continue;
			// Ensure that variable
			int currentSort = DEFAULT_SORT;
			for (VarUsage usage : info.getUsages()) {
				String desc = usage.getImpliedType();
				if (!Types.isValidDesc(desc)) {
					validator.addMessage(error(VAR_ILLEGAL_DESC, "Invalid variable descriptor: '" + desc + "'"));
					break;
				}
				int usageSort = Type.getType(desc).getSort();
				if (usage.getUsageType() == VarUsageType.STORE) {
					currentSort = usageSort;
				} else {
					if (usageSort != currentSort) {
						String currentSortName = Types.getSortName(currentSort);
						String usageSortName = Types.getSortName(usageSort);
						validator.addMessage(error(VAR_USE_OF_DIFF_TYPE,
								"Tried to use type as '" + usageSortName + "' but expected '" + currentSortName + "'"));
					}
				}
			}
		}
	}

	private static Map<String, VarInfo> fromUnit(Validator validator, Unit unit) {
		Map<String, VarInfo> variables = new HashMap<>();
		// Skip for fields
		if (unit.isField())
			return variables;
		MethodDefinition definition = (MethodDefinition) unit.getDefinition();
		// Skip if no code-items
		if (unit.getCode() == null)
			return variables;
		Code code = unit.getCode();
		// Pull from parameters
		for (MethodParameter parameter : definition.getParams().getParameters()) {
			String desc = parameter.getDesc();
			VarInfo info = new VarInfo(parameter.getName(), -1);
			info.addUsage(-1, desc, VarUsageType.STORE);
			variables.put(parameter.getName(), info);
			if (!Types.isValidDesc(desc)) {
				validator.addMessage(error(VAR_ILLEGAL_DESC,
						"Parameters must use the descriptor format, '" + desc + "' is invalid"));
			}
		}
		// Pull refs from instructions.
		// TODO: We are currently iterating over instructions in order of their appearance.
		//       But we will eventually want to iterate over them in order of logical flow.
		//        - Branch-not-taken will be visited first, then branch-taken
		//        - Continue following program PC until all paths are taken
		//        - Any non-visited code is "dead code" and we can mark as a warning
		//       May want to make a "InstructionFlowVisitor" that generically visits in order
		//       and takes in a consumer to invoke.
		for (AbstractInstruction instruction : code.getInstructions()) {
			// Get usage info
			String varId = null;
			Type varType = null;
			VarUsageType usage = null;
			if (instruction instanceof VarInstruction) {
				int opcode = instruction.getOpcodeVal();
				varId = ((VarInstruction) instruction).getIdentifier();
				varType = Types.fromVarOpcode(opcode);
				usage = opcode == ALOAD || opcode == ILOAD || opcode == FLOAD || opcode == DLOAD || opcode == LLOAD
						? VarUsageType.LOAD : VarUsageType.STORE;
			} else if (instruction instanceof IincInstruction) {
				varId = ((IincInstruction) instruction).getIdentifier();
				varType = Type.INT_TYPE;
				usage = VarUsageType.IINC;
			}
			// Record new variable info, update existing info
			if (varId != null && varType != null) {
				VarInfo info = variables.get(varId);
				if (info == null) {
					info = new VarInfo(varId, instruction.getLine());
					info.addUsage(instruction.getLine(), varType.getDescriptor(), usage);
					variables.put(varId, info);
					// Can't "use" before value is not set
					if (usage == VarUsageType.LOAD || usage == VarUsageType.IINC) {
						info.markUsedBeforeDefined();
						validator.addMessage(error(VAR_USE_BEFORE_DEF, "'" + varId + "' used before declared"));
					}
				} else {
					info.addUsage(instruction.getLine(), varType.getDescriptor(), usage);
				}
			}
		}
		return variables;
	}
}
