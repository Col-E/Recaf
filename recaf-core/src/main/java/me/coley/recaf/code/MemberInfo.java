package me.coley.recaf.code;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Member information of a field or method.
 *
 * @author Matt Coley
 */
public abstract class MemberInfo implements AccessibleInfo, ItemInfo {
	private final String signature;
	private final int access;
	private final MemberSignature memberSignature;

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
	public MemberInfo(String owner, String name, String descriptor, @Nullable String signature, int access) {
		this.signature = signature;
		this.access = access;
		memberSignature = new MemberSignature(owner, name, descriptor);
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
		return memberSignature.owner;
	}

	@Override
	public String getName() {
		return memberSignature.name;
	}

	/**
	 * @return Member descriptor.
	 */
	public String getDescriptor() {
		return memberSignature.descriptor;
	}

	/**
	 * @return Member's generic signature. May be {@code null}.
	 */
	@Nullable
	public String getSignature() {
		return signature;
	}

	public MemberSignature getMemberSignature() {
		return memberSignature;
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
				Objects.equals(memberSignature.owner, that.memberSignature.owner) &&
				Objects.equals(getName(), that.getName()) &&
				Objects.equals(memberSignature.descriptor, that.memberSignature.descriptor) &&
				Objects.equals(signature, that.signature);
	}

	@Override
	public int hashCode() {
		int result = Objects.hashCode(memberSignature.owner);
		result = 31 * result + Objects.hashCode(memberSignature.name);
		result = 31 * result + Objects.hashCode(memberSignature.descriptor);
		result = 31 * result + Objects.hashCode(signature);
		result = 31 * result + access;
		return result;
	}

	@Override
	public String toString() {
		return "MemberInfo{" +
				"owner='" + memberSignature.owner + '\'' +
				", name='" + getName() + '\'' +
				", descriptor='" + memberSignature.descriptor + '\'' +
				", access=" + access +
				'}';
	}
}
