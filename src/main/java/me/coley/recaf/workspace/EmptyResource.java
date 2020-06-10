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
	private static final ResourceLocation LOCATION = LiteralResourceLocation.ofKind(ResourceKind.EMPTY, "Empty");

	/**
	 * Constructs an empty resource.
	 */
	public EmptyResource() {
		super(ResourceKind.EMPTY);
	}

	@Override
	protected Map<String, byte[]> loadClasses() throws IOException {
		return Collections.emptyMap();
	}

	@Override
	protected Map<String, byte[]> loadFiles() throws IOException {
		return Collections.emptyMap();
	}

	@Override
	public ResourceLocation getShortName() {
		return LOCATION;
	}

	@Override
	public ResourceLocation getName() {
		return LOCATION;
	}
}
