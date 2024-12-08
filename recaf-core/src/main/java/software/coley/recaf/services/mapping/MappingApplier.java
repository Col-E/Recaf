package software.coley.recaf.services.mapping;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.properties.builtin.HasMappedReferenceProperty;
import software.coley.recaf.info.properties.builtin.OriginalClassNameProperty;
import software.coley.recaf.info.properties.builtin.RemapOriginTaskProperty;
import software.coley.recaf.services.inheritance.InheritanceGraph;
import software.coley.recaf.services.mapping.aggregate.AggregateMappingManager;
import software.coley.recaf.services.workspace.WorkspaceManager;
import software.coley.recaf.util.threading.ThreadPoolFactory;
import software.coley.recaf.util.threading.ThreadUtil;
import software.coley.recaf.util.visitors.IllegalSignatureRemovingVisitor;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

/**
 * Applies mappings to workspaces and workspace resources, wrapping the results in a {@link MappingResults}.
 * To update the workspace with the mapping results, use {@link MappingResults#apply()}.
 *
 * @author Matt Coley
 * @see MappingResults
 */
public class MappingApplier {
	private static final ExecutorService applierThreadPool = ThreadPoolFactory.newFixedThreadPool(MappingApplierService.SERVICE_ID);
	private final InheritanceGraph inheritanceGraph;
	private final AggregateMappingManager aggregateMappingManager;
	private final MappingListeners listeners;
	private final Workspace workspace;

	/**
	 * @param workspace
	 * 		Workspace to apply mappings in.
	 * @param inheritanceGraph
	 * 		Inheritance graph for the given workspace.
	 * @param listeners
	 * 		Application mapping listeners
	 * 		<i>(If the target workspace is the {@link WorkspaceManager#getCurrent() current one})</i>
	 * @param aggregateMappingManager
	 * 		Aggregate mappings for tracking applications in the current workspace
	 * 		<i>(If the target workspace is the {@link WorkspaceManager#getCurrent() current one})</i>
	 */
	public MappingApplier(@Nonnull Workspace workspace,
	                      @Nonnull InheritanceGraph inheritanceGraph,
	                      @Nullable MappingListeners listeners,
	                      @Nullable AggregateMappingManager aggregateMappingManager) {
		this.inheritanceGraph = inheritanceGraph;
		this.aggregateMappingManager = aggregateMappingManager;
		this.listeners = listeners;
		this.workspace = workspace;
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
	                                     @Nonnull Collection<JvmClassInfo> classes) {
		mappings = enrich(mappings);
		MappingApplicationListener listener = listeners == null ? null : listeners.createBundledMappingApplicationListener();
		MappingResults results = new MappingResults(mappings, listener);
		if (aggregateMappingManager != null)
			results.withAggregateManager(aggregateMappingManager);

		// Apply mappings to the provided classes, collecting into the results model.
		Mappings finalMappings = mappings;
		ExecutorService service = ThreadUtil.phasingService(applierThreadPool);
		for (JvmClassInfo classInfo : classes)
			service.execute(() -> dumpIntoResults(results, workspace, resource, bundle, classInfo, finalMappings));
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
		mappings = enrich(mappings);
		MappingApplicationListener listener = listeners == null ? null : listeners.createBundledMappingApplicationListener();
		MappingResults results = new MappingResults(mappings, listener);
		if (aggregateMappingManager != null)
			results.withAggregateManager(aggregateMappingManager);

		// Apply mappings to all classes in the primary resource, collecting into the results model.
		Mappings finalMappings = mappings;
		ExecutorService service = ThreadUtil.phasingService(applierThreadPool);
		WorkspaceResource resource = workspace.getPrimaryResource();
		Stream.concat(resource.jvmClassBundleStream(), resource.versionedJvmClassBundleStream()).forEach(bundle -> {
			bundle.forEach(classInfo -> {
				service.execute(() -> dumpIntoResults(results, workspace, resource, bundle, classInfo, finalMappings));
			});
		});
		ThreadUtil.blockUntilComplete(service);

		// Yield results
		return results;
	}

	@Nonnull
	private Mappings enrich(@Nonnull Mappings mappings) {
		// Map intermediate mappings to the adapter so that we can pass in the inheritance graph for better coverage
		// of cases inherited field/method references.
		if (mappings instanceof IntermediateMappings intermediateMappings) {
			// Mapping formats that export to intermediate should mark whether they support
			// differentiation of field and variable types.
			boolean fieldDifferentiation = mappings.doesSupportFieldTypeDifferentiation();
			boolean varDifferentiation = mappings.doesSupportVariableTypeDifferentiation();
			MappingsAdapter adapter = new MappingsAdapter(fieldDifferentiation, varDifferentiation);
			adapter.importIntermediate(intermediateMappings);
			mappings = adapter;
		}

		// Check if mappings can be enriched with type look-ups
		if (mappings instanceof MappingsAdapter adapter) {
			// If we have "Dog extends Animal" and both define "jump" this lets "Dog.jump()" see "Animal.jump()"
			// allowing mappings that aren't complete for their type hierarchies to be filled in.
			adapter.enableHierarchyLookup(inheritanceGraph);
		}

		return mappings;
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
		ClassVisitor cv = classInfo.hasValidSignatures() ? remapVisitor : new IllegalSignatureRemovingVisitor(remapVisitor); // Because ASM crashes otherwise.
		cr.accept(cv, 0);

		// Update class if it has any modified references
		if (remapVisitor.hasMappingBeenApplied()) {
			JvmClassInfo updatedInfo = classInfo.toJvmClassBuilder()
					.adaptFrom(cw.toByteArray())
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
}
