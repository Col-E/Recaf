package me.coley.recaf.workspace;

import org.objectweb.asm.ClassReader;

import java.util.*;
import java.util.zip.ZipEntry;

import static me.coley.recaf.util.Log.*;

/**
 * Standard jar content loader.
 *
 * @author Matt
 */
public class JarEntryLoader {
	private final Map<String, byte[]> classes = new HashMap<>();
	private final Map<String, byte[]> files = new HashMap<>();
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
			warn("Invalid class \"{}\"\nAdding as a file instead.", entryName);
			invalidClasses.add(entryName);
			onFile(entryName, in);
			return false;
		}
	}

	/**
	 * Load a file.
	 *
	 * @param entryName
	 * 		File's jar entry name.
	 * @param value
	 * 		File's raw value.
	 *
	 * @return Addition was a success.
	 */
	public boolean onFile(String entryName, byte[] value) {
		files.put(entryName, value);
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
	 * @return If the entry indicates the content is a valid file.
	 */
	public boolean isValidFile(ZipEntry entry) {
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
	 * Called when all files in the jar have been read.
	 */
	public void finishFiles() {}

	/**
	 * @return Loaded classes.
	 */
	public Map<String, byte[]> getClasses() {
		return classes;
	}

	/**
	 * @return Loaded files.
	 */
	public Map<String, byte[]> getFiles() {
		return files;
	}

	/**
	 * @return Set of classes that failed to load.
	 */
	public Set<String> getInvalidClasses() {
		return invalidClasses;
	}
}
