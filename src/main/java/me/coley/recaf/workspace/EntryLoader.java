package me.coley.recaf.workspace;

import me.coley.recaf.plugin.PluginsManager;
import me.coley.recaf.plugin.api.LoadInterceptor;
import org.objectweb.asm.ClassReader;

import java.util.*;
import java.util.zip.ZipEntry;

import static me.coley.recaf.util.Log.*;

/**
 * Standard archive content loader.
 *
 * @author Matt
 */
public class EntryLoader {
	private final Map<String, byte[]> classes = new HashMap<>();
	private final Map<String, byte[]> files = new HashMap<>();
	private final Set<String> invalidClasses = new HashSet<>();

	/**
	 * Load a class.
	 *
	 * @param entryName
	 * 		Class's archive entry name.
	 * @param value
	 * 		Class's bytecode.
	 *
	 * @return Addition was a success.
	 */
	public boolean onClass(String entryName, byte[] value) {
		try {
			String name = new ClassReader(value).getClassName();
			for (LoadInterceptor interceptor : PluginsManager.getInstance().ofType(LoadInterceptor.class)) {
				value = interceptor.interceptClass(name, value);
				name = new ClassReader(value).getClassName();
			}
			classes.put(name, value);
			return true;
		} catch(ArrayIndexOutOfBoundsException | IllegalArgumentException ex) {
			// invalid class?
			warn("Invalid class \"{}\"\nAdding as a file instead.", entryName);
			invalidClasses.add(entryName);
			onFile(entryName, value);
			return false;
		}
	}

	/**
	 * Load a file.
	 *
	 * @param entryName
	 * 		File's archive entry name.
	 * @param value
	 * 		File's raw value.
	 *
	 * @return Addition was a success.
	 */
	public boolean onFile(String entryName, byte[] value) {
		for (LoadInterceptor interceptor : PluginsManager.getInstance().ofType(LoadInterceptor.class)) {
			value = interceptor.interceptClass(entryName, value);
		}
		files.put(entryName, value);
		return true;
	}

	/**
	 * @param entry
	 * 		Zip entry in the archive.
	 *
	 * @return {@code true} if the entry indicates the content should be a class file.
	 */
	public boolean isValidClass(ZipEntry entry) {
		return isFileValidClassName(entry.getName());
	}

	/**
	 * @param name
	 * 		File name.
	 *
	 * @return {@code true} if the entry indicates the content should be a class file.
	 */
	public boolean isFileValidClassName(String name) {
		// Must end in .class or .class/
		return name.endsWith(".class") || name.endsWith(".class/");
	}

	/**
	 * @param entry
	 * 		Zip entry in the archive.
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
		return !name.contains("//");
	}

	/**
	 * Called when all classes in the jar have been read.
	 */
	public void finishClasses() {}

	/**
	 * Called when all files in the archive have been read.
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
