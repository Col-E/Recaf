package me.coley.recaf.decompile.fallback.print;

import me.coley.cafedude.classfile.annotation.Annotation;
import me.coley.recaf.decompile.fallback.model.ClassModel;
import me.coley.recaf.decompile.fallback.model.FieldModel;
import me.coley.recaf.util.AccessFlag;
import me.coley.recaf.util.EscapeUtil;
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
		appendAnnotations(sb, model);
		appendFlags(sb, model);
		appendTypeAndName(sb, model);
		appendConstValue(sb, model);
		sb.append(';');
		return sb.toString();
	}

	protected void appendAnnotations(StringBuilder sb, FieldModel model) {
		for (Annotation annotation : model.getAnnotations())
			sb.append(PrintUtils.annotationToString(model.getPool(), annotation)).append("\n");
	}

	protected void appendFlags(StringBuilder sb, FieldModel model) {
		Collection<AccessFlag> flags = AccessFlag.getApplicableFlags(AccessFlag.Type.FIELD, model.getAccess());
		flags.remove(AccessFlag.ACC_ENUM); // we don't want to print 'enum' as a flag
		flags = AccessFlag.sort(AccessFlag.Type.FIELD, flags);
		if (!flags.isEmpty())
			sb.append(AccessFlag.toString(flags)).append(' ');
	}

	protected void appendTypeAndName(StringBuilder sb, FieldModel model) {
		Type type = Type.getType(model.getDesc());
		String typeName = EscapeUtil.escapeNonValid(type.getClassName());
		if (typeName.contains("."))
			typeName = typeName.substring(typeName.lastIndexOf(".") + 1);
		sb.append(typeName).append(' ').append(PrintBase.filterName(model.getName()));
	}

	protected void appendConstValue(StringBuilder sb, FieldModel model) {
		Object value = model.getConstValue();
		if (value != null) {
			if (value instanceof String)
				value = "\"" + EscapeUtil.escapeCommon((String) value) + "\"";
			else if (value instanceof Float)
				value = value + "F";
			else if (value instanceof Long)
				value = value + "L";
			sb.append(" = ").append(value);
		}
	}
}
