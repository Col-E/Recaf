package me.coley.recaf.code;

import java.util.List;

/**
 * Method information.
 *
 * @author Matt Coley
 */
public class MethodInfo extends MemberInfo {
	private final List<String> exceptions;

	/**
	 * @param owner
	 * 		Name of type defining the member.
	 * @param name
	 * 		Method name.
	 * @param descriptor
	 * 		Method descriptor.
	 * @param signature
	 * 		Method generic signature.
	 * @param access
	 * 		Method access modifiers.
	 * @param exceptions
	 * 		Exception types thrown.
	 */
	public MethodInfo(String owner, String name, String descriptor, String signature,
					  int access, List<String> exceptions) {
		super(owner, name, descriptor, signature, access);
		this.exceptions = exceptions;
	}

	/**
	 * @return Exception types thrown.
	 */
	public List<String> getExceptions() {
		return exceptions;
	}

	@Override
	public boolean isMethod() {
		return true;
	}
}
