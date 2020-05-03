package me.coley.recaf.workspace;

import java.io.File;
import java.io.IOException;

/**
 * Importable archive base.
 *
 * @author Matt
 */
public abstract class ArchiveResource extends FileSystemResource{
	/**
	 * Constructs an archive file resource.
	 *
	 * @param kind
	 * 		The kind of resource implementation.
	 * @param file
	 * 		The reference to the file resource.
	 *
	 * @throws IOException
	 * 		When the file does not exist.
	 */
	public ArchiveResource(ResourceKind kind, File file) throws IOException {
		super(kind, file);
	}
}
