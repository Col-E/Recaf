package me.coley.recaf.code;

import java.util.Objects;

/**
 * Member information of a field or method.
 *
 * @author Matt Coley
 */
public abstract class MemberInfo implements ItemInfo {
	private final String owner;
	private final String name;
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
		this.name = name;
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

	@Override
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

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		MemberInfo that = (MemberInfo) o;
		return access == that.access &&
				Objects.equals(owner, that.owner) &&
				Objects.equals(getName(), that.getName()) &&
				Objects.equals(descriptor, that.descriptor);
	}

	@Override
	public int hashCode() {
		return Objects.hash(owner, getName(), descriptor, access);
	}

	@Override
	public String toString() {
		return "MemberInfo{" +
				"owner='" + owner + '\'' +
				", name='" + getName() + '\'' +
				", descriptor='" + descriptor + '\'' +
				", access=" + access +
				'}';
	}
}
