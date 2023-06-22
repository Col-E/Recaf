package software.coley.recaf.services.cell.builtin;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.services.cell.IconProvider;
import software.coley.recaf.services.cell.PackageIconProviderFactory;
import software.coley.recaf.util.Icons;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.Bundle;
import software.coley.recaf.workspace.model.bundle.ClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

/**
 * Basic implementation for {@link PackageIconProviderFactory}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class BasicPackageIconProviderFactory implements PackageIconProviderFactory {
	private static final IconProvider PROVIDER = Icons.createProvider(Icons.FOLDER_PACKAGE);

	@Nonnull
	@Override
	public IconProvider getPackageIconProvider(@Nonnull Workspace workspace,
											   @Nonnull WorkspaceResource resource,
											   @Nonnull ClassBundle<? extends ClassInfo> bundle,
											   @Nonnull String packageName) {
		return PROVIDER;
	}
}
