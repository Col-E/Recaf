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
	 * @param skippedPrefixes Prefixes to skip.
	 */
	public void setSkippedPrefixes(List<String> skippedPrefixes) {
		this.skippedPrefixes = skippedPrefixes;
	}

	/**
	 * @return Map of class names to their bytecode.
	 */
	public ListeningMap<String, byte[]> getClasses() {
		if(cachedClasses == null) {
			try {
				cachedClasses = new ListeningMap<>(loadClasses());
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
		cachedResources = null;
		cachedClasses = null;
	}

	/**
	 * @return Map of class names to their bytecode.
	 * @throws IOException when the resource could not be fetched or parsed.
	 */
	protected abstract Map<String, byte[]> loadClasses() throws IOException;

	/**
	 * @return Map of resource names to their raw data.
	 * @throws IOException when the resource could not be fetched or parsed.
	 */
	protected abstract Map<String, byte[]> loadResources() throws IOException;
}
// TODO: Allow resources to have update-checks, ex: the referenced resource is modified externally