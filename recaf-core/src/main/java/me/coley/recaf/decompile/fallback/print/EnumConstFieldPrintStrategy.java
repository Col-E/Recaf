package me.coley.recaf.decompile.fallback.print;

import me.coley.recaf.decompile.fallback.model.ClassModel;
import me.coley.recaf.decompile.fallback.model.FieldModel;

import java.util.List;

/**
 * Field printing strategy for enum constants.
 *
 * @author Matt Coley
 */
public class EnumConstFieldPrintStrategy implements FieldPrintStrategy {
	@Override
	public String print(ClassModel parent, FieldModel model) {
		List<FieldModel> fields = parent.getFields();
		int fieldIndex = fields.indexOf(model);
		boolean isNextEnumConst = fieldIndex < fields.size() - 1 &&
				fields.get(fieldIndex + 1).isEnumConst();
		String name = model.getName();
		if (isNextEnumConst) {
			return name + ", ";
		} else {
			return name + ";" + Printer.FORCE_NEWLINE;
		}
	}
}
