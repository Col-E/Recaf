package software.coley.recaf.services.search;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.recaf.info.AndroidClassInfo;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.path.BundlePathNode;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.FilePathNode;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.path.PathNodes;
import software.coley.recaf.path.ResourcePathNode;
import software.coley.recaf.path.WorkspacePathNode;
import software.coley.recaf.services.Service;
import software.coley.recaf.services.search.query.AndroidClassQuery;
import software.coley.recaf.services.search.query.DeclarationQuery;
import software.coley.recaf.services.search.query.FileQuery;
import software.coley.recaf.services.search.query.JvmClassQuery;
import software.coley.recaf.services.search.query.NumberQuery;
import software.coley.recaf.services.search.query.Query;
import software.coley.recaf.services.search.query.ReferenceQuery;
import software.coley.recaf.services.search.query.StringQuery;
import software.coley.recaf.services.search.result.ClassReference;
import software.coley.recaf.services.search.result.ClassReferenceResult;
import software.coley.recaf.services.search.result.MemberReference;
import software.coley.recaf.services.search.result.MemberReferenceResult;
import software.coley.recaf.services.search.result.NumberResult;
import software.coley.recaf.services.search.result.Result;
import software.coley.recaf.services.search.result.Results;
import software.coley.recaf.services.search.result.StringResult;
import software.coley.recaf.util.threading.ThreadPoolFactory;
import software.coley.recaf.util.threading.ThreadUtil;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.AndroidClassBundle;
import software.coley.recaf.workspace.model.bundle.FileBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceFileResource;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

/**
 * Outline for running various searches.
 *
 * @author Matt Coley
 * @see NumberQuery
 * @see ReferenceQuery
 * @see DeclarationQuery
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
			if (query instanceof AndroidClassQuery androidClassQuery)
				androidClassVisitorTemp = androidClassQuery.visitor(androidClassVisitorTemp);
			if (query instanceof JvmClassQuery jvmClassQuery)
				jvmClassVisitorTemp = jvmClassQuery.visitor(jvmClassVisitorTemp);
			if (query instanceof FileQuery fileQuery)
				fileVisitorTemp = fileQuery.visitor(fileVisitorTemp);
		}
		AndroidClassSearchVisitor androidClassVisitor = androidClassVisitorTemp;
		JvmClassSearchVisitor jvmClassVisitor = jvmClassVisitorTemp;
		FileSearchVisitor fileVisitor = fileVisitorTemp;

		// Run visitors on contents of workspace
		ExecutorService service = ThreadPoolFactory.newFixedThreadPool(SERVICE_ID + ":" + queries.hashCode());
		WorkspacePathNode workspaceNode = PathNodes.workspacePath(workspace);
		for (WorkspaceResource resource : workspace.getAllResources(false))
			searchResource(results, service, feedback, resource, workspaceNode,
					androidClassVisitor, jvmClassVisitor, fileVisitor);
		ThreadUtil.blockUntilComplete(service);
		return results;
	}

	/**
	 * @param results
	 * 		Result container to dump into.
	 * @param service
	 * 		Thread scheduler service.
	 * @param feedback
	 * 		Search feedback mechanism <i>(To allow user cancellation and such)</i>
	 * @param resource
	 * 		Resource to search within.
	 * @param workspacePath
	 * 		Root workspace path node.
	 * @param androidClassVisitor
	 * 		Android class search visitor.
	 * 		Can be {@code null} to skip searching respective content.
	 * @param jvmClassVisitor
	 * 		JVM class search visitor.
	 * 		Can be {@code null} to skip searching respective content.
	 * @param fileVisitor
	 * 		File search visitor.
	 * 		Can be {@code null} to skip searching respective content.
	 */
	private static void searchResource(@Nonnull Results results,
	                                   @Nonnull ExecutorService service,
	                                   @Nonnull SearchFeedback feedback,
	                                   @Nonnull WorkspaceResource resource,
	                                   @Nonnull WorkspacePathNode workspacePath,
	                                   @Nullable AndroidClassSearchVisitor androidClassVisitor,
	                                   @Nullable JvmClassSearchVisitor jvmClassVisitor,
	                                   @Nullable FileSearchVisitor fileVisitor) {
		// Recursively search embedded resources.
		for (WorkspaceFileResource embeddedResource : resource.getEmbeddedResources().values()) {
			searchResource(results, service, feedback, embeddedResource, workspacePath,
					androidClassVisitor, jvmClassVisitor, fileVisitor);
		}

		// Visit android content
		ResourcePathNode resourcePath = workspacePath.child(resource);
		if (androidClassVisitor != null) {
			for (AndroidClassBundle bundle : resource.getAndroidClassBundles().values()) {
				BundlePathNode bundlePath = resourcePath.child(bundle);
				for (AndroidClassInfo classInfo : bundle) {
					if (feedback.hasRequestedCancellation())
						break;
					if (!feedback.doVisitClass(classInfo))
						continue;
					ClassPathNode classPath = bundlePath
							.child(classInfo.getPackageName())
							.child(classInfo);
					service.submit(() -> {
						if (feedback.hasRequestedCancellation())
							return;
						androidClassVisitor.visit(getResultSink(results, feedback), classPath, classInfo);
					});
				}
			}
		}

		// Visit JVM content
		if (jvmClassVisitor != null) {
			Stream.concat(resource.jvmClassBundleStream(), resource.versionedJvmClassBundleStream()).forEach(bundle -> {
				BundlePathNode bundlePath = resourcePath.child(bundle);
				for (JvmClassInfo classInfo : bundle) {
					if (feedback.hasRequestedCancellation())
						break;
					if (!feedback.doVisitClass(classInfo))
						continue;
					ClassPathNode classPath = bundlePath
							.child(classInfo.getPackageName())
							.child(classInfo);
					service.submit(() -> {
						if (feedback.hasRequestedCancellation())
							return;
						jvmClassVisitor.visit(getResultSink(results, feedback), classPath, classInfo);
					});
				}
			});
		}

		// Visit file content
		if (fileVisitor != null) {
			FileBundle fileBundle = resource.getFileBundle();
			BundlePathNode bundlePath = resourcePath.child(fileBundle);
			for (FileInfo fileInfo : fileBundle) {
				if (feedback.hasRequestedCancellation())
					break;
				if (!feedback.doVisitFile(fileInfo))
					continue;
				FilePathNode filePath = bundlePath
						.child(fileInfo.getDirectoryName())
						.child(fileInfo);
				service.submit(() -> {
					if (feedback.hasRequestedCancellation())
						return;
					fileVisitor.visit(getResultSink(results, feedback), filePath, fileInfo);
				});
			}
		}
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
		if (value instanceof Number number)
			return new NumberResult(path, number);
		if (value instanceof String string)
			return new StringResult(path, string);
		if (value instanceof ClassReference reference)
			return new ClassReferenceResult(path, reference);
		if (value instanceof MemberReference reference)
			return new MemberReferenceResult(path, reference);

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
