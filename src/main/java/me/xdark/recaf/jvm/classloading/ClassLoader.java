package me.xdark.recaf.jvm.classloading;

import me.xdark.recaf.jvm.Class;
import me.xdark.recaf.jvm.Compiler;
import me.xdark.recaf.jvm.VMException;
import org.objectweb.asm.ClassReader;
import sun.misc.CompoundEnumeration;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class ClassLoader {
	private static final boolean DEBUG = true;
	private final Object lock = new Object();
	private final Map<String, Class> classMap = new HashMap<>();
	protected final Compiler compiler;
	private final ClassLoader parent;

	public ClassLoader(Compiler compiler, ClassLoader parent) {
		this.compiler = compiler;
		this.parent = parent;
	}

	public Class findClass(String name) throws ClassNotFoundException {
		throw new ClassNotFoundException(name);
	}

	public Class loadClass(String name) throws ClassNotFoundException {
		return loadClass(name, false);
	}

	public Class findLoadedClass(String name) {
		synchronized (this.lock) {
			return this.classMap.get(name);
		}
	}

	protected Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
		synchronized (this.lock) {
			Class c = findLoadedClass(name);
			if (c == null) {
				if (parent != null) {
					try {
						c = parent.loadClass(name, false);
						if (DEBUG) {
							System.out.println("Parent loader found class " + parent + '/' + c.getName());
						}
					} catch (VMException ignored) {
					}
				}
				if (c == null) {
					c = findClass(name);
				}
			}
			if (resolve) {
				if (DEBUG) {
					System.out.println("Resolving class " + c.getName() + " (no lazy)");
				}
				c.resolve();
			}
			return c;
		}
	}

	public URL getResource(String path) {
		URL url = parent.getResource(path);
		if (url == null) {
			url = findResource(path);
		}
		return url;
	}

	public Enumeration<URL> getResources(String name) throws IOException {
		Enumeration<URL>[] tmp = new Enumeration[2];
		tmp[0] = parent.getResources(name);
		tmp[1] = findResources(name);
		return new CompoundEnumeration<>(tmp);
	}

	protected URL findResource(String name) {
		return null;
	}

	protected Enumeration<URL> findResources(String path) throws IOException {
		return Collections.emptyEnumeration();
	}

	protected Class defineClass(String name, byte[] bytes, int off, int len) throws ClassNotFoundException {
		ClassReader reader = new ClassReader(off == 0 && len == bytes.length ? bytes : Arrays.copyOfRange(bytes, off, len));
		if (name != null) {
			String asmName = reader.getClassName();
			if (!asmName.equals(name)) {
				throw new VerifyError("Class name mismatch: " + name + " is not " + asmName);
			}
		}
		String superName = reader.getSuperName();
		Class parent = superName == null ? null : loadClass(superName);
		if (DEBUG) {
			System.out.println("Compiling class " + this +'/' + name);
		}
		Class c = compiler.compileClass(parent, reader);
		linkClass(c);
		return c;
	}

	private void linkClass(Class c) {
		c.setClassLoader(this);
		synchronized (this.lock) {
			this.classMap.put(c.getName(), c);
		}
	}
}
