package software.coley.recaf.services.workspace.io;

import jakarta.annotation.Nonnull;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.UUID;

/**
 * Export consumer to write to a given {@link Path}, either directly or through a sibling staging output.
 *
 * @author Matt Coley
 */
public class PathWorkspaceExportConsumer implements WorkspaceExportConsumer, AutoCloseable {
	private final Path path;
	private final Mode mode;
	private boolean firstSingleWrite = true;
	private Path stagedPath;
	private Path backupPath;

	/**
	 * @param path
	 * 		Path to write to.
	 */
	public PathWorkspaceExportConsumer(@Nonnull Path path) {
		this(path, Mode.DIRECT);
	}

	/**
	 * @param path
	 * 		Path to write to.
	 * @param mode
	 * 		Output installation strategy.
	 */
	public PathWorkspaceExportConsumer(@Nonnull Path path, @Nonnull Mode mode) {
		this.path = mode == Mode.DIRECT ? path : path.toAbsolutePath().normalize();
		this.mode = mode;
	}

	@Override
	public void write(@Nonnull byte[] bytes) throws IOException {
		if (mode == Mode.MIRROR_DIRECTORY)
			throw new IOException("Cannot write a single-file output in directory mirror mode");

		// Pick the output path based on the mode.
		// In atomic file mode, we write to a staged file.
		Path output = mode == Mode.ATOMIC_FILE ? ensureStagedFile() : path;

		// Write the output.
		if (firstSingleWrite) {
			Files.write(output, bytes);
			firstSingleWrite = false;
		} else {
			Files.write(output, bytes, StandardOpenOption.APPEND);
		}
	}

	@Override
	public void writeRelative(@Nonnull String relativePath, @Nonnull byte[] bytes) throws IOException {
		// Pick the output path based on the mode.
		Path root = mode == Mode.MIRROR_DIRECTORY ? ensureStagedDirectory() : path;
		Path destination = mode == Mode.MIRROR_DIRECTORY
				? resolveInsideStagingRoot(root, relativePath)
				: root.resolve(relativePath);

		// Ensure the parent directory exists.
		Path parent = destination.getParent();
		if (!Files.isDirectory(parent))
			Files.createDirectories(parent);

		// Write the output.
		Files.write(destination, bytes);
	}

	@Override
	public void commit() throws IOException {
		switch (mode) {
			case DIRECT -> { /* Direct exporting is already done by now. */ }
			case ATOMIC_FILE -> moveStagedFile();
			case MIRROR_DIRECTORY -> moveStagedDirectory();
		}
	}

	@Override
	public void close() throws IOException {
		IOException failure = null;

		// Cleanup any staged or backup files that may have been left behind.
		if (stagedPath != null && Files.exists(stagedPath, LinkOption.NOFOLLOW_LINKS)) {
			try {
				deleteTree(stagedPath);
			} catch (IOException ex) {
				failure = ex;
			}
		}
		if (backupPath != null && Files.exists(backupPath, LinkOption.NOFOLLOW_LINKS)) {
			try {
				deleteTree(backupPath);
			} catch (IOException ex) {
				if (failure == null)
					failure = ex;
				else
					failure.addSuppressed(ex);
			}
		}

		// If any cleanup failed, throw the first failure.
		// This is better than immediately throwing on the first failure, as it allows for more cleanup to be attempted.
		if (failure != null)
			throw failure;
	}

	/**
	 * @return Path to the output file.
	 */
	@Nonnull
	private Path ensureStagedFile() throws IOException {
		if (stagedPath == null) {
			Path parent = requireParentDirectory();
			stagedPath = Files.createTempFile(parent, stagingPrefix(), ".tmp");
		}
		return stagedPath;
	}

	/**
	 * @return Path to the output directory.
	 */
	@Nonnull
	private Path ensureStagedDirectory() throws IOException {
		if (stagedPath == null) {
			Path parent = requireParentDirectory();
			stagedPath = Files.createTempDirectory(parent, stagingPrefix());
		}
		return stagedPath;
	}

	/**
	 * @return Parent directory of the output path.
	 *
	 * @throws IOException
	 * 		When the parent directory cannot be created or does not exist.
	 */
	@Nonnull
	private Path requireParentDirectory() throws IOException {
		Path parent = path.getParent();
		if (parent == null)
			throw new IOException("Output path has no parent directory: " + path);
		Files.createDirectories(parent);
		return parent;
	}

	/**
	 * @return Prefix for staging files/directories.
	 */
	@Nonnull
	private String stagingPrefix() {
		Path name = path.getFileName();
		return "." + (name == null ? "workspace" : name) + ".recaf-";
	}

