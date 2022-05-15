package me.coley.recaf.assemble.ast.arch;

import me.coley.recaf.util.EscapeUtil;

/**
 * Definition of a field.
 *
 * @author Matt Coley
 */
public class FieldDefinition extends AbstractMemberDefinition {
	private final String name;
	private final String type;
	private ConstVal val;

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
		sb.append("field ");
		if (modifiers.value() > 0) {
			sb.append(modifiers.print().toLowerCase()).append(' ');
		}

		// make sure to escape the name

		sb.append(EscapeUtil.escapeSpace(name)).append(' ').append(EscapeUtil.escape(type));
		return sb.toString();
	}

	@Override
	public boolean isMethod() {
		return false;
	}

	public boolean isClass() {
		return false;
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

	public ConstVal getConstVal() {
		return val;
	}

	public void setConstVal(ConstVal val) {
		this.val = val;
	}
}
