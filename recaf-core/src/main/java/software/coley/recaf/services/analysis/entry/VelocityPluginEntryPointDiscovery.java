package software.coley.recaf.services.analysis.entry;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.collections.Sets;
import software.coley.recaf.info.annotation.AnnotationInfo;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.PathNodes;
import software.coley.recaf.services.inheritance.InheritanceGraph;
import software.coley.recaf.services.inheritance.InheritanceGraphService;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Discovery process for locating entry points in Velocity plugins.
 *
 * @author Matt Coley
 * @author TheWakz
 * @see <a href="https://docs.papermc.io/velocity/dev/api-basics/#create-the-plugin-class">Velocity plugin entry points</a>
 */
@ApplicationScoped
public class VelocityPluginEntryPointDiscovery implements EntryPointDiscovery {
    private final InheritanceGraphService inheritanceGraphService;

    @Inject
    public VelocityPluginEntryPointDiscovery(@Nonnull InheritanceGraphService inheritanceGraphService) {
        this.inheritanceGraphService = inheritanceGraphService;
    }

    @Nonnull
    @Override
    public EntryPointKind kind() {
        return EntryPointKind.MC_VELOCITY_PLUGIN_INIT;
    }

    @Nonnull
    @Override
    public List<EntryPoint> findEntryPoints(@Nonnull Workspace workspace, @Nonnull WorkspaceResource resource) {
        InheritanceGraph graph = inheritanceGraphService.getOrCreateInheritanceGraph(workspace);
        List<EntryPoint> entries = new ArrayList<>();
        resource.jvmAllClassBundleStream().forEach(bundle -> bundle.forEach(cls -> {
            ClassPathNode classPath = null;
            EntryPoint classEntry = null;

            // Check for @Plugin annotations
            Set<String> annotations = Sets.combine(
                    cls.getAnnotations().stream().map(AnnotationInfo::getDescriptor).collect(Collectors.toSet()),
                    cls.getTypeAnnotations().stream().map(AnnotationInfo::getDescriptor).collect(Collectors.toSet())
            );
            for (String annotation : annotations) {
                if ("Lcom/velocitypowered/api/plugin/Plugin;".equals(annotation)) {
                    classPath = PathNodes.classPath(workspace, resource, bundle, cls);
                    classEntry = new EntryPoint(kind(), classPath, null);
                    break;
                }
            }

            // Check for initialization event receivers
            boolean foundEventReceiver = false;
            for (MethodMember method : cls.getMethods()) {
                String desc = method.getDescriptor();
                if ("(Lcom/velocitypowered/api/event/proxy/ProxyInitializeEvent;)V".equals(desc)
                        || "(Lcom/velocitypowered/api/event/proxy/ProxyReloadEvent;)V".equals(desc)
                ) {
                    if (classPath == null)
                        classPath = PathNodes.classPath(workspace, resource, bundle, cls);
                    entries.add(new EntryPoint(kind(), classPath, classPath.child(method)));
                    foundEventReceiver = true;
                }
            }

            // Only add the class as an entry point if it wasn't already added as an event receiver.
            // The event receiver is more specific and the containing class shares the same path.
            if (!foundEventReceiver && classEntry != null)
                entries.add(classEntry);
        }));
        return entries;
    }
}
