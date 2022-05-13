package me.coley.recaf.assemble.ast.arch;

import me.coley.recaf.assemble.ast.meta.Signature;

import java.util.ArrayList;
import java.util.List;

/**
 * Definition of a method.
 *
 * @author Matt Coley
 */
public class MethodDefinition extends AbstractMemberDefinition {
	private final String name;
	private final MethodParameters params;
	private final String returnType;

	private final List<ThrownException> thrownExceptions = new ArrayList<>();

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
		for(ThrownException thrownException : thrownExceptions)
			sb.append(thrownException.print()).append("\n");
		for(Annotation annotation : annotations)
			sb.append(annotation.print());
		sb.append("method ");
		if (modifiers.value() > 0) {
			sb.append(modifiers.print().toLowerCase()).append(' ');
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

	public List<ThrownException> getThrownExceptions() {
		return thrownExceptions;
	}

	public void addThrownException(ThrownException thrownException) {
		thrownExceptions.add(thrownException);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getDesc() {
		return params.getDesc() + returnType;
	}

	/**
	 * @return Method parameters.
	 */
	public MethodParameters getParams() {
		return params;
	}

	/**
	 * @return Method return descriptor.
	 */
	public String getReturnType() {
		return returnType;
	}
}
