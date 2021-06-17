package me.coley.recaf.workspace;

import me.coley.cafedude.InvalidClassException;
import me.coley.cafedude.io.ClassFileReader;
import me.coley.recaf.plugin.PluginsManager;
import me.coley.recaf.plugin.api.LoadInterceptorPlugin;
import me.coley.recaf.util.ClassUtil;
import me.coley.recaf.util.IOUtil;
import me.coley.recaf.util.IllegalBytecodePatcherUtil;
import me.coley.recaf.util.Log;
import org.objectweb.asm.ClassReader;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
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
	private final Map<String, byte[]> invalidClasses = new HashMap<>();
	private final Map<String, byte[]> invalidJunkClasses = new HashMap<>();

	/**
	 * @return New archive entry loader instance.
	 */
	public static EntryLoader create() {
		EntryLoader loader = PluginsManager.getInstance().getEntryLoader();
		// Fallback to default
		if (loader == null)
			loader = new EntryLoader();
		return loader;
	}

	/**
	 * Load a class from the input.
	 * <br>
	 * Checks if the class is invalid and adds it to a temporary store to be parsed later
	 * if it contains illegal bytecode patterns.
	 *
	 * @param entryName
	 * 		Class's archive entry name.
	 * @param value
	 * 		Class's bytecode.
	 *
	 * @return Addition was a success.
	 */
	public boolean onClass(String entryName, byte[] value) {
		// Check if class is valid. If it is not it will be stored for later.
		if (!ClassUtil.isValidClass(value)) {
			try {
				// If the data can be read, overwrite whatever entry we have previously seen
				new ClassFileReader().read(value);
				invalidClasses.put(entryName, value);
				if (invalidJunkClasses.remove(entryName) != null) {
					debug("Replacing class '{}' previously associated with non-class junk with" +
							" newly discovered class data", entryName);
				}
				return false;
			} catch (InvalidClassException e) {
				// Skip if we think this is junk data that is masking an invalid class we already recovered
				if (invalidClasses.containsKey(entryName)) {
					debug("Skipping masking junk data for class '{}'", entryName);
					return false;
				}
				// Doesnt look like the CAFEDOOD backup parser can read it either.
				if (invalidJunkClasses.containsKey(entryName)) {
					// Already seen it. Probably dupe junk data.
					debug("Skipping duplicate invalid class '{}'", entryName);
					return false;
				} else {
					debug("Invalid class detected, not parsable by backup reader \"{}\"", entryName);
				}
				invalidJunkClasses.put(entryName, value);
				return false;
			}
		}
		// Check if we've already seen this class
		String clsName = new ClassReader(value).getClassName();
		if (classes.containsKey(clsName)) {
			debug("Skipping duplicate class '{}'", clsName);
			return false;
		}
		// Load the class
		handleAddClass(entryName, value);
		return true;
	}

	/**
	 * Add the class to the loaded classes map.
	 *
	 * @param entryName
	 * 		Class's archive entry name.
	 * @param value
	 * 		Class's bytecode.
	 *
	 * @return Addition was a success.
	 */
	private boolean handleAddClass(String entryName, byte[] value) {
		String name = new ClassReader(value).getClassName();
		for(LoadInterceptorPlugin interceptor :
				PluginsManager.getInstance().ofType(LoadInterceptorPlugin.class)) {
			// Intercept class
			try {
				value = interceptor.interceptClass(name, value);
			} catch(Throwable t) {
				Log.error(t, "Plugin '{}' threw exception when reading the class '{}'", interceptor.getName(), name);
			}
			// Make sure the class interception doesn't break the class
			if (!ClassUtil.isValidClass(value)) {
				warn("Invalid class '{}' due to modifications by plugin '{}'\nAdding as a file instead.", entryName);
				onFile(entryName, value);
				return false;
			}
			// Update name
			name = new ClassReader(value).getClassName();
		}
		classes.put(name, value);
		return true;
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
		for (LoadInterceptorPlugin interceptor : PluginsManager.getInstance().ofType(LoadInterceptorPlugin.class)) {
			value = interceptor.interceptFile(entryName, value);
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
	public boolean isValidClassEntry(ZipEntry entry) {
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
	 * @param input
	 * 		An {@link InputStream} to test.
	 *
	 * @return {@code true} if the entry indicates the content should be a class file.
	 *
	 * @throws IOException
	 * 		If any I/O occurs.
	 */
	public boolean isValidClassFile(InputStream input) throws IOException {
		// Try to read class file header
		byte[] tmp = new byte[4];
		if (input.read(tmp) != 4) {
			return false;
		}
		return IOUtil.isClassHeader(tmp);
	}

	/**
	 * @param entry
	 * 		Zip entry in the archive.
	 *
	 * @return If the entry indicates the content is a valid file.
	 */
	public boolean isValidFileEntry(ZipEntry entry) {
		// If the entry is a directory, then skip it....
		// Unless its a "fake" directory because archive manipulation by obfuscation
		if (entry.isDirectory() && !isValidClassEntry(entry))
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
	public void finishClasses() {
		Collection<LoadInterceptorPlugin> interceptors =
				PluginsManager.getInstance().ofType(LoadInterceptorPlugin.class);
		for (Map.Entry<String, byte[]> e : invalidClasses.entrySet()) {
			String entryName = e.getKey();
			byte[] value = e.getValue();
			try {
				// Attempt to patch invalid classes.
				// If the internal measure fails, allow plugins to patch invalid classes
				if (!ClassUtil.isValidClass(value)) {
					debug("Attempting to patch invalid class '{}'", entryName);
					byte[] patched = IllegalBytecodePatcherUtil.fix(classes, invalidClasses, value);
					if (ClassUtil.isValidClass(patched)) {
						value = patched;
					} else if (!interceptors.isEmpty()) {
						for (LoadInterceptorPlugin interceptor : interceptors) {
							try {
								value = interceptor.interceptInvalidClass(entryName, value);
							} catch (Throwable t) {
								Log.error(t, "Plugin '{}' threw an exception when reading the invalid class '{}'",
										interceptor.getName(), entryName);
							}
						}
					}
				}
				// Check if class is valid
				if (ClassUtil.isValidClass(value)) {
					debug("Illegal class patching success!");
					handleAddClass(entryName, value);
				} else {
					warn("Invalid class \"{}\" - Cannot be parsed with ASM reader\n" +
							"Adding as a file instead.", entryName);
					onFile(entryName, value);
				}
			} catch (Throwable t) {
				error(t, "Failed to patch invalid class due to patcher crash \"{}\"", entryName);
			}
		}
		for (Map.Entry<String, byte[]> e : invalidJunkClasses.entrySet()) {
			if (classes.containsKey(e.getKey()) || files.containsKey(e.getKey()))
				continue;
			onFile(e.getKey(), e.getValue());
		}
	}

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
		return invalidClasses.keySet();
	}
}
