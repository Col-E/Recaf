package me.coley.recaf.workspace;

import java.io.IOException;
import java.nio.file.*;

/**
 * Common utility methods for interfacing with the virtual file-system recaf uses.
 *
 * @author Matt
 */
public interface InputFileSystem {
	String FILE_EXT = ".rcf";

	/**
	 * @return Files contained within input as navigable file system.
	 */
	FileSystem getFileSystem();

	/**
	 * Wrapper for system.getPath().
	 *
	 * @param name
	 * 		File name.
	 *
	 * @return File path.
	 */
	default Path getPath(String name) {
		return getPath(getFileSystem(), name);
	}

	/**
	 * Wrapper for system.getPath().
	 *
	 * @param system
	 * 		File system to fetch path from.
	 * @param name
	 * 		File name.
	 *
	 * @return File path.
	 */
	default Path getPath(FileSystem system, String name) {
		Path path = system.getPath("/" + name);
		path = extensionify(path);
		return path;
	}

	/**
	 * Retrieve bytes of file in the {@link #getFileSystem() virtual system}.
	 *
	 * @param name
	 * 		File name. For classes, use internal names.
	 *
	 * @return Bytes of requested files.
	 *
	 * @throws IOException
	 * 		File could not be read from.
	 */
	default byte[] getFile(String name) throws IOException {
		return Files.readAllBytes(getPath(name));
	}

	/**
	 * Removes file by name from local system.
	 *
	 * @param name
	 * 		File name.
	 *
	 * @throws IOException
	 * 		Thrown if the file could not be removed.
	 */
	default void removeFile(String name) throws IOException {
		Files.delete(getPath(name));
	}

	/**
	 * Retrieve bytes of file in the {@link #getFileSystem() virtual system}.
	 *
	 * @param path
	 * 		File path.
	 *
	 * @return Bytes of requested files.
	 *
	 * @throws IOException
	 * 		File could not be read from.
	 */
	default byte[] getFile(Path path) throws IOException {
		return Files.readAllBytes(path);
	}

	/**
	 * Add file to local system.
	 *
	 * @param path
	 * 		Path to write to.
	 * @param value
	 * 		Bytes to write.
	 *
	 * @throws IOException
	 * 		Thrown if could not write to path.
	 */
	default void write(Path path, byte[] value) throws IOException {
		ensureParentExists(path);
		Files.write(path, value, StandardOpenOption.CREATE);
	}

	/**
	 * Ensures the parent path exists. If the parent of the given path does not
	 * exist, all directories leading up to it will be created.
	 *
	 * @param path
	 * 		Path to some file.
	 *
	 * @throws IOException
	 * 		Thrown if the parent of the given path could not be created
	 * 		as a directory.
	 */
	default void ensureParentExists(Path path) throws IOException {
		Path parent = path.getParent();
		if(parent != null && !Files.isDirectory(parent)) {
			Files.createDirectories(parent);
		}
	}

	/**
	 * Append extension to given path.
	 *
	 * @param path
	 * 		Path to a file.
	 *
	 * @return Path to file, with added extension.
	 */
	default Path extensionify(Path path) {
		if(path.endsWith(FILE_EXT)) {
			return path;
		}
		return path.resolveSibling(path.getFileName() + FILE_EXT);
	}

}
