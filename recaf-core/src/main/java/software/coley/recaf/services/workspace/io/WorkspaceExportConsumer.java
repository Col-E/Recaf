package software.coley.recaf.services.workspace.io;

import jakarta.annotation.Nonnull;

import java.io.IOException;

/**
 * Outline of IO writing for {@link WorkspaceExporter} output.
 *
 * @author Matt Coley
 */
public interface WorkspaceExportConsumer {
	/**
	 * Called when writing content to a single given location based on the implementation.
	 * This may be called multiple times before {@link #commit()} is invoked.
	 *
	 * @param bytes
	 * 		Bytes to write/append to the output.
	 *
	 * @throws IOException
	 * 		When the content cannot be written to.
	 */
	void write(@Nonnull byte[] bytes) throws IOException;

	/**
	 * Called when writing content to a relative location based on the implementation.
	 * This may be called multiple times for a given relative path before {@link #commit()} is invoked.
	 *
	 * @param relative
	 * 		Relative path of content.
	 * @param bytes
	 * 		Bytes to write/append to the given relative path.
	 *
	 * @throws IOException
	 * 		When the content cannot be written to.
	 */
	void writeRelative(@Nonnull String relative, @Nonnull byte[] bytes) throws IOException;

	/**
	 * Called when the export process is completed.
	 *
	 * @throws IOException
	 * 		When the content couldn't be committed.
	 */
	void commit() throws IOException;
}
