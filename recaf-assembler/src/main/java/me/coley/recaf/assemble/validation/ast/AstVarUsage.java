package me.coley.recaf.assemble.validation.ast;

import me.coley.recaf.assemble.ast.Element;
import me.coley.recaf.assemble.ast.VariableReference;

/**
 * Outlines a usage case of a variable.
 *
 * @author Matt Coley
 */
public class AstVarUsage {
	private final Element source;
	private final int line;
	private final String impliedType;
	private final VariableReference.OpType usageType;

	/**
	 * @param source
	 * 		Element of usage.
	 * @param line
	 * 		Line of the usage.
	 * @param impliedType
	 * 		Implied type of the usage.
	 * @param usageType
	 * 		Type of usage.
	 */
	public AstVarUsage(Element source, int line, String impliedType, VariableReference.OpType usageType) {
		this.source = source;
		this.line = line;
		this.impliedType = impliedType;
		this.usageType = usageType;
	}

	/**
	 * @return Element of usage.
	 */
	public Element getSource() {
		return source;
	}

	/**
	 * @return Line of the usage.
	 */
	public int getLine() {
		return line;
	}

	/**
	 * @return Implied type of the usage.
	 */
	public String getImpliedType() {
		return impliedType;
	}

	/**
	 * @return Type of usage.
	 */
	public VariableReference.OpType getUsageType() {
		return usageType;
	}
}
