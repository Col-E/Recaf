package me.coley.recaf.code;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Member information of a field or method.
 *
 * @author Matt Coley
 */
public abstract class MemberInfo implements AccessibleInfo, ItemInfo {
	private final String owner;
	private final String name;
	private final String descriptor;
	private final String signature;
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
	public MemberInfo(String owner, String name, String descriptor, String signature, int access) {
		this.name = name;
		this.owner = owner;
		this.descriptor = descriptor;
		this.signature = signature;
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
	 * @return Member's generic signature. May be {@code null}.
	 */
	@Nullable
	public String getSignature() {
		return signature;
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
				Objects.equals(descriptor, that.descriptor) &&
				Objects.equals(signature, that.signature);
	}

	@Override
	public int hashCode() {
		int result = Objects.hashCode(owner);
		result = 31 * result + Objects.hashCode(name);
		result = 31 * result + Objects.hashCode(descriptor);
		result = 31 * result + Objects.hashCode(signature);
		result = 31 * result + access;
		return result;
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
