package me.coley.recaf.decompile.fallback.print;

import me.coley.cafedude.classfile.annotation.ElementValue;
import me.coley.recaf.decompile.fallback.model.MethodModel;

/**
 * Method printing strategy for annotation methods.
 *
 * @author Matt Coley
 */
public class AnnotationMethodPrintStrategy extends InterfaceMethodPrintStrategy {
	@Override
	protected void buildDeclarationFlags(StringBuilder sb, MethodModel model) {
		// no-op since all methods are 'public abstract' per interface contract (with additional restrictions)
	}

	@Override
	protected void appendAbstractBody(Printer out, MethodModel model) {
		ElementValue value = model.getAnnotationDefaultValue();
		if (value != null) {
			out.appendLine(" default " + PrintUtils.elementToString(model.getPool(), value) + ";");
		} else {
			out.appendLine(";");
		}
	}
}
