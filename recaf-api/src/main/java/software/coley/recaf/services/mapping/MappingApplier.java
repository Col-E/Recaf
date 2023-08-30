package software.coley.recaf.services.mapping;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import software.coley.recaf.cdi.WorkspaceScoped;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.properties.builtin.HasMappedReferenceProperty;
import software.coley.recaf.info.properties.builtin.OriginalClassNameProperty;
import software.coley.recaf.info.properties.builtin.RemapOriginTaskProperty;
import software.coley.recaf.services.Service;
import software.coley.recaf.services.inheritance.InheritanceGraph;
import software.coley.recaf.services.mapping.aggregate.AggregateMappingManager;
import software.coley.recaf.util.threading.ThreadPoolFactory;
import software.coley.recaf.util.threading.ThreadUtil;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

/**
 * Applies mappings to workspaces and workspace resources, wrapping the results in a {@link MappingResults}.
 * To update the workspace with the mapping results, use {@link MappingResults#apply()}.
 *
 * @author Matt Coley
 * @see MappingResults
 */
@WorkspaceScoped
public class MappingApplier implements Service {
	public static final String SERVICE_ID = "mapping-applier";
	private static final ExecutorService applierThreadPool = ThreadPoolFactory.newFixedThreadPool(SERVICE_ID);
	private final List<MappingApplicationListener> mappingApplicationListeners = new ArrayList<>();
	private final InheritanceGraph inheritanceGraph;
	private final AggregateMappingManager aggregateMappingManager;
	private final Workspace workspace;
	private final MappingApplierConfig config;

	@Inject
	public MappingApplier(@Nonnull MappingApplierConfig config,
						  @Nonnull InheritanceGraph inheritanceGraph,
						  @Nonnull AggregateMappingManager aggregateMappingManager,
						  @Nonnull Workspace workspace) {
		this.inheritanceGraph = inheritanceGraph;
		this.aggregateMappingManager = aggregateMappingManager;
		this.workspace = workspace;
		this.config = config;
	}

	/**
	 * Adds a listener which is passed to created {@link MappingResults} from {@link #applyToPrimaryResource(Mappings)}
	 * and {@link #applyToClasses(Mappings, WorkspaceResource, JvmClassBundle, List)}.
	 * <p>
	 * This allows you to listen to all mapping operations done via proper API usage, intercepting before they
	 * execute the task, and after they complete the mapping task.
	 *
	 * @param listener
	 * 		Listener to add.
	 */
	public void addMappingApplicationListener(@Nonnull MappingApplicationListener listener) {
		mappingApplicationListeners.add(listener);
	}

	/**
	 * @param listener
	 * 		Listener to remove.
	 *
	 * @return {@code true} when item was removed.
	 * {@code false} when item was not in the list to begin with.
	 */
	public boolean removeMappingApplicationListener(@Nonnull MappingApplicationListener listener) {
		return mappingApplicationListeners.remove(listener);
	}

	/**
	 * @return Application listener encompassing all the current items in {@link #mappingApplicationListeners},
	 * or {@code null} if there are no listeners.
	 */
	@Nullable
	private MappingApplicationListener createBundledMappingApplicationListener() {
		final List<MappingApplicationListener> listeners = mappingApplicationListeners;

		// Simple edge cases.
		if (listeners.isEmpty())
			return null;
		else if (listeners.size() == 1)
			return listeners.get(0);

		// Bundle multiple listeners.
		return new MappingApplicationListener() {
			@Override
			public void onPreApply(@Nonnull MappingResults mappingResults) {
				for (MappingApplicationListener listener : listeners) {
					listener.onPreApply(mappingResults);
				}
			}

			@Override
			public void onPostApply(@Nonnull MappingResults mappingResults) {
				for (MappingApplicationListener listener : listeners) {
					listener.onPostApply(mappingResults);
				}
			}
		};
	}

