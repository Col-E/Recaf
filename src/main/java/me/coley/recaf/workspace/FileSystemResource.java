package me.coley.recaf.workspace;

import me.coley.recaf.util.IOUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Importable resource from the file system.
 *
 * @author Matt
 */
public abstract class FileSystemResource extends JavaResource {
	private final Path path;

	/**
	 * Constructs a file system resource.
	 *
	 * @param kind
	 * 		The kind of resource implementation.
	 * @param path
	 * 		The reference to the file resource.
	 *
	 * @throws IOException
	 * 		When the path does not exist.
	 */
	public FileSystemResource(ResourceKind kind, Path path) throws IOException {
		super(kind);
		this.path = path;
		verify();
	}

	/**
	 * Create a FileSystemResource from the given file.
	 *
	 * @param path
	 * 		File to load as a resource.
	 *
	 * @return File resource.
	 *
	 * @throws IOException
	 * 		When the file cannot be read.
	 * @throws UnsupportedOperationException
	 * 		When the file extension is not supported.
	 */
	public static FileSystemResource of(Path path) throws IOException {
		if (Files.isDirectory(path))
			return new DirectoryResource(path);
		String ext = IOUtil.getExtension(path);
		switch(ext) {
			case "class":
				return new ClassResource(path);
			case "jar":
				return new JarResource(path);
			case "war":
				return new WarResource(path);
			default:
				throw new UnsupportedOperationException("File type '" + ext + "' is not " +
						"allowed for libraries");
		}
	}

	/**
	 * @return The path imported from.
	 */
	public Path getPath() {
		return path;
	}

	/**
	 * Verify the file exists.
	 *
	 * @throws IOException
	 * 		When the file does not exist.
	 */
	protected void verify() throws IOException {
		if (!Files.exists(path))
			throw new IOException("The file \"" + path + "\" does not exist!");
	}

	@Override
	public ResourceLocation getShortName() {
		return new FileSystemResourceLocation(getKind(), path.getFileName());
	}

	@Override
	public ResourceLocation getName() {
		return new FileSystemResourceLocation(getKind(), path);
	}

	@Override
	public String toString() {
		return path.getFileName().toString();
	}
}
