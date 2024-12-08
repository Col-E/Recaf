package software.coley.recaf.services.workspace;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import software.coley.collections.Unchecked;
import software.coley.recaf.Bootstrap;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.cdi.EagerInitialization;
import software.coley.recaf.services.Service;
import software.coley.recaf.workspace.model.Workspace;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Applies all discovered {@link WorkspaceProcessor} instances to {@link Workspace} instances upon loading them via
 * {@link WorkspaceManager#setCurrent(Workspace)}.
 *
 * @author Matt Coley
 * @see WorkspaceProcessor Processor type to implement.
 */
@EagerInitialization
@ApplicationScoped
public class WorkspaceProcessingService implements Service {
	public static final String SERVICE_ID = "workspace-processing";
	private static final Logger logger = Logging.get(WorkspaceProcessingService.class);
	private final Map<Class<? extends WorkspaceProcessor>, Supplier<WorkspaceProcessor>> processorSuppliers = new IdentityHashMap<>();
	private final WorkspaceProcessingConfig config;

	/**
	 * @param workspaceManager
	 * 		Manager to facilitate listening to new opened workspaces.
	 * @param config
	 * 		Service config.
	 * @param processors
	 * 		Discovered processors to apply.
	 */
	@Inject
	public WorkspaceProcessingService(@Nonnull WorkspaceManager workspaceManager,
	                                  @Nonnull WorkspaceProcessingConfig config,
	                                  @Nonnull Instance<WorkspaceProcessor> processors) {
		this.config = config;
		for (Instance.Handle<WorkspaceProcessor> handle : processors.handles()) {
			Bean<WorkspaceProcessor> bean = handle.getBean();
			Class<? extends WorkspaceProcessor> processorClass = Unchecked.cast(bean.getBeanClass());
			processorSuppliers.put(processorClass, () -> {
				// Even though our processors may be @Dependent scoped, we need to do a new lookup each time we want
				// a new instance to get our desired scope behavior. If we re-use the instance handle that is injected
				// here then even @Dependent scoped beans will yield the same instance again and again.
				return Bootstrap.get().get(processorClass);
			});
		}

		// Apply processors when new workspace is opened
		workspaceManager.addWorkspaceOpenListener(this::processWorkspace);
	}

	/**
	 * @param processorClass
	 * 		Class of processor to register.
	 * @param processorSupplier
	 * 		Supplier of processor instances.
	 * @param <T>
	 * 		Processor type.
	 */
	public <T extends WorkspaceProcessor> void register(@Nonnull Class<T> processorClass, @Nonnull Supplier<T> processorSupplier) {
		processorSuppliers.put(Unchecked.cast(processorClass), Unchecked.cast(processorSupplier));
	}

	/**
	 * @param processorClass
	 * 		Class of processor to unregister.
	 * @param <T>
	 * 		Processor type.
	 */
	public <T extends WorkspaceProcessor> void unregister(@Nonnull Class<T> processorClass) {
		processorSuppliers.remove(Unchecked.cast(processorClass));
	}

	/**
	 * Applies all processors to the given workspace.
	 *
	 * @param workspace
	 * 		Workspace to process.
	 */
	public void processWorkspace(@Nonnull Workspace workspace) {
		for (Supplier<WorkspaceProcessor> processorSupplier : processorSuppliers.values()) {
			WorkspaceProcessor processor = processorSupplier.get();

			logger.trace("Applying workspace processor: {}", processor.name());
			processor.processWorkspace(workspace);
		}
	}

	@Nonnull
	@Override
	public String getServiceId() {
		return SERVICE_ID;
	}

	@Nonnull
	@Override
	public WorkspaceProcessingConfig getServiceConfig() {
		return config;
	}
}
