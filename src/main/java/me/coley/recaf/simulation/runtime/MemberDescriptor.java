package me.coley.recaf.simulation.runtime;

import java.util.Objects;

public final class MemberDescriptor {
	private final String name;
	private final String descriptor;

	public MemberDescriptor(String name, String descriptor) {
		this.name = name;
		this.descriptor = descriptor;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		MemberDescriptor that = (MemberDescriptor) o;
		return name.equals(that.name) &&
				descriptor.equals(that.descriptor);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, descriptor);
	}
}
