package me.coley.recaf.assemble.ast.arch;

import me.coley.recaf.assemble.ast.Code;

import java.util.ArrayList;
import java.util.List;

/**
 * Definition of a method.
 *
 * @author Matt Coley
 */
public class MethodDefinition extends AbstractDefinition {
	private final List<ThrownException> thrownExceptions = new ArrayList<>();
	private final String name;
	private final MethodParameters params;
	private final String returnType;
	private final Code code;

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
	public MethodDefinition(Modifiers modifiers, String name, MethodParameters params, String returnType, Code code) {
		this.name = name;
		this.params = params;
		this.returnType = returnType;
		this.code = code;
		setModifiers(modifiers);
	}

	@Override
	public String print() {
		StringBuilder sb = new StringBuilder();
		for (ThrownException thrownException : thrownExceptions)
			sb.append(thrownException.print()).append("\n");
		sb.append(super.buildDefString("method"));
		sb.append(name).append(' ');
		sb.append('(').append(params.print()).append(')');
		sb.append(returnType);
		sb.append('\n');
		sb.append(code.print());
		sb.append('\n');
		sb.append("end");
		return sb.toString();
	}

	@Override
	public boolean isClass() {
		return false;
	}

	@Override
	public boolean isField() {
		return false;
	}

	@Override
	public boolean isMethod() {
		return true;
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
	 * @return Exceptions thrown by the method.
	 */
	public List<ThrownException> getThrownExceptions() {
		return thrownExceptions;
	}

	/**
	 * @param thrownException
	 * 		Exception to add.
	 *
	 * @see #getThrownExceptions()
	 */
	public void addThrownException(ThrownException thrownException) {
		thrownExceptions.add(thrownException);
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

	/**
	 * @return Code body of the method.
	 */
	public Code getCode() {
		return code;
	}
}
