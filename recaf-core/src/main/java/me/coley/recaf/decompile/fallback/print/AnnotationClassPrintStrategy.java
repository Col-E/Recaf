package me.coley.recaf.decompile.fallback.print;

import me.coley.cafedude.classfile.annotation.Annotation;
import me.coley.recaf.decompile.fallback.model.ClassModel;
import me.coley.recaf.util.AccessFlag;
import me.coley.recaf.util.EscapeUtil;
import me.coley.recaf.util.StringUtil;

import java.util.Set;

/**
 * Class printing strategy for annotation types.
 *
 * @author Matt Coley
 */
public class AnnotationClassPrintStrategy extends InterfaceClassPrintStrategy {
	@Override
	protected void appendDeclaration(Printer out, ClassModel model) {
		int acc = model.getAccess();
		// Get flag-set and remove 'interface' and 'abstract'.
		// We will add 'interface' ourselves, and 'abstract' is redundant.
		Set<AccessFlag> flagSet = AccessFlag.getApplicableFlags(AccessFlag.Type.CLASS, acc);
		flagSet.remove(AccessFlag.ACC_ANNOTATION);
		flagSet.remove(AccessFlag.ACC_INTERFACE);
		flagSet.remove(AccessFlag.ACC_ABSTRACT);
		String decFlagsString = AccessFlag.sortAndToString(AccessFlag.Type.CLASS, flagSet);
		StringBuilder sb = new StringBuilder();
		for (Annotation annotation : model.getAnnotations())
			sb.append(PrintUtils.annotationToString(model.getPool(), annotation)).append("\n");
		if (decFlagsString.isBlank()) {
			sb.append("@interface ");
		} else {
			sb.append(decFlagsString)
					.append(" @interface ");
		}
		sb.append(PrintBase.filterShortenName(model.getName()));
		out.appendLine(sb.toString());
	}
}
