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
 * Discovery process for locating entry points in Bukkit plugins.
 *
 * @author Matt Coley
 * @author TheWakz
 * @see <a href="https://docs.papermc.io/paper/dev/how-do-plugins-work/#plugin-lifecycle">Bukkit plugin entry points</a>
 */
@ApplicationScoped
public class BukkitPluginEntryPointDiscovery implements EntryPointDiscovery {
    private final InheritanceGraphService inheritanceGraphService;

    @Inject
    public BukkitPluginEntryPointDiscovery(@Nonnull InheritanceGraphService inheritanceGraphService) {
        this.inheritanceGraphService = inheritanceGraphService;
    }

    @Nonnull
    @Override
    public EntryPointKind kind() {
        return EntryPointKind.MC_BUKKIT_PLUGIN_INIT;
    }

    @Nonnull
    @Override
    public List<EntryPoint> findEntryPoints(@Nonnull Workspace workspace, @Nonnull WorkspaceResource resource) {
        InheritanceGraph graph = inheritanceGraphService.getOrCreateInheritanceGraph(workspace);
        List<EntryPoint> entries = new ArrayList<>();
        resource.jvmAllClassBundleStream().forEach(bundle -> bundle.forEach(cls -> {
            String className = cls.getName();
            ClassPathNode classPath = null;
            Boolean isPlugin = null;
            for (MethodMember method : cls.getMethods()) {
                // Must be a plugin enable or load method name.
                String methodName = method.getName();
                if (!methodName.equals("onEnable") && !methodName.equals("onLoad"))
                    continue;

                // Lazily check if this class is a plugin subtype.
                if (isPlugin == null)
                    isPlugin = isPlugin(graph, className);

                // Skip methods if the class is not a valid plugin.
                if (!isPlugin)
                    continue;

                // Add the entry point.
                if (classPath == null)
                    classPath = PathNodes.classPath(workspace, resource, bundle, cls);
                entries.add(new EntryPoint(kind(), classPath, classPath.child(method)));
            }
        }));
        return entries;
    }

    private static boolean isPlugin(@Nonnull InheritanceGraph graph, @Nonnull String className) {
        return graph.isAssignableFrom("org/bukkit/plugin/java/JavaPlugin", className);
    }
}
