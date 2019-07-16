package me.coley.recaf.workspace;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

/**
 * An importable unit.
 *
 * @author Matt
 */
public abstract class JavaResource {
	private final ResourceKind kind;

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
	 * @return Map of class names to their bytecode.
	 * @throws IOException when the resource could not be fetched or parsed.
	 */
	public abstract Map<String, byte[]> loadClasses() throws IOException;


	/**
	 * @return Map of resource names to their raw data.
	 * @throws IOException when the resource could not be fetched or parsed.
	 */
	public abstract Map<String, byte[]> loadResources() throws IOException;
}
// TODO: Allow resources to have update-checks, ex: the referenced resource is modified externally