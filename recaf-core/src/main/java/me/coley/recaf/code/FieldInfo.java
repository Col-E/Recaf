package me.coley.recaf.code;

/**
 * Field information.
 *
 * @author Matt Coley
 */
public class FieldInfo extends MemberInfo {
	/**
	 * @param name
	 * 		Field name.
	 * @param descriptor
	 * 		Field descriptor.
	 * @param access
	 * 		Field access modifiers.
	 */
	public FieldInfo(String name, String descriptor, int access) {
		super(name, descriptor, access);
	}

	@Override
	public boolean isMethod() {
		return false;
	}
}
