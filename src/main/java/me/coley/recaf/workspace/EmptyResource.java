package me.coley.recaf.workspace;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

/**
 * Empty resource, useful for attaching additional sources/javadocs.
 *
 * @author Matt
 */
public class EmptyResource extends JavaResource {

	/**
	 * Constructs an empty resource.
	 */
	public EmptyResource() {
		super(ResourceKind.EMPTY);
	}

	@Override
	public String toString() {
		return "Empty";
	}

	@Override
	protected Map<String, byte[]> loadClasses() throws IOException {
		return Collections.emptyMap();
	}

	@Override
	protected Map<String, byte[]> loadFiles() throws IOException {
		return Collections.emptyMap();
	}
}
