package me.xdark.recaf.jvm.classloading;

import me.coley.recaf.util.ClasspathUtil;
import me.coley.recaf.util.IOUtil;
import me.xdark.recaf.jvm.Class;
import me.xdark.recaf.jvm.Compiler;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public final class SystemClassLoader extends ClassLoader {
	private final byte[] buffer = new byte[4096];
	private final java.lang.ClassLoader scl;

	public SystemClassLoader(Compiler compiler) {
		super(compiler, null);
		this.scl = ClasspathUtil.scl;
	}

	@Override
	public Class findClass(String name) throws ClassNotFoundException {
		URL url = scl.getResource(name.replace('.', '/') + ".class");
		if (url == null) {
			throw new ClassNotFoundException(name);
		}
		byte[] bytes;
		try (InputStream in = url.openStream()) {
			bytes = IOUtil.toByteArray(in, buffer);
		} catch (IOException ex) {
			throw new ClassNotFoundException(name, ex);
		}
		return defineClass(name, bytes, 0, bytes.length);
	}
}
