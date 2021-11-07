package me.coley.recaf.assemble.validation;

import java.util.ArrayList;
import java.util.List;

/**
 * Outlines a summary of variable usage throughout a method.
 *
 * @author Matt Coley
 */
public class VarInfo {
	private final List<VarUsage> usages = new ArrayList<>();
	private final String identifier;
	private final int declaredPos;
	private boolean usedBeforeDefined;

	/**
	 * @param identifier
	 * 		Variable identifier.
	 * @param declaredPos
	 * 		Initial declared position.
	 */
	public VarInfo(String identifier, int declaredPos) {
		this.identifier = identifier;
		this.declaredPos = declaredPos;
	}

	/**
	 * @param line
	 * 		Line used on.
	 * @param desc
	 * 		Type descriptor used.
	 * @param usageType
	 *        Type of usage.
	 */
	public void addUsage(int line, String desc, VarUsageType usageType) {
		usages.add(new VarUsage(line, desc, usageType));
	}

	/**
	 * @return Set of usage cases of the variable.
	 */
	public List<VarUsage> getUsages() {
		return usages;
	}

	/**
	 * @return Variable identifier.
	 */
	public String getIdentifier() {
		return identifier;
	}

	/**
	 * @return First position where the variable is used.
	 */
	public int getDeclaredPos() {
		return declaredPos;
	}

	/**
	 * The first offense of this will always be {@link #getDeclaredPos()}.
	 *
	 * @return {@code true} when the variable is referenced with a load instruction before it's been defined anywhere.
	 */
	public boolean isUsedBeforeDefined() {
		return usedBeforeDefined;
	}

	/**
	 * Used to mark that there is a load attempt on the variable before its ever defined.
	 * See {@link #isUsedBeforeDefined()}.
	 */
	public void markUsedBeforeDefined() {
		usedBeforeDefined = true;
	}
}
