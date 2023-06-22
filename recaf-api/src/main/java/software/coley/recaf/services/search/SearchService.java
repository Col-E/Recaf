package software.coley.recaf.services.search;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.recaf.info.AndroidClassInfo;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.path.*;
import software.coley.recaf.services.Service;
import software.coley.recaf.services.search.builtin.NumberQuery;
import software.coley.recaf.services.search.builtin.ReferenceQuery;
import software.coley.recaf.services.search.builtin.StringQuery;
import software.coley.recaf.services.search.result.*;
import software.coley.recaf.util.threading.ThreadPoolFactory;
import software.coley.recaf.util.threading.ThreadUtil;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.AndroidClassBundle;
import software.coley.recaf.workspace.model.bundle.FileBundle;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
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
	public Results search(@Nonnull Workspace workspace, @Nonnull Query query) {
		return search(workspace, Collections.singletonList(query));
	}

	/**
	 * @param workspace
	 * 		Workspace to search in.
	 * @param queries
	 * 		Multiple queries of search parameters.
	 *
	 * @return Results of search.
	 */
	public Results search(@Nonnull Workspace workspace, @Nonnull List<Query> queries) {
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
		WorkspacePathNode workspaceNode = new WorkspacePathNode(workspace);
		for (WorkspaceResource resource : workspace.getAllResources(false)) {
			ResourcePathNode resourceNode = workspaceNode.child(resource);
			// Visit android content
			if (androidClassVisitor != null) {
				for (AndroidClassBundle bundle : resource.getAndroidClassBundles().values()) {
					BundlePathNode bundleNode = resourceNode.child(bundle);
					for (AndroidClassInfo classInfo : bundle) {
						ClassPathNode classPath = bundleNode
								.child(classInfo.getPackageName())
								.child(classInfo);
						service.submit(() -> {
							androidClassVisitor.visit((path, value) -> results.add(createResult(path, value)), classPath, classInfo);
						});
					}
				}
			}

			// Visit JVM content
			if (jvmClassVisitor != null) {
				JvmClassBundle primaryBundle = resource.getJvmClassBundle();
				BundlePathNode primaryBundleNode = resourceNode.child(primaryBundle);
				for (JvmClassInfo classInfo : primaryBundle) {
					ClassPathNode classPath = primaryBundleNode
							.child(classInfo.getPackageName())
							.child(classInfo);
					service.submit(() -> {
						jvmClassVisitor.visit((path, value) -> results.add(createResult(path, value)), classPath, classInfo);
					});
				}
				for (JvmClassBundle bundle : resource.getVersionedJvmClassBundles().values()) {
					BundlePathNode bundleNode = resourceNode.child(bundle);
					for (JvmClassInfo classInfo : bundle) {
						ClassPathNode classPath = bundleNode
								.child(classInfo.getPackageName())
								.child(classInfo);
						service.submit(() -> {
							jvmClassVisitor.visit((path, value) -> results.add(createResult(path, value)), classPath, classInfo);
						});
					}
				}
			}

			// Visit file content
			if (fileVisitor != null) {
				FileBundle fileBundle = resource.getFileBundle();
				BundlePathNode bundleNode = resourceNode.child(fileBundle);
				for (FileInfo fileInfo : fileBundle) {
					FilePathNode filePath = bundleNode
							.child(fileInfo.getDirectoryName())
							.child(fileInfo);
					service.submit(() -> {
						fileVisitor.visit((path, value) -> results.add(createResult(path, value)), filePath, fileInfo);
					});
				}
			}
		}

		ThreadUtil.blockUntilComplete(service);
		return results;
	}

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
