package software.coley.recaf.services.analysis.entry;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.PathNodes;
import software.coley.recaf.services.inheritance.InheritanceGraph;
import software.coley.recaf.services.inheritance.InheritanceGraphService;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.ArrayList;
import java.util.List;

/**
 * Discovery process for locating entry points in Fabric mods.
 *
 * @author Matt Coley
 * @see <a href="https://docs.fabricmc.net/develop/getting-started/project-structure#entrypoints">Fabric entry points</a>
 */
@ApplicationScoped
public class FabricModEntryPointDiscovery implements EntryPointDiscovery {
	private final InheritanceGraphService inheritanceGraphService;

	@Inject
	public FabricModEntryPointDiscovery(@Nonnull InheritanceGraphService inheritanceGraphService) {
		this.inheritanceGraphService = inheritanceGraphService;
	}

	@Nonnull
	@Override
	public EntryPointKind kind() {
		return EntryPointKind.MC_FABRIC_MOD_INIT;
	}

	@Nonnull
	@Override
	public List<EntryPoint> findEntryPoints(@Nonnull Workspace workspace, @Nonnull WorkspaceResource resource) {
		InheritanceGraph graph = inheritanceGraphService.getOrCreateInheritanceGraph(workspace);
		List<EntryPoint> entries = new ArrayList<>();
		resource.jvmAllClassBundleStream().forEach(bundle -> bundle.forEach(cls -> {
			String className = cls.getName();
			ClassPathNode classPath = null;
			Boolean isModInitializer = null;
			for (MethodMember method : cls.getMethods()) {
				// Must be a common or client initializer method name.
				String methodName = method.getName();
				if (!methodName.equals("onInitialize") && !methodName.equals("onInitializeClient"))
					continue;

				// Lazily check if this class is a mod-initializer subtype.
				if (isModInitializer == null)
					isModInitializer = isInitializer(graph, className);

				// Skip methods if the class is not a valid mod initializer.
				if (!isModInitializer)
					continue;

				// Add the entry point.
				if (classPath == null)
					classPath = PathNodes.classPath(workspace, resource, bundle, cls);
				entries.add(new EntryPoint(kind(), classPath, classPath.child(method)));
			}
		}));
		return entries;
	}

	private static boolean isInitializer(@Nonnull InheritanceGraph graph, @Nonnull String className) {
		// Fabric and Quilt (lol) both use the same interface names
		return graph.isAssignableFrom("net/fabricmc/api/ClientModInitializer", className)
				|| graph.isAssignableFrom("net/fabricmc/api/ModInitializer", className);
	}
}
