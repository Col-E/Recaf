package software.coley.recaf.services.callgraph;

import jakarta.annotation.Nonnull;

/**
 * Basic method outline.
 *
 * @author Amejonah
 */
public class MethodRef {
	private final String owner;
	private final String name;
	private final String desc;

	/**
	 * @param owner
	 * 		Method reference owner.
	 * @param name
	 * 		Method name.
	 * @param desc
	 * 		Method descriptor.
	 */
	public MethodRef(@Nonnull String owner, @Nonnull String name, @Nonnull String desc) {
		this.owner = owner;
		this.name = name;
		this.desc = desc;
	}

	/**
	 * @return Method reference owner.
	 */
	@Nonnull
	public String getOwner() {
		return owner;
	}

	/**
	 * @return Method name.
	 */
	@Nonnull
	public String getName() {
		return name;
	}

	/**
	 * @return Method descriptor.
	 */
	@Nonnull
	public String getDesc() {
		return desc;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		MethodRef methodRef = (MethodRef) o;

		if (!owner.equals(methodRef.owner)) return false;
		if (!name.equals(methodRef.name)) return false;
		return desc.equals(methodRef.desc);
	}

	@Override
	public int hashCode() {
		int result = owner.hashCode();
		result = 31 * result + name.hashCode();
		result = 31 * result + desc.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "MethodRef{" +
				"owner='" + owner + '\'' +
				", name='" + name + '\'' +
				", desc='" + desc + '\'' +
				'}';
	}
}
