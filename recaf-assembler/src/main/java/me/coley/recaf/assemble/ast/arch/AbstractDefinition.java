package me.coley.recaf.assemble.ast.arch;

import me.coley.recaf.assemble.ast.PrintContext;

/**
 * Common base implementation for definition types.
 *
 * @author Matt Coley
 * @author Justus Garbe
 */
public abstract class AbstractDefinition extends AttributeContainer implements Definition {
	private Modifiers modifiers;
	private boolean deprecated;

	@Override
	public Modifiers getModifiers() {
		return modifiers;
	}

	public void setModifiers(Modifiers modifiers) {
		this.modifiers = modifiers;
	}

	@Override
	public boolean isDeprecated() {
		return deprecated;
	}

	public void setDeprecated(boolean deprecated) {
		this.deprecated = deprecated;
	}

	protected String buildDefString(PrintContext context, String type) {
		StringBuilder sb = new StringBuilder();
		sb.append(super.buildDefString(context));
		if (deprecated)
			sb.append(context.fmtKeyword("deprecated")).append("\n");
		sb.append(type).append(" ");
		if (getModifiers().value() > 0) {
			sb.append(getModifiers().print(context).toLowerCase()).append(' ');
		}
		return sb.toString();
	}
}
