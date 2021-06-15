package me.coley.recaf.code;

/**
 * Method information.
 *
 * @author Matt Coley
 */
public class MethodInfo extends MemberInfo {
	/**
	 * @param owner
	 * 		Name of type defining the member.
	 * @param name
	 * 		Method name.
	 * @param descriptor
	 * 		Method descriptor.
	 * @param access
	 * 		Method access modifiers.
	 */
	public MethodInfo(String owner, String name, String descriptor, int access) {
		super(owner, name, descriptor, access);
	}

	@Override
	public boolean isMethod() {
		return true;
	}
}
