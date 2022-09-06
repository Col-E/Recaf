package me.coley.recaf.code;

/**
 * Field information.
 *
 * @author Matt Coley
 */
public class FieldInfo extends MemberInfo {
	private final Object value;

	/**
	 * @param owner
	 * 		Name of type defining the member.
	 * @param name
	 * 		Field name.
	 * @param descriptor
	 * 		Field descriptor.
	 * @param signature
	 * 		Field generic signature.  May be {@code null}.
	 * @param access
	 * 		Field access modifiers.
	 * @param value
	 * 		Default value. May be {@code null}.
	 */
	public FieldInfo(String owner, String name, String descriptor, String signature, int access, Object value) {
		super(owner, name, descriptor, signature, access);
		this.value = value;
	}

	/**
	 * @return Default value. May be {@code null}.
	 */
	public Object getValue() {
		return value;
	}

	@Override
	public boolean isMethod() {
		return false;
	}
}
