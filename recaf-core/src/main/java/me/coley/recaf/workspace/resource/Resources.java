package me.coley.recaf.workspace.resource;

import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.code.DexClassInfo;
import me.coley.recaf.code.FileInfo;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Wrapper for multiple resources.
 *
 * @author Matt Coley
 */
public class Resources implements Iterable<Resource> {
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
	public Stream<ClassInfo> getClasses() {
		return Stream.concat(
				getPrimary().getClasses().stream(),
				getLibraries().stream().map(Resource::getClasses).flatMap(ClassMap::stream)
		);
	}

	/**
	 * @return All dex classes among all resources.
	 */
	public Stream<DexClassInfo> getDexClasses() {
		return Stream.concat(
				getPrimary().getDexClasses().stream(),
				getLibraries().stream().map(Resource::getDexClasses).flatMap(MultiDexClassMap::stream)
		);
	}

	/**
	 * @return All files among all resources.
	 */
	public Stream<FileInfo> getFiles() {
		return Stream.concat(
				getPrimary().getFiles().stream(),
				getLibraries().stream().map(Resource::getFiles).flatMap(ResourceItemMap::stream)
		);
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
		if (info != null)
			return info;
		// Check libraries for class
		for (Resource resource : libraries) {
			info = resource.getClasses().get(name);
			if (info != null)
				return info;
		}
		// Check for class in runtime if not found in a given resource
		for (Resource resource : internalLibraries) {
			info = resource.getClasses().get(name);
			if (info != null)
				return info;
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

	/**
	 * @param name
	 * 		Class name.
	 *
	 * @return Resource that contains the class.
	 */
	public Resource getContainingForClass(String name) {
		return getContaining(r -> r.getClasses().containsKey(name));
	}

	/**
	 * @param name
	 * 		Class name.
	 *
	 * @return Resource that contains the class.
	 */
	public Resource getContainingForDexClass(String name) {
		return getContaining(r -> r.getDexClasses().containsKey(name));
	}

	/**
	 * @param name
	 * 		Package name.
	 *
	 * @return Resource that contains the package.
	 */
	public Resource getContainingForPackage(String name) {
		Resource resource = getContaining(r -> r.getClasses().keySet().stream().anyMatch(f -> f.startsWith(name)));
		if (resource == null)
			resource = getContaining(r -> r.getDexClasses().keySet().stream().anyMatch(f -> f.startsWith(name)));
		return resource;
	}

	/**
	 * @param name
	 * 		File name.
	 *
	 * @return Resource that contains the file.
	 */
	public Resource getContainingForFile(String name) {
		return getContaining(r -> r.getFiles().containsKey(name));
	}

	/**
	 * @param name
	 * 		Directory name.
	 *
	 * @return Resource that contains the directory.
	 */
	public Resource getContainingForDirectory(String name) {
		return getContaining(r -> r.getFiles().keySet().stream().anyMatch(f -> f.startsWith(name)));
	}

	/**
	 * @param func
	 * 		Some resource predicate.
	 *
	 * @return Resource that matches the predicate.
	 */
	public Resource getContaining(Predicate<Resource> func) {
		if (func.test(primary))
			return primary;
		for (Resource library : getLibraries()) {
			if (func.test(library))
				return library;
		}
		for (Resource library : getInternalLibraries()) {
			if (func.test(library))
				return library;
		}
		return null;
	}

	@Override
	public Iterator<Resource> iterator() {
		return Stream.concat(
				Stream.of(primary),
				Stream.concat(
						libraries.stream(),
						internalLibraries.stream()
				)
		).iterator();
	}
}
