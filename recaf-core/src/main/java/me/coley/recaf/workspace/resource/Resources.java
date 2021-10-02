package me.coley.recaf.workspace.resource;

import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.code.DexClassInfo;
import me.coley.recaf.code.FileInfo;

import java.util.*;

/**
 * Wrapper for multiple resources.
 *
 * @author Matt Coley
 */
public class Resources {
	private final Resource primary;
	private final List<Resource> libraries;
	private final List<Resource> internalLibraries = new ArrayList<>();

	/**
	 * @param primary
	 * 		Primary resource.
	 */
	public Resources(Resource primary) {
		this(primary, Collections.emptyList());
	}

	/**
	 * @param primary
	 * 		Primary resource.
	 * @param libraries
	 * 		Library resources.
	 */
	public Resources(Resource primary, List<Resource> libraries) {
		this.primary = Objects.requireNonNull(primary, "Primary resource must not be null!");
		this.libraries = new ArrayList<>(libraries);
		this.internalLibraries.add(RuntimeResource.get());
	}

	/**
	 * @return Primary resource.
	 */
	public Resource getPrimary() {
		return primary;
	}

	/**
	 * Library resources are used when content is not in {@link #getPrimary() the primary resource}.
	 *
	 * @return Library resources.
	 */
	public List<Resource> getLibraries() {
		return libraries;
	}

	/**
	 * An extension of {@link #getLibraries() standard workspace libraries}, but these are managed internally.
	 * They are not meant to be shown to the user, but still allow workspace utility.
	 *
	 * @return Internally managed libraries.
	 */
	public List<Resource> getInternalLibraries() {
		return internalLibraries;
	}

	/**
	 * @return All classes among all <i>(Non-internal)</i> resources.
	 */
	public Collection<ClassInfo> getClasses() {
		List<ClassInfo> list = new ArrayList<>(getPrimary().getClasses().values());
		getLibraries().forEach(library -> list.addAll(library.getClasses().values()));
		return list;
	}

	/**
	 * @return All dex classes among all resources.
	 */
	public Collection<DexClassInfo> getDexClasses() {
		List<DexClassInfo> list = new ArrayList<>(getPrimary().getDexClasses().values());
		getLibraries().forEach(library -> list.addAll(library.getDexClasses().values()));
		return list;
	}

	/**
	 * @return All files among all resources.
	 */
	public Collection<FileInfo> getFiles() {
		List<FileInfo> list = new ArrayList<>(getPrimary().getFiles().values());
		getLibraries().forEach(library -> list.addAll(library.getFiles().values()));
		return list;
	}

	/**
	 * Get a class by name. Unlike {@link #getClasses()} this can also fetch
	 * classes from {@link #getInternalLibraries() internally managed libraries}.
	 *
	 * @param name
	 * 		Internal class name.
	 *
	 * @return Class wrapper, if present. Otherwise {@code null}.
	 */
	public ClassInfo getClass(String name) {
		// Check primary resource for class
		ClassInfo info = primary.getClasses().get(name);
		if (info == null) {
			// Check libraries for class
			for (Resource resource : libraries) {
				info = resource.getClasses().get(name);
				if (info != null) {
					break;
				}
			}
			// Check for class in runtime if not found in a given resource
			for (Resource resource : internalLibraries) {
				info = resource.getClasses().get(name);
				if (info != null) {
					break;
				}
			}
		}
		return info;
	}

	/**
	 * @param name
	 * 		Internal class name.
	 *
	 * @return Dex class wrapper, if present. Otherwise {@code null}.
	 */
	public DexClassInfo getDexClass(String name) {
		// Check primary resource for class
		DexClassInfo info = primary.getDexClasses().get(name);
		if (info == null) {
			// Check libraries for class
			for (Resource resource : libraries) {
				info = resource.getDexClasses().get(name);
				if (info != null) {
					break;
				}
			}
		}
		return info;
	}

	/**
	 * @param name
	 * 		File path name.
	 *
	 * @return File wrapper, if present. Otherwise {@code null}.
	 */
	public FileInfo getFile(String name) {
		// Check primary resource for file
		FileInfo info = primary.getFiles().get(name);
		if (info == null) {
			// Check libraries for file
			for (Resource resource : libraries) {
				info = resource.getFiles().get(name);
				if (info != null) {
					break;
				}
			}
		}
		return info;
	}
}
