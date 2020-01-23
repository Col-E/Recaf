package me.xdark.recaf.jvm.classloading;

import me.coley.recaf.util.ClasspathUtil;
import me.xdark.recaf.jvm.Class;

import java.net.URL;

public final class SystemClassloader extends ClassLoader {
	private final byte[] buffer = new byte[4096];
	private final java.lang.ClassLoader scl;

	public SystemClassloader() {
		super(null);
		this.scl = ClasspathUtil.scl;
	}

	@Override
	public Class findClass(String name) throws ClassNotFoundException {
		URL url = scl.getResource(name.replace('.', '/') + ".class");
		if (url == null) {
			throw new ClassNotFoundException(name);
		}
		throw new UnsupportedOperationException();
	}
}
