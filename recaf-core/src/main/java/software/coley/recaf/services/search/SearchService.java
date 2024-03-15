package software.coley.recaf.services.search;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.recaf.info.AndroidClassInfo;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.path.*;
import software.coley.recaf.services.Service;
import software.coley.recaf.services.search.query.*;
import software.coley.recaf.services.search.result.*;
import software.coley.recaf.util.threading.ThreadPoolFactory;
import software.coley.recaf.util.threading.ThreadUtil;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.AndroidClassBundle;
import software.coley.recaf.workspace.model.bundle.FileBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Outline for running various searches.
 *
 * @author Matt Coley
 * @see NumberQuery
 * @see ReferenceQuery
 * @see StringQuery
 */
@ApplicationScoped
public class SearchService implements Service {
	public static final String SERVICE_ID = "search";
	private final SearchServiceConfig config;

	@Inject
	public SearchService(SearchServiceConfig config) {
		this.config = config;
	}

	/**
	 * @param workspace
	 * 		Workspace to search in.
	 * @param query
	 * 		Query of search parameters.
	 *
	 * @return Results of search.
	 */
	@Nonnull
	public Results search(@Nonnull Workspace workspace, @Nonnull Query query) {
		return search(workspace, Collections.singletonList(query));
	}

	/**
	 * @param workspace
	 * 		Workspace to search in.
	 * @param query
	 * 		Query of search parameters.
	 * @param feedback
	 * 		Search visitation feedback. Allows early cancellation of searches.
	 *
	 * @return Results of search.
	 */
	@Nonnull
	public Results search(@Nonnull Workspace workspace, @Nonnull Query query, @Nonnull SearchFeedback feedback) {
		return search(workspace, Collections.singletonList(query), feedback);
	}

	/**
	 * @param workspace
	 * 		Workspace to search in.
	 * @param queries
	 * 		Multiple queries of search parameters.
	 *
	 * @return Results of search.
	 */
	@Nonnull
	public Results search(@Nonnull Workspace workspace, @Nonnull List<Query> queries) {
		return search(workspace, queries, SearchFeedback.NO_OP);
	}

	/**
	 * @param workspace
	 * 		Workspace to search in.
	 * @param queries
	 * 		Multiple queries of search parameters.
	 * @param feedback
	 * 		Search visitation feedback. Allows early cancellation of searches.
	 *
	 * @return Results of search.
	 */
	@Nonnull
	public Results search(@Nonnull Workspace workspace, @Nonnull List<Query> queries, @Nonnull SearchFeedback feedback) {
		Results results = new Results();

		// Build visitors
		AndroidClassSearchVisitor androidClassVisitorTemp = null;
		JvmClassSearchVisitor jvmClassVisitorTemp = null;
		FileSearchVisitor fileVisitorTemp = null;
		for (Query query : queries) {
			if (query instanceof AndroidClassQuery androidClassQuery) {
				androidClassVisitorTemp = androidClassQuery.visitor(androidClassVisitorTemp);
			}
			if (query instanceof JvmClassQuery jvmClassQuery) {
				jvmClassVisitorTemp = jvmClassQuery.visitor(jvmClassVisitorTemp);
			}
			if (query instanceof FileQuery fileQuery) {
				fileVisitorTemp = fileQuery.visitor(fileVisitorTemp);
			}
		}
		AndroidClassSearchVisitor androidClassVisitor = androidClassVisitorTemp;
		JvmClassSearchVisitor jvmClassVisitor = jvmClassVisitorTemp;
		FileSearchVisitor fileVisitor = fileVisitorTemp;

		// Run visitors on contents of workspace
		ExecutorService service = ThreadPoolFactory.newFixedThreadPool(SERVICE_ID + ":" + queries.hashCode());
		WorkspacePathNode workspaceNode = PathNodes.workspacePath(workspace);
		for (WorkspaceResource resource : workspace.getAllResources(false)) {
			ResourcePathNode resourceNode = workspaceNode.child(resource);
			// Visit android content
			if (androidClassVisitor != null) {
				for (AndroidClassBundle bundle : resource.getAndroidClassBundles().values()) {
					BundlePathNode bundleNode = resourceNode.child(bundle);
					for (AndroidClassInfo classInfo : bundle) {
						if (!feedback.doVisitClass(classInfo))
							continue;
						ClassPathNode classPath = bundleNode
								.child(classInfo.getPackageName())
								.child(classInfo);
						service.submit(() -> {
							if (feedback.hasRequestedStop())
								return;
							androidClassVisitor.visit(getResultSink(results, feedback), classPath, classInfo);
						});
					}
				}
			}

			// Visit JVM content
			if (jvmClassVisitor != null) {
				resource.jvmClassBundleStream().forEach(bundle -> {
					BundlePathNode bundlePathNode = resourceNode.child(bundle);
					for (JvmClassInfo classInfo : bundle) {
						if (!feedback.doVisitClass(classInfo))
							continue;
						ClassPathNode classPath = bundlePathNode
								.child(classInfo.getPackageName())
								.child(classInfo);
						service.submit(() -> {
							if (feedback.hasRequestedStop())
								return;
							jvmClassVisitor.visit(getResultSink(results, feedback), classPath, classInfo);
						});
					}
				});
			}

			// Visit file content
			if (fileVisitor != null) {
				FileBundle fileBundle = resource.getFileBundle();
				BundlePathNode bundleNode = resourceNode.child(fileBundle);
				for (FileInfo fileInfo : fileBundle) {
					if (!feedback.doVisitFile(fileInfo))
						continue;
					FilePathNode filePath = bundleNode
							.child(fileInfo.getDirectoryName())
							.child(fileInfo);
					service.submit(() -> {
						if (feedback.hasRequestedStop())
							return;
						fileVisitor.visit(getResultSink(results, feedback), filePath, fileInfo);
					});
				}
			}
		}

		ThreadUtil.blockUntilComplete(service);
		return results;
	}

	@Nonnull
	private static ResultSink getResultSink(@Nonnull Results results, @Nullable SearchFeedback feedback) {
		return (path, value) -> {
			Result<?> result = createResult(path, value);
			if (feedback == null || feedback.doAcceptResult(result))
				results.add(result);
		};
	}

	@Nonnull
	private static Result<?> createResult(@Nonnull PathNode<?> path, @Nonnull Object value) {
		if (value instanceof Number)
			return new NumberResult(path, (Number) value);
		if (value instanceof String)
			return new StringResult(path, (String) value);
		if (value instanceof ClassReferenceResult.ClassReference)
			return new ClassReferenceResult(path, (ClassReferenceResult.ClassReference) value);
		if (value instanceof MemberReferenceResult.MemberReference)
			return new MemberReferenceResult(path, (MemberReferenceResult.MemberReference) value);

		// Unknown value type
		throw new UnsupportedOperationException("Unsupported search result value type: " + value.getClass().getName());
	}

	@Nonnull
	@Override
	public String getServiceId() {
		return SERVICE_ID;
	}

	@Nonnull
	@Override
	public SearchServiceConfig getServiceConfig() {
		return config;
	}
}
