package me.coley.recaf.code;

import javax.annotation.Nonnull;

public final class MemberSignature {
	@Nonnull
	final String owner;
	@Nonnull
	final String name;
	@Nonnull
	final String descriptor;

	public MemberSignature(@Nonnull String owner, @Nonnull String name, @Nonnull String descriptor) {
		this.owner = owner;
		this.name = name;
		this.descriptor = descriptor;
	}

	@Nonnull
	public String getOwner() {
		return owner;
	}

	@Nonnull
	public String getName() {
		return name;
	}

	@Nonnull
	public String getDescriptor() {
		return descriptor;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		MemberSignature that = (MemberSignature) o;

		if (!owner.equals(that.owner)) return false;
		if (!name.equals(that.name)) return false;
		return descriptor.equals(that.descriptor);
	}

	@Override
	public int hashCode() {
		int result = owner.hashCode();
		result = 31 * result + name.hashCode();
		result = 31 * result + descriptor.hashCode();
		return result;
	}
}
