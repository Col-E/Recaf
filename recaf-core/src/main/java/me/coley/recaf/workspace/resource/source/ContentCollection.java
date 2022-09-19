package me.coley.recaf.workspace.resource.source;

import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.DexClassInfo;
import me.coley.recaf.code.FileInfo;
import me.coley.recaf.workspace.resource.DexClassMap;
import me.coley.recaf.workspace.resource.MultiDexClassMap;
import me.coley.recaf.workspace.resource.Resource;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.DexFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Summary of data read from {@link ContentSource}.
 * <br>
 * Collects different categories of <i>"invalid"</i> input, allowing {@link ContentSourceListener} listeners
 * to define custom handling. For an example see {@link me.coley.recaf.workspace.resource.ClassPatchingListener}.
 *
 * @author Matt Coley
 */
public class ContentCollection {
	private final Resource resource;
	private final Map<String, ClassInfo> classes = new HashMap<>();
	private final MultiDexClassMap dexClasses = new MultiDexClassMap();
	private final Map<String, FileInfo> files = new HashMap<>();
	// Items that need to be acted on
	private final List<ClassInfo> pendingDuplicateClasses = new ArrayList<>();
	private final List<FileInfo> pendingDuplicateFiles = new ArrayList<>();
	private final Map<String, ClassInfo> pendingNameMismatchedClasses = new HashMap<>();
	private final Map<String, byte[]> pendingInvalidClasses = new HashMap<>();
	private final Map<String, byte[]> pendingNonClassClasses = new HashMap<>();

	/**
	 * @param resource
	 * 		Resource the content is for.
	 */
	public ContentCollection(Resource resource) {
		this.resource = resource;
	}

	/**
	 * Adds the given class. Checks for existing entries and puts duplicates in {@link #getPendingDuplicateClasses()}.
	 *
	 * @param info
	 * 		Class to add.
	 */
	public synchronized void addClass(ClassInfo info) {
		String name = info.getName();
		if (classes.containsKey(name))
			addDuplicateClass(info);
		else
			replaceClass(info);
	}

	/**
	 * Adds the given class, replacing any existing entry.
	 *
	 * @param info
	 * 		Class to replace.
	 */
	public synchronized void replaceClass(ClassInfo info) {
		classes.put(info.getName(), info);
	}

	/**
	 * @param dexPath
	 * 		Path to dex file <i>(Internal to apk/zip)</i>.
	 * @param dexFile
	 * 		Dex file instance to pull classes from.
	 */
	public synchronized void addDexClasses(String dexPath, DexFile dexFile) {
		Opcodes op = dexFile.getOpcodes();
		DexClassMap dexClassMap = dexClasses.getBackingMap()
				.computeIfAbsent(dexPath, k -> new DexClassMap(resource, op));
		for (ClassDef dexClass : dexFile.getClasses()) {
			DexClassInfo clazz = DexClassInfo.parse(dexPath, op, dexClass);
			dexClassMap.put(clazz);
		}
	}

	/**
	 * Used when {@link #addClass(ClassInfo)} encounters a class name that already exists.
	 *
	 * @param info
	 * 		Data with duplicate {@link CommonClassInfo#getName()}.
	 */
	private void addDuplicateClass(ClassInfo info) {
		pendingDuplicateClasses.add(info);
	}

	/**
	 * @param name
	 * 		Path name to class.
	 * @param info
	 * 		Actual class info where {@link CommonClassInfo#getName()} does not match.
	 */
	public synchronized void addMismatchedNameClass(String name, ClassInfo info) {
		pendingNameMismatchedClasses.put(name, info);
	}

	/**
	 * Used when we encounter a class that is not able to be parsed with ASM.
	 *
	 * @param name
	 * 		Path name to class.
	 * @param data
	 * 		Class bytecode.
	 */
	public synchronized void addInvalidClass(String name, byte[] data) {
		pendingInvalidClasses.put(name, data);
	}

	/**
	 * Used when we encounter a file that has a class extension, but does not have the correct file header.
	 *
	 * @param name
	 * 		Path name to class.
	 * @param data
	 * 		Class bytecode.
	 */
	public synchronized void addNonClassClass(String name, byte[] data) {
		pendingNonClassClasses.put(name, data);
	}

	/**
	 * @param info
	 * 		File to add.
	 */
	public synchronized void addFile(FileInfo info) {
		String name = info.getName();
		if (files.containsKey(name))
			addDuplicateFile(info);
		else
			files.put(name, info);
	}

	/**
	 * Used when {@link #addFile(FileInfo)} encounters a file name that already exists.
	 *
	 * @param info
	 * 		Data with duplicate {@link FileInfo#getName()}.
	 */
	private void addDuplicateFile(FileInfo info) {
		pendingDuplicateFiles.add(info);
	}

	/**
	 * @return Classes found.
	 */
	public Map<String, ClassInfo> getClasses() {
		return classes;
	}

	/**
	 * @return Dex classes found.
	 */
	public MultiDexClassMap getDexClasses() {
		return dexClasses;
	}

	/**
	 * @return Files found.
	 */
	public Map<String, FileInfo> getFiles() {
		return files;
	}

	/**
	 * @return List of classes that already exist in {@link #getClasses()}
	 * but other matching classes were found in the content source.
	 */
	public List<ClassInfo> getPendingDuplicateClasses() {
		return pendingDuplicateClasses;
	}

	/**
	 * @return List of files that already exist in {@link #getFiles()}
	 * but other matching files were found in the content source.
	 */
	public List<FileInfo> getPendingDuplicateFiles() {
		return pendingDuplicateFiles;
	}

	/**
	 * @return Classes that could not be parsed by ASM.
	 */
	public Map<String, byte[]> getPendingInvalidClasses() {
		return pendingInvalidClasses;
	}

	/**
	 * @return Files that have {@code '.class'} in their path names, but do not contain actual classes.
	 */
	public Map<String, byte[]> getPendingNonClassClasses() {
		return pendingNonClassClasses;
	}

	/**
	 * @return Classes that do not match their path names.
	 */
	public Map<String, ClassInfo> getPendingNameMismatchedClasses() {
		return pendingNameMismatchedClasses;
	}

	/**
	 * @return Number of classes read.
	 */
	public int getClassCount() {
		return getClasses().size();
	}

	/**
	 * @return Number of dex classes read.
	 */
	public int getDexClassCount() {
		return getDexClasses().size();
	}

	/**
	 * @return Number of files read.
	 */
	public int getFileCount() {
		return getFiles().size();
	}

	/**
	 * @return Number of items with pending actions to take.
	 */
	public int getPendingCount() {
		return pendingDuplicateClasses.size() +
				pendingDuplicateFiles.size() +
				pendingInvalidClasses.size() +
				pendingNonClassClasses.size() +
				pendingNameMismatchedClasses.size();
	}
}
