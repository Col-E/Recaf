package me.coley.recaf.code;

/**
 * Field information.
 *
 * @author Matt Coley
 */
public class FieldInfo extends MemberInfo {
	/**
	 * @param owner
	 * 		Name of type defining the member.
	 * @param name
	 * 		Field name.
	 * @param descriptor
	 * 		Field descriptor.
	 * @param access
	 * 		Field access modifiers.
	 */
	public FieldInfo(String owner, String name, String descriptor, int access) {
		super(owner, name, descriptor, access);
	}

	@Override
	public boolean isMethod() {
		return false;
	}
}
