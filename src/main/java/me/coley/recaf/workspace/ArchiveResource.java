package me.coley.recaf.workspace;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Importable archive base.
 *
 * @author Matt
 */
public abstract class ArchiveResource extends FileSystemResource {
	/**
	 * Constructs an archive file resource.
	 *
	 * @param kind
	 * 		The kind of resource implementation.
	 * @param path
	 * 		The reference to the file resource.
	 *
	 * @throws IOException
	 * 		When the file does not exist.
	 */
	public ArchiveResource(ResourceKind kind, Path path) throws IOException {
		super(kind, path);
	}
}
