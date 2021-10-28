package me.coley.recaf.assemble.ast.arch;

import me.coley.recaf.assemble.ast.BaseElement;

/**
 * Definition of a method.
 *
 * @author Matt Coley
 */
public class MethodDefinition extends BaseElement implements MemberDefinition {
	private final Modifiers modifiers;
	private final String name;
	private final MethodParameters params;
	private final String returnType;

	/**
	 * @param modifiers
	 * 		Method modifiers.
	 * @param name
	 * 		Method name.
	 * @param params
	 * 		Method parameters.
	 * @param returnType
	 * 		Method return descriptor.
	 */
	public MethodDefinition(Modifiers modifiers, String name, MethodParameters params, String returnType) {
		this.modifiers = modifiers;
		this.name = name;
		this.params = params;
		this.returnType = returnType;
	}

	@Override
	public String print() {
		StringBuilder sb = new StringBuilder();
		if (modifiers.value() > 0) {
			sb.append(modifiers.print()).append(' ');
		}
		sb.append(name);
		sb.append('(').append(params.print()).append(')');
		sb.append(returnType);
		return sb.toString();
	}

	@Override
	public boolean isMethod() {
		return true;
	}

	@Override
	public Modifiers getModifiers() {
		return modifiers;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getDesc() {
		return  params.getDesc() + returnType;
	}
}
