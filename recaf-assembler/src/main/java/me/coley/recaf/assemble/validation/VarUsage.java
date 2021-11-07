package me.coley.recaf.assemble.validation;

/**
 * Outlines a usage case of a variable.
 *
 * @author Matt Coley
 */
public class VarUsage {
	private final int line;
	private final String impliedType;
	private final VarUsageType usageType;

	/**
	 * @param line
	 * 		Line of the usage.
	 * @param impliedType
	 * 		Implied type of the usage.
	 * @param usageType
	 * 		Type of usage.
	 */
	public VarUsage(int line, String impliedType, VarUsageType usageType) {
		this.line = line;
		this.impliedType = impliedType;
		this.usageType = usageType;
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
	public VarUsageType getUsageType() {
		return usageType;
	}
}
