package me.coley.recaf.graph.call;

import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.code.MethodInfo;
import me.coley.recaf.workspace.Workspace;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

class UnresolvedMethod {
	@Nonnull
	final String owner;
	@Nonnull
	final String name;
	@Nonnull
	final String descriptor;

	public UnresolvedMethod(@Nonnull String owner, @Nonnull String name, @Nonnull String descriptor) {
		this.owner = owner;
		this.name = name;
		this.descriptor = descriptor;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		UnresolvedMethod that = (UnresolvedMethod) o;

		if (!Objects.equals(owner, that.owner)) return false;
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

	public boolean is(MethodInfo cm) {
		return Objects.equals(owner, cm.getOwner()) && name.equals(cm.getName()) && descriptor.equals(cm.getDescriptor());
	}

	public @Nullable MethodInfo resolve(Workspace workspace) {
		final ClassInfo classInfo = workspace.getResources().getClass(owner);
		if (classInfo == null) return null;
		return resolve(classInfo);
	}

	public @Nullable MethodInfo resolve(@Nonnull ClassInfo classInfo) {
		return classInfo.getMethods().stream().filter(this::is).findFirst().orElse(null);
	}

	public static UnresolvedMethod of(MethodInfo methodInfo) {
		return new UnresolvedMethod(methodInfo.getOwner(), methodInfo.getName(), methodInfo.getDescriptor());
	}
}