	/**
	 * @param root
	 * 		Root of the staging directory.
	 * @param relativePath
	 * 		Relative path to resolve inside the staging directory.
	 *
	 * @return Resolved path inside the staging directory.
	 *
	 * @throws IOException
	 * 		When the relative path is invalid or escapes the staging directory.
	 */
	@Nonnull
	private static Path resolveInsideStagingRoot(@Nonnull Path root, @Nonnull String relativePath) throws IOException {
		try {
			// Ensure the relative path is actually relative and does not escape the staging directory.
			Path relative = Path.of(relativePath);
			if (relative.isAbsolute())
				throw new IOException("Workspace output path must be relative: " + relativePath);

			// Ensure the resolved path is inside the staging directory.
			Path destination = root.resolve(relative).normalize();
			if (!destination.startsWith(root))
				throw new IOException("Workspace output path escapes staging directory: " + relativePath);

			return destination;
		} catch (InvalidPathException ex) {
			throw new IOException("Invalid workspace output path: " + relativePath, ex);
		}
	}

	/**
	 * Moves the staged file to the {@link #path final destination}.
	 *
	 * @throws IOException
	 * 		When the staged file cannot be moved to the final destination.
	 */
	private void moveStagedFile() throws IOException {
		if (stagedPath == null)
			throw new IOException("No staged file output was generated for: " + path);
		moveReplacing(stagedPath, path);
		stagedPath = null;
	}

	/**
	 * Moves the staged directory to the {@link #path final destination}.
	 *
	 * @throws IOException
	 * 		When the staged directory cannot be moved to the final destination.
	 */
	private void moveStagedDirectory() throws IOException {
		Path stage = ensureStagedDirectory();

		// If the target already exists, move it to a backup location first.
		// Unlike a single file, with multiple files in a directory, we cannot guarantee that the move will be atomic.
		boolean hadTarget = Files.exists(path, LinkOption.NOFOLLOW_LINKS);
		if (hadTarget) {
			Path parent = requireParentDirectory();
			backupPath = parent.resolve(stagingPrefix() + UUID.randomUUID() + ".backup");
			Files.move(path, backupPath);
		}

		// Move the staged directory to the final destination.
		try {
			moveNew(stage, path);
			stagedPath = null;
		} catch (IOException ex) {
			// If the move failed, attempt to restore the backup if it exists.
			if (hadTarget) {
				try {
					moveNew(backupPath, path);
					backupPath = null;
				} catch (IOException restoreFailure) {
					ex.addSuppressed(restoreFailure);
				}
			}
			throw ex;
		}

		// If the move succeeded, delete the backup if it somehow still exists.
		if (backupPath != null) {
			deleteTree(backupPath);
			backupPath = null;
		}
	}

	/**
	 * Moves a file/directory to a new location, replacing the destination if it exists.
	 *
	 * @param source
	 * 		Path to move.
	 * @param destination
	 * 		Path to move to.
	 *
	 * @throws IOException
	 * 		When the move fails.
	 */
	private static void moveReplacing(@Nonnull Path source, @Nonnull Path destination) throws IOException {
		try {
			Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
		} catch (AtomicMoveNotSupportedException ex) {
			Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	/**
	 * Moves a file/directory to a new location, without replacing the destination if it exists.
	 *
	 * @param source
	 * 		Path to move.
	 * @param destination
	 * 		Path to move to.
	 *
	 * @throws IOException
	 * 		When the move fails.
	 */
	private static void moveNew(@Nonnull Path source, @Nonnull Path destination) throws IOException {
		try {
			Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE);
		} catch (AtomicMoveNotSupportedException ex) {
			Files.move(source, destination);
		}
	}

	/**
	 * Deletes a file/directory tree.
	 *
	 * @param root
	 * 		Path to delete.
	 *
	 * @throws IOException
	 * 		When the deletion fails.
	 */
	private static void deleteTree(@Nonnull Path root) throws IOException {
		Files.walkFileTree(root, new SimpleFileVisitor<>() {
			@Override
			public FileVisitResult visitFile(@Nonnull Path file, @Nonnull BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(@Nonnull Path dir, IOException ex) throws IOException {
				if (ex != null)
					throw ex;
				Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}
		});
	}

	/**
	 * Output writing strategy.
	 */
	public enum Mode {
		/** Write directly to the destination. */
		DIRECT,
		/** Write to a sibling file and replace the destination after generation. */
		ATOMIC_FILE,
		/** Write a complete tree to a sibling directory and replace the destination tree. */
		MIRROR_DIRECTORY
	}
}
