package me.xdark.recaf.jvm.classloading;

import me.xdark.recaf.jvm.Class;
import me.xdark.recaf.jvm.VMException;
import sun.misc.CompoundEnumeration;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class ClassLoader {
	private final Object lock = new Object();
	private final Map<String, Class> classMap = new HashMap<>();
	private final ClassLoader parent;

	public ClassLoader(ClassLoader parent) {
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

	Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
		synchronized (this.lock) {
			Class c = findLoadedClass(name);
			if (c == null) {
				if (parent != null) {
					try {
						c = parent.loadClass(name, false);
					} catch (VMException ignored) {
					}
				}
				if (c == null) {
					c = findClass(name);
				}
			}
			if (resolve) {
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
		tmp[1] =findResources(name);
		return new CompoundEnumeration<>(tmp);
	}

	protected URL findResource(String name) {
		return null;
	}

	protected Enumeration<URL> findResources(String path) throws IOException {
		return Collections.emptyEnumeration();
	}

	private void linkClass(Class c) {
		assert this == c.getClassLoader();
		synchronized (this.lock) {
			this.classMap.put(c.getName(), c);
		}
	}
}
