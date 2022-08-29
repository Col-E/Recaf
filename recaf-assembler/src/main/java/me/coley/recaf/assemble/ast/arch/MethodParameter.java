package me.coley.recaf.assemble.ast.arch;

import me.coley.recaf.assemble.ast.*;
import me.coley.recaf.util.EscapeUtil;

/**
 * Part of a {@link MethodParameters}.
 *
 * @author Matt Coley
 */
public class MethodParameter extends BaseElement implements Named, Descriptor, VariableReference {
	private final String desc;
	private final String name;

	/**
	 * @param desc
	 * 		Parameter descriptor.
	 * @param name
	 * 		Parameter name.
	 */
	public MethodParameter(String desc, String name) {
		this.desc = desc;
		this.name = name;
	}

	@Override
	public String getVariableIdentifier() {
		return getName();
	}

	@Override
	public String getVariableDescriptor() {
		return getDesc();
	}

	@Override
	public boolean isObjectDescriptorExplicitlyDeclared() {
		// Parameter can specify a descriptor, so the type is explicitly declared.
		return true;
	}

	@Override
	public OpType getVariableOperation() {
		return OpType.ASSIGN;
	}

	@Override
	public String getDesc() {
		return desc;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String print(PrintContext context) {
		return getEscapedVariableDescriptor() + " " + getEscapedVariableIdentifier();
	}
}
