package software.coley.recaf.services.cell.builtin;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.path.AssemblerPathData;
import software.coley.recaf.services.cell.AssemblerContextMenuProviderFactory;
import software.coley.recaf.services.cell.ContextMenuProvider;
import software.coley.recaf.services.cell.ContextSource;
import software.coley.recaf.services.cell.IconProviderService;
import software.coley.recaf.services.cell.TextProviderService;
import software.coley.recaf.services.navigation.Actions;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.ClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * Basic implementation for {@link AssemblerContextMenuProviderFactory}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class BasicAssemblerContextMenuProviderFactory extends AbstractContextMenuProviderFactory implements AssemblerContextMenuProviderFactory {
	private static final Logger logger = Logging.get(BasicAnnotationContextMenuProviderFactory.class);

	@Inject
	public BasicAssemblerContextMenuProviderFactory(@Nonnull TextProviderService textService,
													@Nonnull IconProviderService iconService,
													@Nonnull Actions actions) {
		super(textService, iconService, actions);
	}

	@Nonnull
	@Override
	public ContextMenuProvider getProvider(@Nonnull ContextSource source,
										   @Nonnull Workspace workspace,
										   @Nonnull WorkspaceResource resource,
										   @Nonnull ClassBundle<? extends ClassInfo> bundle,
										   @Nonnull ClassInfo declaringClass,
										   @Nonnull AssemblerPathData assemblerData) {
		logger.info("{}", assemblerData);
		// TODO: Properly implement for different resolution impls
		return AssemblerContextMenuProviderFactory.super.getProvider(source, workspace, resource, bundle, declaringClass, assemblerData);
	}
}
