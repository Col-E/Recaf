package me.coley.recaf.workspace;

import me.coley.recaf.util.struct.ListeningMap;
import org.pmw.tinylog.Logger;

import java.io.IOException;
import java.util.*;

/**
 * An importable unit.
 *
 * @author Matt
 */
public abstract class JavaResource {
	private final ResourceKind kind;
	private List<String> skippedPrefixes = Collections.emptyList();
	private ListeningMap<String, byte[]> cachedClasses;
	private ListeningMap<String, byte[]> cachedResources;
	private final Map<String, History> classHistory = new HashMap<>();
	private final Map<String, History> resourceHistory = new HashMap<>();
	private final Set<String> dirtyClasses = new HashSet<>();
	private final Set<String> dirtyResources = new HashSet<>();

	/**
	 * Constructs a java resource.
	 *
	 * @param kind
	 * 		The kind of resource implementation.
	 */
	public JavaResource(ResourceKind kind) {
		this.kind = kind;
	}

	/**
	 * @return Type of referenced resource.
	 */
	public ResourceKind getKind() {
		return kind;
	}

	/**
	 * @return Prefixes to skip.
	 */
	public List<String> getSkippedPrefixes() {
		return skippedPrefixes;
	}

	/**
	 * @param skippedPrefixes
	 * 		Prefixes to skip.
	 */
	public void setSkippedPrefixes(List<String> skippedPrefixes) {
		this.skippedPrefixes = skippedPrefixes;
	}

	/**
	 * @return Set of classes that have been modified since initially loading.
	 */
	public Set<String> getDirtyClasses() {
		return dirtyClasses;
	}

	/**
	 * @return Set of resources that have been modified since initially loading.
	 */
	public Set<String> getDirtyResources() {
		return dirtyResources;
	}

	/**
	 * @param name
	 * 		Class name.
	 *
	 * @return History for class. {@code null} if no save-states for the class exist.
	 */
	public History getClassHistory(String name) {
		return classHistory.get(name);
	}

	/**
	 * @param name
	 * 		Resource name.
	 *
	 * @return History for resource. {@code null} if no save-states for the resource exist.
	 */
	public History getResourceHistory(String name) {
		return resourceHistory.get(name);
	}

	/**
	 * Create a save-state for the class.
	 *
	 * @param name
	 * 		Class name.
	 *
	 * @return {@code true} if the save-state was created successfully.
	 */
	public boolean createClassSave(String name) {
		byte[] value = cachedClasses.get(name);
		if(value == null)
			return false;
		History history = classHistory.computeIfAbsent(name, key -> new History(cachedClasses, key));
		history.push(value);
		return true;
	}

	private void addClassSave(String name, byte[] value) {
		History history = classHistory.computeIfAbsent(name, key -> new History(cachedClasses, key));
		history.push(value);
	}

	/**
	 * Create a save-state for the resource.
	 *
	 * @param name
	 * 		Resource name.
	 *
	 * @return {@code true} if the save-state was created successfully.
	 */
	public boolean createResourceSave(String name) {
		byte[] value = cachedResources.get(name);
		if(value == null)
			return false;
		History history = resourceHistory.computeIfAbsent(name, key -> new History(cachedResources, key));
		history.push(value);
		return true;
	}

	private void addResourceSave(String name, byte[] value) {
		History history = resourceHistory.computeIfAbsent(name, key -> new History(cachedResources, key));
		history.push(value);
	}

	/**
	 * @return Map of class names to their bytecode.
	 */
	public ListeningMap<String, byte[]> getClasses() {
		if(cachedClasses == null) {
			try {
				cachedClasses = new ListeningMap<>(loadClasses());
				cachedClasses.getPutListeners().add((name, code) -> dirtyClasses.add(name));
				cachedClasses.getRemoveListeners().add(name -> dirtyClasses.remove(name));
				// Create initial save state
				for (Map.Entry<String, byte[]> e : cachedClasses.entrySet()) {
					addClassSave(e.getKey(), e.getValue());
				}
				// Add listener to create initial save states for newly made classes
				cachedClasses.getPutListeners().add((name, code) -> {
					if (!cachedClasses.containsKey(name)) {
						addClassSave(name, code);
					}
				});
			} catch(IOException ex) {
				Logger.error(ex, "Failed to load classes from resource \"{}\"", this);
			}
		}
		return cachedClasses;
	}

	/**
	 * @return Map of resource names to their raw data.
	 */
	public ListeningMap<String, byte[]> getResources() {
		if(cachedResources == null) {
			try {
				cachedResources = new ListeningMap<>(loadResources());
				cachedResources.getPutListeners().add((name, code) -> dirtyResources.add(name));
				cachedResources.getRemoveListeners().add(name -> dirtyResources.remove(name));
				// Create initial save state
				for (Map.Entry<String, byte[]> e : cachedResources.entrySet()) {
					addResourceSave(e.getKey(), e.getValue());
				}
				// Add listener to create initial save states for newly made resources
				cachedResources.getPutListeners().add((name, code) -> {
					if (!cachedResources.containsKey(name)) {
						addResourceSave(name, code);
					}
				});
			} catch(IOException ex) {
				Logger.error(ex, "Failed to load resources from resource \"{}\"", this);
			}
		}
		return cachedResources;
	}

	/**
	 * Refresh this resource.
	 */
	public void invalidate() {
		// TODO: Store old listeners (if existing) to copy over to new maps
		cachedResources = null;
		cachedClasses = null;
	}

	/**
	 * @return Map of class names to their bytecode.
	 *
	 * @throws IOException
	 * 		when the resource could not be fetched or parsed.
	 */
	protected abstract Map<String, byte[]> loadClasses() throws IOException;

	/**
	 * @return Map of resource names to their raw data.
	 *
	 * @throws IOException
	 * 		when the resource could not be fetched or parsed.
	 */
	protected abstract Map<String, byte[]> loadResources() throws IOException;
}
// TODO: Allow resources to have update-checks, ex: the referenced resource is modified externally