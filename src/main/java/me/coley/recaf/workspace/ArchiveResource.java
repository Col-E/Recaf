package me.coley.recaf.workspace;

import java.io.File;
import java.io.IOException;

/**
 * Importable archive base.
 *
 * @author Matt
 */
public abstract class ArchiveResource extends FileSystemResource{
	private EntryLoader entryLoader = new EntryLoader();

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

	/**
	 * @return Loader used to read content from archive files.
	 */
	public EntryLoader getEntryLoader() {
		return entryLoader;
	}

	/**
	 * Set the archive entry loader. Custom entry loaders could allow handling of some non-standard
	 * inputs such as obfuscated or packed archive.
	 *
	 * @param entryLoader
	 * 		Loader used to read content from archives.
	 */
	public void setEntryLoader(EntryLoader entryLoader) {
		this.entryLoader = entryLoader;
	}
}
