package me.coley.recaf.decompile.fallback.print;

import me.coley.recaf.decompile.fallback.model.ClassModel;
import me.coley.recaf.decompile.fallback.model.MethodModel;

/**
 * Method printing strategy for static initializers.
 *
 * @author Matt Coley
 */
public class StaticInitMethodPrintStrategy extends BasicMethodPrintStrategy {

	@Override
	public String print(ClassModel parent, MethodModel model) {
		return super.print(parent, model);
	}

	@Override
	protected void buildDeclarationReturnType(StringBuilder sb, MethodModel model) {
		// no-op
	}

	@Override
	protected void buildDeclarationFlags(StringBuilder sb, MethodModel model) {
		// force only printing the modifier 'static' even if other flags are present
		sb.append("static ");
	}

	@Override
	protected void buildDeclarationName(StringBuilder sb, MethodModel model) {
		// no-op
	}

	@Override
	protected void buildDeclarationArgs(StringBuilder sb, MethodModel model) {
		// no-op
	}
}
