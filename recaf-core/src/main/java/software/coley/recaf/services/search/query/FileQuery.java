package software.coley.recaf.services.search.query;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.services.search.FileSearchVisitor;

/**
 * Query targeting {@link FileInfo}.
 *
 * @author Matt Coley
 */
public interface FileQuery extends Query {
	@Nonnull
	FileSearchVisitor visitor(@Nullable FileSearchVisitor delegate);
}
