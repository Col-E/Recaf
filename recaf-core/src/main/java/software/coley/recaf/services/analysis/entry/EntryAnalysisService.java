package software.coley.recaf.services.analysis.entry;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import software.coley.recaf.services.Service;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;

/**
 * Service for entry point discovery.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class EntryAnalysisService implements Service {
	public static final String SERVICE_ID = "entry-analysis";
	private static final Comparator<EntryPointDiscovery> DISCOVERY_ORDER =
			Comparator.comparingInt(EntryPointDiscovery::getPriority)
					.thenComparing(discovery -> discovery.kind().id());
	private final EntryAnalysisConfig config;
	private final List<EntryPointDiscovery> discoveries = new ArrayList<>();

	@Inject
	public EntryAnalysisService(@Nonnull EntryAnalysisConfig config,
	                            @Nonnull Instance<EntryPointDiscovery> discoveries) {
		this.config = config;

		for (EntryPointDiscovery discovery : discoveries)
			registerDiscovery(discovery);
	}

	/**
	 * Register an entry point discovery with the service.
	 *
	 * @param discovery
	 * 		Discovery to register.
	 *
	 * @throws IllegalArgumentException
	 * 		When a discovery with the same kind identifier is already registered.
	 */
	public void registerDiscovery(@Nonnull EntryPointDiscovery discovery) {
		String kindId = discovery.kind().id();
		if (discoveries.stream().anyMatch(existing -> existing.kind().id().equals(kindId)))
			throw new IllegalArgumentException("Discovery with kind ID already registered: " + kindId);
		discoveries.add(discovery);
		discoveries.sort(DISCOVERY_ORDER);
	}

	/**
	 * Unregister an entry point discovery by kind identifier.
	 *
	 * @param kindId
	 * 		Discovery kind identifier.
	 */
	public void unregisterDiscovery(@Nonnull String kindId) {
		discoveries.removeIf(discovery -> discovery.kind().id().equals(kindId));
	}

	/**
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Resource to inspect, including embedded resources.
	 *
	 * @return Entry points discovered in traversal order.
	 */
	@Nonnull
	public List<EntryPoint> findEntryPoints(@Nonnull Workspace workspace,
	                                        @Nonnull WorkspaceResource resource) {
		List<EntryPoint> entries = new ArrayList<>();
		Queue<WorkspaceResource> resourceQueue = new ArrayDeque<>();
		resourceQueue.add(resource);
		while (!resourceQueue.isEmpty()) {
			WorkspaceResource currentResource = resourceQueue.remove();
			for (EntryPointDiscovery discovery : discoveries)
				entries.addAll(discovery.findEntryPoints(workspace, currentResource));

			resourceQueue.addAll(currentResource.getEmbeddedResources().values());
		}
		return entries;
	}

	@Nonnull
	@Override
	public String getServiceId() {
		return SERVICE_ID;
	}

	@Nonnull
	@Override
	public EntryAnalysisConfig getServiceConfig() {
		return config;
	}
}
