package software.coley.recaf.services.compile;


import jakarta.annotation.Nonnull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * {@link javax.tools.JavaFileObject} map wrapper for managing class inputs for the compiler.
 *
 * @author Matt Coley
 */
public class VirtualUnitMap {
	private final Map<String, VirtualJavaFileObject> unitMap = new HashMap<>();

	/**
	 * Add class to compilation process.
	 *
	 * @param className
	 * 		Name of class to compile.
	 * @param content
	 * 		Source code of class.
	 */
	public void addSource(String className, String content) {
		addFile(className, new VirtualJavaFileObject(className, content));
	}

	/**
	 * Add class to compilation process.
	 *
	 * @param className
	 * 		Name of class to compile.
	 * @param fileObject
	 * 		File object for source code of class.
	 */
	public void addFile(String className, VirtualJavaFileObject fileObject) {
		unitMap.put(className, fileObject);
	}

	/**
	 * @param className
	 * 		Name of class.
	 *
	 * @return File object for source code of class.
	 */
	public VirtualJavaFileObject getFile(String className) {
		return unitMap.get(className);
	}

	/**
	 * @return Collection of file objects for input classes.
	 */
	public Collection<VirtualJavaFileObject> getFiles() {
		return unitMap.values();
	}

	/**
	 * @return Map of class names to bytecode.
	 * Items that failed to compile will not have entries.
	 */
	@Nonnull
	public CompileMap getCompilations() {
		Map<String, byte[]> map = unitMap.entrySet().stream()
				.filter(e -> e.getValue().hasOutput())
				.collect(Collectors.toMap(Map.Entry::getKey, v -> v.getValue().getBytecode()));
		return new CompileMap(map);
	}
}
