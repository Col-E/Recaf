package me.coley.recaf.assemble.validation.ast;

import me.coley.recaf.assemble.ast.Unit;
import me.coley.recaf.assemble.ast.arch.ConstVal;
import me.coley.recaf.assemble.ast.arch.FieldDefinition;
import me.coley.recaf.util.AccessFlag;
import me.coley.recaf.util.Types;
import org.objectweb.asm.Type;

import static me.coley.recaf.assemble.validation.ValidationMessage.*;

/**
 * Validates {@link ConstVal} is applied correctly.
 *
 * @author Matt Coley
 */
public class AstFieldConstValidator implements AstValidationVisitor {
	@Override
	public void visit(AstValidator validator) {
		Unit unit = validator.getUnit();
		if (unit.isClass()) {
			for (FieldDefinition field : unit.getDefinitionAsClass().getDefinedFields()) {
				handle(validator, field);
			}
		} else if (unit.isField())
			handle(validator, unit.getDefinitionAsField());
	}

	private static void handle(AstValidator validator, FieldDefinition fieldDefinition) {
		// Check const-val on field of wrong type
		// Check const-val on field that isn't a "constant" thus it won't apply at runtime
		ConstVal value = fieldDefinition.getConstVal();
		if (value == null)
			return;
		if (!AccessFlag.isStatic(fieldDefinition.getModifiers().value())) {
			validator.addMessage(warn(CV_VAL_ON_NON_STATIC, value, "Constant value will be ignored, " +
					"field is not 'static'"));
		}
		String desc = fieldDefinition.getDesc();
		switch (value.getValueType()) {
			case HANDLE:
				validator.addMessage(error(CV_VAL_NOT_ALLOWED, value, "Constant value of 'handle' type is not allowed"));
				break;
			case TYPE:
				validator.addMessage(error(CV_VAL_NOT_ALLOWED, value, "Constant value of 'class' type is not allowed"));
				break;
			case STRING:
				if (!desc.equals("Ljava/lang/String;")) {
					validator.addMessage(warn(CV_VAL_WRONG_TYPE, value, "Constant value of 'String' will be ignored, " +
							"field is not a 'String'"));
				}
				break;
			case INTEGER:
				if (!Types.isPrimitive(desc)) {
					validator.addMessage(warn(CV_VAL_WRONG_TYPE, value, "Constant value of 'int' will be ignored, " +
							"field is not an 'int' or other integer based primitive"));
					break;
				}
				int sort = Type.getType(desc).getSort();
				if (sort > Type.INT || sort == Type.VOID) {
					validator.addMessage(warn(CV_VAL_WRONG_TYPE, value, "Constant value of 'int' will be ignored, " +
							"field is not an 'int' or other integer based primitive"));
				}
				// Value must be in range
				int valueInt = (int) value.getValue();
				if ((valueInt > Short.MAX_VALUE || valueInt < Short.MIN_VALUE) && desc.equals("S")) {
					validator.addMessage(warn(CV_VAL_TOO_BIG, value, "Constant value of 'int' will be ignored, " +
							"field is 'short' and " + valueInt + " is too big for 'short'"));

				} else if ((valueInt > Character.MAX_VALUE || valueInt < Character.MIN_VALUE) && desc.equals("C")) {
					validator.addMessage(warn(CV_VAL_TOO_BIG, value, "Constant value of 'int' will be ignored, " +
							"field is 'char' and " + valueInt + " is too big for 'char'"));

				} else if ((valueInt > Byte.MAX_VALUE || valueInt < Byte.MIN_VALUE) && desc.equals("B")) {
					validator.addMessage(warn(CV_VAL_TOO_BIG, value, "Constant value of 'int' will be ignored, " +
							"field is 'byte' and " + valueInt + " is too big for 'byte'"));
				}
				break;
			case FLOAT:
				if (!desc.equals("F")) {
					validator.addMessage(warn(CV_VAL_WRONG_TYPE, value, "Constant value of 'float' will be ignored, " +
							"field is not a 'float'"));
				}
				break;
			case DOUBLE:
				if (!desc.equals("D")) {
					validator.addMessage(warn(CV_VAL_WRONG_TYPE, value, "Constant value of 'double' will be ignored, " +
							"field is not a 'double'"));
				}
				break;
			case LONG:
				if (!desc.equals("J")) {
					validator.addMessage(warn(CV_VAL_WRONG_TYPE, value, "Constant value of 'long' will be ignored, " +
							"field is not a 'long'"));
				}
				break;
		}
	}
}
