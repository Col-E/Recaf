package me.coley.recaf.assemble.ast.arch;

import me.coley.recaf.util.EscapeUtil;

/**
 * Definition of a field.
 *
 * @author Matt Coley
 */
public class FieldDefinition extends AbstractDefinition {
	private final String name;
	private final String type;
	private ConstVal val;

	/**
	 * @param modifiers
	 * 		Field modifiers.
	 * @param name
	 * 		Field name.
	 * @param type
	 * 		Field descriptor.
	 */
	public FieldDefinition(Modifiers modifiers, String name, String type) {
		this.name = name;
		this.type = type;
		setModifiers(modifiers);
	}

	@Override
	public String print() {
		return super.buildDefString("field") +
				// make sure to escape the name
				EscapeUtil.escapeSpace(name) + ' ' + EscapeUtil.escape(type);
	}

	@Override
	public boolean isClass() {
		return false;
	}

	@Override
	public boolean isField() {
		return true;
	}

	@Override
	public boolean isMethod() {
		return false;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getDesc() {
		return type;
	}

	/**
	 * Per <a href="https://docs.oracle.com/javase/specs/jvms/se17/html/jvms-4.html#jvms-4.7.2">4.7.2.
	 * The ConstantValue Attribute</a>, this value should only be applied when {@link #getModifiers()}
	 * contains {@code static}.
	 *
	 * @return Constant value of the field.
	 */
	public ConstVal getConstVal() {
		return val;
	}

	public void setConstVal(ConstVal val) {
		this.val = val;
	}
}
