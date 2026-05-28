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
 * Discovery process for locating entry points in Forge mods.
 *
 * @author Matt Coley
 * @see <a href="https://docs.neoforged.net/docs/gettingstarted/modfiles/#mod-entrypoints">Forge entry points</a>
 * @see <a href="https://docs.neoforged.net/docs/concepts/events#the-mod-lifecycle">Forge mod lifecycle</a>
 */
@ApplicationScoped
public class ForgeModEntryPointDiscovery implements EntryPointDiscovery {
	private final InheritanceGraphService inheritanceGraphService;

	@Inject
	public ForgeModEntryPointDiscovery(@Nonnull InheritanceGraphService inheritanceGraphService) {
		this.inheritanceGraphService = inheritanceGraphService;
	}

	@Nonnull
	@Override
	public EntryPointKind kind() {
		return EntryPointKind.MC_FORGE_MOD_INIT;
	}

	@Nonnull
	@Override
	public List<EntryPoint> findEntryPoints(@Nonnull Workspace workspace, @Nonnull WorkspaceResource resource) {
		InheritanceGraph graph = inheritanceGraphService.getOrCreateInheritanceGraph(workspace);
		List<EntryPoint> entries = new ArrayList<>();
		resource.jvmAllClassBundleStream().forEach(bundle -> bundle.forEach(cls -> {
			String className = cls.getName();
			ClassPathNode classPath = null;
			EntryPoint classEntry = null;

			// Check for @Mod annotations
			Set<String> annotations = Sets.combine(
					cls.getAnnotations().stream().map(AnnotationInfo::getDescriptor).collect(Collectors.toSet()),
					cls.getTypeAnnotations().stream().map(AnnotationInfo::getDescriptor).collect(Collectors.toSet())
			);
			for (String annotation : annotations) {
				if ("Lnet/minecraftforge/fml/common/Mod;".equals(annotation)
						|| "Lnet/neoforged/fml/common/Mod;".equals(annotation)) {
					classPath = PathNodes.classPath(workspace, resource, bundle, cls);
					classEntry = new EntryPoint(kind(), classPath, null);
					break;
				}
			}

			// Check for initialization event receivers
			boolean foundEventReceiver = false;
			for (MethodMember method : cls.getMethods()) {
				String desc = method.getDescriptor();
				if ("(Lnet/minecraftforge/fml/event/lifecycle/FMLClientSetupEvent;)V".equals(desc)
						|| "(Lnet/minecraftforge/fml/event/lifecycle/FMLCommonSetupEvent;)V".equals(desc)
						|| "(Lnet/minecraftforge/fml/event/lifecycle/FMLDedicatedServerSetupEvent;)V".equals(desc)
						|| "(Lnet/neoforged/fml/event/lifecycle/FMLClientSetupEvent;)V".equals(desc)
						|| "(Lnet/neoforged/fml/event/lifecycle/FMLCommonSetupEvent;)V".equals(desc)
						|| "(Lnet/neoforged/fml/event/lifecycle/FMLDedicatedServerSetupEvent;)V".equals(desc)
						// Not sure if we should consider events like this as entry points...
						// || "(Lnet/minecraftforge/event/server/ServerAboutToStartEvent;)V".equals(desc)
						// || "(Lnet/minecraftforge/event/server/ServerStartingEvent;)V".equals(desc)
						// || "(Lnet/minecraftforge/event/server/ServerStartedEvent;)V".equals(desc)
						// || "(Lnet/neoforged/neoforge/event/server/ServerAboutToStartEvent;)V".equals(desc)
						// || "(Lnet/neoforged/neoforge/event/server/ServerStartingEvent;)V".equals(desc)
						// || "(Lnet/neoforged/neoforge/event/server/ServerStartedEvent;)V".equals(desc)
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
