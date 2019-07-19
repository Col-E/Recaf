package me.coley.recaf.workspace;

import me.coley.recaf.graph.inheritance.Hierarchy;
import org.objectweb.asm.ClassReader;

import java.util.*;
import java.util.stream.Collectors;

public class Workspace {
	/**
	 * Primary file being worked on.
	 */
	private JavaResource primary;
	/**
	 * Libraries of the primary file. Useful for additional analysis capabilities.
	 */
	private List<JavaResource> libraries;
	/**
	 * Inheritance hierarchy utility.
	 */
	private Hierarchy hierarchy;

	public Workspace(JavaResource primary) {
		this(primary, new ArrayList<>());
	}

	public Workspace(JavaResource primary, List<JavaResource> libraries) {
		this.primary = primary;
		this.libraries = libraries;
		this.hierarchy = new Hierarchy(this);
	}

	/**
	 * @return Primary file being worked on.
	 */
	public JavaResource getPrimary() {
		return primary;
	}

	/**
	 * @return Libraries of the {@link #getPrimary() primary file}.
	 */
	public List<JavaResource> getLibraries() {
		return libraries;
	}

	/**
	 * @return Inheritance hierarchy utility.
	 */
	public Hierarchy getHierarchy() {
		return hierarchy;
	}

	/**
	 * @return Set of all class names loaded in the workspace.
	 */
	public Set<String> getClassNames() {
		Set<String> names = getPrimaryClassNames();
		for(JavaResource resource : getLibraries())
			names.addAll(resource.getClasses().keySet());
		return names;
	}

	/**
	 * @return Set of all class names loaded in the primary resource.
	 */
	public Set<String> getPrimaryClassNames() {
		Set<String> names = new HashSet<>();
		names.addAll(primary.getClasses().keySet());
		return names;
	}

	/**
	 * @return Set of all classes loaded in the primary resource.
	 */
	public Set<byte[]> getPrimaryClasses() {
		Set<byte[]> values = new HashSet<>();
		values.addAll(primary.getClasses().values());
		return values;
	}

	/**
	 * @return Set of all classes loaded in the primary resource as
	 * {@link org.objectweb.asm.ClassReader}.
	 */
	public Set<ClassReader> getPrimaryClassReaders() {
		return getPrimaryClasses().stream()
				.map(clazz -> new ClassReader(clazz))
				.collect(Collectors.toSet());
	}

	/**
	 * @param name
	 * 		Class name.
	 *
	 * @return {@code true} if one of the workspace sources contains the class.
	 */
	public boolean hasClass(String name) {
		if(primary.getClasses().containsKey(name))
			return true;
		for(JavaResource resource : getLibraries())
			if(resource.getClasses().containsKey(name))
				return true;
		return false;
	}

	/**
	 * @param name
	 * 		Resource name.
	 *
	 * @return {@code true} if one of the workspace sources contains the resource.
	 */
	public boolean hasResource(String name) {
		if(primary.getResources().containsKey(name))
			return true;
		for(JavaResource resource : getLibraries())
			if(resource.getResources().containsKey(name))
				return true;
		return false;
	}

	/**
	 * @param name
	 * 		Class name.
	 *
	 * @return Raw bytecode of the class by the given name.
	 */
	public byte[] getRawClass(String name) {
		byte[] ret = primary.getClasses().get(name);
		if(ret != null)
			return ret;
		for(JavaResource resource : getLibraries())
			ret = resource.getClasses().get(name);
		if(ret != null)
			return ret;
		return null;
	}

	/**
	 * @param name
	 * 		Class name.
	 *
	 * @return {@link org.objectweb.asm.ClassReader} for the given class.
	 */
	public ClassReader getClassReader(String name) {
		byte[] ret = getRawClass(name);
		if(ret != null)
			return new ClassReader(ret);
		return null;
	}

	/**
	 * @param name
	 * 		Resource name.
	 *
	 * @return Resource binary by the given name.
	 */
	public byte[] getResource(String name) {
		byte[] ret = primary.getResources().get(name);
		if(ret != null)
			return ret;
		for(JavaResource resource : getLibraries())
			ret = resource.getResources().get(name);
		if(ret != null)
			return ret;
		return null;
	}
}