	/**
	 * Applies the mapping operation to the given classes.
	 *
	 * @param mappings
	 * 		The mappings to apply.
	 * @param resource
	 * 		Resource containing the classes.
	 * @param bundle
	 * 		Bundle containing the classes.
	 * @param classes
	 * 		Classes to apply mappings to.
	 *
	 * @return Result wrapper detailing affected classes from the mapping operation.
	 */
	@Nonnull
	public MappingResults applyToClasses(@Nonnull Mappings mappings,
										 @Nonnull WorkspaceResource resource,
										 @Nonnull JvmClassBundle bundle,
										 @Nonnull List<JvmClassInfo> classes) {
		enrich(mappings);
		MappingResults results = new MappingResults(mappings, createBundledMappingApplicationListener())
				.withAggregateManager(aggregateMappingManager);

		// Apply mappings to the provided classes, collecting into the results model.
		ExecutorService service = ThreadUtil.phasingService(applierThreadPool);
		for (JvmClassInfo classInfo : classes)
			service.execute(() -> dumpIntoResults(results, workspace, resource, bundle, classInfo, mappings));
		ThreadUtil.blockUntilComplete(service);

		// Yield results
		return results;
	}

	/**
	 * Applies the mapping operation to the current workspace's primary resource.
	 *
	 * @param mappings
	 * 		The mappings to apply.
	 *
	 * @return Result wrapper detailing affected classes from the mapping operation.
	 */
	@Nonnull
	public MappingResults applyToPrimaryResource(@Nonnull Mappings mappings) {
		enrich(mappings);
		WorkspaceResource resource = workspace.getPrimaryResource();

		MappingResults results = new MappingResults(mappings, createBundledMappingApplicationListener())
				.withAggregateManager(aggregateMappingManager);

		// Apply mappings to all classes in the primary resource, collecting into the results model.
		ExecutorService service = ThreadUtil.phasingService(applierThreadPool);
		Stream.concat(resource.jvmClassBundleStream(), resource.versionedJvmClassBundleStream()).forEach(bundle -> {
			bundle.forEach(classInfo -> {
				service.execute(() -> dumpIntoResults(results, workspace, resource, bundle, classInfo, mappings));
			});
		});
		ThreadUtil.blockUntilComplete(service);

		// Yield results
		return results;
	}

	private void enrich(Mappings mappings) {
		// Check if mappings can be enriched with type look-ups
		if (mappings instanceof MappingsAdapter adapter) {
			// If we have "Dog extends Animal" and both define "jump" this lets "Dog.jump()" see "Animal.jump()"
			// allowing mappings that aren't complete for their type hierarchies to be filled in.
			adapter.enableHierarchyLookup(inheritanceGraph);
		}
	}

	/**
	 * Applies mappings locally and dumps them into the provided results collection.
	 * <p>
	 * To apply these mappings you need to call {@link MappingResults#apply()}.
	 *
	 * @param results
	 * 		Results collection to insert into.
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param classInfo
	 * 		The class to apply mappings to.
	 * @param mappings
	 * 		The mappings to apply.
	 */
	private static void dumpIntoResults(@Nonnull MappingResults results,
										@Nonnull Workspace workspace,
										@Nonnull WorkspaceResource resource,
										@Nonnull JvmClassBundle bundle,
										@Nonnull JvmClassInfo classInfo,
										@Nonnull Mappings mappings) {
		String originalName = classInfo.getName();

		// Apply renamer
		ClassWriter cw = new ClassWriter(0);
		ClassReader cr = classInfo.getClassReader();
		WorkspaceClassRemapper remapVisitor = new WorkspaceClassRemapper(cw, workspace, mappings);
		cr.accept(remapVisitor, 0);

		// Update class if it has any modified references
		if (remapVisitor.hasMappingBeenApplied()) {
			JvmClassInfo updatedInfo = classInfo.toJvmClassBuilder()
					.adaptFrom(new ClassReader(cw.toByteArray()))
					.build();

			// Mark has referencing something mapped.
			HasMappedReferenceProperty.set(updatedInfo);

			// Set the result wrapper that caused this class to update.
			updatedInfo.setProperty(new RemapOriginTaskProperty(results));

			// If the name changed, mark what the original was.
			// If this property was set before (A --> B, now B --> C) then we won't update it.
			if (!updatedInfo.getName().equals(originalName))
				updatedInfo.setPropertyIfMissing(OriginalClassNameProperty.KEY,
						() -> new OriginalClassNameProperty(originalName));

			// Add to the results collection.
			results.add(workspace, resource, bundle, classInfo, updatedInfo);
		}
	}

	@Nonnull
	@Override
	public String getServiceId() {
		return SERVICE_ID;
	}

	@Nonnull
	@Override
	public MappingApplierConfig getServiceConfig() {
		return config;
	}
}
