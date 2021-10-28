package me.coley.recaf.assemble.ast.arch;

import me.coley.recaf.assemble.ast.BaseElement;

/**
 * Definition of a field.
 *
 * @author Matt Coley
 */
public class FieldDefinition extends BaseElement implements MemberDefinition {
	private final Modifiers modifiers;
	private final String name;
	private final String type;

	/**
	 * @param modifiers Field modifiers.
	 * @param name Field name.
	 * @param type Field descriptor.
	 */
	public FieldDefinition(Modifiers modifiers, String name, String type) {
		this.modifiers = modifiers;
		this.name = name;
		this.type = type;
	}

	@Override
	public String print() {
		StringBuilder sb = new StringBuilder();
		if (modifiers.value() > 0) {
			sb.append(modifiers.print()).append(' ');
		}
		sb.append(name).append(' ').append(type);
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
		return type;
	}
}
