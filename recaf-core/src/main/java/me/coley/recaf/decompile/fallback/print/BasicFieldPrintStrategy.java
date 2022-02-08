package me.coley.recaf.decompile.fallback.print;

import me.coley.cafedude.annotation.Annotation;
import me.coley.recaf.decompile.fallback.model.ClassModel;
import me.coley.recaf.decompile.fallback.model.FieldModel;
import me.coley.recaf.util.AccessFlag;
import org.objectweb.asm.Type;

import java.util.Collection;

/**
 * Field printing strategy for normal fields.
 *
 * @author Matt Coley
 */
public class BasicFieldPrintStrategy implements FieldPrintStrategy {
	@Override
	public String print(ClassModel parent, FieldModel model) {
		StringBuilder sb = new StringBuilder();
		Type type = Type.getType(model.getDesc());
		String typeName = type.getClassName();
		if (typeName.contains("."))
			typeName = typeName.substring(typeName.lastIndexOf(".") + 1);

		for (Annotation annotation : model.getAnnotations())
			sb.append(PrintUtils.annotationToString(model.getPool(), annotation)).append("\n");

		Collection<AccessFlag> flags = AccessFlag.getApplicableFlags(AccessFlag.Type.FIELD, model.getAccess());
		flags = AccessFlag.sort(AccessFlag.Type.FIELD, flags);
		if (flags.isEmpty()) {
			sb.append(typeName);
		} else {
			sb.append(AccessFlag.toString(flags)).append(' ').append(typeName);
		}

		sb.append(' ').append(model.getName()).append(';');

		// TODO: Support for ConstVal

		return sb.toString();
	}
}
