package me.coley.recaf.workspace;

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
	private Map<String, byte[]> cachedClasses;
	private Map<String, byte[]> cachedResources;

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
	public Map<String, byte[]> getClasses() {
		if(cachedClasses == null) {
			try {
				cachedClasses = loadClasses();
			} catch(IOException ex) {
				Logger.error(ex, "Failed to load classes from resource \"{}\"", this);
			}
		}
		return cachedClasses;
	}

	/**
	 * @return Map of resource names to their raw data.
	 */
	public Map<String, byte[]> getResources() {
		if(cachedResources == null) {
			try {
				cachedResources = loadResources();
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