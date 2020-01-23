package me.xdark.recaf.jvm;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

public final class UnresolvedClass {
	private static final ClassNode NULL = new ClassNode();
	private ClassReader classReader;
	private ClassNode resolved;

	public UnresolvedClass(ClassReader classReader) {
		this.classReader = classReader;
	}

	public String getClassName() {
		synchronized (this) {
			ClassNode resolved = this.resolved;
			if (resolved != null && resolved != NULL) {
				return resolved.name;
			}
		}
		return classReader.getClassName();
	}

	public String getSuperName() {
		synchronized (this) {
			ClassNode resolved = this.resolved;
			if (resolved != null && resolved != NULL) {
				return resolved.superName;
			}
		}
		return classReader.getSuperName();
	}

	public int getAccess() {
		synchronized (this) {
			ClassNode resolved = this.resolved;
			if (resolved != null && resolved != NULL) {
				return resolved.access;
			}
		}
		return classReader.getAccess();
	}

	public ClassReader getClassReader() {
		return classReader;
	}

	public ClassNode resolve() {
		synchronized (this) {
			ClassNode resolved = this.resolved;
			if (resolved != null) return resolved;
			this.resolved = NULL;
		}
		ClassNode resolved = this.resolved = new ClassNode();
		classReader.accept(resolved, 0);
		classReader = null;
		return resolved;
	}
}
