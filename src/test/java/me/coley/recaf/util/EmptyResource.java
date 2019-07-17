package me.coley.recaf.util;

import me.coley.recaf.workspace.JavaResource;
import me.coley.recaf.workspace.ResourceKind;

import java.io.IOException;
import java.util.*;

/**
 * Dummy resource.
 *
 * @author Matt
 */
public class EmptyResource extends JavaResource {
	public EmptyResource() {
		super(ResourceKind.JAR);
	}

	@Override
	protected Map<String, byte[]> loadClasses() throws IOException {
		return new HashMap<>();
	}

	@Override
	protected Map<String, byte[]> loadResources() throws IOException {
		return new HashMap<>();
	}
}
