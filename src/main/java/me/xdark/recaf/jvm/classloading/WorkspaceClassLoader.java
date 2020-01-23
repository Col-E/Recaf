package me.xdark.recaf.jvm.classloading;

import me.coley.recaf.workspace.Workspace;
import me.xdark.recaf.jvm.Class;
import me.xdark.recaf.jvm.Compiler;

public final class WorkspaceClassLoader extends ClassLoader {
	private final Workspace workspace;

	public WorkspaceClassLoader(Workspace workspace, Compiler compiler, ClassLoader parent) {
		super(compiler, parent);
		this.workspace = workspace;
	}

	@Override
	public Class findClass(String name) throws ClassNotFoundException {
		byte[] bytes = workspace.getRawClass(name);
		if (bytes == null) {
			throw new ClassNotFoundException(name);
		}
		return defineClass(name, bytes, 0, bytes.length);
	}
}
