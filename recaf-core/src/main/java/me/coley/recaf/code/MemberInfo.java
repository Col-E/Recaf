package me.coley.recaf.code;

/**
 * Member information of a field or method.
 *
 * @author Matt Coley
 */
public abstract class MemberInfo extends ItemInfo {
	private final String owner;
	private final String descriptor;
	private final int access;

	/**
	 * @param owner
	 * 		Name of type defining the member.
	 * @param name
	 * 		Member name.
	 * @param descriptor
	 * 		Member descriptor.
	 * @param access
	 * 		Member access modifiers.
	 */
	public MemberInfo(String owner, String name, String descriptor, int access) {
		super(name);
		this.owner = owner;
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
	 * @return Name of type defining the member.
	 */
	public String getOwner() {
		return owner;
	}

	/**
	 * @return Member name.
	 */
	public String getName() {
		return super.getName();
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
