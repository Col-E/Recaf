package me.xdark.recaf.jvm;

import java.util.Objects;

public class Member {
	private final String name;
	private final String descriptor;
	private final Class declaringClass;
	private final int modifiers;
	private final boolean synthetic;

	protected Member(String name, String descriptor, Class declaringClass, int modifiers, boolean synthetic) {
		this.name = name;
		this.descriptor = descriptor;
		this.declaringClass = declaringClass;
		this.modifiers = modifiers;
		this.synthetic = synthetic;
	}

	public String getName() {
		return name;
	}

	public String getDescriptor() {
		return descriptor;
	}

	public Class getDeclaringClass() {
		return declaringClass;
	}

	public int getModifiers() {
		return modifiers;
	}

	public boolean isSynthetic() {
		return synthetic;
	}

	@Override
	public boolean equals(java.lang.Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Member vmMember = (Member) o;
		return Objects.equals(name, vmMember.name) &&
				Objects.equals(descriptor, vmMember.descriptor);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, descriptor);
	}
}
