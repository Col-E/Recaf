package software.coley.recaf.services.search;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.AndroidClassInfo;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.services.search.result.Results;

/**
 * Visitor for {@link AndroidClassInfo}
 *
 * @author Matt Coley
 */
public interface AndroidClassSearchVisitor extends SearchVisitor {
	/**
	 * Visits an Android class.
	 *
	 * @param resultSink
	 * 		Consumer to feed result values into, typically populating a {@link Results} instance.
	 * @param classPath
	 * 		Path to class being visited.
	 * @param classInfo
	 * 		Class to visit.
	 */
	void visit(@Nonnull ResultSink resultSink,
			   @Nonnull ClassPathNode classPath,
			   @Nonnull AndroidClassInfo classInfo);
}
