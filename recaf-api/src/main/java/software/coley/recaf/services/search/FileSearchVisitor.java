package software.coley.recaf.services.search;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.path.FilePathNode;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.services.search.result.Results;

import java.util.function.BiConsumer;

/**
 * Visitor for {@link FileInfo}
 *
 * @author Matt Coley
 */
public interface FileSearchVisitor extends SearchVisitor {
	/**
	 * Visits a generic file.
	 *
	 * @param resultSink
	 * 		Consumer to feed result values into, typically populating a {@link Results} instance.
	 * @param filePath
	 * 		Path to file being visited.
	 * @param fileInfo
	 * 		File to visit.
	 */
	void visit(@Nonnull BiConsumer<PathNode<?>, Object> resultSink,
			   @Nonnull FilePathNode filePath,
			   @Nonnull FileInfo fileInfo);
}
