package software.coley.recaf.services.analysis.structure;

import com.google.devrel.gmscore.tools.apk.arsc.XmlAttribute;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.path.ClassMemberPathNode;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.PathNodes;
import software.coley.recaf.services.Service;
import software.coley.recaf.util.JavaVersion;
import software.coley.recaf.util.android.AndroidXmlUtil;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import static java.lang.reflect.Modifier.PUBLIC;
import static java.lang.reflect.Modifier.STATIC;

/**
 * Service for high-level application flow insights.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class FlowAnalysisService implements Service {
	public static final String SERVICE_ID = "flow-analysis";
	private final FlowAnalysisConfig config;

	@Inject
	public FlowAnalysisService(@Nonnull FlowAnalysisConfig config) {
		this.config = config;
	}

	/**
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Resource to inspect, including embedded resources.
	 *
	 * @return Entry-point groups discovered in traversal order.
	 */
	@Nonnull
	public List<EntryPointGroup> findEntryPointGroups(@Nonnull Workspace workspace,
	                                                  @Nonnull WorkspaceResource resource) {
		List<EntryPointGroup> groups = new ArrayList<>();
		Queue<WorkspaceResource> resourceQueue = new ArrayDeque<>();
		resourceQueue.add(resource);
		while (!resourceQueue.isEmpty()) {
			WorkspaceResource currentResource = resourceQueue.remove();

			// For JVM resources, we want to find all main methods and group them by class.
			currentResource.jvmAllClassBundleStream().forEach(bundle -> bundle.forEach(cls -> {
				List<ClassMemberPathNode> memberPaths = cls.getMethods().stream()
						.filter(m -> isJvmEntry(cls, m))
						.map(method -> PathNodes.memberPath(workspace, currentResource, bundle, cls, method))
						.toList();
				if (!memberPaths.isEmpty()) {
					ClassPathNode classPath = PathNodes.classPath(workspace, currentResource, bundle, cls);
					groups.add(new EntryPointGroup(classPath, EntryPointSource.JVM_MAIN_METHOD, memberPaths));
				}
			}));

			// For Android resources, we want to find all activities.
			for (AndroidXmlUtil.XmlElementData element : AndroidXmlUtil.getManifestStartElements(workspace, currentResource)) {
				if (!"activity".equals(element.element().getName()))
					continue;

				// We have an activity, we need to extract the associated class name from it.
				for (XmlAttribute attribute : element.element().getAttributes()) {
					String attributeName = AndroidXmlUtil.getString(element.strings(), attribute.nameIndex());
					if (!"name".equals(attributeName))
						continue;
					String activityName = AndroidXmlUtil.getString(element.strings(), attribute.rawValueIndex());
					if (activityName == null)
						continue;
					ClassPathNode activityPath = workspace.findAndroidClass(activityName.replace('.', '/'));
					if (activityPath != null)
						groups.add(new EntryPointGroup(activityPath, EntryPointSource.ANDROID_ACTIVITY, List.of()));
				}
			}

			resourceQueue.addAll(currentResource.getEmbeddedResources().values());
		}
		return groups;
	}

	@Nonnull
	@Override
	public String getServiceId() {
		return SERVICE_ID;
	}

	@Nonnull
	@Override
	public FlowAnalysisConfig getServiceConfig() {
		return config;
	}

	private static boolean isJvmEntry(@Nonnull JvmClassInfo owner, @Nonnull MethodMember method) {
		// Must be named "main".
		if (!method.getName().equals("main"))
			return false;

		// From Java 20 and below the main method contract is the classic one.
		String descriptor = method.getDescriptor();
		int version = JavaVersion.adaptFromClassFileVersion(owner.getVersion());
		if (version <= 20)
			return method.hasModifierMask(PUBLIC | STATIC)
					&& descriptor.equals("([Ljava/lang/String;)V");

		// Modern Java has more flexible entry points (JEP-445/463/477/495/512).
		// - static void main(String[] args)
		// - static void main()
		// - void main(String[] args)
		// - void main()
		return descriptor.equals("([Ljava/lang/String;)V") || descriptor.equals("()V");
	}
}
