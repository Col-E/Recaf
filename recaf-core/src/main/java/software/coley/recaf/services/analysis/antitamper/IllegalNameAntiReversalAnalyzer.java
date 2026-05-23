package software.coley.recaf.services.analysis.antitamper;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.PathNodes;
import software.coley.recaf.services.mapping.gen.filter.IncludeEmptyPackageNameFilter;
import software.coley.recaf.services.mapping.gen.filter.IncludeKeywordNameFilter;
import software.coley.recaf.services.mapping.gen.filter.IncludeNonAsciiNameFilter;
import software.coley.recaf.services.mapping.gen.filter.IncludeNonJavaIdentifierNameFilter;
import software.coley.recaf.services.mapping.gen.filter.IncludeWhitespaceNameFilter;
import software.coley.recaf.services.mapping.gen.filter.NameGeneratorFilter;
import software.coley.recaf.util.threading.ThreadPoolFactory;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;

/**
 * Analyzer for illegal JVM class and member names.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class IllegalNameAntiReversalAnalyzer implements AntiReversalAnalyzer<IllegalNameAnalysis> {
	public static final String SERVICE_ID = "illegal-name-analysis";
	private static final NameGeneratorFilter ILLEGAL_NAME_FILTER =
			new IncludeWhitespaceNameFilter(
					new IncludeNonAsciiNameFilter(
							new IncludeKeywordNameFilter(
									new IncludeNonJavaIdentifierNameFilter(
											new IncludeEmptyPackageNameFilter(null)))));

	/**
	 * @return Filter used to determine if a class or member has an illegal name.
	 */
	@Nonnull
	public static NameGeneratorFilter getIllegalNameFilter() {
		return ILLEGAL_NAME_FILTER;
	}

	@Nonnull
	@Override
	public Class<IllegalNameAnalysis> getResultType() {
		return IllegalNameAnalysis.class;
	}

	@Nonnull
	@Override
	public IllegalNameAnalysis analyze(@Nonnull Workspace workspace, @Nonnull WorkspaceResource resource) {
		Collection<ClassPathNode> classesWithIllegalNames = new ConcurrentLinkedQueue<>(); // Needs to be a concurrent collection.
		Queue<WorkspaceResource> resourceQueue = new ArrayDeque<>();
		resourceQueue.add(resource);
		try (ExecutorService service = ThreadPoolFactory.newFixedThreadPool("illegal-name-analysis")) {
			List<Callable<Void>> tasks = new ArrayList<>(1000);
			while (!resourceQueue.isEmpty()) {
				WorkspaceResource currentResource = resourceQueue.remove();
				currentResource.jvmAllClassBundleStream().forEach(bundle -> bundle.forEach(cls -> {
					// While this task is 'simple' it can be a lot of work for workspaces with 1000's of classes
					// hence why each class is checked in parallel.
					tasks.add(() -> {
						if (hasIllegalName(cls))
							classesWithIllegalNames.add(PathNodes.classPath(workspace, currentResource, bundle, cls));
						return null;
					});
				}));
				resourceQueue.addAll(currentResource.getEmbeddedResources().values());
			}

			// Wait for all classes to be processed
			try {
				service.invokeAll(tasks);
			} catch (InterruptedException ignored) {}
		}
		return new IllegalNameAnalysis(List.copyOf(classesWithIllegalNames));
	}

	private static boolean hasIllegalName(@Nonnull JvmClassInfo cls) {
		if (ILLEGAL_NAME_FILTER.shouldMapClass(cls))
			return true;
		for (FieldMember field : cls.getFields())
			if (ILLEGAL_NAME_FILTER.shouldMapField(cls, field))
				return true;
		for (MethodMember method : cls.getMethods())
			if (ILLEGAL_NAME_FILTER.shouldMapMethod(cls, method))
				return true;
		return false;
	}

	@Nonnull
	@Override
	public String getServiceId() {
		return SERVICE_ID;
	}
}
