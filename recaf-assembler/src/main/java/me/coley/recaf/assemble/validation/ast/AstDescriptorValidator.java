package me.coley.recaf.assemble.validation.ast;

import me.coley.recaf.assemble.ast.ArgType;
import me.coley.recaf.assemble.ast.Code;
import me.coley.recaf.assemble.ast.Unit;
import me.coley.recaf.assemble.ast.arch.MethodDefinition;
import me.coley.recaf.assemble.ast.insn.*;
import me.coley.recaf.util.Types;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;

import java.util.function.Predicate;

import static me.coley.recaf.assemble.validation.ValidationMessage.ILLEGAL_DESC;
import static me.coley.recaf.assemble.validation.ValidationMessage.error;

/**
 * Validates descriptor strings are valid.
 *
 * @author Matt Coley
 */
public class AstDescriptorValidator implements AstValidationVisitor {
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
		Predicate<String> isValid = Types::isValidDesc;
		Code code = methodDefinition.getCode();
		if (code == null)
			return;
		for (AbstractInstruction instruction : code.getInstructions()) {
			if (instruction instanceof FieldInstruction) {
				String desc = ((FieldInstruction) instruction).getDesc();
				if (!isValid.test(desc)) {
					validator.addMessage(error(ILLEGAL_DESC, instruction,
							instruction.getOpcode() + " field descriptor '" + desc + "' is malformed"));
				}
			} else if (instruction instanceof MethodInstruction) {
				String desc = ((MethodInstruction) instruction).getDesc();
				if (!isValid.test(desc)) {
					validator.addMessage(error(ILLEGAL_DESC, instruction,
							instruction.getOpcode() + " method descriptor '" + desc + "' is malformed"));
				}
			} else if (instruction instanceof MultiArrayInstruction) {
				String desc = ((MultiArrayInstruction) instruction).getDesc();
				if (!isValid.test(desc)) {
					validator.addMessage(error(ILLEGAL_DESC, instruction,
							instruction.getOpcode() + " multi-array descriptor '" + desc + "' is malformed"));
				}
			} else if (instruction instanceof LdcInstruction) {
				LdcInstruction ldc = (LdcInstruction) instruction;
				if (ldc.getValueType() == ArgType.TYPE) {
					Type type = (Type) ldc.getValue();
					if (type.getSort() < Type.ARRAY)
						continue;
					String desc = type.getDescriptor();
					if (desc.equals(type.getInternalName()))
						continue;
					if (!isValid.test(desc)) {
						validator.addMessage(error(ILLEGAL_DESC, instruction,
								instruction.getOpcode() + " const descriptor '" + desc + "' is malformed"));
					}
				} else if (ldc.getValueType() == ArgType.HANDLE) {
					String desc = ((Handle) ldc.getValue()).getDesc();
					if (!isValid.test(desc)) {
						validator.addMessage(error(ILLEGAL_DESC, instruction,
								instruction.getOpcode() + " const handle descriptor '" + desc + "' is malformed"));
					}
				}
			} else if (instruction instanceof IndyInstruction) {
				IndyInstruction indy = (IndyInstruction) instruction;
				String desc = indy.getDesc();
				if (!isValid.test(desc)) {
					validator.addMessage(error(ILLEGAL_DESC, instruction,
							instruction.getOpcode() + " descriptor '" + desc + "' is malformed"));
				}
				desc = indy.getBsmHandle().getDesc();
				if (!isValid.test(desc)) {
					validator.addMessage(error(ILLEGAL_DESC, instruction,
							instruction.getOpcode() + "handle descriptor '" + desc + "' is malformed"));
				}
				for (IndyInstruction.BsmArg arg : indy.getBsmArguments()) {
					if (arg.getType() == ArgType.TYPE) {
						Type type = (Type) arg.getValue();
						if (type.getSort() < Type.ARRAY)
							continue;
						desc = type.getDescriptor();
						if (desc.equals(type.getInternalName()))
							continue;
						if (!isValid.test(desc)) {
							validator.addMessage(error(ILLEGAL_DESC, instruction,
									instruction.getOpcode() + " bsm-arg descriptor '" + desc + "' is malformed"));
						}
					} else if (arg.getType() == ArgType.HANDLE) {
						desc = ((Handle) arg.getValue()).getDesc();
						if (!isValid.test(desc)) {
							validator.addMessage(error(ILLEGAL_DESC, instruction,
									instruction.getOpcode() + " bsm-arg handle descriptor '" + desc + "' is malformed"));
						}
					}
				}
			}
		}
	}
}
