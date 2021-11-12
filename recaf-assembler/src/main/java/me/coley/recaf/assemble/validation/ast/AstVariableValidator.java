package me.coley.recaf.assemble.validation.ast;

import me.coley.recaf.assemble.ast.Code;
import me.coley.recaf.assemble.ast.Unit;
import me.coley.recaf.assemble.ast.VariableReference;
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
public class AstVariableValidator implements AstValidationVisitor, Opcodes {
	private static final int DEFAULT_SORT = -1;

	@Override
	public void visit(AstValidator validator) {
		Map<String, AstVarInfo> variables = fromUnit(validator, validator.getUnit());
		for (AstVarInfo info : variables.values()) {
			// Skip if the variable is used incorrectly anyways
			if (info.isUsedBeforeDefined())
				continue;
			// Ensure that variable
			int currentSort = DEFAULT_SORT;
			for (AstVarUsage usage : info.getUsages()) {
				String desc = usage.getImpliedType();
				if (!Types.isValidDesc(desc)) {
					validator.addMessage(error(VAR_ILLEGAL_DESC, "Invalid variable descriptor: '" + desc + "'"));
					break;
				}
				int usageSort = Type.getType(desc).getSort();
				if (usage.getUsageType() == VariableReference.OpType.ASSIGN) {
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

	private static Map<String, AstVarInfo> fromUnit(AstValidator validator, Unit unit) {
		Map<String, AstVarInfo> variables = new HashMap<>();
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
			AstVarInfo info = new AstVarInfo(parameter.getName(), -1);
			info.addUsage(-1, desc, parameter.getVariableOperation());
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
			VariableReference.OpType usage = null;
			if (instruction instanceof VarInstruction) {
				VarInstruction varInsn = (VarInstruction) instruction;
				int opcode = instruction.getOpcodeVal();
				varId = varInsn.getVariableIdentifier();
				varType = Types.fromVarOpcode(opcode);
				usage = varInsn.getVariableOperation();
			} else if (instruction instanceof IincInstruction) {
				IincInstruction iinc = (IincInstruction) instruction;
				varId = iinc.getVariableIdentifier();
				varType = Type.INT_TYPE;
				usage = iinc.getVariableOperation();
			}
			// Record new variable info, update existing info
			if (varId != null && varType != null) {
				AstVarInfo info = variables.get(varId);
				if (info == null) {
					info = new AstVarInfo(varId, instruction.getLine());
					info.addUsage(instruction.getLine(), varType.getDescriptor(), usage);
					variables.put(varId, info);
					// Can't "use" before value is not set
					if (usage != VariableReference.OpType.ASSIGN) {
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
