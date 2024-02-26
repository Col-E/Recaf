package software.coley.recaf.services.workspace.io;

import jakarta.annotation.Nonnull;
import software.coley.recaf.util.io.ByteSource;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;

/**
 * Service outline for supporting creation of {@link WorkspaceResource} instances.
 *
 * @author Matt Coley
 */
public interface ResourceImporter {
	String SERVICE_ID = "resource-importer";

	/**
	 * @param source
	 * 		Some generic content source.
	 *
	 * @return Workspace resource representing the content.
	 *
	 * @throws IOException
	 * 		When the content cannot be read from.
	 */
	@Nonnull
	WorkspaceResource importResource(@Nonnull ByteSource source) throws IOException;

	/**
	 * @param file
	 * 		File/directory to import from.
	 *
	 * @return Workspace resource representing the file/directory.
	 *
	 * @throws IOException
	 * 		When the content at the file path cannot be read from.
	 */
	@Nonnull
	default WorkspaceResource importResource(@Nonnull File file) throws IOException {
		return importResource(file.toPath());
	}

	/**
	 * @param path
	 * 		File/directory path to import from.
	 *
	 * @return Workspace resource representing the file/directory.
	 *
	 * @throws IOException
	 * 		When the content at the file path cannot be read from.
	 */
	@Nonnull
	WorkspaceResource importResource(@Nonnull Path path) throws IOException;

	/**
	 * @param url
	 * 		URL to content to import from.
	 *
	 * @return Workspace resource representing the remote content.
	 *
	 * @throws IOException
	 * 		When content from the URL cannot be accessed.
	 */
	@Nonnull
	WorkspaceResource importResource(@Nonnull URL url) throws IOException;

	/**
	 * @param uri
	 * 		URI to content to import from.
	 *
	 * @return Workspace resource representing the remote content.
	 *
	 * @throws IOException
	 * 		When reading from the URI fails either due to a malformed URI,
	 * 		or the content being inaccessible.
	 */
	@Nonnull
	default WorkspaceResource importResource(@Nonnull URI uri) throws IOException {
		return importResource(uri.toURL());
	}
}
