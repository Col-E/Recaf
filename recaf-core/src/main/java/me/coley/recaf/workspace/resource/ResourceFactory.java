package me.coley.recaf.workspace.resource;

import me.coley.recaf.workspace.resource.source.ContentSource;

import java.io.IOException;

/**
 * Resource factory.
 *
 * @author xDark
 */
@FunctionalInterface
public interface ResourceFactory {

	/**
	 * Creates new resource.
	 *
	 * @param source
	 * 		Content source.
	 *
	 * @return New resource.
	 *
	 * @throws IOException
	 * 		If any I/O error occurs.
	 */
	Resource create(ContentSource source) throws IOException;

	/**
	 * Enables auto-read for this factory.
	 *
	 * @return Factory with auto-read enabled.
	 */
	default ResourceFactory autoRead() {
		return source -> {
			Resource resource = create(source);
			resource.read();
			return resource;
		};
	}

	/**
	 * @return Default resource factory.
	 */
	static ResourceFactory defaultFactory() {
		return Resource::new;
	}
}
