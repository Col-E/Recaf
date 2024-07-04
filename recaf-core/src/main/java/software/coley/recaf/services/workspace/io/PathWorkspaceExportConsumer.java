package software.coley.recaf.services.workspace.io;

import jakarta.annotation.Nonnull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Export consumer to write to a given {@link Path}, either as a single file or as the root of a directory of items.
 *
 * @author Matt Coley
 */
public class PathWorkspaceExportConsumer implements WorkspaceExportConsumer {
	private final Path path;
	private boolean firstSingleWrite = true;

	/**
	 * @param path
	 * 		Path to write to.
	 */
	public PathWorkspaceExportConsumer(@Nonnull Path path) {
		this.path = path;
	}

	@Override
	public void write(@Nonnull byte[] bytes) throws IOException {
		if (firstSingleWrite) {
			Files.write(path, bytes);
			firstSingleWrite = false;
		} else {
			Files.write(path, bytes, StandardOpenOption.APPEND);
		}
	}

	@Override
	public void writeRelative(@Nonnull String relativePath, @Nonnull byte[] bytes) throws IOException {
		Path destination = path.resolve(relativePath);
		Path parent = destination.getParent();
		if (!Files.isDirectory(parent))
			Files.createDirectories(parent);
		Files.write(destination, bytes);
	}

	@Override
	public void commit() throws IOException {
		// No-need to do anything
	}
}
