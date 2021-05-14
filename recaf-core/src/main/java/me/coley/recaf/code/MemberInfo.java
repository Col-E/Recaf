package me.coley.recaf.code;

/**
 * Member information of a field or method.
 *
 * @author Matt Coley
 */
public abstract class MemberInfo {
	private final String name;
	private final String descriptor;
	private final int access;

	/**
	 * @param name
	 * 		Member name.
	 * @param descriptor
	 * 		Member descriptor.
	 * @param access
	 * 		Member access modifiers.
	 */
	public MemberInfo(String name, String descriptor, int access) {
		this.name = name;
		this.descriptor = descriptor;
		this.access = access;
	}

	/**
	 * @return {@code true} if member represents a method.
	 */
	public abstract boolean isMethod();

	/**
	 * @return {@code true} if member represents a field.
	 */
	public boolean isField() {
		return !isMethod();
	}

	/**
	 * @return Member name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return Member descriptor.
	 */
	public String getDescriptor() {
		return descriptor;
	}

	/**
	 * @return Member access modifiers.
	 */
	public int getAccess() {
		return access;
	}
}
