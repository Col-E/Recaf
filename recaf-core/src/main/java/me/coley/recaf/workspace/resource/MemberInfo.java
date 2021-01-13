package me.coley.recaf.workspace.resource;

/**
 * Member information of a field or method belonging to a {@link ClassInfo}.
 *
 * @author Matt Coley
 */
public class MemberInfo {
	private final String name;
	private final String descriptor;
	private final int access;
	private final boolean isMethod;

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
		this.isMethod = descriptor.charAt(0) == '(';
	}

	/**
	 * @return {@code true} if member represents a method.
	 */
	public boolean isMethod() {
		return isMethod;
	}

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
