package me.coley.recaf.decompile.fallback.print;

import me.coley.recaf.decompile.fallback.model.ClassModel;
import me.coley.recaf.decompile.fallback.model.FieldModel;

/**
 * Field printing strategy for enum constants.
 *
 * @author Matt Coley
 */
public class EnumConstFieldPrintStrategy implements FieldPrintStrategy {
	@Override
	public String print(ClassModel parent, FieldModel model) {
		int fieldIndex = parent.getFields().indexOf(model);
		boolean isNextEnumConst = parent.getFields().size() >= fieldIndex &&
				parent.getFields().get(fieldIndex + 1).isEnumConst();
		String name = model.getName();

		if (isNextEnumConst) {
			return name + ", ";
		} else {
			return name + ";" + Printer.FORCE_NEWLINE;
		}
	}
}
