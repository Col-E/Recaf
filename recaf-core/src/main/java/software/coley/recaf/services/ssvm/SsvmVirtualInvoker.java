package software.coley.recaf.services.ssvm;

import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import software.coley.recaf.cdi.WorkspaceScoped;
import software.coley.recaf.services.ServiceConfig;

@WorkspaceScoped
public class SsvmVirtualInvoker implements VirtualInvoker {
	@Inject
	public SsvmVirtualInvoker(@Nonnull CommonVirtualService virtualService) {
		// TODO: https://github.com/xxDark/SSVM/pull/27
		//  - Figure out if we shallow copy the shared VM or make a new VM for just this service
	}

	@Nonnull
	@Override
	public String getServiceId() {
		return SERVICE_ID;
	}

	@Nonnull
	@Override
	public ServiceConfig getServiceConfig() {
		return null;
	}
}
