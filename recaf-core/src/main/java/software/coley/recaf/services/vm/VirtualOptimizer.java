package software.coley.recaf.services.vm;

import dev.xdark.ssvm.invoke.Argument;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import software.coley.recaf.cdi.WorkspaceScoped;
import software.coley.recaf.services.Service;
import software.coley.recaf.workspace.model.Workspace;

/**
 * Service for optimizing methods in the current {@link Workspace} via SSVM.
 * VM initialization and configuration is handled by {@link CommonVirtualService}.
 *
 * @author Matt Coley
 * @see ArgumentBuilder Helper for creating {@link Argument} arrays.
 */
@WorkspaceScoped
public class VirtualOptimizer implements Service {
	public static final String SERVICE_ID = "virtual-optimizer";
	private final CommonVirtualService virtualService;
	private final VirtualOptimizerConfig config;

	@Inject
	public VirtualOptimizer(@Nonnull CommonVirtualService virtualService, @Nonnull VirtualOptimizerConfig config) {
		this.virtualService = virtualService;
		this.config = config;

		// We can't rely on VM state to know what can be optimized
		//  - Need to make a property per-class tracking which methods are inlinable
		//  - To be inlinable:
		//    - Call only whitelisted methods (JDK methods, so 'String.length' and 'new Random(int)' but not 'new Random())
		//    - Call only other inlinable methods

		// We probably don't want to always edit the ClassNode in our optimizer handlers
		//  - Find some way to track a mirrored copy and optimize that, then return that once execution completes
	}

	@Nonnull
	@Override
	public String getServiceId() {
		return SERVICE_ID;
	}

	@Nonnull
	@Override
	public VirtualOptimizerConfig getServiceConfig() {
		return config;
	}
}