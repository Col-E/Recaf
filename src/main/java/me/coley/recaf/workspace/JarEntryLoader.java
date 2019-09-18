package me.coley.recaf.workspace;

import org.objectweb.asm.ClassReader;
import org.tinylog.Logger;

import java.util.*;
import java.util.zip.ZipEntry;

/**
 * Standard jar content loader.
 *
 * @author Matt
 */
public class JarEntryLoader {
	private final Map<String, byte[]> classes = new HashMap<>();
	private final Map<String, byte[]> resources = new HashMap<>();
	private final Set<String> invalidClasses = new HashSet<>();

	/**
	 * Load a class.
	 *
	 * @param entryName
	 * 		Class's jar entry name.
	 * @param in
	 * 		Class's bytecode.
	 *
	 * @return Addition was a success.
	 */
	public boolean onClass(String entryName, byte[] in) {
		try {
			String name = new ClassReader(in).getClassName();
			classes.put(name, in);
			return true;
		} catch(ArrayIndexOutOfBoundsException | IllegalArgumentException ex) {
			// invalid class?
			Logger.warn("Invalid class \"{}\"\nAdding as a resource instead.", entryName);
			invalidClasses.add(entryName);
			onResource(entryName, in);
			return false;
		}
	}

	/**
	 * Load a resource.
	 *
	 * @param entryName
	 * 		Resources's jar entry name.
	 * @param value
	 * 		Resource's raw value.
	 *
	 * @return Addition was a success.
	 */
	public boolean onResource(String entryName, byte[] value) {
		resources.put(entryName, value);
		return true;
	}

	/**
	 * @param entry
	 * 		Zip entry in the jar.
	 *
	 * @return If the entry indicates the content should be a class file.
	 */
	public boolean isValidClass(ZipEntry entry) {
		// Must end in class
		String name = entry.getName();
		return name.endsWith(".class");
	}

	/**
	 * @param entry
	 * 		Zip entry in the jar.
	 *
	 * @return If the entry indicates the content is a valid resource.
	 */
	public boolean isValidResource(ZipEntry entry) {
		if (entry.isDirectory())
			return false;
		String name = entry.getName();
		// name / directory escaping
		if (name.contains("../"))
			return false;
		// empty directory names is a no
		if (name.contains("//"))
			return false;
		return true;
	}

	/**
	 * Called when all classes in the jar have been read.
	 */
	public void finishClasses() {}

	/**
	 * Called when all resources in the jar have been read.
	 */
	public void finishResources() {}

	/**
	 * @return Loaded classes.
	 */
	public Map<String, byte[]> getClasses() {
		return classes;
	}

	/**
	 * @return Loaded resources.
	 */
	public Map<String, byte[]> getResources() {
		return resources;
	}

	/**
	 * @return Set of classes that failed to load.
	 */
	public Set<String> getInvalidClasses() {
		return invalidClasses;
	}
}
