package me.coley.recaf.assemble.validation.ast;

import me.coley.recaf.assemble.ast.Element;
import me.coley.recaf.assemble.ast.VariableReference;

import java.util.ArrayList;
import java.util.List;

/**
 * Outlines a summary of variable usage throughout a method.
 *
 * @author Matt Coley
 */
public class AstVarInfo {
	private final List<AstVarUsage> usages = new ArrayList<>();
	private final String name;
	private final int declaredPos;
	private boolean usedBeforeDefined;

	/**
	 * @param name
	 * 		Variable identifier.
	 * @param declaredPos
	 * 		Initial declared position.
	 */
	public AstVarInfo(String name, int declaredPos) {
		this.name = name;
		this.declaredPos = declaredPos;
	}

	/**
	 * @param element
	 * 		Element of usage.
	 * @param desc
	 * 		Type descriptor used.
	 * @param usageType
	 * 		Type of usage.
	 */
	public void addUsage(Element element, String desc, VariableReference.OpType usageType) {
		usages.add(new AstVarUsage(element, element.getLine(), desc, usageType));
	}

	/**
	 * @return Set of usage cases of the variable.
	 */
	public List<AstVarUsage> getUsages() {
		return usages;
	}

	/**
	 * @return Variable identifier.
	 */
	public String getName() {
		return name;
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
