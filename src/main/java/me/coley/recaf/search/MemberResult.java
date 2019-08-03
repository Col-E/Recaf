package me.coley.recaf.search;

import java.util.Objects;

/**
 * Search result of a matched member.
 *
 * @author Matt
 */
public class MemberResult extends SearchResult {
	private final int access;
	private final String owner;
	private final String name;
	private final String desc;

	/**
	 * Constructs a member result.
	 *
	 * @param access
	 * 		Member modifers.
	 * @param owner
	 * 		Name of class containing the member.
	 * @param name
	 * 		Member name.
	 * @param desc
	 * 		Member descriptor.
	 */
	public MemberResult(int access, String owner, String name, String desc) {
		this.access = access;
		this.owner = owner;
		this.name = name;
		this.desc = desc;
	}

	/**
	 * @return Member modifiers.
	 */
	public int getAccess() {
		return access;
	}

	/**
	 * @return Name of class containing the member.
	 */
	public String getOwner() {
		return owner;
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
	public String getDesc() {
		return desc;
	}

	/**
	 * @return {@code true} if the {@link #getDesc() descriptor} outlines a field type.
	 */
	public boolean isField() {
		return !isMethod();
	}

	/**
	 * @return {@code true} if the {@link #getDesc() descriptor} outlines a method type.
	 */
	public boolean isMethod() {
		return desc.contains("(");
	}

	@Override
	public String toString() {
		if(isMethod()) {
			return owner + "." + name + desc;
		} else {
			return owner + "." + name + " " + desc;
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(owner, name, desc);
	}

	@Override
	public boolean equals(Object other) {
		if(other instanceof MemberResult)
			return hashCode() == other.hashCode();
		return false;
	}

	@Override
	public int compareTo(SearchResult other) {
		int ret = super.compareTo(other);
		if (ret == 0) {
			if (other instanceof MemberResult) {
				MemberResult otherResult = (MemberResult) other;
				if (isField() && otherResult.isMethod())
					return 1;
				if (isMethod() && otherResult.isField())
					return -1;
				return toString().compareTo(otherResult.toString());
			}
		}
		return ret;
	}
}
