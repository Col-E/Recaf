package me.coley.recaf.code;

/**
 * Method information.
 *
 * @author Matt Coley
 */
public class MethodInfo extends MemberInfo {
	/**
	 * @param name
	 * 		Method name.
	 * @param descriptor
	 * 		Method descriptor.
	 * @param access
	 * 		Method access modifiers.
	 */
	public MethodInfo(String name, String descriptor, int access) {
		super(name, descriptor, access);
	}

	@Override
	public boolean isMethod() {
		return true;
	}
}
