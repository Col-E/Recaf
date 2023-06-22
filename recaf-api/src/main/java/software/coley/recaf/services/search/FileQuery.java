package software.coley.recaf.services.search;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.FileInfo;

/**
 * Query targeting {@link FileInfo}.
 *
 * @author Matt Coley
 */
public interface FileQuery extends Query {
	@Nonnull
	FileSearchVisitor visitor(@Nullable FileSearchVisitor delegate);
}
