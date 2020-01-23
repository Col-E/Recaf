package me.xdark.recaf.jvm;

import java.util.Objects;

final class MemberIdentifier {
	private final String name;
	private final String descriptor;

	MemberIdentifier(String name, String descriptor) {
		this.name = name;
		this.descriptor = descriptor;
	}

	@Override
	public boolean equals(java.lang.Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		MemberIdentifier that = (MemberIdentifier) o;
		return Objects.equals(name, that.name) &&
				Objects.equals(descriptor, that.descriptor);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, descriptor);
	}
}
