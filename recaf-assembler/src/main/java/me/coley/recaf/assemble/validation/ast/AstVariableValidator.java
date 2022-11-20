package me.coley.recaf.assemble.validation.ast;

import me.coley.recaf.assemble.AstException;
import me.coley.recaf.assemble.IllegalAstException;
import me.coley.recaf.assemble.ast.Code;
import me.coley.recaf.assemble.ast.Unit;
import me.coley.recaf.assemble.ast.VariableReference;
import me.coley.recaf.assemble.ast.arch.MethodDefinition;
import me.coley.recaf.assemble.ast.arch.MethodParameter;
import me.coley.recaf.assemble.ast.insn.AbstractInstruction;
import me.coley.recaf.assemble.ast.insn.IincInstruction;
import me.coley.recaf.assemble.ast.insn.VarInstruction;
import me.coley.recaf.util.AccessFlag;
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
	public void visit(AstValidator validator) throws AstException {
		Unit unit = validator.getUnit();
		if (unit.isClass()) {
			for (MethodDefinition method : unit.getDefinitionAsClass().getDefinedMethods()) {
				handle(validator, method);
			}
		} else if (unit.isMethod())
			handle(validator, unit.getDefinitionAsMethod());
	}

	private static void handle(AstValidator validator, MethodDefinition methodDefinition) {
		Map<String, AstVarInfo> variables = fromUnit(validator, methodDefinition);
		// Ensure 'this' is allowed
		if (!AccessFlag.isStatic(validator.getUnit().getDefinition().getModifiers().value())) {
			AstVarInfo thisVar = variables.get("this");
			if (thisVar != null)
				thisVar.addUsage(validator.getUnit().getDefinition(), Types.OBJECT_TYPE.getDescriptor(), VariableReference.OpType.ASSIGN);
		}
		for (AstVarInfo info : variables.values()) {
			// Skip if the variable is used incorrectly anyways
			if (info.isUsedBeforeDefined())
				continue;
			// Ensure that variable
			int currentSort = -1;
			try {
				currentSort = getDefaultSort(info);
			} catch (IllegalAstException ex) {
				validator.addMessage(error(ILLEGAL_DESC, ex.getSource(), "Invalid variable descriptor for '" + info.getName() + "'"));
				continue;
			}
			for (AstVarUsage usage : info.getUsages()) {
				String desc = usage.getImpliedType();
				if (!Types.isValidDesc(desc)) {
					validator.addMessage(error(ILLEGAL_DESC, usage.getSource(), "Invalid variable descriptor: '" + desc + "'"));
					break;
				}
				int usageSort = Type.getType(desc).getSort();
				if (usage.getUsageType() == VariableReference.OpType.ASSIGN) {
					currentSort = usageSort;
				} else {
					if (usageSort == currentSort) {
						// Same type
					} else if (usageSort <= Type.INT && currentSort <= Type.INT) {
						// Any int sub-type can be used together with other int sub-types.
					} else if (usageSort >= Type.ARRAY && currentSort >= Type.ARRAY) {
						// Any object type can be used with another
					} else {
						String currentTypeName = Types.getSortName(usageSort);
						String lastTypeName = Types.getSortName(currentSort);
						validator.addMessage(error(VAR_USE_OF_DIFF_TYPE, usage.getSource(),
								"Tried to use var '" + info.getName() + "' type as '" + currentTypeName + "' but expected '" + lastTypeName + "'"));
					}
				}
			}
		}
	}

	private static int getDefaultSort(AstVarInfo info) throws IllegalAstException {
		if (info.getUsages().isEmpty())
			return DEFAULT_SORT;
		AstVarUsage usage = info.getUsages().get(info.getUsages().size() - 1);
		String typeDesc = usage.getImpliedType();
		if (!Types.isValidDesc(typeDesc))
			throw new IllegalAstException(usage.getSource(), "Malformed descriptor: " + typeDesc);
		return Type.getType(typeDesc).getSort();
	}

	private static Map<String, AstVarInfo> fromUnit(AstValidator validator, MethodDefinition methodDefinition) {
		Map<String, AstVarInfo> variables = new HashMap<>();
		// Skip if no code-items
		if (methodDefinition.getCode().isEmpty())
			return variables;
		Code code = methodDefinition.getCode();
		// Pull from parameters
		for (MethodParameter parameter : methodDefinition.getParams().getParameters()) {
			String desc = parameter.getDesc();
			AstVarInfo info = new AstVarInfo(parameter.getName(), -1);
			info.addUsage(parameter, desc, parameter.getVariableOperation());
			variables.put(parameter.getName(), info);
			if (!Types.isValidDesc(desc)) {
				validator.addMessage(error(ILLEGAL_DESC, parameter,
						"Parameters must use the descriptor format, '" + desc + "' is invalid"));
			}
		}
		//  Pull refs from instructions.
		// TODO: Fix problem visiting in order of logical flow rather than linear order of appearance.
		for (AbstractInstruction instruction : code.getInstructions()) {
			// Get usage info
			String varId = null;
			Type varType = null;
			VariableReference.OpType usage = null;
			if (instruction instanceof VarInstruction) {
				VarInstruction varInsn = (VarInstruction) instruction;
				int opcode = instruction.getOpcodeVal();
				varId = varInsn.getEscapedVariableIdentifier();
				varType = Types.fromVarOpcode(opcode);
				usage = varInsn.getVariableOperation();
			} else if (instruction instanceof IincInstruction) {
				IincInstruction iinc = (IincInstruction) instruction;
				varId = iinc.getEscapedVariableIdentifier();
				varType = Type.INT_TYPE;
				usage = iinc.getVariableOperation();
			}
			// Record new variable info, update existing info
			if (varId != null && varType != null) {
				AstVarInfo info = variables.get(varId);
				if (info == null) {
					info = new AstVarInfo(varId, instruction.getLine());
					info.addUsage(instruction, varType.getDescriptor(), usage);
					variables.put(varId, info);
					// Can't "use" before value is not set... Unless it's "this" which always exists
					if ("this".equals(varId) && !AccessFlag.isStatic(methodDefinition.getModifiers().value()))
						continue;
					if (usage != VariableReference.OpType.ASSIGN) {
						info.markUsedBeforeDefined();
						validator.addMessage(error(VAR_USE_BEFORE_DEF, instruction, "'" + varId + "' used before declared"));
					}
				} else {
					info.addUsage(instruction, varType.getDescriptor(), usage);
				}
			}
		}
		return variables;
	}
}
