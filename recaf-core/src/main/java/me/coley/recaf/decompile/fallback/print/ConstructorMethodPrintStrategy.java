package me.coley.recaf.decompile.fallback.print;

import me.coley.recaf.decompile.fallback.model.ClassModel;
import me.coley.recaf.decompile.fallback.model.MethodModel;
import me.coley.recaf.util.StringUtil;

/**
 * Method printing strategy for constructors.
 *
 * @author Matt Coley
 */
public class ConstructorMethodPrintStrategy extends BasicMethodPrintStrategy {
	private ClassModel lastParentModel;

	@Override
	public String print(ClassModel parent, MethodModel model) {
		lastParentModel = parent;
		return super.print(parent, model);
	}

	@Override
	protected void buildDeclarationReturnType(StringBuilder sb, MethodModel model) {
		// no-op
	}

	@Override
	protected void buildDeclarationName(StringBuilder sb, MethodModel model) {
		// The name is always the class name
		sb.append(PrintBase.filterShortenName(lastParentModel.getName()));
	}
}
